package com.agri.cms.system.service.playback;

import com.agri.cms.common.exception.ServiceException;
import com.agri.cms.system.service.boundary.BoundaryQueryService;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.agri.cms.system.service.playback.PlaybackModels.*;

/** B-11：对授权回放数据生成可复核的异常标记。 */
@Service
public class TrajectoryAnomalyService {
    static final Rules RULES = new Rules(60, 60, 5, 20, 30, 3, 30, 5);
    private static final GeometryFactory GEOMETRIES = new GeometryFactory();

    public enum Kind { GAP, ARCHIVE_GAP, STOP, OVERSPEED, GPS_SPIKE, TRANSFER_CANDIDATE }
    public record Rules(int gapSeconds, int stopSeconds, int stopRadiusMetres,
                        int overspeedKmh, int transferMetres, int spikeLegMaxSeconds,
                        int spikeLegMinMetres, int spikeReturnRadiusMetres) { }
    public record PointRef(int segmentIndex, int pointIndex, Point point) { }
    public record Event(Kind kind, PointRef start, PointRef end, double durationSeconds,
                        Double displacementMetres, Double inferredSpeedKmh,
                        Double maxDisplacementMetres, PointRef spikePoint) { }
    public record Analysis(String trajectoryId, String plotId, String type, String source,
                           String coordinateSystem, String startTime, String endTime, int pointCount,
                           boolean transferChecked, Rules rules, List<Event> events, List<String> warnings) { }

    private final TrajectoryPlaybackService playback;
    private final BoundaryQueryService boundaries;

    public TrajectoryAnomalyService(TrajectoryPlaybackService playback, BoundaryQueryService boundaries) {
        this.playback = playback;
        this.boundaries = boundaries;
    }

    public Analysis analyze(Query query, Actor actor) {
        if (query == null || query.trajectoryId() == null || query.trajectoryId().isBlank())
            throw new ServiceException("异常识别需要指定作业单号");
        Result replay = playback.query(query, actor);
        List<String> warnings = new ArrayList<>(replay.warnings());
        warnings.add("异常按当前规则从采样点推断，供人工复核；不会修改轨迹或核算结果");
        Kind gapKind = "EFFECTIVE".equals(replay.type()) ? Kind.ARCHIVE_GAP : Kind.GAP;
        if (gapKind == Kind.ARCHIVE_GAP) {
            warnings.add("有效轨迹可能经过筛选，时间缺口仅表示归档断点；疑似漏采请查原始轨迹");
        }
        if (replay.tracks().isEmpty()) {
            warnings.add("没有可分析的轨迹");
            return result(query, replay, null, false, List.of(), warnings);
        }
        Track track = replay.tracks().get(0);
        PreparedGeometry field = track.pointCount() < 2 ? null : outerBoundary(track, actor, warnings);
        if (track.pointCount() < 2) warnings.add("有效定位点不足两个，无法识别异常");
        return result(query, replay, track, field != null, detect(track, field, gapKind), warnings);
    }

    private PreparedGeometry outerBoundary(Track track, Actor actor, List<String> warnings) {
        if (track.plotId() == null) {
            warnings.add("作业单未关联地块，无法判断转场");
            return null;
        }
        try {
            return boundaries.outerBoundary(Long.valueOf(track.plotId()),
                    new BoundaryQueryService.Actor(actor.userId(), actor.phone(), actor.admin()));
        } catch (ServiceException unavailable) {
            if (!Integer.valueOf(403).equals(unavailable.getCode())) throw unavailable;
        } catch (IllegalArgumentException invalid) {
            // 损坏的地块边界不能用于判断转场，其他规则仍可运行。
        }
        warnings.add("地块边界不可用或无访问权限，跳过转场识别");
        return null;
    }

    private Analysis result(Query query, Result replay, Track track, boolean transferChecked,
                            List<Event> events, List<String> warnings) {
        return new Analysis(query.trajectoryId(), track == null ? null : track.plotId(), replay.type(),
                track == null ? null : track.source(), replay.coordinateSystem(), replay.startTime(), replay.endTime(),
                replay.pointCount(), transferChecked, RULES, events, List.copyOf(warnings));
    }

    private record SampleRef(PointRef ref, Instant time) {
        private SampleRef(int segment, int index, Point point) {
            this(new PointRef(segment, index, point), OffsetDateTime.parse(point.time()).toInstant());
        }
    }

