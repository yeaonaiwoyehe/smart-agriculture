package com.agri.cms.system.domain.dto;

import com.agri.cms.system.domain.FarmWork;
import com.agri.cms.system.domain.FarmWorkCrop;
import com.agri.cms.system.domain.FarmWorkLandPlot;
import com.agri.cms.system.domain.FarmWorkType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.elasticsearch.search.DocValueFormat;
import com.agri.cms.common.utils.uuid.UUID;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author 姚锦伦
 */
public class FarmWorkDTO {
    /**
     * 农活id
     */
    private String id;
    /**
     * 农活名称
     */
    @NotBlank(message = "农活名称不能为空")
    private String name;
    /**
     * 地块总面积
     */
    @NotNull(message = "亩数不能为空")
    private BigDecimal area;
    /**
     * 截止时间
     */
    @NotNull(message = "作业期限不能为空")
    private LocalDateTime endTime;
    /**
     * 手机号
     */
    @NotBlank(message = "联系方式不能为空")
    private String phoneNumber;
    /**
     * 创建者
     */
    private String createBy;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    /**
     * 状态
     */
    private Integer status;
    /**
     * 是够按照创建时间降序排序
     */
    private Boolean orderByCreateTimeDesc;
    /**
     * 是否是我发布的
     */
    private Boolean myFarmWork;
    /**
     * 地块id
     */
    private List<String> landPlotId;
    /**
     * 最小面积
     */
    private BigDecimal minArea;
    /**
     * 最大面积
     */
    private BigDecimal maxArea;
    /**
     * 作物id
     */
    private List<Long> cropId;
    /**
     * 作业类型id
     */
    private List<Long> workTypeId;
    /**
     * 地块位置
     */
    private List<String> location;

    public List<String> getLocation() {
        return location;
    }

    public void setLocation(List<String> location) {
        this.location = location;
    }

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

    public BigDecimal getMinArea() {
        return minArea;
    }

    public void setMinArea(BigDecimal minArea) {
        this.minArea = minArea;
    }

    public BigDecimal getMaxArea() {
        return maxArea;
    }

    public void setMaxArea(BigDecimal maxArea) {
        this.maxArea = maxArea;
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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Boolean getOrderByCreateTimeDesc() {
        return orderByCreateTimeDesc;
    }

    public void setOrderByCreateTimeDesc(Boolean orderByCreateTimeDesc) {
        this.orderByCreateTimeDesc = orderByCreateTimeDesc;
    }

    public Boolean getMyFarmWork() {
        return myFarmWork;
    }

    public void setMyFarmWork(Boolean myFarmWork) {
        this.myFarmWork = myFarmWork;
    }

    public List<String> getLandPlotId() {
        return landPlotId;
    }

    public void setLandPlotId(List<String> landPlotId) {
        this.landPlotId = landPlotId;
    }

    public FarmWork toFarmWork(UUID uuid) {
        var farmWork = new FarmWork();
        farmWork.setId(uuid.toString());
        farmWork.setName(this.getName());
        farmWork.setArea(this.getArea());
        farmWork.setEndTime(this.getEndTime());
        farmWork.setPhoneNumber(this.getPhoneNumber());
        farmWork.setCreateBy(this.getCreateBy());
        farmWork.setCreateTime(this.getCreateTime());
        return farmWork;
    }

    public FarmWork toFarmWork() {
        var farmWork = new FarmWork();
        farmWork.setId(this.getId());
        farmWork.setName(this.getName());
        farmWork.setArea(this.getArea());
        farmWork.setEndTime(this.getEndTime());
        farmWork.setPhoneNumber(this.getPhoneNumber());
        farmWork.setCreateBy(this.getCreateBy());
        farmWork.setCreateTime(this.getCreateTime());
        farmWork.setStatus(this.getStatus());
        return farmWork;
    }

    public List<FarmWorkCrop> toFarmWorkCrop(UUID uuid) {
        List<FarmWorkCrop> farmWorkCropList = new ArrayList<>();
        if (this.getWorkTypeId() != null) {
            List<Long> cropIdList = this.getCropId();
            if (cropIdList != null) {
                cropIdList.forEach(cropId -> {
                    FarmWorkCrop farmWorkCrop = new FarmWorkCrop();
                    farmWorkCrop.setFarmWorkId(uuid.toString());
                    farmWorkCrop.setCropId(cropId);
                    farmWorkCropList.add(farmWorkCrop);
                });
            }
        }
        return farmWorkCropList;
    }

    public List<FarmWorkCrop> toFarmWorkCrop() {
        List<FarmWorkCrop> farmWorkCropList = new ArrayList<>();
        this.getCropId().forEach(item -> {
            FarmWorkCrop farmWorkCrop = new FarmWorkCrop();
            farmWorkCrop.setFarmWorkId(this.getId());
            farmWorkCrop.setCropId(item);
            farmWorkCropList.add(farmWorkCrop);
        });
        return farmWorkCropList;
    }

    public List<FarmWorkLandPlot> toFarmWorkLandPlot(UUID uuid) {
        List<FarmWorkLandPlot> farmWorkLandPlotList = new ArrayList<>();

        // 创建地块ID列表
        List<Long> landPlotIdList = new ArrayList<>();
        // 如果已有Long类型的地块ID列表，添加到集合中
        if (this.getLandPlotId() != null) {
            landPlotIdList.addAll(this.getLandPlotId().stream()
                    .map(Long::parseLong)
                    .toList());
        }

        // 转换为FarmWorkLandPlot对象
        return landPlotIdList.stream()
                .map(landPlotId -> {
                    FarmWorkLandPlot farmWorkLandPlot = new FarmWorkLandPlot();
                    farmWorkLandPlot.setFarmWorkId(uuid.toString());
                    farmWorkLandPlot.setLandPlotId(landPlotId);
                    return farmWorkLandPlot;
                })
                .collect(Collectors.toList());
    }

    public List<FarmWorkLandPlot> toFarmWorkLandPlot() {
        List<FarmWorkLandPlot> farmWorkLandPlotList = new ArrayList<>();
        this.getLandPlotId().forEach(landPlotId -> {
            var farmWorkLandPlot = new FarmWorkLandPlot();
            farmWorkLandPlot.setFarmWorkId(this.getId());
            farmWorkLandPlot.setLandPlotId(Long.parseLong(landPlotId));
            farmWorkLandPlotList.add(farmWorkLandPlot);
        });
        return farmWorkLandPlotList;
    }

    public List<FarmWorkType> toFarmWorkType(UUID uuid) {
        List<FarmWorkType> farmWorkTypeList = new ArrayList<>();
        this.getWorkTypeId().forEach(workTypeId -> {
            var farmWorkType = new FarmWorkType();
            farmWorkType.setFarmWorkId(uuid.toString());
            farmWorkType.setWorkTypeId(workTypeId);
            farmWorkTypeList.add(farmWorkType);
        });
        return farmWorkTypeList;
    }

    public List<FarmWorkType> toFarmWorkType() {
        List<FarmWorkType> farmWorkTypeList = new ArrayList<>();
        this.getWorkTypeId().forEach(workTypeId -> {
            var farmWorkType = new FarmWorkType();
            farmWorkType.setFarmWorkId(this.getId());
            farmWorkType.setWorkTypeId(workTypeId);
            farmWorkTypeList.add(farmWorkType);
        });
        return farmWorkTypeList;
    }
}
