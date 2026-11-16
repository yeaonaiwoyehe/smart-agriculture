package com.agri.cms.system.domain.vo;

import com.agri.cms.system.domain.TrajectoryExclude;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class Trajectory {
    private String trajectoryInfoId;
    /**
     * 手机号
     */
    private String phoneNumber;
    /**
     * 作业类型
     */
    @NotNull(message = "作业类型不能为空")
    private String workType;
    /**
     * 作物
     */
    @NotNull(message = "作物不能为空")
    private String crop;
    /**
     * 位置
     */
    @NotNull(message = "位置不能为空")
    private String location;
    /**
     * 幅宽
     */
    private int width;
    /**
     * 地块id
     */
    private long plotId;
    /**
     * 点集
     */
    private List<Point> points;
    /**
     * 轨迹图片
     */
    private String trajectoryImg;
    /**
     * 轨迹面积
     */
    private BigDecimal area;
    /**
     * 排除区域
     */
    private List<TrajectoryExclude> trajectoryExclude;

    public String getTrajectoryImg() {
        return trajectoryImg;
    }

    public void setTrajectoryImg(String trajectoryImg) {
        this.trajectoryImg = trajectoryImg;
    }

    /**
     * 开始时间
     */
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    /**
     * 认证码
     */
    public String getTrajectoryInfoId() {
        return trajectoryInfoId;
    }

    public void setTrajectoryInfoId(String trajectoryInfoId) {
        this.trajectoryInfoId = trajectoryInfoId;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getWorkType() {
        return workType;
    }

    public void setWorkType(String workType) {
        this.workType = workType;
    }

    public String getCrop() {
        return crop;
    }

    public void setCrop(String crop) {
        this.crop = crop;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public long getPlotId() {
        return plotId;
    }

    public void setPlotId(long plotId) {
        this.plotId = plotId;
    }

    public List<Point> getPoints() {
        return points;
    }

    public void setPoints(List<Point> points) {
        this.points = points;
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

    public BigDecimal getArea() {
        return area;
    }

    public void setArea(BigDecimal area) {
        this.area = area;
    }

    public List<TrajectoryExclude> getTrajectoryExclude() {
        return trajectoryExclude;
    }

    public void setTrajectoryExclude(List<TrajectoryExclude> trajectoryExclude) {
        this.trajectoryExclude = trajectoryExclude;
    }

    public Trajectory() {
    }
}