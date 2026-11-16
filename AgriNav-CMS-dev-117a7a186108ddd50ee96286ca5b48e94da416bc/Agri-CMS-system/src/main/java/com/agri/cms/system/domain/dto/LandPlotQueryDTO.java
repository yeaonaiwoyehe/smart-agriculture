package com.agri.cms.system.domain.dto;

import com.agri.cms.common.constant.Constants;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

/**
 * 地块查询条件
 * @author mfr
 * @since 2024/11/25 */
public class LandPlotQueryDTO {
    private String landType;
    private String landName;

    // 时间格式验证，允许为空
    @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$", message = "时间格式应为yyyy-MM-dd")
    private String startTime;

    @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$", message = "时间格式应为yyyy-MM-dd")
    private String endTime;

    // 面积格式验证，允许为空
    @Pattern(regexp = "^$|^\\d+(\\.\\d+)?$", message = "面积必须为数字")
    private String beginArea;

    @Pattern(regexp = "^$|^\\d+(\\.\\d+)?$", message = "面积必须为数字")
    private String endArea;

    private String measurementMethod;
    private String address;
    private boolean isCurrent = true;
    private long current = Constants.PAGE_NUM;
    private long size = Constants.PAGE_SIZE_DEFAULT;
    //是否删除
    private int isDeleted = Constants.NOT_DELETED;

    public String getLandType() {
        return landType;
    }

    public void setLandType(String landType) {
        this.landType = landType;
    }

    public String getLandName() {
        return landName;
    }

    public void setLandName(String landName) {
        this.landName = landName;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getBeginArea() {
        return beginArea;
    }

    public void setBeginArea(String beginArea) {
        this.beginArea = beginArea;
    }

    public String getEndArea() {
        return endArea;
    }

    public void setEndArea(String endArea) {
        this.endArea = endArea;
    }

    public String getMeasurementMethod() {
        return measurementMethod;
    }

    public void setMeasurementMethod(String measurementMethod) {
        this.measurementMethod = measurementMethod;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public boolean isCurrent() {
        return isCurrent;
    }

    public void setIsCurrent(boolean current) {
        isCurrent = current;
    }

    public long getCurrent() {
        return current;
    }

    public void setCurrent(long current) {
        this.current = current;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public int getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(int isDeleted) {
        this.isDeleted = isDeleted;
    }

    @Override
    public String toString() {
        return "LandPlotQueryDTO{" +
                "landType='" + landType + '\'' +
                ", landName='" + landName + '\'' +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                ", beginArea='" + beginArea + '\'' +
                ", endArea='" + endArea + '\'' +
                ", measurementMethod='" + measurementMethod + '\'' +
                ", address='" + address + '\'' +
                ", isCurrent=" + isCurrent +
                ", current=" + current +
                ", size=" + size +
                '}';
    }
}