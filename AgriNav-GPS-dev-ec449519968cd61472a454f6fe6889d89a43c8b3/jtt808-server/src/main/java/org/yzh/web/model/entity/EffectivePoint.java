package org.yzh.web.model.entity;
/**
 * @Author: 姚锦伦
 * @CreateTime: 2024-09-28
 * @Description:
 * @Version: 1.0
 */
public class EffectivePoint {
    private String longitude;
    private String latitude;
    private String plotId;
    public EffectivePoint(String longitude, String latitude, String plotId) {
        this.longitude = longitude;
        this.latitude = latitude;
        this.plotId = plotId;
    }
    @Override
    public String toString() {
        return "EffectivePoint{" +
                "longitude='" + longitude + '\'' +
                ", latitude='" + latitude + '\'' +
                ", plotId='" + plotId + '\'' +
                '}';
    }
    public String getLongitude() {
        return longitude;
    }
    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }
    public String getLatitude() {
        return latitude;
    }
    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }
    public String getPlotId() {
        return plotId;
    }
    public void setPlotId(String plotId) {
        this.plotId = plotId;
    }
}