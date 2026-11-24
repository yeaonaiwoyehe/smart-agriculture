package com.agri.cms.system.service.impl;
import com.agri.cms.ai.agent.WorkOrderService;
import com.agri.cms.ai.agent.mapper.WorkOrderGeoMapper;
import com.agri.cms.ai.tool.ReceiptGenerator;
import com.agri.cms.system.domain.*;
import com.agri.cms.system.mapper.*;
import org.junit.Before;
import org.junit.Test;
import java.time.LocalDateTime;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
public class WorkOrderBoundaryIntegrationTest {
    private final TrajectoryInfoMapper orders=mock(TrajectoryInfoMapper.class);
    private final WorkOrderGeoMapper geo=mock(WorkOrderGeoMapper.class);
    private final LandPlotMapper plots=mock(LandPlotMapper.class);
    private final FieldWorkMapper types=mock(FieldWorkMapper.class);
    private final ReceiptGenerator receipts=mock(ReceiptGenerator.class);
    private final WorkOrderService service=new WorkOrderService(orders,geo,plots,types,receipts);
    @Before public void setup(){LandPlot plot=new LandPlot();plot.setId(1L);plot.setLandName("测试田");when(plots.selectById(1L)).thenReturn(plot);when(types.selectById(4001L)).thenReturn(new FieldWork());when(receipts.next()).thenReturn("TEST-RECEIPT");}
    @Test public void boundaryLocationStartsWorkWithoutFalseWarning(){when(geo.countPointInside(1L,114d,38d)).thenReturn(1);var result=service.startWork(1L,4001L,300,114d,38d,"42","15888888888");assertTrue(result.success());assertNull(result.boundaryWarning());verify(orders).insert(any(TrajectoryInfo.class));}
    @Test public void outsideWarningDoesNotChangeExistingStartPolicy(){when(geo.countPointInside(1L,114d,38d)).thenReturn(0);var result=service.startWork(1L,4001L,300,114d,38d,"42","15888888888");assertTrue(result.success());assertTrue(result.boundaryWarning().contains("超界"));}
    @Test public void missingBoundaryIsExplicitlyUnverifiedRatherThanOutside(){when(geo.countPointInside(1L,114d,38d)).thenThrow(new IllegalArgumentException("bad boundary"));var result=service.startWork(1L,4001L,300,114d,38d,"42","15888888888");assertTrue(result.success());assertTrue(result.boundaryWarning().contains("未确认"));}
    @Test public void badOrPartialCoordinatesNeverCreateOrUpdateAnOrder(){assertFalse(service.startWork(1L,4001L,300,Double.NaN,38d,"42","15888888888").success());assertFalse(service.endWork("WO-TEST",114d,null,"42").success());verifyNoInteractions(orders,geo,plots,types);}
    @Test public void endingInsideKeepsOriginalOrderAndCompletionStatus(){var order=new TrajectoryInfo();order.setTrajectoryInfoId("WO-TEST");order.setCreatedBy("42");order.setPlotId(1L);order.setStatus((short)0);order.setStartTime(LocalDateTime.now().minusHours(1));when(orders.selectById("WO-TEST")).thenReturn(order);when(geo.countPointInside(1L,114d,38d)).thenReturn(1);var result=service.endWork("WO-TEST",114d,38d,"42");assertTrue(result.success());assertEquals("WO-TEST",result.workOrderId());assertEquals(Short.valueOf((short)1),order.getStatus());assertNull(order.getArea());verify(orders).updateById(order);}
    @Test public void optionalCoordinatesRemainOptional(){assertTrue(service.startWork(1L,4001L,300,null,null,"42","15888888888").success());verifyNoInteractions(geo);}
}
