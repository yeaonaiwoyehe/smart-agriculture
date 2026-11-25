package com.agri.cms.system.service.impl;

import com.agri.cms.system.domain.vo.Point;
import com.agri.cms.system.domain.vo.Trajectory;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.CoordinateFilter;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.operation.union.UnaryUnionOp;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 作业覆盖面积，输入为 WGS84 经纬度和厘米幅宽，返回平方米。
 * 每个采样段建平头幅宽矩形，取并集后裁剪；不按阈值丢弃整段。
 * 使用以首个定位点为中心的球面等距方位投影在米制平面计算，适用于局部田块。
 * 这是轨迹覆盖模型；幅宽、定位误差及田间精度仍需实测校准。
 */
@Service
public class AreaCalculationServiceImpl {
    private static final double EARTH_RADIUS = 6371000;
    private static final GeometryFactory GEOMETRIES = new GeometryFactory();

    /** 原始长度乘幅宽，保留重复计量的原有接口语义。 */
    public BigDecimal calculateTrajectoryArea(Trajectory trajectory) {
        if (trajectory == null) {
            throw new IllegalArgumentException("轨迹不能为空");
        }
        validate(trajectory.getPoints(), trajectory.getWidth());
        double length = 0;
        for (int i = 1; i < trajectory.getPoints().size(); i++) {
            length += distance(trajectory.getPoints().get(i - 1), trajectory.getPoints().get(i));
        }
        return squareMetres(length * trajectory.getWidth() / 100.0);
    }

    public BigDecimal calculateTrajectoryAreaAndDuplicateRemoval(List<Point> points, int width) {
        return calculateCoverage(points, width, null, Collections.emptyList(), false);
    }

    /** 边界为必填；空洞和额外排除区均只扣除一次，坐标系须与轨迹一致。 */
    public BigDecimal calculateClippedArea(List<Point> points, int width,
                                         org.postgis.Polygon boundary,
                                         List<org.postgis.Polygon> exclusions) {
        return calculateCoverage(points, width, boundary, exclusions, true);
    }

    private BigDecimal calculateCoverage(List<Point> points, int width,
                                        org.postgis.Polygon boundary,
                                        List<org.postgis.Polygon> exclusions, boolean clip) {
        validate(points, width);
        return segmentedCoverage(List.of(points), width, boundary, exclusions, clip).effectiveAreaSquareMetres();
    }

    /** B-10：各片段内构建幅宽带，片段间不连线，全部幅宽带统一去重。 */
    public CoverageBreakdown calculateSegmentedCoverage(List<List<Point>> segments, int width,
                                                        org.postgis.Polygon boundary,
                                                        List<org.postgis.Polygon> exclusions) {
        return segmentedCoverage(segments, width, boundary, exclusions, true);
    }

    public record CoverageBreakdown(BigDecimal travelLengthMetres, BigDecimal grossAreaSquareMetres,
                                    BigDecimal uniqueAreaSquareMetres, BigDecimal inBoundaryAreaSquareMetres,
                                    BigDecimal overlapDeductionSquareMetres, BigDecimal outsideDeductionSquareMetres,
                                    BigDecimal exclusionDeductionSquareMetres, BigDecimal effectiveAreaSquareMetres,
                                    BigDecimal effectiveAreaMu) { }

    private CoverageBreakdown segmentedCoverage(List<List<Point>> segments, int width,
                                                org.postgis.Polygon boundary,
                                                List<org.postgis.Polygon> exclusions, boolean clip) {
        if (segments == null || width <= 0) {
            throw new IllegalArgumentException("轨迹片段不能为空引用，幅宽必须为正数（厘米）");
        }
        for (List<Point> segment : segments) validate(segment, width);
        if (clip && boundary == null) {
            throw new IllegalArgumentException("地块边界不能为空");
        }
        if (exclusions == null) {
            throw new IllegalArgumentException("排除区域列表不能为空引用");
        }
        // 即使轨迹不足两个点也校验输入几何，避免把坏数据当作正常零面积。
        Point origin = segments.stream().filter(segment -> !segment.isEmpty())
                .findFirst().map(segment -> segment.get(0)).orElse(null);
        if (origin == null && boundary != null) {
            try {
                Coordinate first = new WKTReader(GEOMETRIES).read(boundary.toString()
                        .replaceFirst("^SRID=[0-9]+;", "")).getCoordinate();
                if (first == null) throw new IllegalArgumentException("地块边界为空");
                origin = new Point(first.x, first.y);
                validateCoordinate(first.x, first.y);
            } catch (ParseException invalid) {
                throw new IllegalArgumentException("地块边界无法解析", invalid);
            }
        }
        if (origin == null) origin = new Point(0, 0);
        Projection projection = new Projection(origin);
        Geometry field = clip ? projectPolygon(boundary, projection) : null;
        List<Geometry> excluded = new ArrayList<>();
        for (org.postgis.Polygon polygon : exclusions) {
            excluded.add(projectPolygon(polygon, projection));
        }
        List<Geometry> strips = new ArrayList<>();
        double travelLength = 0;
        double halfWidth = width / 200.0;
        for (List<Point> points : segments) {
          if (points.isEmpty()) continue;
          Coordinate start = projection.project(points.get(0).getLongitude(), points.get(0).getLatitude());
          for (int i = 1; i < points.size(); i++) {
            Coordinate end = projection.project(points.get(i).getLongitude(), points.get(i).getLatitude());
            double dx = end.x - start.x;
            double dy = end.y - start.y;
            double length = Math.hypot(dx, dy);
            if (length > 1e-8) {
                travelLength += length;
                double nx = -dy / length * halfWidth;
                double ny = dx / length * halfWidth;
                strips.add(GEOMETRIES.createPolygon(new Coordinate[]{
                        new Coordinate(start.x + nx, start.y + ny),
                        new Coordinate(end.x + nx, end.y + ny),
                        new Coordinate(end.x - nx, end.y - ny),
                        new Coordinate(start.x - nx, start.y - ny),
                        new Coordinate(start.x + nx, start.y + ny)}));
            }
            start = end;
          }
        }
        if (strips.isEmpty()) {
            return breakdown(0, 0, 0, 0, 0);
        }
        Geometry coverage = UnaryUnionOp.union(strips);
        double gross = travelLength * width / 100.0;
        double unique = Math.min(gross, coverage.getArea());
        if (field != null) {
            coverage = coverage.intersection(field);
        }
        double inBoundary = Math.min(unique, coverage.getArea());
        if (!excluded.isEmpty()) {
            coverage = coverage.difference(UnaryUnionOp.union(excluded));
        }
        return breakdown(travelLength, gross, unique, inBoundary, Math.min(inBoundary, coverage.getArea()));
    }

