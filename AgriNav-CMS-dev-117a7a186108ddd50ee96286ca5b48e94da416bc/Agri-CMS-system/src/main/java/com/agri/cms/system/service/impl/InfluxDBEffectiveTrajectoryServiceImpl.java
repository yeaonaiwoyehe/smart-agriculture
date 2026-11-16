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
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.influxdb.query.FluxTable;
import org.postgis.LinearRing;
import org.postgis.Polygon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private CheckPathServiceImpl checkPathService;
    @Autowired
    private TrajectoryExcludeService trajectoryExcludeService;
    @Autowired
    private SysUserServiceImpl userService;
    private final String DEFAULT_TRAJECTORY_IMG_URL = "http://140.143.189.217:9000/bkt-map/default.jpg";
    private final Integer TWO_POINTS = 2;
    @Value("${spring.influxdb.startTime}")
    private String startTime;
    private final Double MINI_DISTENCE = 0.000000001;
    private final Double SQUARE_METER_TO_MU = 666.67;
    private final int DELETE = 2;

    @Override
    public List<Trajectory> queryByPhoneNumber(String phoneNumber, LocalDateTime startTime, LocalDateTime endTime) {
        List<FluxTable> result = effectiveTrajectoryDao.queryByPhoneNumber(phoneNumber, startTime, endTime);
        Map<String, Trajectory> trajectoryMap = new HashMap<>();

        result.forEach(table -> {
            table.getRecords().forEach(record -> {
                //获取轨迹信息
                String plotId = Objects.requireNonNull(record.getValueByKey("plotId")).toString();
                double latitude = Double.parseDouble(Objects.requireNonNull(record.getValueByKey("latitude")).toString());
                double longitude = Double.parseDouble(Objects.requireNonNull(record.getValueByKey("longitude")).toString());
                LocalDateTime deviceTime = LocalDateTime.parse(Objects.requireNonNull(record.getValueByKey("deviceTime")).toString());
                int width = Integer.parseInt(Objects.requireNonNull(record.getValueByKey("width")).toString());
                Integer workTypeId = Integer.parseInt(Objects.requireNonNull(record.getValueByKey("workType")).toString());

                //每个地块对应一个轨迹
                Trajectory trajectory = trajectoryMap.computeIfAbsent(plotId, k -> new Trajectory());
                trajectory.setPlotId(Long.parseLong(plotId));
                trajectory.setWidth(width);
                trajectory.setWorkType(String.valueOf(workTypeId));
                trajectory.setPhoneNumber(phoneNumber);
                Point point = new Point();
                point.setLatitude(latitude);
                point.setLongitude(longitude);
                point.setTime(deviceTime);

                if (trajectory.getPoints() == null) {
                    trajectory.setPoints(new ArrayList<>());
                }
                trajectory.getPoints().add(point);
                for (Trajectory trajectory1 : trajectoryMap.values()) {
                    List<Point> points = trajectory1.getPoints();
                    //根据时间排序
                    points.sort(Comparator.comparing(Point::getTime));
                    //设置轨迹的开始时间和结束时间
                    if (!points.isEmpty()) {
                        trajectory.setStartTime(startTime);
                        trajectory.setEndTime(endTime);
                    }
                }
            });
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
        List<Trajectory> trajectories = queryByPhoneNumber(phoneNumber, startTime, endTime);
        //通过phoneNumber查询userId
        String userId = userService.selectUserByUserName(phoneNumber).getUserId().toString();
        //移除小于2个点的轨迹
        trajectories.removeIf(trajectory -> trajectory.getPoints().size() < TWO_POINTS);
        trajectories.forEach(trajectory -> {
            UUID uuid = UUID.randomUUID();
            try {
                    //拿到地块边界点集
                    Polygon boundary = landPlotService.getById(trajectory.getPlotId()).getBoundary();
                    //拿到所属地块，判断所属地块的类型
                    LandPlot landPlot = landPlotService.getById(trajectory.getPlotId());
                    List<Point> boundaryPoints = PolygonUtil.polygonToListPoint(boundary);
                    if (landPlot.getLandType().equals(LandPlotConstants.LAND_FOR_ARRANGING_FLOWERS)){
                        //走插花地的逻辑
                        //查询出所有大地块中的小地块
                        LambdaQueryWrapper<LandPlotExclude> wrapper = new LambdaQueryWrapper<>();
                        wrapper.eq(LandPlotExclude::getLandPlotId, trajectory.getPlotId()).
                                eq(LandPlotExclude::getIsDeleted, Constants.NOT_DELETED);
                        List<LandPlotExclude> landPlotExcludes = landPlotExcludeService.list(wrapper);
                        //拿到小地块中的点集
                        Map<Long, List<Point>> longListMap = assignPointsToPlots(trajectory.getPoints(), landPlotExcludes);
                        //插入到pg数据库
                        trajectoryExcludeService.saveTrajectoryExclude(longListMap,uuid);
                        //生成缩略图
                        String thumbnailUrl = gaodeMapService.generateThumbnail(trajectory.getPoints(), boundaryPoints);
                        trajectory.setTrajectoryImg(thumbnailUrl);
                        //计算面积
                        BigDecimal sumArea = areaCalculationService.calculateTrajectoryAreaAndDuplicateRemoval(trajectory.getPoints(),trajectory.getWidth());
                        //拿到Map中的List<point>，封装成list<list<point>>
                        List<List<Point>> listPoints = longListMap.values().stream().toList();
                        BigDecimal needToSubtractArea = listPoints.stream().
                                                        map(points -> areaCalculationService.calculateTrajectoryAreaAndDuplicateRemoval(points, trajectory.getWidth())).
                                                        reduce(BigDecimal.ZERO, BigDecimal::add);
                        BigDecimal areaInMu = sumArea.subtract(needToSubtractArea).divide(BigDecimal.valueOf(SQUARE_METER_TO_MU), 4, RoundingMode.HALF_UP);
                        trajectory.setArea(areaInMu);

                    } else {
                        //生成缩略图
                        String thumbnailUrl = gaodeMapService.generateThumbnail(trajectory.getPoints(), boundaryPoints);
                        trajectory.setTrajectoryImg(thumbnailUrl);
                        //计算面积
                        BigDecimal areaInSquareMeters = areaCalculationService.calculateTrajectoryAreaAndDuplicateRemoval(trajectory.getPoints(),trajectory.getWidth());
                        //换算单位为亩
                        BigDecimal areaInMu = areaInSquareMeters.divide(BigDecimal.valueOf(SQUARE_METER_TO_MU), 4, RoundingMode.HALF_UP);
                        trajectory.setArea(areaInMu);
                    }
            } catch (Exception e) {
                //设置默认缩略图
                trajectory.setArea(BigDecimal.ZERO);
                trajectory.setTrajectoryImg(DEFAULT_TRAJECTORY_IMG_URL);
            }

            //如果两个点，在第二点经纬度相差很小的位置再生成一个点，保证能够进行插入
            if (trajectory.getPoints().size() == TWO_POINTS) {
                Point point = new Point();
                point.setLongitude(trajectory.getPoints().get(1).getLongitude() + MINI_DISTENCE);
                point.setLatitude(trajectory.getPoints().get(1).getLatitude() + MINI_DISTENCE);
                point.setTime(trajectory.getPoints().get(1).getTime());
                trajectory.getPoints().add(point);
            }
            trajectoryPointsService.savePoints(trajectory, uuid);
            trajectoryInfoService.saveInfo(trajectory, uuid, userId);
        });
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
        //清空该用户influxDB的所有点数据
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        LocalDateTime parseStartTime = LocalDateTime.parse(startTime, formatter);
        deleteByPhoneNumber(username, parseStartTime, LocalDateTime.now());
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
    if (CollectionUtil.isEmpty(plots) || CollectionUtil.isEmpty(points)) {
        return new HashMap<>();
    }

    return points.stream()
        .flatMap(point -> plots.stream()
            .filter(plot -> {
                // 获取地块边界点集
                List<double[]> list = checkPathService.parsePolygonDouble(
                    plot.getBoundary().toString()
                );

                // 转换点的坐标到相同精度
                double testy = point.getLongitude() * 1000000.0;
                double testx = point.getLatitude() * 1000000.0;

                // 使用rayMethod判断点是否在多边形内
                return checkPathService.rayMethod(list, testx, testy) == 1;
            })
            .map(plot -> new AbstractMap.SimpleEntry<>(plot.getId(), point))
        )
        .collect(Collectors.groupingBy(
            Map.Entry::getKey,
            Collectors.mapping(
                Map.Entry::getValue,
                Collectors.toList()
            )
        ));
    }


}
