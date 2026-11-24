package com.agri.cms.system.service.impl;
import com.agri.cms.ai.agent.mapper.WorkOrderGeoMapper;
import com.agri.cms.common.exception.ServiceException;
import com.agri.cms.system.domain.*;
import com.agri.cms.system.domain.vo.Point;
import com.agri.cms.system.mapper.*;
import com.agri.cms.system.service.boundary.BoundaryQueryService;
import org.apache.ibatis.session.Configuration;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
public class BoundaryQueryServiceTest {
    private final LandPlotMapper plots=mock(LandPlotMapper.class);
    private final LandPlotExcludeMapper exclusions=mock(LandPlotExcludeMapper.class);
    private final BoundaryAccessMapper access=mock(BoundaryAccessMapper.class);
    private final WorkOrderGeoMapper geo=mock(WorkOrderGeoMapper.class);
    private final BoundaryQueryService service=new BoundaryQueryService(plots,exclusions,access,geo,new BoundaryClippingService(new AreaCalculationServiceImpl()));
    private final BoundaryQueryService.Actor actor=new BoundaryQueryService.Actor(42,"15888888888",false);
    private void plot(String type)throws Exception{LandPlot plot=new LandPlot();plot.setId(1L);plot.setLandType(type);plot.setBoundary(BoundaryClippingServiceTest.box(0,-10,100,10));when(plots.selectById(1L)).thenReturn(plot);when(access.canRead(1L,"42",actor.username())).thenReturn(true);}
    @Test public void noPermissionCannotFetchBoundaryOrClassify(){var error=assertThrows(ServiceException.class,()->service.check(1L,114d,38d,actor));assertEquals(Integer.valueOf(403),error.getCode());verifyNoInteractions(plots,geo);}
    @Test public void unavailableIsDistinctFromOutside()throws Exception{plot("普通地");when(geo.pointState(1L,114d,38d)).thenReturn("UNAVAILABLE");assertNull(service.check(1L,114d,38d,actor).inBounds());when(geo.pointState(1L,114d,38d)).thenReturn("OUTSIDE");assertEquals(Boolean.FALSE,service.check(1L,114d,38d,actor).inBounds());}
    @Test public void invalidCoordinatesDoNotQueryDatabase(){assertThrows(ServiceException.class,()->service.check(1L,114d,91d,actor));verifyNoInteractions(plots,geo,access);}
    @Test public void insertedExclusionMatchesAreaCalculationPolicy()throws Exception{plot("插花地");LandPlotExclude hole=new LandPlotExclude();hole.setBoundary(BoundaryClippingServiceTest.box(40,-10,60,10));when(exclusions.selectList(any())).thenReturn(List.of(hole));var result=service.clip(1L,List.of(BoundaryClippingServiceTest.point(0,0),BoundaryClippingServiceTest.point(100,0)),actor);assertEquals(80,result.insideLength().doubleValue(),.001);assertEquals(20,result.excludedLength().doubleValue(),.001);}
    @Test public void normalPlotsDoNotUnexpectedlyActivateSpecialPlotExclusions()throws Exception{plot("普通地");assertEquals(100,service.clip(1L,List.of(BoundaryClippingServiceTest.point(0,0),BoundaryClippingServiceTest.point(100,0)),actor).insideLength().doubleValue(),.001);verifyNoInteractions(exclusions);}
    @Test public void legacyAdapterCoversBoundaryAndRejectsUnknown(){var adapter=mock(WorkOrderGeoMapper.class,CALLS_REAL_METHODS);when(adapter.pointState(1L,114d,38d)).thenReturn("BOUNDARY");assertEquals(Integer.valueOf(1),adapter.countPointInside(1L,114d,38d));when(adapter.pointState(1L,114d,38d)).thenReturn("EXCLUDED");assertEquals(Integer.valueOf(0),adapter.countPointInside(1L,114d,38d));when(adapter.pointState(1L,114d,38d)).thenReturn("UNAVAILABLE");assertThrows(IllegalArgumentException.class,()->adapter.countPointInside(1L,114d,38d));}
    @Test public void pointSqlUsesCoversGuardsAndBoundParameters(){var config=new Configuration();config.addMapper(WorkOrderGeoMapper.class);var sql=config.getMappedStatement(WorkOrderGeoMapper.class.getName()+".pointState").getBoundSql(Map.of("plotId",1L,"longitude",114d,"latitude",38d));assertTrue(sql.getSql().contains("ST_Covers"));assertTrue(sql.getSql().contains("ST_IsValid"));assertTrue(sql.getSql().contains("e.is_deleted=0"));assertFalse(sql.getSql().contains("114.0"));}
}
