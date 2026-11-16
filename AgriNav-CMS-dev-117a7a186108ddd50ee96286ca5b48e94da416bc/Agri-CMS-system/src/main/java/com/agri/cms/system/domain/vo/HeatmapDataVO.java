package com.agri.cms.system.domain.vo;

/**
 * 热力图数据
 * @author mfr
 * @since 2024/11/25 */
public class HeatmapDataVO {
    private Long id;
    // 纬度
    private Double longitude;
    // 经度
    private Double latitude;
    // 权重/热度值
    private Double weight;

    // 无参构造
    public HeatmapDataVO() {
    }

    // 全参构造
    public HeatmapDataVO(Long id, Double latitude, Double longitude, Double weight) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.weight = weight;
    }

    // getter和setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    @Override
    public String toString() {
        return "HeatmapDataVO{" +
                "id=" + id +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", weight=" + weight +
                '}';
    }
}