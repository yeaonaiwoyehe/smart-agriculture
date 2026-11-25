package com.agri.cms.system.service.impl;

import com.agri.cms.common.exception.ServiceException;
import com.agri.cms.system.domain.TrajectoryInfo;
import com.agri.cms.system.mapper.TrajectoryPlaybackMapper;
import com.agri.cms.system.service.accounting.AccountingModels.*;
import com.agri.cms.system.service.accounting.TrajectoryAccountingService;
import com.agri.cms.system.service.boundary.BoundaryQueryService;
import com.agri.cms.system.service.boundary.BoundaryQueryService.Actor;
import com.agri.cms.system.service.playback.PlaybackModels;
import com.agri.cms.system.service.playback.RawPlaybackReader;
import com.agri.cms.system.service.playback.TrajectoryPlaybackService;
import org.junit.Before;
import org.junit.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

public class TrajectoryAccountingServiceTest {
    private final BoundaryQueryService boundaries=mock(BoundaryQueryService.class);
    private final TrajectoryPlaybackMapper mapper=mock(TrajectoryPlaybackMapper.class);
    private final TrajectoryPlaybackService playback=mock(TrajectoryPlaybackService.class);
    private final TrajectoryAccountingService service=new TrajectoryAccountingService(boundaries,new AreaCalculationServiceImpl(),mapper,playback);
    private final Actor user=new Actor(42,"15888888888",false);
    private final LocalDateTime start=LocalDateTime.of(2026,11,24,8,0);
    private Coordinate c(double x) {var point=WidthAccountingTest.point(x,0);return new Coordinate(point.getLongitude(),point.getLatitude());}
    private PreviewRequest preview(Integer width) {return new PreviewRequest(1L,width,List.of(List.of(c(0),c(100))));}
    private TrajectoryInfo order() {
        var order=new TrajectoryInfo();order.setTrajectoryInfoId("ORDER-A");order.setCreatedBy("42");order.setPhoneNumber(user.username());
        order.setWidth(300);order.setPlotId(1L);order.setArea(new BigDecimal("1.0000"));order.setStatus((short)1);
        order.setStartTime(start);order.setEndTime(start.plusHours(1));when(mapper.order("ORDER-A")).thenReturn(order);return order;
    }
    private PlaybackModels.Result saved(String plot,int skipped,List<List<PlaybackModels.Point>> segments) {
        int count=segments.stream().mapToInt(List::size).sum();
        return new PlaybackModels.Result("EFFECTIVE","WGS84","Asia/Shanghai",user.username(),start.toString(),start.plusHours(1).toString(),
                List.of(new PlaybackModels.Track("ORDER-A",plot,"POSTGIS_ARCHIVE",segments,count)),count,0,skipped,List.of());
    }
    private void archive() {
        var p0=c(0);var p100=c(100);
        when(playback.query(any(),any())).thenReturn(saved("1",0,List.of(List.of(
                new PlaybackModels.Point(p0.longitude(),p0.latitude(),start.toString()),
                new PlaybackModels.Point(p100.longitude(),p100.latitude(),start.plusSeconds(10).toString())))));
    }
    @Before public void boundary() throws Exception {
        when(boundaries.accountingBoundary(eq(1L),any())).thenReturn(new BoundaryQueryService.AccountingBoundary(WidthAccountingTest.box(-10,-10,210,10),List.of()));
    }
    @Test public void previewReturnsUnitsSourceAndDoesNotReadArchive() {
        var result=service.preview(preview(300),user);
        assertTrue(result.preview());assertEquals("WGS84",result.coordinateSystem());assertEquals("SUBMITTED_SEGMENTS",result.source());
        assertEquals("1",result.plotId());assertEquals(300,result.widthCm());assertEquals(2,result.pointCount());
        assertEquals(new BigDecimal("0.4500"),result.calculation().effectiveAreaMu());assertNull(result.recordedAreaMu());
        assertNull(result.differenceFromRecordedAreaMu());assertFalse(result.warnings().isEmpty());verifyNoInteractions(mapper,playback);
    }
    @Test public void anonymousRequestsCannotReadAnyBoundaryOrOrder() {
        assertEquals(Integer.valueOf(401),assertThrows(ServiceException.class,()->service.preview(preview(300),null)).getCode());
        assertThrows(ServiceException.class,()->service.archived("ORDER-A",new Actor(0,"",false)));
        verifyNoInteractions(boundaries,mapper,playback);
    }
    @Test public void missingNonpositiveAndImplausibleWidthsAreNotGuessed() {
        for(Integer width:Arrays.asList(null,-1,0,10001,Integer.MAX_VALUE))
            assertThrows(ServiceException.class,()->service.preview(preview(width),user));
        verifyNoInteractions(boundaries,mapper,playback);
    }
    @Test public void nullCoordinatesAreNotConvertedToZeroAndInvalidCoordinatesAreRejected() {
        for(Coordinate c:Arrays.asList(null,new Coordinate(null,38d),new Coordinate(114d,null),new Coordinate(181d,38d),new Coordinate(114d,91d),new Coordinate(Double.NaN,0d)))
            assertThrows(ServiceException.class,()->service.preview(new PreviewRequest(1L,300,List.of(Arrays.asList(c))),user));
        verifyNoInteractions(boundaries);
    }
    @Test public void nullSegmentsAndOversizedRequestsAreRejectedBeforeGeometryWork() {
        assertThrows(ServiceException.class,()->service.preview(null,user));
        assertThrows(ServiceException.class,()->service.preview(new PreviewRequest(1L,300,null),user));
        assertThrows(ServiceException.class,()->service.preview(new PreviewRequest(1L,300,Arrays.asList((List<Coordinate>)null)),user));
        assertThrows(ServiceException.class,()->service.preview(new PreviewRequest(1L,300,Collections.nCopies(1001,List.of())),user));
        assertThrows(ServiceException.class,()->service.preview(new PreviewRequest(1L,300,List.of(Collections.nCopies(10001,c(0)))),user));
        verifyNoInteractions(boundaries);
    }
    @Test public void previewRequiresTheSamePlotAccessAsBoundaryChecks() {
        when(boundaries.accountingBoundary(1L,user)).thenThrow(new ServiceException("地块不存在或无权访问",403));
        assertEquals(Integer.valueOf(403),assertThrows(ServiceException.class,()->service.preview(preview(300),user)).getCode());
        verifyNoInteractions(mapper,playback);
    }
    @Test public void archiveUsesRecordedWidthAndReturnsComparisonWithoutUpdatingTheOrder() {
        var order=order();archive();var result=service.archived("ORDER-A",user);
        assertEquals("POSTGIS_ARCHIVE",result.source());assertEquals("ORDER-A",result.trajectoryId());
        assertEquals(new BigDecimal("1.0000"),result.recordedAreaMu());assertEquals(new BigDecimal("-0.5500"),result.differenceFromRecordedAreaMu());
        assertEquals(new BigDecimal("1.0000"),order.getArea());assertEquals(Integer.valueOf(300),order.getWidth());
        verify(mapper).order("ORDER-A");verifyNoMoreInteractions(mapper);
        verify(playback).query(eq(new PlaybackModels.Query("EFFECTIVE","ORDER-A",null,null,null)),eq(new PlaybackModels.Actor(42,user.username(),false)));
    }
    @Test public void missingUnownedAndDeletedOrdersHaveTheSameDenialWithoutPlaybackLookup() {
        var absent=assertThrows(ServiceException.class,()->service.archived("MISSING",user));
        var order=order();order.setCreatedBy("99");order.setPhoneNumber("13900000000");
        var denied=assertThrows(ServiceException.class,()->service.archived("ORDER-A",user));
        order.setCreatedBy("42");order.setStatus((short)2);
        var deleted=assertThrows(ServiceException.class,()->service.archived("ORDER-A",new Actor(1,"admin",true)));
        assertEquals(absent.getMessage(),denied.getMessage());assertEquals(denied.getMessage(),deleted.getMessage());
        assertEquals(Integer.valueOf(403),denied.getCode());verifyNoInteractions(playback,boundaries);
    }
    @Test public void terminalBindingAndAdministratorAccessFollowPlaybackRules() {
        var order=order();order.setCreatedBy("99");archive();assertEquals(2,service.archived("ORDER-A",user).pointCount());
        assertEquals(2,service.archived("ORDER-A",new Actor(1,"admin",true)).pointCount());
    }
    @Test public void missingTerminalIdentifiersDoNotGrantAccessToAnUnownedOrder() {
        var order=order();order.setCreatedBy("99");order.setPhoneNumber(null);
        assertEquals(Integer.valueOf(403),assertThrows(ServiceException.class,()->service.archived("ORDER-A",new Actor(42,null,false))).getCode());
        verifyNoInteractions(playback,boundaries);
    }
    @Test public void missingArchiveOrSkippedBadPointsDoesNotReturnMisleadingZeroArea() {
        order();when(playback.query(any(),any())).thenReturn(new PlaybackModels.Result("EFFECTIVE","WGS84","Asia/Shanghai",user.username(),null,null,List.of(),0,0,0,List.of()));
        assertThrows(ServiceException.class,()->service.archived("ORDER-A",user));
        when(playback.query(any(),any())).thenReturn(saved("1",1,List.of()));
        assertThrows(ServiceException.class,()->service.archived("ORDER-A",user));verifyNoInteractions(boundaries);
    }
    @Test public void archivePlotMismatchAndInvalidWidthAreRejected() {
        var order=order();archive();order.setPlotId(2L);
        assertThrows(ServiceException.class,()->service.archived("ORDER-A",user));
        order.setPlotId(1L);order.setWidth(-1);assertThrows(ServiceException.class,()->service.archived("ORDER-A",user));
        verifyNoInteractions(boundaries);
    }
    @Test public void invalidGeometryIsReportedAsBusinessFailure() {
        when(boundaries.accountingBoundary(1L,user)).thenReturn(new BoundaryQueryService.AccountingBoundary(null,List.of()));
        assertThrows(ServiceException.class,()->service.preview(preview(300),user));
    }
    @Test public void recordedZeroAndStationaryCoverageRemainNumericZero() {
        var result=service.preview(new PreviewRequest(1L,300,List.of(List.of(c(0),c(0)))),user);
        assertEquals(new BigDecimal("0.0000"),result.calculation().effectiveAreaMu());assertEquals(new BigDecimal("0.00"),result.calculation().grossAreaSquareMetres());
    }
    @Test public void realPlaybackSegmentsArePreservedThroughAccounting() throws Exception {
        var order=order();var raw=mock(RawPlaybackReader.class);
        var a=c(0);var b=c(100);var c=c(110);
        String wkt="POLYGON(("+a.longitude()+" 0,"+b.longitude()+" 0,"+c.longitude()+" 0,"+a.longitude()+" 0))";
        when(mapper.archive("ORDER-A")).thenReturn(new TrajectoryPlaybackMapper.Archive(wkt,start+","+start.plusMinutes(10)+","+start.plusMinutes(10).plusSeconds(1)));
        var connected=new TrajectoryAccountingService(boundaries,new AreaCalculationServiceImpl(),mapper,new TrajectoryPlaybackService(mapper,raw));
        var result=connected.archived("ORDER-A",user);
        assertEquals(2,result.segmentCount());assertEquals(3,result.pointCount());assertEquals(30,result.calculation().effectiveAreaSquareMetres().doubleValue(),.01);
        assertEquals(new BigDecimal("1.0000"),order.getArea());verifyNoInteractions(raw);
    }
}
