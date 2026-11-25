package com.agri.cms.system.service.grouping;

import com.agri.cms.system.mapper.LandPlotGroupingMapper;
import org.junit.Before;
import org.junit.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import javax.sql.DataSource;
import java.sql.Connection;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** 实际 Spring 事务拦截器配合模拟 JDBC 连接；不写共享数据。 */
public class LandPlotGroupingServiceTest {
    private final LandPlotGroupingMapper mapper=mock(LandPlotGroupingMapper.class);
    private final DataSource source=mock(DataSource.class);
    private final Connection connection=mock(Connection.class);
    private LandPlotGroupingService service;
    @Before public void setup() throws Exception {
        when(source.getConnection()).thenReturn(connection);when(connection.getAutoCommit()).thenReturn(true);
        when(mapper.isReady()).thenReturn(true);when(mapper.groupName("REGION","NORTH")).thenReturn("北片区");
        when(mapper.lockOwnedPlots(anyList(),eq("42"))).thenAnswer(i->i.getArgument(0));
        when(mapper.assign(anyList(),anyString(),anyString(),eq("42"))).thenAnswer(i->((List<?>)i.getArgument(0)).size());
        ProxyFactory factory=new ProxyFactory(new LandPlotGroupingService(mapper));
        factory.addAdvice(new TransactionInterceptor(new DataSourceTransactionManager(source),new AnnotationTransactionAttributeSource()));
        service=(LandPlotGroupingService)factory.getProxy();
    }
    @Test public void unavailableSchemaDoesNotAttemptGroupQueries() {
        when(mapper.isReady()).thenReturn(false);var result=service.summary("VILLAGE",42);
        assertEquals(false,result.get("available"));assertEquals(List.of(),result.get("groups"));
        verify(mapper,never()).summary(anyString(),anyString());verifyNoInteractions(source);
    }
    @Test public void summaryAlwaysPassesAuthenticatedOwner() {
        when(mapper.summary("REGION","42")).thenReturn(List.of());service.summary("REGION",42);
        verify(mapper).summary("REGION","42");verifyNoInteractions(source);
    }
    @Test public void assignmentReadsKeepLongIdsAndApplyOwnerScope() {
        service.assignments(List.of("9007199254740993"),42);
        verify(mapper).assignments(List.of(9007199254740993L),"42");
    }
    @Test public void identicalDefinitionIsIdempotentButDifferentNameRollsBack() throws Exception {
        service.create("REGION","NORTH","北片区",42);verify(connection).commit();
        assertThrows(IllegalArgumentException.class,()->service.create("REGION","NORTH","南片区",42));
        verify(connection).rollback();
    }
    @Test public void malformedCodesAndControlCharactersNeverReachDictionaryWrites() {
        assertThrows(IllegalArgumentException.class,()->service.create("VILLAGE","123","村",42));
        assertThrows(IllegalArgumentException.class,()->service.create("REGION","A' OR 1=1","片区",42));
        assertThrows(IllegalArgumentException.class,()->service.create("REGION","A","片\n区",42));
        verifyNoInteractions(mapper);
    }
    @Test public void invalidTypesAndActorsAreRejectedBeforeStorage() {
        assertThrows(IllegalArgumentException.class,()->service.summary("anything",42));
        assertThrows(IllegalArgumentException.class,()->service.summary("VILLAGE",0));verifyNoInteractions(mapper);
    }
    @Test public void missingTargetGroupCannotChangeAnyAssignments() {
        assertThrows(IllegalArgumentException.class,()->service.assign("REGION","MISSING",List.of("1"),42));
        verify(mapper,never()).lockOwnedPlots(anyList(),anyString());verify(mapper,never()).assign(anyList(),anyString(),anyString(),anyString());
    }
    @Test public void oneUnownedOrDeletedPlotBlocksWholeBatch() throws Exception {
        when(mapper.lockOwnedPlots(anyList(),eq("42"))).thenReturn(List.of(1L));
        assertThrows(IllegalArgumentException.class,()->service.assign("REGION","NORTH",List.of("1","2"),42));
        verify(mapper,never()).assign(anyList(),anyString(),anyString(),anyString());verify(connection).rollback();
    }
    @Test public void assigningRegionCannotReplaceVillageAndCommitsAfterAllRows() throws Exception {
        service.assign("REGION","NORTH",List.of("1","2"),42);
        var order=inOrder(mapper,connection);order.verify(mapper).lockOwnedPlots(List.of(1L,2L),"42");
        order.verify(mapper).assign(List.of(1L,2L),"REGION","NORTH","42");order.verify(connection).commit();
        verify(mapper,never()).unassign(anyList(),anyString());
    }
    @Test public void unexpectedRowCountRollsBackRatherThanClaimingPartialSuccess() throws Exception {
        when(mapper.assign(anyList(),anyString(),anyString(),anyString())).thenReturn(1);
        assertThrows(IllegalStateException.class,()->service.assign("REGION","NORTH",List.of("1","2"),42));
        verify(connection).rollback();verify(connection,never()).commit();
    }
    @Test public void removingVillageDoesNotRequireTargetOrTouchRegion() throws Exception {
        service.assign("VILLAGE",null,List.of("1","2"),42);
        verify(mapper).unassign(List.of(1L,2L),"VILLAGE");verify(mapper,never()).groupName(anyString(),anyString());verify(connection).commit();
    }
    @Test public void invalidDuplicateOrExcessiveIdsFailBeforeStorage() {
        for(List<String> ids:List.of(List.<String>of(),List.of("1","1"),List.of("0"),List.of("9223372036854775808"),Collections.nCopies(1001,"1")))
            assertThrows(IllegalArgumentException.class,()->service.assign("REGION","NORTH",ids,42));
        verifyNoInteractions(mapper);
    }
    @Test public void missingSchemaWriteRollsBackWithActionableMessage() throws Exception {
        when(mapper.isReady()).thenReturn(false);
        var error=assertThrows(IllegalArgumentException.class,()->service.assign("REGION","NORTH",List.of("1"),42));
        assertTrue(error.getMessage().contains("land_plot_grouping_ddl.sql"));verify(connection).rollback();
    }
}
