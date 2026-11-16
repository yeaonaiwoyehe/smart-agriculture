package com.agri.cms.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.postgis.Polygon;
import java.math.BigDecimal;

/**
 * 地块无用区域实体类
 *
 * @author simon
 */
@TableName("land_plot_exclude")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LandPlotExclude {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @TableField("land_plot_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @NotNull(message = "地块父id不能为空")
    private Long landPlotId;

    @TableField("boundary")
    @NotNull(message = "边界不能为空")
    private Polygon boundary;

    @TableField("area")
    @NotNull(message = "面积不能为空")
    private Float area;

    @TableField("is_deleted")
    private int isDeleted;

    public LandPlotExclude() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getLandPlotId() {
        return landPlotId;
    }

    public void setLandPlotId(Long landPlotId) {
        this.landPlotId = landPlotId;
    }

    public Polygon getBoundary() {
        return boundary;
    }

    public void setBoundary(Polygon boundary) {
        this.boundary = boundary;
    }

    public Float getArea() {
        return area;
    }

    public void setArea(Float area) {
        this.area = area;
    }

    public int getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(int isDeleted) {
        this.isDeleted = isDeleted;
    }

    @Override
    public String toString() {
        return "LandPlotExclude{" +
                "id=" + id +
                ", landPlotId=" + landPlotId +
                ", boundary=" + boundary +
                ", area=" + area +
                '}';
    }
}