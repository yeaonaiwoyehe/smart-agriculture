package com.agri.cms.system.utils;

import com.agri.cms.system.domain.vo.Point;
import org.postgis.LinearRing;
import org.postgis.Polygon;

import java.util.ArrayList;
import java.util.List;

/**
 * 多边形工具类，用于判断点是否在多边形内并过滤点集
 */

/**
 * 多边形工具类
 *
 * @author mfr
 * @since 2024/10/12
 */
public class PolygonUtil {

    private static final double EPSILON = 1e-12; // 提高精度

    /**
     * 线段类，表示多边形的一条边
     */
    private static class LineSegment {
        Point start, end;

        LineSegment(Point start, Point end) {
            this.start = start;
            this.end = end;
        }
    }

    /**
     * 矩形类，用于R树的边界框
     */
    private static class Rectangle {
        double minLat, minLon, maxLat, maxLon;

        Rectangle(double minLat, double minLon, double maxLat, double maxLon) {
            this.minLat = minLat;
            this.minLon = minLon;
            this.maxLat = maxLat;
            this.maxLon = maxLon;
        }

        boolean contains(Point p) {
            return p.getLatitude() >= minLat - EPSILON && p.getLatitude() <= maxLat + EPSILON &&
                    p.getLongitude() >= minLon - EPSILON && p.getLongitude() <= maxLon + EPSILON;
        }

        boolean intersects(Rectangle other) {
            return !(other.minLat > maxLat + EPSILON || other.maxLat < minLat - EPSILON ||
                    other.minLon > maxLon + EPSILON || other.maxLon < minLon - EPSILON);
        }
    }

    /**
     * R树节点类
     */
    private static class RTreeNode {
        Rectangle bounds;
        List<RTreeNode> children;
        LineSegment segment;

        RTreeNode(Rectangle bounds) {
            this.bounds = bounds;
            this.children = new ArrayList<>();
        }

        RTreeNode(LineSegment segment) {
            this.segment = segment;
            this.bounds = new Rectangle(
                    Math.min(segment.start.getLatitude(), segment.end.getLatitude()),
                    Math.min(segment.start.getLongitude(), segment.end.getLongitude()),
                    Math.max(segment.start.getLatitude(), segment.end.getLatitude()),
                    Math.max(segment.start.getLongitude(), segment.end.getLongitude())
            );
        }
    }

    private static RTreeNode root;

    /**
     * 过滤点集，只保留多边形内的点
     *
     * @param polygonWkt WKT格式的多边形字符串
     * @param points     待过滤的点集
     * @return 位于多边形内的点集
     */
    public static List<Point> filterPointsInPolygon(String polygonWkt, List<Point> points) {
        List<Point> polygon = parsePolygonWkt(polygonWkt);
        if (polygon == null || polygon.size() < 3) {
            throw new IllegalArgumentException("无效的多边形：至少需要3个点");
        }
        initPolygon(polygon);
        List<Point> filteredPoints = new ArrayList<>();
        for (Point point : points) {
            if (point != null && isPointInPolygon(point)) {
                filteredPoints.add(point);
            }
        }
        return filteredPoints;
    }

    /**
     * 解析WKT格式的POLYGON字符串
     *
     * @param polygonWkt WKT格式的多边形字符串
     * @return 多边形的顶点列表
     */
    private static List<Point> parsePolygonWkt(String polygonWkt) {
        if (polygonWkt == null || !polygonWkt.startsWith("POLYGON")) {
            throw new IllegalArgumentException("无效的WKT格式");
        }
        List<Point> polygon = new ArrayList<>();
        String coordinates = polygonWkt.substring(polygonWkt.indexOf("((") + 2, polygonWkt.indexOf("))"));
        String[] points = coordinates.split(",");
        for (String point : points) {
            String[] latLon = point.trim().split(" ");
            if (latLon.length != 2) {
                throw new IllegalArgumentException("无效的坐标格式");
            }
            try {
                double latitude = Double.parseDouble(latLon[0]);
                double longitude = Double.parseDouble(latLon[1]);
                polygon.add(new Point(longitude, latitude));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("无效的坐标值", e);
            }
        }
        return polygon;
    }

    /**
     * 初始化多边形数据
     *
     * @param polygon 多边形的顶点列表
     */
    private static void initPolygon(List<Point> polygon) {
        if (polygon == null || polygon.size() < 3) {
            throw new IllegalArgumentException("多边形至少需要3个有效点");
        }
        buildRTree(polygon);
    }

    /**
     * 构建R树
     *
     * @param polygon 多边形的顶点列表
     */
    private static void buildRTree(List<Point> polygon) {
        List<RTreeNode> leafNodes = new ArrayList<>();
        for (int i = 0; i < polygon.size(); i++) {
            Point start = polygon.get(i);
            Point end = polygon.get((i + 1) % polygon.size());
            if (start != null && end != null) {
                leafNodes.add(new RTreeNode(new LineSegment(start, end)));
            }
        }
        if (leafNodes.isEmpty()) {
            throw new IllegalStateException("无法构建R树：没有有效的边");
        }
        root = buildRTreeLevel(leafNodes);
    }

