package com.agri.cms.system.domain.dto;

import com.agri.cms.system.domain.AgriculturalMachinery;
import com.agri.cms.system.domain.AgriculturalMachineryCrop;
import com.agri.cms.system.domain.AgriculturalMachineryImage;
import com.agri.cms.system.domain.AgriculturalMachineryLocation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import com.agri.cms.common.utils.uuid.UUID;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 姚锦伦
 */
public class AgriculturalMachineryDTO {
    private String id;
    /**
     * 农机任务名称
     */
    @NotBlank(message = "名称不能为空")
    private String name;
    /**
     * 农机型号
     */
    @NotNull(message = "农机型号不能为空")
    private String typeId;
    /**
     * 可作物类型
     */
    @NotEmpty(message = "可作物类型不能为空")
    private List<Long> cropId;
    /**
     * 是够配备运粮车
     */
    @NotNull(message = "是否配备运粮车不能为空")
    private Boolean havingGrainCarrier;
    /**
     * 作业区域
     */
    @NotEmpty(message = "作业区域不能为空")
    private List<String> location;
    /**
     * 联系方式
     */
    @NotBlank(message = "联系方式不能为空")
    private String phoneNumber;
    /**
     * 农机图片
     */
    private List<String> image;
    /**
     * 有效时间
     */
    @NotNull(message = "有效时间不能为空")
    private LocalDateTime endTime;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    /**
     * 农机找活状态
     */
    private Integer status;
    private Boolean orderByCreateTimeDesc;
    private Boolean myAgriculturalMachinery;
    /**
     * 省市前缀
     */
    private String addressPrefix;

    public Boolean getMyAgriculturalMachinery() {
        return myAgriculturalMachinery;
    }

    public void setMyAgriculturalMachinery(Boolean myAgriculturalMachinery) {
        this.myAgriculturalMachinery = myAgriculturalMachinery;
    }

    public Boolean getOrderByCreateTimeDesc() {
        return orderByCreateTimeDesc;
    }


    public void setOrderByCreateTimeDesc(Boolean orderByCreateTimeDesc) {
        this.orderByCreateTimeDesc = orderByCreateTimeDesc;
    }

    public String getAddressPrefix() {
        return addressPrefix;
    }

