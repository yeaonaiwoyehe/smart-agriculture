package com.agri.cms.system.domain.vo;


import java.time.LocalDateTime;

public class Point {
    private double longitude;
    private double latitude;

    private LocalDateTime time;

    public Point(double longitude, double latitude, LocalDateTime time) {
        this.longitude = longitude;
        this.latitude = latitude;
        this.time = time;
    }

    public Point() {
    }

    public Point(double longitude, double latitude) {
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return "Point{" +
                " longitude=" + longitude +
                ", latitude=" + latitude +
                ", time=" + time +
                '}';
    }
}