    private static RTreeNode buildRTreeLevel(List<RTreeNode> nodes) {
        if (nodes.size() == 1) {
            return nodes.get(0);
        }
        List<RTreeNode> parents = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i += 4) {
            RTreeNode parent = new RTreeNode(nodes.get(i).bounds);
            for (int j = i; j < Math.min(i + 4, nodes.size()); j++) {
                parent.children.add(nodes.get(j));
                parent.bounds = expandRectangle(parent.bounds, nodes.get(j).bounds);
            }
            parents.add(parent);
        }
        return buildRTreeLevel(parents);
    }

    private static Rectangle expandRectangle(Rectangle r1, Rectangle r2) {
        return new Rectangle(
                Math.min(r1.minLat, r2.minLat),
                Math.min(r1.minLon, r2.minLon),
                Math.max(r1.maxLat, r2.maxLat),
                Math.max(r1.maxLon, r2.maxLon)
        );
    }

    /**
     * 判断点是否在多边形内
     *
     * @param point 待检查的点
     * @return 是否在多边形内
     */
    private static boolean isPointInPolygon(Point point) {
        if (point == null) {
            return false;
        }
        if (isPointOnBoundary(point)) {
            return true;
        }
        return isPointInsidePolygon(point);
    }

    private static boolean isPointOnBoundary(Point point) {
        List<LineSegment> candidates = new ArrayList<>();
        searchRTree(root, point, candidates);
        for (LineSegment segment : candidates) {
            if (distanceToSegment(point, segment) < EPSILON) {
                return true;
            }
        }
        return false;
    }

    private static void searchRTree(RTreeNode node, Point point, List<LineSegment> candidates) {
        if (node == null || !node.bounds.contains(point)) {
            return;
        }
        if (node.segment != null) {
            candidates.add(node.segment);
        } else if (node.children != null) {
            for (RTreeNode child : node.children) {
                searchRTree(child, point, candidates);
            }
        }
    }

    private static double distanceToSegment(Point p, LineSegment segment) {
        double x = p.getLongitude();
        double y = p.getLatitude();
        double x1 = segment.start.getLongitude();
        double y1 = segment.start.getLatitude();
        double x2 = segment.end.getLongitude();
        double y2 = segment.end.getLatitude();

        double A = x - x1;
        double B = y - y1;
        double C = x2 - x1;
        double D = y2 - y1;

        double dot = A * C + B * D;
        double len_sq = C * C + D * D;
        double param = -1;
        if (len_sq != 0) {
            param = dot / len_sq;
        }

        double xx, yy;

        if (param < 0) {
            xx = x1;
            yy = y1;
        } else if (param > 1) {
            xx = x2;
            yy = y2;
        } else {
            xx = x1 + param * C;
            yy = y1 + param * D;
        }

        double dx = x - xx;
        double dy = y - yy;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private static boolean isPointInsidePolygon(Point point) {
        if (root == null || root.children == null || root.children.isEmpty()) {
            throw new IllegalStateException("多边形未正确初始化");
        }

        int crossings = 0;
        List<RTreeNode> nodesToCheck = new ArrayList<>(root.children);

        while (!nodesToCheck.isEmpty()) {
            RTreeNode node = nodesToCheck.remove(0);

            if (node.children != null && !node.children.isEmpty()) {
                // 如果是中间节点，将其子节点添加到待检查列表
                nodesToCheck.addAll(node.children);
                continue;
            }

            if (node.segment == null) {
                // 跳过没有segment的叶子节点
                continue;
            }

            Point p1 = node.segment.start;
            Point p2 = node.segment.end;

            if (p1 == null || p2 == null) {
                // 跳过无效的线段
                continue;
            }

            if (((p1.getLatitude() <= point.getLatitude() && point.getLatitude() < p2.getLatitude()) ||
                    (p2.getLatitude() <= point.getLatitude() && point.getLatitude() < p1.getLatitude())) &&
                    (point.getLongitude() < (p2.getLongitude() - p1.getLongitude()) * (point.getLatitude() - p1.getLatitude()) /
                            (p2.getLatitude() - p1.getLatitude()) + p1.getLongitude())) {
                crossings++;
            }
        }

        return (crossings % 2 != 0);
    }

    /**
     * 将polygon转换为List<Point>
     *
     * @param polygon
     * @return List<Point>
     * @author 姚锦伦
     * @since 2024/10/16
     */
    public static List<Point> polygonToListPoint(Polygon polygon) {
        List<Point> points = new ArrayList<>();
        LinearRing ring = polygon.getRing(0);
        org.postgis.Point[] pgPoints = ring.getPoints();
        for (org.postgis.Point pgPoint : pgPoints) {
            Point point = new Point(pgPoint.y, pgPoint.x);
            points.add(point);
        }
        return points;
    }
}