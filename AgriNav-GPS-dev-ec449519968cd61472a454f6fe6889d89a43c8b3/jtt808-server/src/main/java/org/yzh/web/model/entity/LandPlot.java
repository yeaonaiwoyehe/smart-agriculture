package org.yzh.web.model.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.postgis.Point;
import org.postgis.Polygon;
import java.time.LocalDateTime;
@TableName("land_plot")
public class LandPlot {
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    private String landName;
    private String landType;
    private Float landArea;
    private int isDeleted;
    private String address;
    private String createBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    private String updateBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
    private String image;
    private Polygon boundary;
    private Point centerPoint;
    public LandPlot() {
    }
    public LandPlot(Long id, String landName, String landType, Float landArea, int isDeleted, String address, String createBy, LocalDateTime createTime, String updateBy, LocalDateTime updateTime, String image, Polygon boundary, Point centerPoint) {
        this.id = id;
        this.landName = landName;
        this.landType = landType;
        this.landArea = landArea;
        this.isDeleted = isDeleted;
        this.address = address;
        this.createBy = createBy;
        this.createTime = createTime;
        this.updateBy = updateBy;
        this.updateTime = updateTime;
        this.image = image;
        this.boundary = boundary;
        this.centerPoint = centerPoint;
    }
    public Point getCenterPoint() {
        return centerPoint;
    }
    public void setCenterPoint(Point centerPoint) {
        this.centerPoint = centerPoint;
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
    public Float getLandArea() {
        return landArea;
    }
    public void setLandArea(Float landArea) {
        this.landArea = landArea;
    }
    public int getIsDeleted() {
        return isDeleted;
    }
    public void setIsDeleted(int isDeleted) {
        this.isDeleted = isDeleted;
    }
    public String getAddress() {
        return address;
    }
    public void setAddress(String address) {
        this.address = address;
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
    public String getImage() {
        return image;
    }
    public void setImage(String image) {
        this.image = image;
    }
    public Polygon getBoundary() {
        return boundary;
    }
    public void setBoundary(Polygon boundary) {
        this.boundary = boundary;
    }
}