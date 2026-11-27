package com.agri.cms.system.service.playback;

import com.agri.cms.common.exception.ServiceException;
import com.agri.cms.system.domain.TrajectoryInfo;
import com.agri.cms.system.mapper.TrajectoryPlaybackMapper;
import com.agri.cms.system.service.boundary.BoundaryQueryService;
import com.agri.cms.system.service.impl.AreaCalculationServiceImpl;
import com.agri.cms.system.service.impl.BoundaryClippingService;
import org.junit.Test;
import org.postgis.Polygon;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.agri.cms.system.service.playback.PlaybackModels.*;
import static com.agri.cms.system.service.playback.TrajectoryAnomalyService.Kind.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TrajectoryAnomalyServiceTest {
    private static final OffsetDateTime START = OffsetDateTime.parse("2026-11-27T08:00:00+08:00");
    private final Actor actor = new Actor(42, "15888888888", false);
    private final Query query = new Query("ORIGINAL", "A", null, null, null);
    private final TrajectoryPlaybackService playback = mock(TrajectoryPlaybackService.class);
    private final BoundaryQueryService boundaries = mock(BoundaryQueryService.class);
    private final TrajectoryAnomalyService service = new TrajectoryAnomalyService(playback, boundaries);
    private final BoundaryQueryService.Actor boundaryActor = new BoundaryQueryService.Actor(42, actor.phone(), false);

    private Point point(double x, double y, long seconds) {
        return new Point(Math.toDegrees(x / 6371000), Math.toDegrees(y / 6371000), START.plusSeconds(seconds).toString());
    }

    @SafeVarargs
    private final void archive(String plot, List<Point>... segments) {
        int count = Arrays.stream(segments).mapToInt(List::size).sum();
        when(playback.query(query, actor)).thenReturn(new Result("ORIGINAL", "WGS84", "Asia/Shanghai",
                actor.phone(), START.toString(), START.plusHours(1).toString(),
                List.of(new Track("A", plot, "ELASTICSEARCH", List.of(segments), count)), count, 0, 0, List.of()));
    }

    private void field() throws Exception {
        Polygon boundary = new Polygon("POLYGON((0 0,0.002 0,0.002 0.002,0 0.002,0 0),"
                + "(0.0004 0.0004,0.0008 0.0004,0.0008 0.0008,0.0004 0.0008,0.0004 0.0004))");
        when(boundaries.outerBoundary(1L, boundaryActor)).thenReturn(
                new BoundaryClippingService(new AreaCalculationServiceImpl()).prepareOuterBoundary(boundary));
    }

    @Test public void reportsOneStopDuringGpsDriftAndOneInferredSpeedEvent() {
        archive(null, List.of(point(0,0,0), point(2,0,30), point(3,0,60), point(103,0,70)));
        var events = service.analyze(query, actor).events();
        assertEquals(List.of(STOP, OVERSPEED), events.stream().map(TrajectoryAnomalyService.Event::kind).toList());
        assertEquals(60, events.get(0).durationSeconds(), 0);
        assertEquals(36, events.get(1).inferredSpeedKmh(), .001);
    }

    @Test public void detectsGapInsidePlaybackSegmentAndAcrossPlaybackSegmentsWithoutInferringSpeed() {
        archive(null, List.of(point(0,0,0), point(1000,0,61)),
                List.of(point(2000,0,400), point(2000,0,401)));
        var events = service.analyze(query, actor).events();
        assertEquals(List.of(GAP, GAP), events.stream().map(TrajectoryAnomalyService.Event::kind).toList());
        assertEquals(61, events.get(0).durationSeconds(), 0);
        assertNull(events.get(1).inferredSpeedKmh());
        assertEquals(1, events.get(1).end().segmentIndex());
        assertEquals(0, events.get(1).end().pointIndex());
    }

    @Test public void archiveFilteringIsReportedAsAnArchiveGapEvenWhenOriginalSamplesAreContinuous() {
        var mapper = mock(TrajectoryPlaybackMapper.class);
        var reader = mock(RawPlaybackReader.class);
        var order = new TrajectoryInfo();
        order.setTrajectoryInfoId("A");
        order.setPhoneNumber(actor.phone());
        order.setCreatedBy(String.valueOf(actor.userId()));
        order.setStartTime(START.toLocalDateTime());
        order.setEndTime(START.plusSeconds(100).toLocalDateTime());
        when(mapper.order("A")).thenReturn(order);

        List<Sample> originalSamples = IntStream.rangeClosed(0, 10)
                .mapToObj(i -> new Sample(Math.toDegrees((i <= 1 || i >= 9 ? 10 : -20) / 6371000d),
                        0, START.plusSeconds(i * 10L).toLocalDateTime())).toList();
        when(reader.read(actor.phone(), order.getStartTime(), order.getEndTime())).thenReturn(originalSamples);
        List<Sample> saved = List.of(originalSamples.get(0), originalSamples.get(1),
                originalSamples.get(9), originalSamples.get(10));
        String coordinates = saved.stream().map(p -> p.longitude() + " " + p.latitude())
                .collect(Collectors.joining(","));
        String times = saved.stream().map(p -> p.time().toString()).collect(Collectors.joining(","));
        when(mapper.archive("A")).thenReturn(new TrajectoryPlaybackMapper.Archive(
                "POLYGON((" + coordinates + "," + saved.get(0).longitude() + " 0))", times));

        var connected = new TrajectoryAnomalyService(new TrajectoryPlaybackService(mapper, reader), boundaries);
        var effective = connected.analyze(new Query("EFFECTIVE", "A", null, null, null), actor);
        assertEquals(List.of(ARCHIVE_GAP), effective.events().stream().map(TrajectoryAnomalyService.Event::kind).toList());
        assertEquals(80, effective.events().get(0).durationSeconds(), 0);
        assertTrue(effective.warnings().stream().anyMatch(message -> message.contains("归档断点")));
        verifyNoInteractions(reader);

        var original = connected.analyze(query, actor);
        assertTrue(original.events().stream().noneMatch(event -> event.kind() == GAP || event.kind() == ARCHIVE_GAP));
    }

    @Test public void flagsContinuousTravelOutsideFieldButNotCrossingOrMovingInItsHole() throws Exception {
        field();
        archive("1", List.of(point(-20,60,0), point(-60,60,20), point(-80,60,40),
                point(250,60,100), point(45,60,140), point(85,60,160)));
        var result = service.analyze(query, actor);
        assertTrue(result.transferChecked());
        assertEquals(1, result.events().size());
        var event = result.events().get(0);
        assertEquals(TRANSFER_CANDIDATE, event.kind());
        assertEquals(0, event.start().pointIndex());
        assertEquals(2, event.end().pointIndex());
        assertEquals(60, event.displacementMetres(), .001);
    }

    @Test public void returningToStartDoesNotEraseEarlierMovementOutsideTheField() throws Exception {
        field();
        archive("1", List.of(point(-20,60,0), point(-100,60,40), point(-20,60,80)));
        var events = service.analyze(query, actor).events();
        assertEquals(List.of(TRANSFER_CANDIDATE), events.stream().map(TrajectoryAnomalyService.Event::kind).toList());
        var event = events.get(0);
        assertEquals(0, event.displacementMetres(), .001);
        assertEquals(80, event.maxDisplacementMetres(), .001);
        assertEquals(2, event.end().pointIndex());
    }

    @Test public void fieldBoundariesAndGapsKeepTransferDistancesInTheirOwnRuns() throws Exception {
        field();
        archive("1", List.of(point(-20,60,0), point(-100,60,40), point(-20,60,110), point(-30,60,130)));
        var events = service.analyze(query, actor).events();
        assertEquals(List.of(TRANSFER_CANDIDATE, GAP),
                events.stream().map(TrajectoryAnomalyService.Event::kind).toList());
        assertEquals(1, events.get(0).end().pointIndex());
    }

    @Test public void anIsolatedSpikeHasItsOwnEvidenceAndDoesNotTriggerSpeedOrTransfer() throws Exception {
        field();
        Point spike = point(-120,60,1);
        archive("1", List.of(point(-20,60,0), spike, point(-20,60,2), point(-21,60,3)));
        var events = service.analyze(query, actor).events();
        assertEquals(List.of(GPS_SPIKE), events.stream().map(TrajectoryAnomalyService.Event::kind).toList());
        var marker = events.get(0);
        assertEquals(0, marker.start().pointIndex());
        assertEquals(1, marker.spikePoint().pointIndex());
        assertEquals(spike, marker.spikePoint().point());
        assertEquals(2, marker.end().pointIndex());
        assertNull(marker.inferredSpeedKmh());
    }

    @Test public void consecutiveFastMovementWithoutImmediateReturnRemainsOverspeed() {
        archive(null, List.of(point(0,0,0), point(50,0,1), point(100,0,2)));
        var events = service.analyze(query, actor).events();
        assertEquals(List.of(OVERSPEED, OVERSPEED),
                events.stream().map(TrajectoryAnomalyService.Event::kind).toList());
        assertEquals(180, events.get(0).inferredSpeedKmh(), .001);
    }

    @Test public void positionSpikeRuleDoesNotJoinSeparatePlaybackSegments() {
        archive(null, List.of(point(0,0,0), point(100,0,1)), List.of(point(0,0,2)));
        var events = service.analyze(query, actor).events();
        assertEquals(List.of(OVERSPEED), events.stream().map(TrajectoryAnomalyService.Event::kind).toList());
    }

    @Test public void ambiguityAcrossSegmentsDoesNotManufactureMovementOrStop() {
        archive(null, List.of(point(0,0,0)), List.of(point(500,0,0)),
                List.of(point(500,0,1), point(500,0,60)));
        assertTrue(service.analyze(query, actor).events().isEmpty());
    }

    @Test public void missingBoundaryPermissionDoesNotSuppressOtherRules() {
        archive("1", List.of(point(0,0,0), point(100,0,10)));
        when(boundaries.outerBoundary(1L, boundaryActor)).thenThrow(new ServiceException("无权限", 403));
        var result = service.analyze(query, actor);
        assertFalse(result.transferChecked());
        assertEquals(OVERSPEED, result.events().get(0).kind());
        assertTrue(result.warnings().stream().anyMatch(message -> message.contains("跳过转场")));
    }

    @Test public void absentArchiveReturnsWarningAndDoesNotReadBoundary() {
        when(playback.query(query, actor)).thenReturn(new Result("EFFECTIVE", "WGS84", "Asia/Shanghai",
                actor.phone(), START.toString(), START.plusHours(1).toString(), List.of(), 0, 0, 0, List.of("尚无归档")));
        var result = service.analyze(query, actor);
        assertTrue(result.events().isEmpty());
        assertTrue(result.warnings().contains("尚无归档"));
        verifyNoInteractions(boundaries);
    }

    @Test public void inheritsPlaybackDenialBeforeReadingField() {
        when(playback.query(query, actor)).thenThrow(new ServiceException("作业单不存在或无权访问", 403));
        var error = assertThrows(ServiceException.class, () -> service.analyze(query, actor));
        assertEquals(Integer.valueOf(403), error.getCode());
        verifyNoInteractions(boundaries);
    }
}
