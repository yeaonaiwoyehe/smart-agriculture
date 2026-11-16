package cn.agrinav.trajectory.entity;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * @author 11230
 */
@Data
public class Point {
    private double latitude;
    private double longitude;
    private String time;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        Point point = (Point) o;
        return String.valueOf(latitude).equals(String.valueOf(point.latitude)) && String.valueOf(longitude).equals(String.valueOf(point.longitude))&&time.equals(point.time);
    }
}
