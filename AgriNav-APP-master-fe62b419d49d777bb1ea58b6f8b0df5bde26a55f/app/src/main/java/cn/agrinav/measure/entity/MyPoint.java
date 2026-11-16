package cn.agrinav.measure.entity;

import androidx.annotation.NonNull;

import java.time.LocalDateTime;

/**
 * @author 18506
 * 后端需要的点
 */
public class MyPoint{
    private double x;
    // 经度

    private double y;
    // 纬度

    private LocalDateTime createdAt;
    // 创建时间

    public MyPoint(double x, double y) {
        this.x = x;
        this.y = y;
    }
    @NonNull
    @Override
    public String toString() {
        return "MyPoint{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }

    public MyPoint() {}

    public MyPoint(double x, double y, LocalDateTime createdAt){

        this.x = x;
        this.y = y;
        this.createdAt = createdAt;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

}
