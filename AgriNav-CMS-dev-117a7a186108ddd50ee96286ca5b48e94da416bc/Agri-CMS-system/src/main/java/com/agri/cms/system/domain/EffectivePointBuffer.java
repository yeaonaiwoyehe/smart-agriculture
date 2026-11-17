package com.agri.cms.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 实时有效作业点暂存表（替代 InfluxDB，生成轨迹后清理）
 *
 * @author AgriNav
 */
@TableName(value = "effective_point_buffer")
public class EffectivePointBuffer {

    @TableId(value = "id")
    private Long id;

    /** 终端手机号（T0200 clientId） */
    private String phoneNumber;

    /** 点所属地块 ID */
    private Long plotId;

    /** 作业类型（附加信息 0xEE），无则 -1 */
    private Integer workType;

    /** 作业幅宽（附加信息 0xFF），无则 -1 */
    private Integer width;

    /** 经度 WGS84 */
    private Double longitude;

    /** 纬度 WGS84 */
    private Double latitude;

    /** 设备上报时间 */
    private LocalDateTime deviceTime;

    /** 服务入库时间 */
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Long getPlotId() {
        return plotId;
    }

    public void setPlotId(Long plotId) {
        this.plotId = plotId;
    }

    public Integer getWorkType() {
        return workType;
    }

    public void setWorkType(Integer workType) {
        this.workType = workType;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public LocalDateTime getDeviceTime() {
        return deviceTime;
    }

    public void setDeviceTime(LocalDateTime deviceTime) {
        this.deviceTime = deviceTime;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
