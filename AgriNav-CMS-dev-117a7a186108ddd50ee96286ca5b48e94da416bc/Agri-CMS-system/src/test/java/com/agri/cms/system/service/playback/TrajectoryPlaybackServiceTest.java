package com.agri.cms.system.service.playback;

import com.agri.cms.common.exception.ServiceException;
import com.agri.cms.system.domain.TrajectoryInfo;
import com.agri.cms.system.mapper.TrajectoryPlaybackMapper;
import org.junit.Test;
import java.time.*;
import java.util.*;
import static com.agri.cms.system.service.playback.PlaybackModels.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

public class TrajectoryPlaybackServiceTest {
    private final TrajectoryPlaybackMapper mapper=mock(TrajectoryPlaybackMapper.class);
    private final RawPlaybackReader raw=mock(RawPlaybackReader.class);
    private final TrajectoryPlaybackService service=new TrajectoryPlaybackService(mapper,raw);
    private final LocalDateTime start=LocalDateTime.of(2026,11,23,8,0),end=start.plusHours(1);
    private final Actor user=new Actor(42,"15888888888",false);
    private Query query(String type) {return new Query(type,null,null,start,end);}
    private TrajectoryInfo order(String id,String phone) {
        var info=new TrajectoryInfo();info.setTrajectoryInfoId(id);info.setPhoneNumber(phone);info.setCreatedBy("42");info.setStartTime(start);info.setEndTime(end);info.setPlotId(9007199254740993L);return info;
    }
    private void archive(String id) {
        when(mapper.archive(id)).thenReturn(new TrajectoryPlaybackMapper.Archive("POLYGON((114 38,114.01 38,114.01 38.01,114 38))",start+","+start.plusSeconds(1)+","+start.plusSeconds(2)));
    }
    @Test public void rawSortsDeduplicatesPreservesStopsAndSplitsGaps() {
        when(raw.read(anyString(),any(),any())).thenReturn(List.of(new Sample(114,38,start.plusMinutes(10)),new Sample(114,38,start),new Sample(114,38,start),new Sample(114,38,start.plusSeconds(1))));
        var result=service.query(query("ORIGINAL"),user);assertEquals(3,result.pointCount());assertEquals(1,result.removedDuplicates());
        assertEquals(2,result.tracks().get(0).segments().size());assertEquals(start.atOffset(ZoneOffset.ofHours(8)).toString(),result.tracks().get(0).segments().get(0).get(0).time());verifyNoInteractions(mapper);
    }
    @Test public void sameTimeDifferentLocationsAreIsolatedNotArbitrarilyConnected() {
        when(raw.read(anyString(),any(),any())).thenReturn(List.of(new Sample(114,38,start),new Sample(115,38,start),new Sample(116,38,start.plusSeconds(1))));
        var result=service.query(query("ORIGINAL"),user);assertEquals(3,result.pointCount());assertEquals(3,result.tracks().get(0).segments().size());assertTrue(result.warnings().stream().anyMatch(w->w.contains("先后顺序")));
    }
    @Test public void invalidPointsAreReportedAndRangeIsInclusive() {
        List<Sample> points=new ArrayList<>(List.of(new Sample(181,38,start),new Sample(Double.NaN,38,start),new Sample(114,38,start),new Sample(114,38,end),new Sample(114,38,start.minusNanos(1))));points.add(null);
        when(raw.read(anyString(),any(),any())).thenReturn(points);var result=service.query(query("ORIGINAL"),user);assertEquals(2,result.pointCount());assertEquals(3,result.skippedInvalid());
    }
    @Test public void otherPhoneCannotBeReadByOrdinaryUser() {
        var error=assertThrows(ServiceException.class,()->service.query(new Query("ORIGINAL",null,"13900000000",start,end),user));assertEquals(Integer.valueOf(403),error.getCode());verifyNoInteractions(mapper,raw);
    }
    @Test public void adminMayExplicitlySelectOtherTerminal() {
        when(raw.read(anyString(),any(),any())).thenReturn(List.of());service.query(new Query("ORIGINAL",null,"13900000000",start,end),new Actor(1,"admin",true));verify(raw).read("13900000000",start,end);
    }
    @Test public void badRangeTypeOrActorDoesNotReadAnySource() {
        for(Query input:List.of(new Query("BAD",null,null,start,end),new Query("ORIGINAL",null,null,end,start),new Query("ORIGINAL",null,null,start,start.plusHours(25)),new Query("ORIGINAL",null,null,null,end)))assertThrows(ServiceException.class,()->service.query(input,user));
        assertThrows(ServiceException.class,()->service.query(query("ORIGINAL"),new Actor(0,"15888888888",false)));verifyNoInteractions(mapper,raw);
    }
    @Test public void selectedOrderClampsTimesAndUsesItsTerminal() {
        when(mapper.order("A")).thenReturn(order("A","13900000000"));when(raw.read(anyString(),any(),any())).thenReturn(List.of());
        service.query(new Query("ORIGINAL","A",null,start.minusMinutes(1),end.plusMinutes(1)),user);verify(raw).read("13900000000",start,end);
    }
    @Test public void missingAndUnownedOrdersReturnSameDenialWithoutPointLookup() {
        var other=order("OTHER","13900000000");other.setCreatedBy("99");when(mapper.order("OTHER")).thenReturn(other);
        var a=assertThrows(ServiceException.class,()->service.query(new Query("ORIGINAL","MISSING",null,null,null),user));
        var b=assertThrows(ServiceException.class,()->service.query(new Query("ORIGINAL","OTHER",null,null,null),user));assertEquals(a.getMessage(),b.getMessage());verifyNoInteractions(raw);
    }
    @Test public void phoneBoundOrderIsOwnedEvenIfCreatorIsAnAdministrator() {
        var info=order("A",user.phone());info.setCreatedBy("1");when(mapper.order("A")).thenReturn(info);archive("A");
        assertEquals(3,service.query(new Query("EFFECTIVE","A",null,null,null),user).pointCount());
    }
    @Test public void effectiveSourcesAreSeparateAndDifferentOrdersNeverConnect() {
        when(mapper.orders(user.phone(),start,end)).thenReturn(List.of(order("A",user.phone()),order("B",user.phone())));archive("A");archive("B");
        var result=service.query(query("EFFECTIVE"),user);assertEquals(2,result.tracks().size());assertEquals(6,result.pointCount());assertEquals("9007199254740993",result.tracks().get(0).plotId());verifyNoInteractions(raw);
    }
    @Test public void archiveClosureIsRemovedWithoutDroppingRealTimedLastPoint() {
        var points=TrajectoryPlaybackService.decodeArchive("POLYGON((114 38,115 38,114 38,114 38))",start+","+start.plusSeconds(1)+","+start.plusSeconds(2));assertEquals(3,points.size());assertEquals(start.plusSeconds(2),points.get(2).time());
    }
    @Test public void corruptArchiveCannotInventOrMisalignTime() {
        for(String times:List.of(start.toString(),start+",bad,"+end))assertThrows(ServiceException.class,()->TrajectoryPlaybackService.decodeArchive("POLYGON((114 38,115 38,115 39,114 38))",times));
        assertThrows(ServiceException.class,()->TrajectoryPlaybackService.decodeArchive("LINESTRING(114 38,115 38)",start+","+end));
    }
    @Test public void missingArchiveReturnsExplicitWarningRatherThanBufferSubstitute() {
        when(mapper.orders(user.phone(),start,end)).thenReturn(List.of(order("A",user.phone())));var result=service.query(query("EFFECTIVE"),user);assertEquals(0,result.pointCount());assertTrue(result.warnings().get(0).contains("尚无"));verifyNoInteractions(raw);
    }
    @Test public void emptyRangeResultIsSuccessfulAndStableShape() {
        when(mapper.orders(user.phone(),start,end)).thenReturn(List.of());var result=service.query(query("EFFECTIVE"),user);assertTrue(result.tracks().isEmpty());assertEquals("WGS84",result.coordinateSystem());
    }
    @Test public void overLimitIsAnErrorInsteadOfPartialData() {
        when(raw.read(anyString(),any(),any())).thenReturn(Collections.nCopies(50001,new Sample(114,38,start)));assertThrows(ServiceException.class,()->service.query(query("ORIGINAL"),user));
        when(mapper.orders(user.phone(),start,end)).thenReturn(Collections.nCopies(201,order("A",user.phone())));assertThrows(ServiceException.class,()->service.query(query("EFFECTIVE"),user));
    }
    @Test public void deletedOrderIsDeniedEvenToAdminAndArchivedPointsAreNeverRead() {
        var info=order("A",user.phone());info.setStatus((short)2);when(mapper.order("A")).thenReturn(info);
        assertThrows(ServiceException.class,()->service.query(new Query("EFFECTIVE","A",null,null,null),new Actor(1,"admin",true)));
        verify(mapper,never()).archive(anyString());verifyNoInteractions(raw);
    }
    @Test public void completedAndHistoricalOrdersRemainReplayableButDeletedOnesAreExcluded() {
        var complete=order("A",user.phone());complete.setStatus((short)1);var old=order("B",user.phone());var deleted=order("C",user.phone());deleted.setStatus((short)2);
        when(mapper.orders(user.phone(),start,end)).thenReturn(List.of(complete,old,deleted));archive("A");archive("B");
        assertEquals(6,service.query(query("EFFECTIVE"),user).pointCount());verify(mapper,never()).archive("C");
    }
}
