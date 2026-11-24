package com.agri.cms.system.service.impl;

import com.agri.cms.common.constant.Constants;
import com.agri.cms.common.constant.LandPlotConstants;
import com.agri.cms.common.utils.StringUtils;
import com.agri.cms.system.domain.*;
import com.agri.cms.system.domain.vo.Point;
import com.agri.cms.system.domain.vo.SortMessage;
import com.agri.cms.system.domain.vo.Trajectory;
import com.agri.cms.system.mapper.*;
import com.agri.cms.system.service.TrajectoryExcludeService;
import com.agri.cms.system.service.TrajectoryPointsService;
import com.agri.cms.system.utils.LineStringUtils;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.postgis.Polygon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 *
 * @Author: 姚锦伦
 * @CreateTime: 2024-09-26
 * @Description:
 * @Version: 1.0
 */
@Service
public class TrajectoryPointsServiceImpl extends ServiceImpl<TrajectoryPointsMapper, TrajectoryPoints> implements TrajectoryPointsService {
    @Autowired
    private TrajectoryInfoMapper trajectoryInfoMapper;
    @Autowired
    private TrajectoryPointsMapper trajectoryPointsMapper;
    @Autowired
    private CropMapper cropMapper;
    @Autowired
    private CropWorkMapper cropWorkMapper;
    @Autowired
    private LandPlotMapper landPlotMapper;
    @Autowired
    private FieldWorkMapper fieldWorkMapper;
    @Autowired
    private TrajectoryExcludeService trajectoryExcludeService;
    @Autowired
    private SysUserServiceImpl userService;
    private static final int MIN_POINTS = 2;

    @Override
    @DS("pg")
    @Transactional(rollbackFor = Exception.class)
    public Boolean savePoints(Trajectory trajectory, UUID uuid) {

        List<TrajectoryPoints> trajectoryPointsList = new ArrayList<>();

        // 跳过空的轨迹对象
        if (trajectory == null) {
            return false;
        }

        TrajectoryPoints trajectoryPoints = new TrajectoryPoints();
        trajectoryPoints.setTrajectoryId(uuid.toString());

        List<Point> points = trajectory.getPoints();

        // 跳过没有点的轨迹
        if (points == null || points.isEmpty() || points.size() < MIN_POINTS) {
            return false;
        }

        // 创建POLYGON字符串
        StringBuilder geometryBuilder = new StringBuilder("POLYGON((");
        StringBuilder timesBuilder = new StringBuilder();

        for (int i = 0; i < points.size(); i++) {
            Point point = points.get(i);
            geometryBuilder.append(String.format(Locale.ROOT, "%.12f %.12f", point.getLongitude(), point.getLatitude()));
            timesBuilder.append(point.getTime());

            if (i < points.size() - 1) {
                geometryBuilder.append(",");
                timesBuilder.append(",");
            }
        }
        // 闭合多边形
        if (!points.isEmpty()) {
            Point firstPoint = points.get(0);
            geometryBuilder.append(String.format(Locale.ROOT, ",%.12f %.12f", firstPoint.getLongitude(), firstPoint.getLatitude()));
        }
        geometryBuilder.append("))");

        try {
            // 将字符串转换为 Polygon 对象
            Polygon polygon = new Polygon(geometryBuilder.toString());
            trajectoryPoints.setPoints(polygon);
            trajectoryPoints.setTimes(timesBuilder.toString());
            trajectoryPointsList.add(trajectoryPoints);
        } catch (SQLException e) {
            throw new IllegalStateException("轨迹几何保存失败: " + trajectory.getPlotId(), e);
        }

        // 批量保存到数据库
        return this.saveBatch(trajectoryPointsList);
    }

    @Override
    public Page<Trajectory> queryAll(String phoneNumber, Integer pageNum, Integer pageSize) {
        // 创建分页对象
        Page<TrajectoryInfo> page = new Page<>(pageNum, pageSize);

        // 创建查询条件
        LambdaQueryWrapper<TrajectoryInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TrajectoryInfo::getPhoneNumber, phoneNumber);
        // status 是作业状态：0进行中、1已完成、2已删除；历史空状态也可查看。
        wrapper.and(state -> state.isNull(TrajectoryInfo::getStatus).or().ne(TrajectoryInfo::getStatus, 2));
        //根据创建时间倒序排序
        wrapper.orderByDesc(TrajectoryInfo::getCreatedTime);

