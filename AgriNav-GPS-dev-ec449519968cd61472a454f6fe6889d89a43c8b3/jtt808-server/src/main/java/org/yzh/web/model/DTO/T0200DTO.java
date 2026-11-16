package org.yzh.web.model.DTO;

import java.time.LocalDateTime;

public class T0200DTO {
    private String clientId;
    private int longitude;
    private int latitude;
    private LocalDateTime deviceTime;


    public T0200DTO() {
    }

    public T0200DTO(String clientId, int longitude, int latitude, LocalDateTime deviceTime) {
        this.clientId = clientId;
        this.longitude = longitude;
        this.latitude = latitude;
        this.deviceTime = deviceTime;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public int getLongitude() {
        return longitude;
    }

    public void setLongitude(int longitude) {
        this.longitude = longitude;
    }

    public int getLatitude() {
        return latitude;
    }

    public void setLatitude(int latitude) {
        this.latitude = latitude;
    }

    public LocalDateTime getDeviceTime() {
        return deviceTime;
    }

    public void setDeviceTime(LocalDateTime deviceTime) {
        this.deviceTime = deviceTime;
    }

    @Override
    public String toString() {
        return "T0200DTO{" +
                "clientId='" + clientId + '\'' +
                ", longitude=" + longitude +
                ", latitude=" + latitude +
                ", deviceTime=" + deviceTime +
                '}';
    }
}