    public void setAddressPrefix(String addressPrefix) {
        this.addressPrefix = addressPrefix;
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

    public Boolean getHavingGrainCarrier() {
        return havingGrainCarrier;
    }

    public void setHavingGrainCarrier(Boolean havingGrainCarrier) {
        this.havingGrainCarrier = havingGrainCarrier;
    }

    public List<String> getLocation() {
        return location;
    }

    public void setLocation(List<String> location) {
        this.location = location;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public List<String> getImage() {
        return image;
    }

    public void setImage(List<String> image) {
        this.image = image;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    /**
     * 转为AgriculturalMachineryCrop
     *
     * @param uuid
     * @return List<AgriculturalMachineryCrop>
     * @author 姚锦伦
     * @since 2024/10/18
     */
    public List<AgriculturalMachineryCrop> toAgriculturalMachineryCrop(UUID uuid) {
        //作物多选
        List<AgriculturalMachineryCrop> agriculturalMachineryCrops = new ArrayList<>();
        this.getCropId().forEach(cropId -> {
            var agriculturalMachineryCrop = new AgriculturalMachineryCrop();
            //农机id
            agriculturalMachineryCrop.setAgriculturalMachineryId(uuid.toString());
            //作物id
            agriculturalMachineryCrop.setCropId(cropId);
            agriculturalMachineryCrops.add(agriculturalMachineryCrop);
        });
        return agriculturalMachineryCrops;
    }

    public List<AgriculturalMachineryCrop> toAgriculturalMachineryCrop() {
        //作物多选
        List<AgriculturalMachineryCrop> agriculturalMachineryCrops = new ArrayList<>();
        this.getCropId().forEach(cropId -> {
            var agriculturalMachineryCrop = new AgriculturalMachineryCrop();
            //农机id
            agriculturalMachineryCrop.setAgriculturalMachineryId(this.getId());
            //作物id
            agriculturalMachineryCrop.setCropId(cropId);
            agriculturalMachineryCrops.add(agriculturalMachineryCrop);
        });
        return agriculturalMachineryCrops;
    }

    /**
     * 转换为AgriculturalMachineryImage
     *
     * @param uuid
     * @return List<AgriculturalMachineryImage>
     * @author 姚锦伦
     * @since 2024/10/18
     */
    public List<AgriculturalMachineryImage> toAgriculturalMachineryImage(UUID uuid) {
        List<AgriculturalMachineryImage> agriculturalMachineryImages = new ArrayList<>();
        if (this.getImage() != null) {
            this.getImage().forEach(image -> {
                var agriculturalMachineryImage = new AgriculturalMachineryImage();
                //农机id
                agriculturalMachineryImage.setAgriculturalMachineryId(uuid.toString());
                //农机图片
                agriculturalMachineryImage.setImage(image);
                agriculturalMachineryImages.add(agriculturalMachineryImage);
            });
        }
        return agriculturalMachineryImages;
    }

    public List<AgriculturalMachineryImage> toAgriculturalMachineryImage() {
        List<AgriculturalMachineryImage> agriculturalMachineryImages = new ArrayList<>();
        if (this.getImage() != null) {
            this.getImage().forEach(image -> {
                var agriculturalMachineryImage = new AgriculturalMachineryImage();
                //农机id
                agriculturalMachineryImage.setAgriculturalMachineryId(this.getId());
                //农机图片
                agriculturalMachineryImage.setImage(image);
                agriculturalMachineryImages.add(agriculturalMachineryImage);
            });
        }
        return agriculturalMachineryImages;
    }

    /**
     * 转换为AgriculturalMachineryLocation
     *
     * @param uuid
     * @return List<AgriculturalMachineryLocation>
     * @author 姚锦伦
     * @since 2024/10/18
     */
    public List<AgriculturalMachineryLocation> toAgriculturalMachineryLocation(UUID uuid) {
        List<AgriculturalMachineryLocation> agriculturalMachineryLocations = new ArrayList<>();
        this.getLocation().forEach(location -> {
            var agriculturalMachineryLocation = new AgriculturalMachineryLocation();
            //农机id
            agriculturalMachineryLocation.setAgriculturalMachineryId(uuid.toString());
            //农机作业区域
            agriculturalMachineryLocation.setLocation(location);
            agriculturalMachineryLocations.add(agriculturalMachineryLocation);
        });
        return agriculturalMachineryLocations;
    }

    public List<AgriculturalMachineryLocation> toAgriculturalMachineryLocation() {
        List<AgriculturalMachineryLocation> agriculturalMachineryLocations = new ArrayList<>();
        this.getLocation().forEach(location -> {
            var agriculturalMachineryLocation = new AgriculturalMachineryLocation();
            //农机id
            agriculturalMachineryLocation.setAgriculturalMachineryId(this.getId());
            //农机作业区域
            agriculturalMachineryLocation.setLocation(location);
            agriculturalMachineryLocations.add(agriculturalMachineryLocation);
        });
        return agriculturalMachineryLocations;
    }

    /**
     * 转换为AgriculturalMachinery
     *
     * @param uuid
     * @return AgriculturalMachinery
     * @author 姚锦伦
     * @since 2024/10/18
     */
    public AgriculturalMachinery toAgriculturalMachinery(UUID uuid) {
        var agriculturalMachinery = new AgriculturalMachinery();
        //农机id
        agriculturalMachinery.setId(uuid.toString());
        //农机找活任务名称
        agriculturalMachinery.setName(this.getName());
        //农机型号
        agriculturalMachinery.setTypeId(this.getTypeId());
        //农机是否配备运粮车
        agriculturalMachinery.setHavingGrainCarrier(this.getHavingGrainCarrier());
        //农机联系方式
        agriculturalMachinery.setPhoneNumber(this.getPhoneNumber());
        //截止时间
        agriculturalMachinery.setEndTime(this.getEndTime());
        //省市前缀
        agriculturalMachinery.setAddressPrefix(this.getAddressPrefix());
        return agriculturalMachinery;
    }

    public AgriculturalMachinery toAgriculturalMachinery() {
        var agriculturalMachinery = new AgriculturalMachinery();
        //农机id
        agriculturalMachinery.setId(this.getId());
        //农机找活任务名称
        agriculturalMachinery.setName(this.getName());
        //农机型号
        agriculturalMachinery.setTypeId(this.getTypeId());
        //农机是否配备运粮车
        agriculturalMachinery.setHavingGrainCarrier(this.getHavingGrainCarrier());
        //农机联系方式
        agriculturalMachinery.setPhoneNumber(this.getPhoneNumber());
        //创建时间
        agriculturalMachinery.setCreateTime(this.getCreateTime());
        //状态
        agriculturalMachinery.setStatus(this.getStatus());
        //截止时间
        agriculturalMachinery.setEndTime(this.getEndTime());
        //省市前缀
        agriculturalMachinery.setAddressPrefix(this.getAddressPrefix());
        return agriculturalMachinery;
    }

    @Override
    public String toString() {
        return "AgriculturalMachineryDTO{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", typeId='" + typeId + '\'' +
                ", cropId=" + cropId +
                ", havingGrainCarrier=" + havingGrainCarrier +
                ", location=" + location +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", image=" + image +
                ", endTime=" + endTime +
                ", createTime=" + createTime +
                ", status=" + status +
                ", orderByCreateTimeDesc=" + orderByCreateTimeDesc +
                ", myAgriculturalMachinery=" + myAgriculturalMachinery +
                '}';
    }
}
