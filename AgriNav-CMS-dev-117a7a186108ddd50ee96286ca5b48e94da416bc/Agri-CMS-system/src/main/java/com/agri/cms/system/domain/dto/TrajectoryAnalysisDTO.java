package com.agri.cms.system.domain.dto;


import java.time.LocalDateTime;
import java.util.Map;

/**
 * @author 李鸿麟
 * 轨迹分析DTO
 */
public class TrajectoryAnalysisDTO {
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Integer totalTrajectories;
        private Map<Long, TrajectoryQualityDTO> plotQualityMap;

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Integer getTotalTrajectories() {
        return totalTrajectories;
    }

    public void setTotalTrajectories(Integer totalTrajectories) {
        this.totalTrajectories = totalTrajectories;
    }

    public Map<Long, TrajectoryQualityDTO> getPlotQualityMap() {
        return plotQualityMap;
    }

    public void setPlotQualityMap(Map<Long, TrajectoryQualityDTO> plotQualityMap) {
        this.plotQualityMap = plotQualityMap;
    }
}
