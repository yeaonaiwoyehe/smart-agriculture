package com.agri.cms.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.agri.cms.common.constant.Constants;
import com.agri.cms.common.exception.ServiceException;
import com.agri.cms.system.domain.*;
import com.agri.cms.system.domain.dto.TrajectoryAnalysisDTO;
import com.agri.cms.system.domain.dto.TrajectoryQualityDTO;
import com.agri.cms.system.mapper.*;
import com.agri.cms.system.service.ITrajectoryAnalysisService;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 李鸿麟
 * 轨迹分析服务实现类
 */
@Service
public class TrajectoryAnalysisServiceImpl extends ServiceImpl<TrajectoryAnalysisMapper, TrajectoryAnalysisDTO>
implements ITrajectoryAnalysisService {

    // 定义常量

    private static final double VALID_AREA_RATIO = 0.3;

    @Resource
    private LandPlotMapper landPlotMapper;

    @Resource
    private TrajectoryInfoMapper trajectoryInfoMapper;

    @Resource
    private FieldWorkMapper fieldWorkMapper;

    @Override
    public TrajectoryQualityDTO analyzeTrajectoryQuality(Long plotId) {
        if (StrUtil.isEmpty(plotId.toString())) {
            throw new ServiceException("地块ID为空");
        }

        LandPlot landPlot = landPlotMapper.selectById(plotId);
        if (landPlot == null) {
            throw new ServiceException("地块不存在");
        }

        // 查询该地块的所有轨迹
        List<TrajectoryInfo> trajectories = trajectoryInfoMapper.selectList(
                new LambdaQueryWrapper<TrajectoryInfo>()
                        .eq(TrajectoryInfo::getPlotId, plotId)
        );

        TrajectoryQualityDTO qualityDTO = new TrajectoryQualityDTO();
        qualityDTO.setPlotId(plotId);
        qualityDTO.setPlotArea(Double.parseDouble(landPlot.getLandArea().toString()));
        qualityDTO.setTotalTrajectories(trajectories.size());

        // 分析无效轨迹
        List<TrajectoryInfo> invalidTrajectories = trajectories.stream()
                .filter(trajectory -> isInvalidTrajectory(trajectory, landPlot))
                .toList();

        qualityDTO.setInvalidTrajectoryCount(invalidTrajectories.size());
        qualityDTO.setValidTrajectoryCount(trajectories.size() - invalidTrajectories.size());

        // 计算平均作业面积
        double avgArea = trajectories.stream()
                .mapToDouble(t -> t.getArea().doubleValue())
                .average()
                .orElse(0.0);
        qualityDTO.setAverageWorkArea(avgArea);

        // 获取所有工作类型ID
        Set<Long> workTypeIds = trajectories.stream()
                .map(trajectory -> Long.parseLong(trajectory.getWorkType()))
                .collect(Collectors.toSet());

        // 查询工作类型信息
        Map<String, String> workTypeMap = fieldWorkMapper.selectList(
                new LambdaQueryWrapper<FieldWork>()
                        .in(FieldWork::getId, workTypeIds)
                        .ne(FieldWork::getIsDeleted, Constants.IS_DELETED)
        ).stream().collect(Collectors.toMap(
                work -> work.getId().toString(),
                FieldWork::getWorkName
        ));

        // 按工作类型统计 - 使用名称替代ID
        Map<String, Long> workTypeStats = trajectories.stream()
                .collect(Collectors.groupingBy(
                        trajectory -> workTypeMap.getOrDefault(
                                trajectory.getWorkType(),
                                trajectory.getWorkType()
                        ),
                        Collectors.counting()
                ));
        qualityDTO.setWorkTypeDistribution(workTypeStats);

        // 计算数据质量得分 基于有效轨迹比例计算
        double qualityScore = calculateQualityScore(trajectories.size(), invalidTrajectories.size());
        qualityDTO.setQualityScore(qualityScore);

        return qualityDTO;
    }

    @Override
    @DS("pg")
    public TrajectoryAnalysisDTO analyzeTrajectoryByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        // 查询时间范围内的有效轨迹
        List<TrajectoryInfo> trajectories = trajectoryInfoMapper.selectList(
                new LambdaQueryWrapper<TrajectoryInfo>()
                        .between(TrajectoryInfo::getStartTime, startTime, endTime)
                        .eq(TrajectoryInfo::getStatus, Constants.NOT_DELETED)
        );

        TrajectoryAnalysisDTO analysisDTO = new TrajectoryAnalysisDTO();
        analysisDTO.setStartTime(startTime);
        analysisDTO.setEndTime(endTime);
        analysisDTO.setTotalTrajectories(trajectories.size());

        // 获取所有相关地块ID
        Set<Long> plotIds = trajectories.stream()
                .map(TrajectoryInfo::getPlotId)
                .collect(Collectors.toSet());

        // 获取所有工作类型ID
        Set<Long> workTypeIds = trajectories.stream()
                .map(trajectory -> Long.parseLong(trajectory.getWorkType()))
                .collect(Collectors.toSet());

        // 一次性查询所有未删除的相关地块
        Map<Long, LandPlot> landPlotMap = landPlotMapper.selectList(
                new LambdaQueryWrapper<LandPlot>()
                        .in(LandPlot::getId, plotIds)
                        .eq(LandPlot::getIsDeleted, Constants.NOT_DELETED)
        ).stream().collect(Collectors.toMap(
                LandPlot::getId,
                plot -> plot
        ));

        // 一次性查询所有工作类型
        Map<String, String> workTypeMap = fieldWorkMapper.selectList(
                new LambdaQueryWrapper<FieldWork>()
                        .in(FieldWork::getId, workTypeIds)
                        .ne(FieldWork::getIsDeleted, Constants.IS_DELETED)
        ).stream().collect(Collectors.toMap(
                work -> work.getId().toString(),
                FieldWork::getWorkName
        ));

        // 使用stream流处理地块轨迹分析
        Map<Long, TrajectoryQualityDTO> plotQualityMap = trajectories.stream()
                .collect(Collectors.groupingBy(TrajectoryInfo::getPlotId))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> analyzeTrajectoryQualityWithPlot(
                                entry.getKey(),
                                entry.getValue(),
                                landPlotMap.get(entry.getKey()),
                                workTypeMap  // 传入工作类型映射
                        )
                ));

        analysisDTO.setPlotQualityMap(plotQualityMap);
        return analysisDTO;
    }

    /**
     * 使用已查询的地块信息进行轨迹分析
     * @param plotId 地块ID
     * @param trajectories 轨迹列表
     * @param landPlot 地块信息
     * @return TrajectoryQualityDTO 轨迹质量分析结果
     */
    private TrajectoryQualityDTO analyzeTrajectoryQualityWithPlot(
            Long plotId,
            List<TrajectoryInfo> trajectories,
            LandPlot landPlot,
            Map<String, String> workTypeMap  // 添加工作类型映射参数
    ) {
        TrajectoryQualityDTO qualityDTO = new TrajectoryQualityDTO();
        qualityDTO.setPlotId(plotId);
        qualityDTO.setTotalTrajectories(trajectories.size());

        if (landPlot == null || landPlot.getIsDeleted() == 1) {
            // 地块不存在或已删除时的处理
            qualityDTO.setPlotArea(0.0);
            qualityDTO.setInvalidTrajectoryCount(trajectories.size());
            qualityDTO.setValidTrajectoryCount(0);
            qualityDTO.setQualityScore(0.0);
        } else {
            // 设置地块面积
            qualityDTO.setPlotArea(Double.parseDouble(landPlot.getLandArea().toString()));

            // 分析无效轨迹
            List<TrajectoryInfo> invalidTrajectories = trajectories.stream()
                    .filter(trajectory -> isInvalidTrajectory(trajectory, landPlot))
                    .toList();

            qualityDTO.setInvalidTrajectoryCount(invalidTrajectories.size());
            qualityDTO.setValidTrajectoryCount(trajectories.size() - invalidTrajectories.size());

            // 计算质量得分
            double qualityScore = calculateQualityScore(trajectories.size(), invalidTrajectories.size());
            qualityDTO.setQualityScore(qualityScore);
        }

        // 计算平均作业面积
        double avgArea = trajectories.stream()
                .mapToDouble(t -> t.getArea() != null ? t.getArea().doubleValue() : 0.0)
                .average()
                .orElse(0.0);
        qualityDTO.setAverageWorkArea(avgArea);

        // 按工作类型统计
        Map<String, Long> workTypeStats = trajectories.stream()
                .collect(Collectors.groupingBy(
                        trajectory -> workTypeMap.getOrDefault(
                                trajectory.getWorkType(),
                                trajectory.getWorkType()
                        ),
                        Collectors.counting()
                ));
        qualityDTO.setWorkTypeDistribution(workTypeStats);

        // 设置工作类型面积分布
        Map<String, Double> workTypeAreaStats = trajectories.stream()
                .filter(t -> t.getArea() != null && t.getWorkType() != null)
                .collect(Collectors.groupingBy(
                        trajectory -> workTypeMap.getOrDefault(
                                trajectory.getWorkType(),
                                trajectory.getWorkType()
                        ),
                        Collectors.averagingDouble(t -> t.getArea().doubleValue())
                ));
        qualityDTO.setWorkTypeAreaDistribution(workTypeAreaStats);

        // 设置最新轨迹时间
        Optional<TrajectoryInfo> latestTrajectory = trajectories.stream()
                .max(Comparator.comparing(TrajectoryInfo::getEndTime));
        latestTrajectory.ifPresent(trajectory ->
                qualityDTO.setLatestTrajectoryTime(trajectory.getEndTime()));

        return qualityDTO;
    }

    /**
     * 判断轨迹是否无效
     * @param trajectory 轨迹信息
     * @param landPlot 地块信息
     * @return boolean 是否无效
     */
    private boolean isInvalidTrajectory(TrajectoryInfo trajectory, LandPlot landPlot) {
        if (trajectory.getArea() == null || landPlot.getLandArea() == 0) {
            return true;
        }
        // 如果轨迹面积小于地块面积的30%，认为是无效轨迹
        return trajectory.getArea().doubleValue() < (landPlot.getLandArea() * VALID_AREA_RATIO);
    }

    /**
     * 计算数据质量得分
     * @param total 总轨迹数
     * @param invalid 无效轨迹数
     * @return double 质量得分
     */
    private double
    calculateQualityScore(int total, int invalid) {
        if (total == 0) {
            return 0.0;
        }
        return ((double)(total - invalid) / total) * 100;
    }
}
