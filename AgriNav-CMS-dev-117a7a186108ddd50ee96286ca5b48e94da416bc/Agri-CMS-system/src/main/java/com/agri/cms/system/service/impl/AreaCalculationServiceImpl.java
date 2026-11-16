package com.agri.cms.system.service.impl;

import com.agri.cms.system.domain.TrajectorySegment;
import com.agri.cms.system.domain.vo.Point;
import com.agri.cms.system.domain.vo.Trajectory;
import org.springframework.stereotype.Service;

import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * @author 姚锦伦
 */
@Service
public class AreaCalculationServiceImpl {
    /**
     * 地球半径，单位：米
     */
    private static final double EARTH_RADIUS = 6371000;
    private static final int MIN_POINTS = 2;
    final double DISTANCE_TOLERANCE = 1.0;
    // 方位角容差
    final double BEARING_TOLERANCE = 5.0;


    public BigDecimal calculateTrajectoryArea(Trajectory trajectory) {
        List<Point> points = trajectory.getPoints();
        // 将厘米转换为米
        double widthInMeters = trajectory.getWidth() / 100.0;

        if (points.size() < MIN_POINTS) {
            return BigDecimal.ZERO;
        }

        double totalDistance = 0;
        for (int i = 0; i < points.size() - 1; i++) {
            totalDistance += calculateDistance(points.get(i), points.get(i + 1));
        }

        double areaInSquareMeters = totalDistance * widthInMeters;

        return BigDecimal.valueOf(areaInSquareMeters).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 面积去重
     * @return BigDecimal
     * @author 姚锦伦
     * @since 2024/10/29
     */

    public BigDecimal calculateTrajectoryAreaAndDuplicateRemoval(List<Point> points, int width) {
        if (points.size() < MIN_POINTS) {
            return BigDecimal.ZERO;
        }

        // 将厘米转换成米
        double widthInMeters = width / 100.0;

        // 将轨迹分段并检测重叠
        List<TrajectorySegment> segments = createSegments(points);
        List<TrajectorySegment> uniqueSegments = removeOverlappingSegments(segments);

        // 计算非重叠段的总面积
        double totalArea = uniqueSegments.stream()
                .mapToDouble(segment -> segment.getDistance() * widthInMeters)
                .sum();

        return BigDecimal.valueOf(totalArea).setScale(2, RoundingMode.HALF_UP);
    }

    private List<TrajectorySegment> createSegments(List<Point> points) {
        List<TrajectorySegment> segments = new ArrayList<>();
        for (int i = 0; i < points.size() - 1; i++) {
            Point start = points.get(i);
            Point end = points.get(i + 1);
            //计算两点之间的距离
            double distance = calculateDistance(start, end);
            //计算两点之间的方位角
            double bearing = calculateBearing(start, end);
            //构建轨迹片段对象
            segments.add(new TrajectorySegment(start, end, distance, bearing));
        }
        return segments;
    }

    private List<TrajectorySegment> removeOverlappingSegments(List<TrajectorySegment> segments) {
        List<TrajectorySegment> uniqueSegments = new ArrayList<>();

        for (TrajectorySegment currentSegment : segments) {
            boolean isOverlapping = false;

            // 检查当前段是否与已有的任何段重叠
            for (TrajectorySegment existingSegment : uniqueSegments) {
                if (areSegmentsOverlapping(currentSegment, existingSegment)) {
                    isOverlapping = true;
                    break;
                }
            }

            if (!isOverlapping) {
                uniqueSegments.add(currentSegment);
            }
        }

        return uniqueSegments;
    }

    private boolean areSegmentsOverlapping(TrajectorySegment seg1, TrajectorySegment seg2) {
        //计算两个轨迹段的方位角之差
        double bearingDiff = Math.abs(seg1.getBearing() - seg2.getBearing());
        // 需要考虑360度循环的情况
        if (bearingDiff > 180) {
            bearingDiff = 360 - bearingDiff;
        }
        //判断方位角是否小于容限，即判断是否是平行或者反向平行
        boolean isParallel = bearingDiff < BEARING_TOLERANCE ||
                Math.abs(180 - bearingDiff) < BEARING_TOLERANCE;

        if (!isParallel) {
            return false;
        }

        // 计算两个线段的端点到另一条线段的最小距离
        double minDistance = Math.min(
                Math.min(pointToSegmentDistance(seg1.getStart(), seg2.getStart(), seg2.getEnd()),
                        pointToSegmentDistance(seg1.getEnd(), seg2.getStart(), seg2.getEnd())),
                Math.min(pointToSegmentDistance(seg2.getStart(), seg1.getStart(), seg1.getEnd()),
                        pointToSegmentDistance(seg2.getEnd(), seg1.getStart(), seg1.getEnd()))
        );

        // 添加重叠长度判断
        if (minDistance < DISTANCE_TOLERANCE) {
            // 计算投影重叠长度
            double overlap = calculateOverlapLength(seg1, seg2);
            // 只有当重叠长度超过较短线段长度的20%时才认为重叠
            return overlap > Math.min(seg1.getDistance(), seg2.getDistance()) * 0.2;
        }

        return false;
    }

    private double pointToSegmentDistance(Point p, Point segStart, Point segEnd) {
        double x = p.getLongitude();
        double y = p.getLatitude();
        double x1 = segStart.getLongitude();
        double y1 = segStart.getLatitude();
        double x2 = segEnd.getLongitude();
        double y2 = segEnd.getLatitude();

        double a = x - x1;
        double b = y - y1;
        double c = x2 - x1;
        double d = y2 - y1;

        double dot = a * c + b * d;
        double lenSq = c * c + d * d;
        double param = -1;

        if (lenSq != 0) {
            param = dot / lenSq;
        }

        double xx, yy;

        if (param < 0) {
            xx = x1;
            yy = y1;
        } else if (param > 1) {
            xx = x2;
            yy = y2;
        } else {
            xx = x1 + param * c;
            yy = y1 + param * d;
        }

        Point projectionPoint = new Point();
        projectionPoint.setLongitude(xx);
        projectionPoint.setLatitude(yy);

        return calculateDistance(p, projectionPoint);
    }

    private double calculateBearing(Point start, Point end) {
        //起点的纬度(弧度)
        double lat1 = Math.toRadians(start.getLatitude());
        //终点的纬度(弧度)
        double lat2 = Math.toRadians(end.getLatitude());
        //起点与终点的经度差(弧度)
        double dLon = Math.toRadians(end.getLongitude() - start.getLongitude());
        //计算两点之间的方位角的前置计算
        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) -
                Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);
        //计算方位角将弧度转为度
        return (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
    }


