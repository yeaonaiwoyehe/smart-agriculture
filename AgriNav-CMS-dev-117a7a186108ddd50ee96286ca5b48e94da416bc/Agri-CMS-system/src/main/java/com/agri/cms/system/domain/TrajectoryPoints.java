package com.agri.cms.system.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.postgis.Polygon;

/**
 * @Author: 姚锦伦
 * @CreateTime: 2024-09-26
 * @Description:
 * @Version: 1.0
 */
@TableName("trajectory_points")
public class TrajectoryPoints {
    @TableId(value = "trajectory_id")
    private String trajectoryId;
    @TableField(value = "points")
    private Polygon points;
    private String times;

    public String getTrajectoryId() {
        return trajectoryId;
    }

    public void setTrajectoryId(String trajectoryId) {
        this.trajectoryId = trajectoryId;
    }

    public Polygon getPoints() {
        return points;
    }

    public void setPoints(Polygon points) {
        this.points = points;
    }

    public String getTimes() {
        return times;
    }

    public void setTimes(String times) {
        this.times = times;
    }
}