        // 执行分页查询
        IPage<TrajectoryInfo> infoPage = trajectoryInfoMapper.selectPage(page, wrapper);
        infoPage.getRecords().forEach(item -> {
            String userName = userService.selectUserById(Long.parseLong(item.getCreatedBy())).getUserName();
            item.setCreatedBy(userName);
        });

        // 创建结果分页对象
        Page<Trajectory> resultPage = new Page<>(infoPage.getCurrent(), infoPage.getSize(), infoPage.getTotal());
        List<Trajectory> records = new ArrayList<>();

        // 遍历 TrajectoryInfo 列表，为每个轨迹查询对应的点
        for (TrajectoryInfo info : infoPage.getRecords()) {
            Trajectory trajectory = new Trajectory();

            // 复制 TrajectoryInfo 的属性到 Trajectory
            Trajectory trajectoryChecked = checkNull(info);

            // 查询对应的 TrajectoryPoints
            LambdaQueryWrapper<TrajectoryPoints> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TrajectoryPoints::getTrajectoryId, info.getTrajectoryInfoId());
            TrajectoryPoints points = trajectoryPointsMapper.selectOne(queryWrapper);
            //判断轨迹对应的地块类型是否是插花地，如果是插花地，则添加上应该排除的数据
            LambdaQueryWrapper<LandPlot> plotQueryWrapper = new LambdaQueryWrapper<>();
            plotQueryWrapper.eq(LandPlot::getId, info.getPlotId()).
                    eq(LandPlot::getIsDeleted, Constants.NOT_DELETED);
            LandPlot landPlotForArrangingFlowers = landPlotMapper.selectOne(plotQueryWrapper);
            if (landPlotForArrangingFlowers != null && landPlotForArrangingFlowers.getLandType().equals(LandPlotConstants.LAND_FOR_ARRANGING_FLOWERS)) {
                List<TrajectoryExclude> processedTrajectoryExcludes = trajectoryExcludeService.queryTrajectaryExcludes(info);
                trajectoryChecked.setTrajectoryExclude(processedTrajectoryExcludes);
            }
            if (points != null) {
                // 解析点数据和时间数据
                List<Point> trajectoryPoints = parsePoints(points.getPoints(), points.getTimes());
                trajectoryChecked.setPoints(trajectoryPoints);
            }
            // 查询并设置location信息
            if (info.getPlotId() != null) {
                LandPlot landPlot = landPlotMapper.selectById(info.getPlotId());
                if (landPlot != null) {
                    trajectoryChecked.setLocation(landPlot.getAddress());
                }
            }
            //查询并设置工作类型信息和作物信息
            if (info.getWorkType() != null) {
                LambdaQueryWrapper<FieldWork> workWapper = new LambdaQueryWrapper<>();
                workWapper.eq(FieldWork::getId, Long.parseLong(info.getWorkType()));
                FieldWork fieldWork = fieldWorkMapper.selectOne(workWapper);
                if (fieldWork != null) {
                    trajectoryChecked.setWorkType(fieldWork.getWorkName());
                }
                //查询工作类型对应的作物id（一个农活可关联多种作物，使用 selectList）
                LambdaQueryWrapper<CropWork> cropWorkWapper = new LambdaQueryWrapper<>();
                cropWorkWapper.eq(CropWork::getWorkId, Long.parseLong(info.getWorkType()));
                List<CropWork> cropWorks = cropWorkMapper.selectList(cropWorkWapper);
                //查询并拼接所有作物名称
                if (cropWorks != null && !cropWorks.isEmpty()) {
                    List<String> cropNames = new ArrayList<>();
                    for (CropWork cw : cropWorks) {
                        Crop crop = cropMapper.selectById(cw.getCropId());
                        if (crop != null) {
                            cropNames.add(crop.getCropName());
                        }
                    }
                    if (!cropNames.isEmpty()) {
                        trajectoryChecked.setCrop(String.join("、", cropNames));
                    }
                }
            }

