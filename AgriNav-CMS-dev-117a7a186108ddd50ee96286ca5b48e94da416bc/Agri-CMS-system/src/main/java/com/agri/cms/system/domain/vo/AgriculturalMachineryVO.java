package com.agri.cms.system.domain.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author 姚锦伦
 */
public class AgriculturalMachineryVO {
    /**
     * ·
     * 农机id
     */
    private String id;
    /**
     * 农机类型
     */
    private String typeName;
    /**
     * 联系方式
     */
    private String phoneNumber;
    /**
     * 农机状态
     */
    private int status;
    /**
     * 创建人
     */
    private String createBy;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    /**
     * 更新人
     */
    private String updateBy;
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    /**
     * 是否配备运粮车
     */
    private boolean havingGrainCarrier;
    /**
     * 截止日期
     */
    private LocalDateTime endTime;
    /**
     * 图片
     */
    private List<String> image;
    /**
     * 位置
     */
    private List<String> location;
    /**
     * 农作物
     */
    private List<String> crop;
    /**
     * 农机找活名称
     */
    private String name;
    /**
     * 农机类型id
     */
    private String typeId;
    /**
     * 农作物id
     */
    private List<Long> cropId;
    /**
     * 省市前缀
     */
    private String addressPrefix;

    public String getAddressPrefix() {
        return addressPrefix;
    }

    public void setAddressPrefix(String addressPrefix) {
        this.addressPrefix = addressPrefix;
    }

    public String getTypeId() {
        return typeId;
    }

    public void setTypeId(String typeId) {
        this.typeId = typeId;
    }

    public List<Long> getCropId() {
        return cropId;
    }

    public void setCropId(List<Long> cropId) {
        this.cropId = cropId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public boolean isHavingGrainCarrier() {
        return havingGrainCarrier;
    }

    public void setHavingGrainCarrier(boolean havingGrainCarrier) {
        this.havingGrainCarrier = havingGrainCarrier;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public List<String> getImage() {
        return image;
    }

    public void setImage(List<String> image) {
        this.image = image;
    }

    public List<String> getLocation() {
        return location;
    }

    public void setLocation(List<String> location) {
        this.location = location;
    }

    public List<String> getCrop() {
        return crop;
    }

    public void setCrop(List<String> crop) {
        this.crop = crop;
    }

    @Override
    public String toString() {
        return "AgriculturalMachineryVO{" +
                "id='" + id + '\'' +
                ", typeName='" + typeName + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", status=" + status +
                ", createBy='" + createBy + '\'' +
                ", createTime=" + createTime +
                ", updateBy='" + updateBy + '\'' +
                ", updateTime=" + updateTime +
                ", havingGrainCarrier=" + havingGrainCarrier +
                ", endTime=" + endTime +
                ", image=" + image +
                ", location=" + location +
                ", crop=" + crop +
                '}';
    }
}
