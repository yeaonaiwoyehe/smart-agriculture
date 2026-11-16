package com.agri.cms.system.utils;

import com.agri.cms.system.domain.vo.Point;
import org.postgis.LineString;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * LineString工具类
 * @author 姚锦伦
 */
public class LineStringUtils {

    /**
     * 将List<Point>转换为LineString
     * @param points 点集合
     * @return LineString对象
     */
    public static LineString createLineString(List<Point> points) {
        if (points == null || points.isEmpty()) {
            return null;
        }

        try {
            org.postgis.Point[] linePoints = points.stream()
                .map(p -> {
                    org.postgis.Point point = new org.postgis.Point(
                        p.getLongitude(), 
                        p.getLatitude()
                    );
                    point.setSrid(4326);
                    return point;
                })
                .toArray(org.postgis.Point[]::new);

            LineString lineString = new LineString(linePoints);
            lineString.setSrid(4326);
            return lineString;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create LineString", e);
        }
    }

    /**
     * 将LineString转换为List<Point>
     * @param lineString LineString对象
     * @return 点集合
     */
    public static List<Point> lineStringToPoints(LineString lineString) {
        if (lineString == null) {
            return Collections.emptyList();
        }

        return Arrays.stream(lineString.getPoints())
            .map(p -> new Point(p.x, p.y))
            .collect(Collectors.toList());
    }

    /**
     * 将LineString转换为带时间的List<Point>
     * @param lineString LineString对象
     * @param times 时间字符串，格式如："2024-10-13T12:46:42,2024-10-13T12:46:47"
     * @return 带时间的点集合
     */
    public static List<Point> lineStringToPointsWithTime(LineString lineString, String times) {
        if (lineString == null || times == null || times.isEmpty()) {
            return Collections.emptyList();
        }

        List<Point> points = new ArrayList<>();
        org.postgis.Point[] linePoints = lineString.getPoints();
        String[] timeArray = times.split(",");

        // 确保点和时间的数量匹配
        int minLength = Math.min(linePoints.length, timeArray.length);

        for (int i = 0; i < minLength; i++) {
            Point point = new Point(
                linePoints[i].x,
                linePoints[i].y,
                LocalDateTime.parse(timeArray[i])
            );
            points.add(point);
        }

        return points;
    }

    /**
     * 获取LineString的点数量
     * @param lineString LineString对象
     * @return 点的数量
     */
    public static int getPointCount(LineString lineString) {
        if (lineString == null) {
            return 0;
        }
        return lineString.numPoints();
    }

    /**
     * 检查LineString是否有效
     * @param lineString LineString对象
     * @return 是否有效
     */
    public static boolean isValid(LineString lineString) {
        return lineString != null && lineString.numPoints() >= 2;
    }
}