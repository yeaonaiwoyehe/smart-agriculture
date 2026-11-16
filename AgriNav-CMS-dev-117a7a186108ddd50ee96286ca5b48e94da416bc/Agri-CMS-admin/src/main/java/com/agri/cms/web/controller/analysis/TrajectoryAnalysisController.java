package com.agri.cms.web.controller.analysis;


import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.system.domain.dto.TrajectoryAnalysisDTO;
import com.agri.cms.system.domain.dto.TrajectoryQualityDTO;
import com.agri.cms.system.service.ITrajectoryAnalysisService;
import jakarta.annotation.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * @author 李鸿麟
 * 轨迹数据分析Controller
 */

@RestController
@RequestMapping("/analysis/trajectory")
public class TrajectoryAnalysisController {

    @Resource
    private ITrajectoryAnalysisService trajectoryAnalysisService;

    /**
     * 获取指定地块的轨迹质量分析
     */
    @GetMapping("/quality/{plotId}")
    public AjaxResult getTrajectoryQuality(@PathVariable Long plotId) {
            TrajectoryQualityDTO qualityDTO = trajectoryAnalysisService.analyzeTrajectoryQuality(plotId);
            return AjaxResult.success(qualityDTO);
    }

    /**
     * 获取时间范围内的轨迹分析
     */
    @GetMapping("/time-range")
    public AjaxResult getTrajectoryAnalysisByTimeRange(
            @RequestParam  @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {

            TrajectoryAnalysisDTO analysisDTO = trajectoryAnalysisService
                    .analyzeTrajectoryByTimeRange(startTime, endTime);

            return AjaxResult.success(analysisDTO);
    }

    /**
     * 获取一个月内的轨迹分析统计概览
     */
    @GetMapping("/overview")
    public AjaxResult getTrajectoryAnalysisOverview() {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusDays(30);
        return AjaxResult.success(trajectoryAnalysisService.analyzeTrajectoryByTimeRange(startTime, endTime));
    }

    /**
     * 获取指定地块的无效轨迹列表
     */
//    @GetMapping("/invalid-trajectories/{plotId}")
//    public AjaxResult getInvalidTrajectories(
//            @PathVariable Long plotId,
//            @RequestParam(defaultValue = "1") Integer pageNum,
//            @RequestParam(defaultValue = "10") Integer pageSize) {
//        try {
//            startPage(pageNum, pageSize);
//            // 这里需要在Service中添加相应的方法
//            return AjaxResult.success(getDataTable(trajectoryAnalysisService.getInvalidTrajectories(plotId)));
//        } catch (Exception e) {
//            return AjaxResult.error("获取无效轨迹列表失败: " + e.getMessage());
//        }
//    }
}
