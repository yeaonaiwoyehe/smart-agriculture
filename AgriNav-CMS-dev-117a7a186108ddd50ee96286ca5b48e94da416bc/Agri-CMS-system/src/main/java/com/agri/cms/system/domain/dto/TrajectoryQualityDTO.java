package com.agri.cms.system.domain.dto;


import java.time.LocalDateTime;
import java.util.Map;

/**
 * @author 李鸿麟
 * 轨迹质量DTO
 */
public class TrajectoryQualityDTO {
    private Long plotId;                           // 地块ID
    private Double plotArea;                       // 地块面积
    private Integer totalTrajectories;             // 轨迹总数
    private Integer validTrajectoryCount;          // 有效轨迹数
    private Integer invalidTrajectoryCount;        // 无效轨迹数
    private Double averageWorkArea;                // 平均作业面积
    private Map<String, Long> workTypeDistribution;// 工作类型分布
    private Map<String, Double> workTypeAreaDistribution; // 各工作类型平均作业面积
    private Double qualityScore;                   // 数据质量得分
    private LocalDateTime latestTrajectoryTime;    // 最新轨迹时间

    public Map<String, Double> getWorkTypeAreaDistribution() {
        return workTypeAreaDistribution;
    }

    public void setWorkTypeAreaDistribution(Map<String, Double> workTypeAreaDistribution) {
        this.workTypeAreaDistribution = workTypeAreaDistribution;
    }

    public LocalDateTime getLatestTrajectoryTime() {
        return latestTrajectoryTime;
    }

    public void setLatestTrajectoryTime(LocalDateTime latestTrajectoryTime) {
        this.latestTrajectoryTime = latestTrajectoryTime;
    }

    public Long getPlotId() {
        return plotId;
    }

    public void setPlotId(Long plotId) {
        this.plotId = plotId;
    }

    public Double getPlotArea() {
        return plotArea;
    }

    public void setPlotArea(Double plotArea) {
        this.plotArea = plotArea;
    }

    public Integer getTotalTrajectories() {
        return totalTrajectories;
    }

    public void setTotalTrajectories(Integer totalTrajectories) {
        this.totalTrajectories = totalTrajectories;
    }

    public Integer getValidTrajectoryCount() {
        return validTrajectoryCount;
    }

    public void setValidTrajectoryCount(Integer validTrajectoryCount) {
        this.validTrajectoryCount = validTrajectoryCount;
    }

    public Integer getInvalidTrajectoryCount() {
        return invalidTrajectoryCount;
    }

    public void setInvalidTrajectoryCount(Integer invalidTrajectoryCount) {
        this.invalidTrajectoryCount = invalidTrajectoryCount;
    }

    public Double getAverageWorkArea() {
        return averageWorkArea;
    }

    public void setAverageWorkArea(Double averageWorkArea) {
        this.averageWorkArea = averageWorkArea;
    }

    public Map<String, Long> getWorkTypeDistribution() {
        return workTypeDistribution;
    }

    public void setWorkTypeDistribution(Map<String, Long> workTypeDistribution) {
        this.workTypeDistribution = workTypeDistribution;
    }

    public Double getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(Double qualityScore) {
        this.qualityScore = qualityScore;
    }
}
