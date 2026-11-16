package com.agri.cms.system.domain;

import com.agri.cms.system.domain.vo.Point;

/**
 * @author 姚锦伦
 */
public class TrajectorySegment {
    /**
     * 起点
     */
    private Point start;
    /**
     * 终点
     */
    private Point end;
    /**
     * 距离
     */
    private double distance;
    /**
     * 方位角
     */
    private double bearing;

    public Point getStart() {
        return start;
    }

    public void setStart(Point start) {
        this.start = start;
    }

    public Point getEnd() {
        return end;
    }

    public void setEnd(Point end) {
        this.end = end;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public double getBearing() {
        return bearing;
    }

    public void setBearing(double bearing) {
        this.bearing = bearing;
    }

    public TrajectorySegment(Point start, Point end, double distance, double bearing) {
        this.start = start;
        this.end = end;
        this.distance = distance;
        this.bearing = bearing;
    }
}
