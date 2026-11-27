package com.agri.cms.system.service.playback;

import com.agri.cms.common.exception.ServiceException;
import com.agri.cms.system.service.impl.BoundaryClippingService;
import com.agri.cms.system.service.impl.AreaCalculationServiceImpl;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.postgis.Polygon;

import java.sql.SQLException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.agri.cms.system.service.playback.PlaybackModels.Point;
import static com.agri.cms.system.service.playback.PlaybackModels.Track;
import static com.agri.cms.system.service.playback.TrajectoryAnomalyService.*;

/** 固定演示数据只存在于内存，真实接口不会读取这些样例。 */
public class TrajectoryAnomalyDemo {
    private static final String ORDER_ID = "DEMO-B11";
    private static final String PLOT_ID = "DEMO-PLOT";
    private static final OffsetDateTime START = OffsetDateTime.parse("2026-11-27T08:00:00+08:00");
    private static final double LONGITUDE = 114;
    private static final double LATITUDE = 38;
    private static final double METRES_PER_LATITUDE_DEGREE = 111_195;
    private static final double METRES_PER_LONGITUDE_DEGREE =
            METRES_PER_LATITUDE_DEGREE * Math.cos(Math.toRadians(LATITUDE));
    private static final int WIDTH_CM = 300;
    private static final List<List<Double>> RING = List.of(
            List.of(114d, 38d), List.of(114.004, 38d), List.of(114.004, 38.002), List.of(114d, 38.002));

    private final Polygon boundary;
    private final PreparedGeometry field;
    private final AreaCalculationServiceImpl area = new AreaCalculationServiceImpl();

    public record Workorder(Analysis analysis, List<Point> track, List<List<Double>> boundary,
                            List<Integer> breakBefore, int widthCm, BigDecimal landAreaMu,
                            BigDecimal grossAreaMu, BigDecimal effectiveAreaMu, BigDecimal outsideAreaMu) { }

    public TrajectoryAnomalyDemo(BoundaryClippingService clipping) {
        try {
            boundary = new Polygon("SRID=4326;POLYGON((114 38,114.004 38,114.004 38.002,114 38.002,114 38))");
            field = clipping.prepareOuterBoundary(boundary);
        } catch (SQLException invalidFixture) {
            throw new IllegalStateException("演示地块边界无效", invalidFixture);
        }
    }

    public Analysis analyze(String type) {
        return analyze(type, points(type));
    }

    private List<Point> points(String type) {
        boolean archived = "EFFECTIVE".equals(type);
        if (!archived && !"ORIGINAL".equals(type))
            throw new ServiceException("type 须为 ORIGINAL 或 EFFECTIVE");
        List<Point> common = List.of(
                point(20, 50, 0), point(22, 51, 30), point(23, 50, 60), // 停留
                point(50, 60, 70), point(140, 60, 80),                  // 推断超速
                point(80, 100, 100), point(145, 135, 101), point(81, 100, 102)); // 孤立跳点
        List<Point> last = archived
                ? List.of(point(85, 155, 182))
                : List.of(point(85, 155, 182), point(-20, 155, 212), point(-100, 155, 252), point(-20, 155, 292));
        return java.util.stream.Stream.concat(common.stream(), last.stream()).toList();
    }

    private Analysis analyze(String type, List<Point> points) {
        boolean archived = "EFFECTIVE".equals(type);
        var track = new Track(ORDER_ID, PLOT_ID, "DEMO_FIXTURE", List.of(points), points.size());
        var events = detect(track, field, archived ? Kind.ARCHIVE_GAP : Kind.GAP);
        return new Analysis(ORDER_ID, PLOT_ID, type, track.source(), "WGS84", points.get(0).time(),
                points.get(points.size() - 1).time(), points.size(), true, RULES, events,
                List.of("教学演示：此作业单、地块与轨迹均为合成样例",
                        archived ? "有效轨迹样例演示归档断点" : "原始轨迹样例演示漏采、停留、超速、跳点及疑似转场"));
    }

    public Workorder workorder() {
        List<Point> points = points("ORIGINAL");
        Analysis analysis = analyze("ORIGINAL", points);
        var breaks = new HashSet<Integer>();
        for (var event : analysis.events()) {
            if (event.kind() == Kind.GAP) breaks.add(event.end().pointIndex());
            if (event.kind() == Kind.GPS_SPIKE) {
                breaks.add(event.spikePoint().pointIndex());
                breaks.add(event.end().pointIndex());
            }
        }
        List<List<com.agri.cms.system.domain.vo.Point>> paths = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            if (i == 0 || breaks.contains(i)) paths.add(new ArrayList<>());
            Point p = points.get(i);
            paths.get(paths.size() - 1).add(new com.agri.cms.system.domain.vo.Point(p.longitude(), p.latitude()));
        }
        var coverage = area.calculateSegmentedCoverage(paths, WIDTH_CM, boundary, List.of());
        return new Workorder(analysis, points, RING, breaks.stream().sorted().toList(), WIDTH_CM,
                toMu(BigDecimal.valueOf(0.004 * METRES_PER_LONGITUDE_DEGREE * 0.002 * METRES_PER_LATITUDE_DEGREE)),
                toMu(coverage.grossAreaSquareMetres()), coverage.effectiveAreaMu(),
                toMu(coverage.outsideDeductionSquareMetres()));
    }

    private static BigDecimal toMu(BigDecimal squareMetres) {
        return squareMetres.multiply(new BigDecimal("0.0015")).setScale(4, RoundingMode.HALF_UP);
    }

    private static Point point(double eastMetres, double northMetres, long second) {
        return new Point(LONGITUDE + eastMetres / METRES_PER_LONGITUDE_DEGREE,
                LATITUDE + northMetres / METRES_PER_LATITUDE_DEGREE, START.plusSeconds(second).toString());
    }
}
