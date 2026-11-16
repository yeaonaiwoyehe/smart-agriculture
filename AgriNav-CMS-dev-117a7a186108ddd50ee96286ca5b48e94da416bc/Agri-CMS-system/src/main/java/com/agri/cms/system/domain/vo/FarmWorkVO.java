package com.agri.cms.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author 姚锦伦
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FarmWorkVO {
    /**
     * 农事作业id
     */
    private String id;
    /**
     * 作业名称
     */
    private String name;
    /**
     * 作业亩数
     */
    private BigDecimal area;
    /**
     * 期限时间
     */
    private LocalDateTime endTime;
    /**
     * 联系方式
     */
    private String phoneNumber;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    /**
     * 创建者
     */
    private String createBy;
    /**
     * 状态
     */
    private Integer status;
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    /**
     * 更新者
     */
    private String updateBy;
    /**
     * 作物id
     */
    private List<Long> cropId;
    /**
     * 作业类型id
     */
    private List<Long> workTypeId;
    /**
     * 地块id
     */
//    @JsonSerialize(using= ToStringSerializer.class)
    private List<String> landPlotId;
    /**
     * 作物名
     */
    private List<String> cropName;
    /**
     * 作业类型名
     */
    private List<String> workTypeName;
    /**
     * 地块名
     */
    private List<String> landPlotName;
    /**
     * 地块图片
     */
    private List<String> landPlotImage;
    private List<String> landAddress;

    public List<Long> getCropId() {
        return cropId;
    }

    public void setCropId(List<Long> cropId) {
        this.cropId = cropId;
    }

    public List<Long> getWorkTypeId() {
        return workTypeId;
    }

    public void setWorkTypeId(List<Long> workTypeId) {
        this.workTypeId = workTypeId;
    }

    public List<String> getLandPlotId() {
        return landPlotId;
    }

    public void setLandPlotId(List<String> landPlotId) {
        this.landPlotId = landPlotId;
    }

    public List<String> getCropName() {
        return cropName;
    }

    public void setCropName(List<String> cropName) {
        this.cropName = cropName;
    }

    public List<String> getWorkTypeName() {
        return workTypeName;
    }

    public void setWorkTypeName(List<String> workTypeName) {
        this.workTypeName = workTypeName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getArea() {
        return area;
    }

    public void setArea(BigDecimal area) {
        this.area = area;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public List<String> getLandPlotName() {
        return landPlotName;
    }

    public void setLandPlotName(List<String> landPlotName) {
        this.landPlotName = landPlotName;
    }

    public List<String> getLandAddress() {
        return landAddress;
    }

    public void setLandAddress(List<String> landAddress) {
        this.landAddress = landAddress;
    }

    public List<String> getLandPlotImage() {
        return landPlotImage;
    }

    public void setLandPlotImage(List<String> landPlotImage) {
        this.landPlotImage = landPlotImage;
    }
}