    static List<Event> detect(Track track, PreparedGeometry field, Kind gapKind) {
        List<Event> events = new ArrayList<>();
        SampleRef previous = null, stopStart = null, transferStart = null;
        double transferMaxDisplacement = 0;
        for (int segmentIndex = 0; segmentIndex < track.segments().size(); segmentIndex++) {
            List<Point> segment = track.segments().get(segmentIndex);
            for (int i = 0; i < segment.size(); i++) {
                SampleRef current = new SampleRef(segmentIndex, i, segment.get(i));
                if (previous == null) {
                    stopStart = current;
                    previous = current;
                    continue;
                }
                double seconds = seconds(previous, current);
                boolean gap = seconds > RULES.gapSeconds();
                if (gap || i == 0 || seconds <= 0) {
                    finishStop(events, stopStart, previous);
                    finishTransfer(events, transferStart, previous, transferMaxDisplacement);
                    if (gap) events.add(event(gapKind, previous, current, null, null));
                    stopStart = current;
                    transferStart = null;
                    transferMaxDisplacement = 0;
                    previous = current;
                    continue;
                }

                double metres = distance(previous, current);
                if (i + 1 < segment.size() && seconds <= RULES.spikeLegMaxSeconds()
                        && metres >= RULES.spikeLegMinMetres()) {
                    SampleRef next = new SampleRef(segmentIndex, i + 1, segment.get(i + 1));
                    if (isSpike(previous, current, next)) {
                        finishStop(events, stopStart, previous);
                        finishTransfer(events, transferStart, previous, transferMaxDisplacement);
                        events.add(new Event(Kind.GPS_SPIKE, previous.ref(), next.ref(), seconds(previous, next),
                                distance(previous, next), null, null, current.ref()));
                        // 保留原始点索引，不将跳点的两条边计入其他规则，也不补线。
                        previous = null;
                        stopStart = null;
                        transferStart = null;
                        transferMaxDisplacement = 0;
                        continue;
                    }
                }
                double kmh = metres / seconds * 3.6;
                if (kmh > RULES.overspeedKmh()) events.add(event(Kind.OVERSPEED, previous, current, metres, kmh));

                if (distance(stopStart, current) > RULES.stopRadiusMetres()) {
                    finishStop(events, stopStart, previous);
                    stopStart = current;
                }
                if (field != null && outside(field, previous, current)) {
                    if (transferStart == null) transferStart = previous;
                    transferMaxDisplacement = Math.max(transferMaxDisplacement, distance(transferStart, current));
                } else {
                    finishTransfer(events, transferStart, previous, transferMaxDisplacement);
                    transferStart = null;
                    transferMaxDisplacement = 0;
                }
                previous = current;
            }
        }
        finishStop(events, stopStart, previous);
        finishTransfer(events, transferStart, previous, transferMaxDisplacement);
        events.sort(Comparator.comparingInt((Event e) -> e.start().segmentIndex())
                .thenComparingInt(e -> e.start().pointIndex()).thenComparing(Event::kind));
        return List.copyOf(events);
    }

    private static boolean isSpike(SampleRef before, SampleRef suspect, SampleRef after) {
        double backSeconds = seconds(suspect, after);
        return backSeconds > 0 && backSeconds <= RULES.spikeLegMaxSeconds()
                && distance(suspect, after) >= RULES.spikeLegMinMetres()
                && distance(before, after) <= RULES.spikeReturnRadiusMetres();
    }

    private static boolean outside(PreparedGeometry field, SampleRef start, SampleRef end) {
        Coordinate a = coordinate(start), b = coordinate(end);
        if (Math.abs(a.x - b.x) >= 180) return false;
        return !field.intersects(a.equals2D(b) ? GEOMETRIES.createPoint(a)
                : GEOMETRIES.createLineString(new Coordinate[]{a, b}));
    }

    private static Coordinate coordinate(SampleRef sample) {
        Point point = sample.ref().point();
        return new Coordinate(point.longitude(), point.latitude());
    }

    private static void finishStop(List<Event> events, SampleRef start, SampleRef end) {
        if (start != null && seconds(start, end) >= RULES.stopSeconds())
            events.add(event(Kind.STOP, start, end, distance(start, end), null));
    }

    private static void finishTransfer(List<Event> events, SampleRef start, SampleRef end, double maxDisplacement) {
        if (start != null && maxDisplacement >= RULES.transferMetres()) {
            events.add(new Event(Kind.TRANSFER_CANDIDATE, start.ref(), end.ref(), seconds(start, end),
                    distance(start, end), null, maxDisplacement, null));
        }
    }

    private static Event event(Kind kind, SampleRef start, SampleRef end, Double metres, Double kmh) {
        return new Event(kind, start.ref(), end.ref(), seconds(start, end), metres, kmh, null, null);
    }

    private static double seconds(SampleRef start, SampleRef end) {
        return Duration.between(start.time(), end.time()).toNanos() / 1_000_000_000d;
    }

    private static double distance(SampleRef start, SampleRef end) {
        Point a = start.ref().point(), b = end.ref().point();
        double latA = Math.toRadians(a.latitude()), latB = Math.toRadians(b.latitude());
        double sinLat = Math.sin((latB - latA) / 2);
        double sinLon = Math.sin(Math.toRadians(b.longitude() - a.longitude()) / 2);
        double haversine = sinLat * sinLat + Math.cos(latA) * Math.cos(latB) * sinLon * sinLon;
        return 12_742_000 * Math.asin(Math.sqrt(Math.max(0, Math.min(1, haversine))));
    }
}
