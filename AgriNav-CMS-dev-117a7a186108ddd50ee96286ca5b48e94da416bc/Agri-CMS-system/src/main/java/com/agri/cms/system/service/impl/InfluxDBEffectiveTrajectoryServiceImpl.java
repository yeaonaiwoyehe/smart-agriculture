package com.agri.cms.system.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.agri.cms.common.constant.Constants;
import com.agri.cms.common.constant.LandPlotConstants;
import com.agri.cms.system.dao.EffectiveTrajectoryDao;
import com.agri.cms.system.domain.*;
import com.agri.cms.system.domain.vo.Point;
import com.agri.cms.system.domain.vo.Trajectory;
import com.agri.cms.system.service.*;
import com.agri.cms.system.utils.PolygonUtil;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.postgis.Polygon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 姚锦伦
 */


@Service
public class InfluxDBEffectiveTrajectoryServiceImpl implements InfluxDBEffectiveTrajectoryService {

    @Autowired
    private EffectiveTrajectoryDao effectiveTrajectoryDao;
    @Autowired
    private GaodeMapService gaodeMapService;
    @Autowired
    private LandPlotService landPlotService;
    @Autowired
    private TrajectoryInfoService trajectoryInfoService;
    @Autowired
    private TrajectoryPointsService trajectoryPointsService;
    @Autowired
    private AreaCalculationServiceImpl areaCalculationService;
    @Autowired
    private LandPlotExcludeServiceImpl landPlotExcludeService;
    @Autowired
    private TrajectoryExcludeService trajectoryExcludeService;
    @Autowired
    private SysUserServiceImpl userService;
    private final String DEFAULT_TRAJECTORY_IMG_URL = "http://140.143.189.217:9000/bkt-map/default.jpg";
    private final Integer TWO_POINTS = 2;
    /** 暂存点清理的历史起点（替代原 InfluxDB 固定 startTime） */
    private final LocalDateTime BUFFER_START = LocalDateTime.of(2024, 1, 1, 0, 0);
    private final Double MINI_DISTENCE = 0.000000001;
    private final int DELETE = 2;

    @Override
    public List<Trajectory> queryByPhoneNumber(String phoneNumber, LocalDateTime startTime, LocalDateTime endTime) {
        List<EffectivePointBuffer> rows = effectiveTrajectoryDao.queryByPhoneNumber(phoneNumber, startTime, endTime);
        Map<Long, Trajectory> trajectoryMap = new HashMap<>();

        rows.forEach(row -> {
            if (row.getWidth() == null || row.getWidth() <= 0 || row.getPlotId() == null
                    || row.getLongitude() == null || row.getLatitude() == null || row.getDeviceTime() == null) {
                throw new IllegalArgumentException("暂存轨迹缺少有效幅宽、地块、坐标或时间");
            }
            //每个地块对应一个轨迹
            Trajectory trajectory = trajectoryMap.computeIfAbsent(row.getPlotId(), k -> new Trajectory());
            if (trajectory.getPoints() != null && (trajectory.getWidth() != row.getWidth()
                    || !Objects.equals(trajectory.getWorkType(), String.valueOf(row.getWorkType())))) {
                throw new IllegalArgumentException("同一地块内幅宽或作业类型发生变化，请按作业批次核算");
            }
            trajectory.setPlotId(row.getPlotId());
            trajectory.setWidth(row.getWidth());
            trajectory.setWorkType(String.valueOf(row.getWorkType()));
            trajectory.setPhoneNumber(phoneNumber);
            Point point = new Point();
            point.setLatitude(row.getLatitude());
            point.setLongitude(row.getLongitude());
            point.setTime(row.getDeviceTime());

            if (trajectory.getPoints() == null) {
                trajectory.setPoints(new ArrayList<>());
            }
            trajectory.getPoints().add(point);
        });

        //按时间排序并设置起止时间
        trajectoryMap.values().forEach(trajectory -> {
            List<Point> points = trajectory.getPoints();
            if (points != null && !points.isEmpty()) {
                points.sort(Comparator.comparing(Point::getTime));
                trajectory.setStartTime(startTime);
                trajectory.setEndTime(endTime);
            }
        });

        return new ArrayList<>(trajectoryMap.values());
    }

    @Override
    public void deleteByPhoneNumber(String phoneNumber, LocalDateTime startTime, LocalDateTime endTime) {
        effectiveTrajectoryDao.deleteByPhoneNumber(phoneNumber, startTime, endTime);
    }

