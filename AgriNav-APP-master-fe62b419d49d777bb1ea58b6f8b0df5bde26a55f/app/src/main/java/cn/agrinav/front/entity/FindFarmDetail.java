package cn.agrinav.front.entity;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import cn.agrinav.measure.entity.LandPlot;

public class FindFarmDetail implements Serializable {
    // id
    private String findFarmId;
    // 任务名
    private String taskName;
    // 地块图片
    private String landImage;
    // 地块列表
    private List<NewLandPlot> landPlots;
    // 地块亩数
    private String landPlotArea;
    // 图片列表
    private List<String> imageList;
    // 作物列表
    private List<String> crops;
    // 作业类型列表
    private List<String> operationTypes;
    // 地址列表
    private List<String> landPlotAddresses;
    // 联系方式
    private String contactMethod;
    // 发布状态
    private int publishStatus;
    // 任务有效期
    private String operationDate;
    // 发布时间
    private String publishDate;

    public FindFarmDetail() {
        crops = new ArrayList<>();
        operationTypes = new ArrayList<>();
        imageList = new ArrayList<>();
        landPlotAddresses = new ArrayList<>();
    }

    public String getFindFarmId() {
        return findFarmId;
    }

    public void setFindFarmId(String  findFarmId) {
        this.findFarmId = findFarmId;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }
    public String getLandImage() {
        return landImage;
    }

    public void setLandImage(String landImage) {
        this.landImage = landImage;
    }
    public List<String> getOperationTypes() {
        return operationTypes;
    }

    public void setOperationTypes(List<String> operationTypes) {
        this.operationTypes = operationTypes;
    }
    public String getLandPlotArea() {
        return landPlotArea;
    }

    public void setLandPlotArea(String landPlotArea) {
        this.landPlotArea = landPlotArea;
    }


    public List<NewLandPlot> getLandPlots() {
        return landPlots;
    }

    public void setLandPlots(List<NewLandPlot> landPlots) {
        this.landPlots = landPlots;
    }

    public List<String> getCrops() {
        return crops;
    }

    public void setCrops(List<String> crops) {
        this.crops = crops;
    }

    public List<String> getImageList() {
        return imageList;
    }

    public void setImageList(List<String> imageList) {
        this.imageList = imageList;
    }

    public List<String> getLandPlotAddresses() {
        return landPlotAddresses;
    }

    public void setLandPlotAddresses(List<String> landPlotAddresses) {
        this.landPlotAddresses = landPlotAddresses;
    }

    public int getPublishStatus() {
        return publishStatus;
    }

    public void setPublishStatus(int publishStatus) {
        this.publishStatus = publishStatus;
    }

    public String getContactMethod() {
        return contactMethod;
    }

    public void setContactMethod(String contactMethod) {
        this.contactMethod = contactMethod;
    }

    public String getOperationDate() {
        return operationDate;
    }

    public void setOperationDate(String operationDate) {
        this.operationDate = operationDate;
    }

    public String getPublishDate() {
        return publishDate;
    }
    public void setPublishDate(String publishDate) {
        this.publishDate = publishDate;
    }

    public void addCrop(String crop) {
        this.crops.add(crop);
    }
    public void addOperationType(String operationType) {
        this.operationTypes.add(operationType);
    }
    public void addImage(String image) {
        this.imageList.add(image);
    }
    public void addLandPlotAddress(String address) {
        //当名称不重复的时候加
        if(!landPlotAddresses.contains(address)){
            this.landPlotAddresses.add(address);
        }

    }


}