            records.add(trajectoryChecked);
        }

        resultPage.setRecords(records);
        return resultPage;
    }

    private List<Point> parsePoints(Polygon polygon, String times) {
        List<Point> points = new ArrayList<>();
        String[] timeArray = times.split(",");

        // 获取多边形的外环
        org.postgis.LinearRing ring = polygon.getRing(0);
        org.postgis.Point[] pgPoints = ring.getPoints();

        // 遍历点，跳过最后一个（它是第一个点的重复）
        for (int i = 0; i < pgPoints.length - 1; i++) {
            org.postgis.Point pgPoint = pgPoints[i];
            Point point = new Point();
            point.setLongitude(pgPoint.x);
            point.setLatitude(pgPoint.y);

            // 将时间字符串解析为 LocalDateTime
            LocalDateTime dateTime = LocalDateTime.parse(timeArray[i], DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            point.setTime(dateTime);

            points.add(point);
        }

        return points;
    }

    @Override
    public Page<Trajectory> queryByConditions(SortMessage sortMessage, String phoneNumber, Integer pageNum, Integer pageSize) {
        // 创建分页对象
        Page<TrajectoryInfo> page = new Page<>(pageNum, pageSize);

        // 创建查询条件
        LambdaQueryWrapper<TrajectoryInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TrajectoryInfo::getPhoneNumber, phoneNumber);
        wrapper.and(state -> state.isNull(TrajectoryInfo::getStatus).or().ne(TrajectoryInfo::getStatus, 2));
        wrapper.orderByDesc(TrajectoryInfo::getCreatedTime);

        // 根据sortMessage应用额外的条件
        wrapper.eq(sortMessage.getWorkType() != null && !sortMessage.getWorkType().isEmpty(), TrajectoryInfo::getWorkType, sortMessage.getWorkType());
        if (StringUtils.isNotEmpty(sortMessage.getLocalDate())) {
            LocalDate date = LocalDate.parse(sortMessage.getLocalDate());
            //如果没有传开始时间和结束时间，就使用当天的整天时间
            LocalTime beginTime = sortMessage.getBeginTime() != null ? sortMessage.getBeginTime() : LocalTime.MIN;
            LocalTime endTime = sortMessage.getEndTime() != null ? sortMessage.getEndTime() : LocalTime.MAX;
            LocalDateTime startDateTime = LocalDateTime.of(date, beginTime);
            LocalDateTime endDateTime = LocalDateTime.of(date, endTime);
            wrapper.ge(TrajectoryInfo::getStartTime, startDateTime)
                    .le(TrajectoryInfo::getEndTime, endDateTime);
        }

        //位置条件
        if (StringUtils.isNotEmpty(sortMessage.getAddress())) {
            LambdaQueryWrapper<LandPlot> wrapperAddress = new LambdaQueryWrapper<>();
            wrapperAddress.like(LandPlot::getAddress, sortMessage.getAddress());
            List<LandPlot> landPlots = landPlotMapper.selectList(wrapperAddress);

            List<Long> plotIds = landPlots.stream()
                    .map(LandPlot::getId)
                    .collect(Collectors.toList());
            if (!plotIds.isEmpty()) {
                wrapper.in(TrajectoryInfo::getPlotId, plotIds);
            } else {
                // 如果没有匹配的地块，返回空的分页结果
                return new Page<>(pageNum, pageSize);
            }
        }

        // 执行分页查询
        IPage<TrajectoryInfo> infoPage = trajectoryInfoMapper.selectPage(page, wrapper);
        infoPage.getRecords().forEach(item -> {
            String userName = userService.selectUserById(Long.parseLong(item.getCreatedBy())).getUserName();
            item.setCreatedBy(userName);
        });

        // 创建结果分页对象
        Page<Trajectory> resultPage = new Page<>(infoPage.getCurrent(), infoPage.getSize(), infoPage.getTotal());
        List<Trajectory> records = new ArrayList<>();

        // 遍历 TrajectoryInfo 列表，为每个轨迹查询对应的点
        for (TrajectoryInfo info : infoPage.getRecords()) {
            Trajectory trajectory = checkNull(info);
            //设置面积信息
            if (info.getArea() != null) {
                trajectory.setArea(info.getArea());
            }
            // 查询并设置location信息
            if (info.getPlotId() != null) {
                LandPlot landPlot = landPlotMapper.selectById(info.getPlotId());
                if (landPlot != null) {
                    trajectory.setLocation(landPlot.getAddress());
                }
            }
            //查询并设置工作类型信息
            if (info.getWorkType() != null) {
                LambdaQueryWrapper<FieldWork> workWapper = new LambdaQueryWrapper<>();
                workWapper.eq(FieldWork::getId, Long.parseLong(info.getWorkType()));
                FieldWork fieldWork = fieldWorkMapper.selectOne(workWapper);
                if (fieldWork != null) {
                    trajectory.setWorkType(fieldWork.getWorkName());
                }
                //查询工作类型对应的作物id（一个农活可关联多种作物，使用 selectList）
                LambdaQueryWrapper<CropWork> cropWorkWapper = new LambdaQueryWrapper<>();
                cropWorkWapper.eq(CropWork::getWorkId, Long.parseLong(info.getWorkType()));
                List<CropWork> cropWorks = cropWorkMapper.selectList(cropWorkWapper);
                //查询并拼接所有作物名称
                if (cropWorks != null && !cropWorks.isEmpty()) {
                    List<String> cropNames = new ArrayList<>();
                    for (CropWork cw : cropWorks) {
                        Crop crop = cropMapper.selectById(cw.getCropId());
                        if (crop != null) {
                            cropNames.add(crop.getCropName());
                        }
                    }
                    if (!cropNames.isEmpty()) {
                        trajectory.setCrop(String.join("、", cropNames));
                    }
                }
            }

            // 查询对应的 TrajectoryPoints
            LambdaQueryWrapper<TrajectoryPoints> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TrajectoryPoints::getTrajectoryId, info.getTrajectoryInfoId());
            TrajectoryPoints points = trajectoryPointsMapper.selectOne(queryWrapper);
            //判断轨迹对应的地块类型是否是插花地，如果是插花地，则添加上应该排除的数据
            LambdaQueryWrapper<LandPlot> plotQueryWrapper = new LambdaQueryWrapper<>();
            plotQueryWrapper.eq(LandPlot::getId, info.getPlotId()).
                    eq(LandPlot::getIsDeleted, Constants.NOT_DELETED);
            LandPlot landPlotForArrangingFlowers = landPlotMapper.selectOne(plotQueryWrapper);
            if (landPlotForArrangingFlowers != null && landPlotForArrangingFlowers.getLandType().equals(LandPlotConstants.LAND_FOR_ARRANGING_FLOWERS)) {
                List<TrajectoryExclude> processedTrajectoryExcludes = trajectoryExcludeService.queryTrajectaryExcludes(info);
                trajectory.setTrajectoryExclude(processedTrajectoryExcludes);
            }
            if (points != null) {
                List<Point> trajectoryPoints = parsePoints(points.getPoints(), points.getTimes());
                trajectory.setPoints(trajectoryPoints);
            }
            records.add(trajectory);
        }

        resultPage.setRecords(records);
        return resultPage;
    }

    Trajectory checkNull(TrajectoryInfo info) {
        Trajectory trajectory = new Trajectory();
        if (info.getPlotId() != null) {
            trajectory.setPlotId(info.getPlotId());
        } else {
            trajectory.setPlotId(-1);
        }
        if (info.getTrajectoryInfoId() != null) {
            trajectory.setTrajectoryInfoId(info.getTrajectoryInfoId());
        }
        if (info.getWorkType() != null) {
            trajectory.setWorkType(info.getWorkType());
        }
        if (info.getStartTime() != null) {
            trajectory.setStartTime(info.getStartTime());
        }
        if (info.getEndTime() != null) {
            trajectory.setEndTime(info.getEndTime());
        }
        if (info.getTrajectoryImg() != null) {
            trajectory.setTrajectoryImg(info.getTrajectoryImg());
        }
        if (info.getWidth() != null) {
            trajectory.setWidth(info.getWidth());
        } else {
            trajectory.setWidth(-1);
        }
        if (info.getPhoneNumber() != null) {
            trajectory.setPhoneNumber(info.getPhoneNumber());
        }
        if (info.getArea() != null) {
            trajectory.setArea(info.getArea());
        }
        return trajectory;
    }
}
