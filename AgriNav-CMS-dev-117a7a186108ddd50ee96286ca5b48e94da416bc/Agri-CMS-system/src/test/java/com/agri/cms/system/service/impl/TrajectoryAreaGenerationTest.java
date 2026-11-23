package com.agri.cms.system.service.impl;

import com.agri.cms.common.core.domain.entity.SysUser;
import com.agri.cms.system.dao.EffectiveTrajectoryDao;
import com.agri.cms.system.domain.EffectivePointBuffer;
import com.agri.cms.system.domain.LandPlot;
import com.agri.cms.system.domain.LandPlotExclude;
import com.agri.cms.system.domain.vo.Trajectory;
import com.agri.cms.system.service.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Isolated orchestration tests: no Spring context and no shared database writes. */
public class TrajectoryAreaGenerationTest {
    private final InfluxDBEffectiveTrajectoryServiceImpl service=new InfluxDBEffectiveTrajectoryServiceImpl();
    private final EffectiveTrajectoryDao dao=mock(EffectiveTrajectoryDao.class);
    private final LandPlotService plots=mock(LandPlotService.class);
    private final GaodeMapService maps=mock(GaodeMapService.class);
    private final TrajectoryInfoService info=mock(TrajectoryInfoService.class);
    private final TrajectoryPointsService points=mock(TrajectoryPointsService.class);
    private final SysUserServiceImpl users=mock(SysUserServiceImpl.class);
    private final LocalDateTime start=LocalDateTime.of(2026,1,1,0,0),end=start.plusMinutes(1);
    @Before public void setup() throws Exception {
        inject("effectiveTrajectoryDao",dao);inject("landPlotService",plots);inject("gaodeMapService",maps);
        inject("trajectoryInfoService",info);inject("trajectoryPointsService",points);inject("userService",users);
        inject("areaCalculationService",new AreaCalculationServiceImpl());
        SysUser user=new SysUser();user.setUserId(1L);when(users.selectUserByUserName("test-driver")).thenReturn(user);
        LandPlot plot=new LandPlot();plot.setBoundary(AreaClippingTest.rectangle(-10,-10,110,10));
        when(plots.getById(anyLong())).thenReturn(plot);when(points.savePoints(any(),any())).thenReturn(true);
    }
    private void inject(String name,Object value) throws Exception {Field f=service.getClass().getDeclaredField(name);f.setAccessible(true);f.set(service,value);}
    private EffectivePointBuffer row(long id,double x,int width) {
        EffectivePointBuffer row=new EffectivePointBuffer();row.setPlotId(id);row.setWidth(width);row.setWorkType(1);
        row.setLongitude(AreaCalculationServiceImplTest.point(x,0).getLongitude());row.setLatitude(0.0);row.setDeviceTime(start.plusSeconds((long)x));return row;
    }
    private void rows(EffectivePointBuffer... rows) {when(dao.queryByPhoneNumber("test-driver",start,end)).thenReturn(Arrays.asList(rows));}
    private void generate() {service.createTrajectory("test-driver",start,end);}
    private void noWrites() {
        verifyNoInteractions(info,points);verify(dao,never()).deleteByPhoneNumber(anyString(),any(),any());
    }
    @Test public void invalidWidthDoesNotCreateZeroAreaOrDeleteBuffer() {
        rows(row(1,0,-1),row(1,100,-1));assertThrows(IllegalArgumentException.class,this::generate);noWrites();
    }
    @Test public void invalidGeometryDoesNotCreateZeroAreaOrDeleteBuffer() throws Exception {
        LandPlot invalid=new LandPlot();invalid.setBoundary(AreaClippingTest.polygon(0,0,100,100,0,100,100,0,0,0));
        when(plots.getById(anyLong())).thenReturn(invalid);rows(row(1,0,300),row(1,100,300));
        assertThrows(IllegalArgumentException.class,this::generate);noWrites();
    }
    @Test public void onePointIsPreservedForRetry() {
        rows(row(1,0,300));assertThrows(IllegalStateException.class,this::generate);noWrites();
    }
    @Test public void entireBatchIsValidatedBeforeAnyPersistence() {
        rows(row(1,0,300),row(1,100,300),row(2,0,300));
        assertThrows(IllegalStateException.class,this::generate);noWrites();
    }
    @Test public void mixedWidthsAreRejectedRatherThanOverwritten() {
        rows(row(1,0,300),row(1,100,400));assertThrows(IllegalArgumentException.class,this::generate);noWrites();
    }
    @Test public void thumbnailFailureDoesNotBlockValidArea() throws Exception {
        rows(row(1,0,300),row(1,100,300));when(maps.generateThumbnail(anyList(),anyList())).thenThrow(new IllegalStateException("offline"));
        generate();ArgumentCaptor<Trajectory> saved=ArgumentCaptor.forClass(Trajectory.class);
        verify(info).saveInfo(saved.capture(),any(),eq("1"));assertEquals(new BigDecimal("0.4500"),saved.getValue().getArea());
        var order=inOrder(points,info,dao);order.verify(points).savePoints(any(),any());order.verify(info).saveInfo(any(),any(),anyString());
        order.verify(dao).deleteByPhoneNumber("test-driver",start,end);
    }
    @Test public void persistenceFailurePreventsBufferCleanup() {
        rows(row(1,0,300),row(1,100,300));when(points.savePoints(any(),any())).thenReturn(false);
        assertThrows(IllegalStateException.class,this::generate);verifyNoInteractions(info);verify(dao,never()).deleteByPhoneNumber(anyString(),any(),any());
    }
    @Test public void emptyWindowDoesNotDeleteAnyData() {rows();generate();noWrites();}
    @Test public void missingTimestampIsRejectedWithoutCleanup() {
        EffectivePointBuffer invalid=row(1,0,300);invalid.setDeviceTime(null);
        rows(invalid,row(1,100,300));assertThrows(IllegalArgumentException.class,this::generate);noWrites();
    }
    @Test public void excludedPointAuditKeepsLongitudeLatitudeOrder() throws Exception {
        LandPlotExclude exclusion=new LandPlotExclude();exclusion.setId(7L);
        exclusion.setBoundary(AreaClippingTest.rectangle(40,-5,60,5));
        var matches=service.assignPointsToPlots(Arrays.asList(AreaCalculationServiceImplTest.point(50,0),
                AreaCalculationServiceImplTest.point(10,0)),Collections.singletonList(exclusion));
        assertEquals(1,matches.get(7L).size());
        assertEquals(AreaCalculationServiceImplTest.point(50,0).getLongitude(),matches.get(7L).get(0).getLongitude(),1e-12);
    }
}
