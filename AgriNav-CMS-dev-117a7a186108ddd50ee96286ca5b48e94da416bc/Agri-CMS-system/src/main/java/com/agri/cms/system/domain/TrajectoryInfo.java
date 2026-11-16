package com.agri.cms.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @author 姚锦伦
 */
@TableName("trajectory_info") // 映射到数据库表名
public class TrajectoryInfo {
    @TableId(value = "trajectory_info_id")
    @NotBlank
    private String trajectoryInfoId;
    @NotBlank
    private String workType;
    private String phoneNumber;
    private Integer width;
    private String createdBy;
    private LocalDateTime createdTime;
    private Short status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String trajectoryImg;
    private Long plotId;
    private BigDecimal area;

    public String getTrajectoryInfoId() {
        return trajectoryInfoId;
    }

    public void setTrajectoryInfoId(String trajectoryInfoId) {
        this.trajectoryInfoId = trajectoryInfoId;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public Short getStatus() {
        return status;
    }

    public void setStatus(Short status) {
        this.status = status;
    }

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


    public Long getPlotId() {
        return plotId;
    }

    public void setPlotId(Long plotId) {
        this.plotId = plotId;
    }

    public String getTrajectoryImg() {
        return trajectoryImg;
    }

    public void setTrajectoryImg(String trajectoryImg) {
        this.trajectoryImg = trajectoryImg;
    }

    public String getTrajectoryId() {
        return trajectoryInfoId;
    }

    public void setTrajectoryId(String trajectoryId) {
        this.trajectoryInfoId = trajectoryId;
    }

    public String getWorkType() {
        return workType;
    }

    public void setWorkType(String workType) {
        this.workType = workType;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public BigDecimal getArea() {
        return area;
    }

    public void setArea(BigDecimal area) {
        this.area = area;
    }
}