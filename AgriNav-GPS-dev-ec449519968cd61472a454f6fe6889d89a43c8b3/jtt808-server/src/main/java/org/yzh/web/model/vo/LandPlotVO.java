package org.yzh.web.model.vo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
/**
 *
 * @author simon
 * @since 2024/9/20 */
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
    @NotEmpty(message = "地块面积不能为空")
    private Float landArea;
    @NotBlank(message = "中心点不能为空")
    private String centerPoint;
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
    @Override
    public String toString() {
        return "LandPlotVO{" +
                "id=" + id +
                ", landName='" + landName + '\'' +
                ", landType='" + landType + '\'' +
                ", address='" + address + '\'' +
                ", boundary='" + boundary + '\'' +
                ", landArea=" + landArea +
                '}';
    }
}