    private double calculateOverlapLength(TrajectorySegment seg1, TrajectorySegment seg2) {
        // 计算两个线段的投影重叠长度
        Point[] projPoints = new Point[]{
                seg1.getStart(), seg1.getEnd(),
                seg2.getStart(), seg2.getEnd()
        };

        // 将点投影到第一条线段方向上
        double[] projDistances = Arrays.stream(projPoints)
                .mapToDouble(p -> calculateProjectedDistance(p, seg1.getStart(), seg1.getBearing()))
                .sorted()
                .toArray();

        // 返回中间两个点之间的距离
        return Math.max(0, projDistances[2] - projDistances[1]);
    }

    private double calculateProjectedDistance(Point p, Point start, double bearing) {
        double distance = calculateDistance(start, p);
        double pointBearing = calculateBearing(start, p);
        return distance * Math.cos(Math.toRadians(pointBearing - bearing));
    }

    private double calculateDistance(Point p1, Point p2) {
        //将经纬度转换为弧度
        double lat1 = Math.toRadians(p1.getLatitude());
        double lon1 = Math.toRadians(p1.getLongitude());
        double lat2 = Math.toRadians(p2.getLatitude());
        double lon2 = Math.toRadians(p2.getLongitude());

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;
        //使用haversine公式计算距离
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }
}