    private CoverageBreakdown breakdown(double length, double gross, double unique, double inside, double effective) {
        BigDecimal raw = squareMetres(gross), union = squareMetres(unique), field = squareMetres(inside);
        BigDecimal result = squareMetres(effective);
        // 先舍入面积阶段值再相减，使展示的扣减项能够精确对账。
        return new CoverageBreakdown(BigDecimal.valueOf(length).setScale(3, RoundingMode.HALF_UP),
                raw, union, field, raw.subtract(union), union.subtract(field), field.subtract(result), result,
                result.multiply(new BigDecimal("0.0015")).setScale(4, RoundingMode.HALF_UP));
    }

    Geometry projectPolygon(org.postgis.Polygon polygon, Projection projection) {
        if (polygon == null || (polygon.getSrid() != 0 && polygon.getSrid() != 4326)) {
            throw new IllegalArgumentException("地块边界必须使用 WGS84 / SRID 4326");
        }
        try {
            // 兼容已有未标 SRID 的 Polygon，拒绝已明确标注为其他坐标系的数据。
            Geometry geometry = new WKTReader(GEOMETRIES)
                    .read(polygon.toString().replaceFirst("^SRID=[0-9]+;", ""));
            if (geometry.isEmpty() || !geometry.isValid()) {
                throw new IllegalArgumentException("地块边界为空或几何无效");
            }
            geometry.apply((CoordinateFilter) coordinate -> {
                validateCoordinate(coordinate.x, coordinate.y);
                Coordinate metric = projection.project(coordinate.x, coordinate.y);
                coordinate.x = metric.x;
                coordinate.y = metric.y;
            });
            geometry.geometryChanged();
            if (!geometry.isValid()) {
                throw new IllegalArgumentException("地块投影后几何无效");
            }
            return geometry;
        } catch (ParseException exception) {
            throw new IllegalArgumentException("地块边界无法解析", exception);
        }
    }

    private void validate(List<Point> points, int width) {
        if (points == null || width <= 0) {
            throw new IllegalArgumentException("轨迹点列表不能为空引用，幅宽必须为正数（厘米）");
        }
        for (Point point : points) {
            if (point == null) {
                throw new IllegalArgumentException("轨迹中不能包含空定位点");
            }
            validateCoordinate(point.getLongitude(), point.getLatitude());
        }
    }

    private static void validateCoordinate(double longitude, double latitude) {
        if (!Double.isFinite(longitude) || !Double.isFinite(latitude)
                || longitude < -180 || longitude > 180 || latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("经纬度必须为有效 WGS84 坐标");
        }
    }

    private BigDecimal squareMetres(double area) {
        if (!Double.isFinite(area) || area < 0) {
            throw new IllegalArgumentException("面积计算结果无效");
        }
        return BigDecimal.valueOf(area).setScale(2, RoundingMode.HALF_UP);
    }

    private double distance(Point first, Point second) {
        double lat1 = Math.toRadians(first.getLatitude());
        double lat2 = Math.toRadians(second.getLatitude());
        double dLat = lat2 - lat1;
        double dLon = Math.toRadians(second.getLongitude() - first.getLongitude());
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dLon / 2), 2);
        a = Math.max(0, Math.min(1, a));
        return 2 * EARTH_RADIUS * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    static class Projection {
        private final double longitude;
        private final double sinLatitude;
        private final double cosLatitude;
        Projection(Point origin) {
            longitude = Math.toRadians(origin.getLongitude());
            sinLatitude = Math.sin(Math.toRadians(origin.getLatitude()));
            cosLatitude = Math.cos(Math.toRadians(origin.getLatitude()));
        }
        Coordinate project(double lon, double lat) {
            double latitude = Math.toRadians(lat);
            double delta = Math.toRadians(lon) - longitude;
            double cosAngle = sinLatitude * Math.sin(latitude)
                    + cosLatitude * Math.cos(latitude) * Math.cos(delta);
            double angle = Math.acos(Math.max(-1, Math.min(1, cosAngle)));
            // 本地田块投影不接受跨半球轨迹，避免投影奇点产生错误结果。
            if (angle >= Math.PI / 2) {
                throw new IllegalArgumentException("轨迹或地块超出局部面积核算范围");
            }
            double scale = angle < 1e-8 ? 1 : angle / Math.sin(angle);
            return new Coordinate(EARTH_RADIUS * scale * Math.cos(latitude) * Math.sin(delta),
                    EARTH_RADIUS * scale * (cosLatitude * Math.sin(latitude)
                            - sinLatitude * Math.cos(latitude) * Math.cos(delta)));
        }
    }
}