    @Override
    @Async("taskExecutor")
    @DSTransactional(rollbackFor = Exception.class)
    public void createTrajectory(String phoneNumber, LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null || startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("作业起止时间无效");
        }
        List<Trajectory> trajectories = queryByPhoneNumber(phoneNumber, startTime, endTime);
        if (trajectories.isEmpty()) {
            return;
        }
        var user = userService.selectUserByUserName(phoneNumber);
        if (user == null) {
            throw new IllegalArgumentException("机手不存在");
        }
        String userId = user.getUserId().toString();
        Map<Trajectory, LandPlot> plots = new IdentityHashMap<>();
        Map<Trajectory, Map<Long, List<Point>>> excludedPoints = new IdentityHashMap<>();
        // 先校验并计算整个批次；任何失败都向上传播，不生成零面积单，也不清理暂存点。
        for (Trajectory trajectory : trajectories) {
            if (trajectory.getPoints().size() < TWO_POINTS) {
                throw new IllegalStateException("轨迹点不足，保留暂存数据等待补齐");
            }
            LandPlot plot = landPlotService.getById(trajectory.getPlotId());
            if (plot == null || plot.getBoundary() == null) {
                throw new IllegalArgumentException("地块不存在或缺少边界");
            }
            plots.put(trajectory, plot);
            List<Polygon> exclusions = new ArrayList<>();
            if (LandPlotConstants.LAND_FOR_ARRANGING_FLOWERS.equals(plot.getLandType())) {
                LambdaQueryWrapper<LandPlotExclude> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(LandPlotExclude::getLandPlotId, trajectory.getPlotId())
                        .eq(LandPlotExclude::getIsDeleted, Constants.NOT_DELETED);
                List<LandPlotExclude> records = landPlotExcludeService.list(wrapper);
                records.forEach(record -> exclusions.add(record.getBoundary()));
                excludedPoints.put(trajectory, assignPointsToPlots(trajectory.getPoints(), records));
            }
            BigDecimal squareMetres = areaCalculationService.calculateClippedArea(
                    trajectory.getPoints(), trajectory.getWidth(), plot.getBoundary(), exclusions);
            // 1 平方米 = 0.0015 亩，避免用截断的 666.67 反除引入误差。
            trajectory.setArea(squareMetres.multiply(new BigDecimal("0.0015"))
                    .setScale(4, RoundingMode.HALF_UP));
        }
        for (Trajectory trajectory : trajectories) {
            UUID uuid = UUID.randomUUID();
            LandPlot plot = plots.get(trajectory);
            // 缩略图失败仅影响展示，不影响已经核验的面积。
            try {
                trajectory.setTrajectoryImg(gaodeMapService.generateThumbnail(
                        trajectory.getPoints(), PolygonUtil.polygonToListPoint(plot.getBoundary())));
            } catch (Exception thumbEx) {
                trajectory.setTrajectoryImg(DEFAULT_TRAJECTORY_IMG_URL);
            }
            if (excludedPoints.containsKey(trajectory)) {
                trajectoryExcludeService.saveTrajectoryExclude(excludedPoints.get(trajectory), uuid);
            }
            // 兼容现有以 Polygon 存储轨迹的格式；补点发生在面积计算之后。
            if (trajectory.getPoints().size() == TWO_POINTS) {
                Point point = new Point();
                point.setLongitude(trajectory.getPoints().get(1).getLongitude() + MINI_DISTENCE);
                point.setLatitude(trajectory.getPoints().get(1).getLatitude() + MINI_DISTENCE);
                point.setTime(trajectory.getPoints().get(1).getTime());
                trajectory.getPoints().add(point);
            }
            if (!Boolean.TRUE.equals(trajectoryPointsService.savePoints(trajectory, uuid))) {
                throw new IllegalStateException("轨迹保存失败，保留暂存数据");
            }
            trajectoryInfoService.saveInfo(trajectory, uuid, userId);
        }
        deleteByPhoneNumber(phoneNumber, startTime, endTime);
    }

    @Override
    @DSTransactional(rollbackFor = Exception.class)
    public void loginOut(String username) {
        String userId = userService.selectUserByUserName(username).getUserId().toString();
        LambdaUpdateWrapper<TrajectoryInfo> wrapper = new LambdaUpdateWrapper<>();
        wrapper.set(TrajectoryInfo::getStatus, DELETE);
        wrapper.eq(TrajectoryInfo::getCreatedBy, userId);
        trajectoryInfoService.update(wrapper);
        //清空该用户在暂存表中的所有点数据
        deleteByPhoneNumber(username, BUFFER_START, LocalDateTime.now());
    }

    /**
     * 判断插花地小地块内的点集
     * @param points
     * @param plots
     * @return Map<Long, List<Point>>
     * @author 姚锦伦
     * @since 2024/11/19
     */
    public Map<Long, List<Point>> assignPointsToPlots(List<Point> points, List<LandPlotExclude> plots) {
        Map<Long, List<Point>> result = new HashMap<>();
        if (CollectionUtil.isEmpty(plots) || CollectionUtil.isEmpty(points)) {
            return result;
        }
        org.locationtech.jts.geom.GeometryFactory factory = new org.locationtech.jts.geom.GeometryFactory();
        for (LandPlotExclude plot : plots) {
            try {
                org.locationtech.jts.geom.Geometry geometry = new org.locationtech.jts.io.WKTReader(factory)
                        .read(plot.getBoundary().toString().replaceFirst("^SRID=[0-9]+;", ""));
                List<Point> matches = points.stream().filter(point -> geometry.covers(factory.createPoint(
                        new org.locationtech.jts.geom.Coordinate(point.getLongitude(), point.getLatitude()))))
                        .collect(Collectors.toCollection(ArrayList::new));
                if (!matches.isEmpty()) {
                    result.put(plot.getId(), matches);
                }
            } catch (org.locationtech.jts.io.ParseException exception) {
                throw new IllegalArgumentException("排除区域边界无法解析", exception);
            }
        }
        return result;
    }
}