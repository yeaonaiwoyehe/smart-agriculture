package com.agri.cms.system.domain;

import com.agri.cms.system.domain.vo.Point;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.postgis.LineString;

import java.util.List;

/**
 * @author 姚锦伦
 */
public class TrajectoryExclude {
    /**
     * id
     */
    private String id;
    /**
     * 所属轨迹id
     */
    private String trajectoryInfoId;
    /**
     * 轨迹点集
     */
    @TableField(value = "points")
    private LineString points;
    /**
     * 时间集
     */
    private String times;
    @TableField(exist = false)
    private List<Point> pointsExclude;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTrajectoryInfoId() {
        return trajectoryInfoId;
    }

    public void setTrajectoryInfoId(String trajectoryInfoId) {
        this.trajectoryInfoId = trajectoryInfoId;
    }

    public LineString getPoints() {
        return points;
    }

    public void setPoints(LineString points) {
        this.points = points;
    }

    public String getTimes() {
        return times;
    }

    public void setTimes(String times) {
        this.times = times;
    }

    public List<Point> getPointsExclude() {
        return pointsExclude;
    }

    public void setPointsExclude(List<Point> pointsExclude) {
        this.pointsExclude = pointsExclude;
    }

    @Override
    public String toString() {
        return "TrajectoryExclude{" +
                "id='" + id + '\'' +
                ", trajectoryInfoId='" + trajectoryInfoId + '\'' +
                ", points=" + points +
                ", times='" + times + '\'' +
                ", pointsExclude=" + pointsExclude +
                '}';
    }
}
