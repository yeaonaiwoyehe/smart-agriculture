package com.agri.cms.system.domain.vo;

import com.agri.cms.system.domain.LandPlotExclude;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author simon
 * @since 2024/9/20
 */

public class LandPlotVO {


    private Long id;
    @NotBlank(message = "地块名不能为空")
    private String landName;
    @NotBlank(message = "地块类型不能为空")
    private String landType;
    @NotBlank(message = "地块地址不能为空")
    private String address;

    @NotBlank(message = "地块边界不能为空")
    private String boundary;
    @NotNull(message = "地块面积不能为空")
    @Positive(message = "地块面积必须大于0")
    private Float landArea;
    @NotBlank(message = "中心点不能为空")
    private String centerPoint;
    @NotBlank(message = "测量方法不能为空")
    private String measurementMethod;
    @NotBlank(message = "所属者不能为空")
    private String ownerName;
    @NotBlank(message = "所属者电话不能为空")
    private String ownerPhone;

    List<String> landPlotExcludes;

    public String getCenterPoint() {
        return centerPoint;
    }

    public void setCenterPoint(String centerPoint) {
        this.centerPoint = centerPoint;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLandName() {
        return landName;
    }

    public void setLandName(String landName) {
        this.landName = landName;
    }

    public String getLandType() {
        return landType;
    }

    public void setLandType(String landType) {
        this.landType = landType;
    }


    public String getBoundary() {
        return boundary;
    }

    public void setBoundary(String boundary) {
        this.boundary = boundary;
    }

    public Float getLandArea() {
        return landArea;
    }

    public void setLandArea(Float landArea) {
        this.landArea = landArea;
    }

    public String getMeasurementMethod() {
        return measurementMethod;
    }

    public void setMeasurementMethod(String measurementMethod) {
        this.measurementMethod = measurementMethod;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getOwnerPhone() {
        return ownerPhone;
    }

    public void setOwnerPhone(String ownerPhone) {
        this.ownerPhone = ownerPhone;
    }

    public List<String> getLandPlotExcludes() {
        return landPlotExcludes;
    }

    public void setLandPlotExcludes(List<String> landPlotExcludes) {
        this.landPlotExcludes = landPlotExcludes;
    }


    @Override
    public String toString() {
        return "LandPlotVO{" +
                "id=" + id +
                ", landName='" + landName + '\'' +
                ", landType='" + landType + '\'' +
                ", address='" + address + '\'' +
                ", boundary='" + boundary + '\'' +
                ", landArea=" + landArea +
                ", centerPoint='" + centerPoint + '\'' +
                ", measurementMethod='" + measurementMethod + '\'' +
                ", ownerName='" + ownerName + '\'' +
                ", ownerPhone='" + ownerPhone + '\'' +
                '}';
    }
}
