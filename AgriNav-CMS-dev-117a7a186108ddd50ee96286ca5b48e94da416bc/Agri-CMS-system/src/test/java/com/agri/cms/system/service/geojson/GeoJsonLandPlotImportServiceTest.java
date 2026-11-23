package com.agri.cms.system.service.geojson;

import com.agri.cms.common.core.redis.RedisCache;
import com.agri.cms.system.mapper.GeoJsonLandPlotImportMapper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import javax.sql.DataSource;
import java.sql.Connection;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Spring 的实际事务拦截器 + 模拟连接，验证 commit/rollback；不访问共享库。 */
public class GeoJsonLandPlotImportServiceTest {
    private final GeoJsonLandPlotImportMapper mapper=mock(GeoJsonLandPlotImportMapper.class);
    private final RedisCache redis=mock(RedisCache.class);
    private final DataSource source=mock(DataSource.class);
    private final Connection connection=mock(Connection.class);
    private GeoJsonLandPlotImportService service;
    private GeoJsonLandPlotImportService target;
    @Before public void setup() throws Exception {
        when(source.getConnection()).thenReturn(connection);when(connection.getAutoCommit()).thenReturn(true);
        when(mapper.hasCodeTable()).thenReturn(true);when(mapper.existingCodes(anyList())).thenReturn(Collections.emptyList());
        when(mapper.insertPlot(anyLong(),any(),anyString(),anyString(),anyString())).thenReturn(1);
        when(mapper.insertBoundary(anyLong())).thenReturn(1);when(mapper.insertCode(anyString(),anyLong(),anyString())).thenReturn(1);
        target=new GeoJsonLandPlotImportService(new GeoJsonPlotParser(),mapper,redis);
        ProxyFactory proxy=new ProxyFactory(target);
        proxy.addAdvice(new TransactionInterceptor(new DataSourceTransactionManager(source),new AnnotationTransactionAttributeSource()));
        service=(GeoJsonLandPlotImportService)proxy.getProxy();
    }
    @Test public void previewDoesNotConnectToDatabaseOrWriteCache() throws Exception {
        GeoJsonImportResult result=service.preview(GeoJsonPlotParserTest.file(GeoJsonPlotParserTest.feature("A")));
        assertTrue(result.isDryRun());assertFalse(result.isDatabaseChecked());assertEquals(0,result.getImportedCount());
        verifyNoInteractions(mapper,source,redis);
    }
    @Test public void invalidFileWritesNothing() {
        var f=GeoJsonPlotParserTest.feature("A");((com.fasterxml.jackson.databind.node.ObjectNode)f.get("properties")).remove("address");
        GeoJsonImportResult result=service.importPlots(GeoJsonPlotParserTest.file(f,GeoJsonPlotParserTest.feature("B")),1);
        assertEquals(1,result.getInvalidCount());assertEquals(0,result.getImportedCount());verifyNoInteractions(mapper,redis);
    }
    @Test public void missingMigrationProducesActionableErrorAndRollback() throws Exception {
        when(mapper.hasCodeTable()).thenReturn(false);
        IllegalStateException error=assertThrows(IllegalStateException.class,()->service.importPlots(GeoJsonPlotParserTest.file(GeoJsonPlotParserTest.feature("A")),1));
        assertTrue(error.getMessage().contains("geojson_import_code_ddl.sql"));verify(connection).rollback();verify(connection,never()).commit();
    }
    @Test public void existingCodeBlocksEntireBatch() {
        when(mapper.existingCodes(anyList())).thenReturn(List.of("A"));
        var result=service.importPlots(GeoJsonPlotParserTest.file(GeoJsonPlotParserTest.feature("A"),GeoJsonPlotParserTest.feature("B")),1);
        assertTrue(result.isDatabaseChecked());assertEquals(1,result.getInvalidCount());assertEquals(0,result.getImportedCount());
        verify(mapper,never()).insertPlot(anyLong(),any(),anyString(),anyString(),anyString());verifyNoInteractions(redis);
    }
    @Test public void successfulImportCommitsBeforeLocationCacheAndReturnsStringIds() throws Exception {
        var result=service.importPlots(GeoJsonPlotParserTest.file(GeoJsonPlotParserTest.feature("A"),GeoJsonPlotParserTest.feature("B")),42);
        assertEquals(2,result.getImportedCount());assertEquals("IMPORTED",result.getItems().get(0).getStatus());
        assertNotNull(result.getItems().get(0).getPlotId());
        ArgumentCaptor<Long> ids=ArgumentCaptor.forClass(Long.class);
        verify(mapper,times(2)).insertPlot(ids.capture(),any(),anyString(),anyString(),eq("42"));
        assertNotEquals(ids.getAllValues().get(0),ids.getAllValues().get(1));
        var order=inOrder(mapper,connection,redis);order.verify(mapper,times(2)).insertCode(anyString(),anyLong(),eq("42"));
        order.verify(connection).commit();order.verify(redis,times(2)).setCacheGeoValue(anyString(),any(),any());
        verify(connection,never()).rollback();
    }
    @Test public void secondRowWriteFailureRollsBackEarlierWritesAndNeverUpdatesCache() throws Exception {
        when(mapper.insertBoundary(anyLong())).thenReturn(1,0);
        assertThrows(IllegalStateException.class,()->service.importPlots(GeoJsonPlotParserTest.file(GeoJsonPlotParserTest.feature("A"),GeoJsonPlotParserTest.feature("B")),1));
        verify(mapper,times(2)).insertPlot(anyLong(),any(),anyString(),anyString(),anyString());
        verify(connection).rollback();verify(connection,never()).commit();verifyNoInteractions(redis);
    }
    @Test public void concurrentCodeCollisionRollsBackAndReportsConflict() throws Exception {
        when(mapper.insertCode(anyString(),anyLong(),anyString())).thenThrow(new DuplicateKeyException("race"));
        IllegalStateException error=assertThrows(IllegalStateException.class,()->service.importPlots(GeoJsonPlotParserTest.file(GeoJsonPlotParserTest.feature("A")),1));
        assertTrue(error.getMessage().contains("其他导入占用"));verify(connection).rollback();verify(connection,never()).commit();verifyNoInteractions(redis);
    }
    @Test public void databaseFailurePropagatesWithoutClaimingSuccess() throws Exception {
        when(mapper.insertPlot(anyLong(),any(),anyString(),anyString(),anyString())).thenThrow(new IllegalStateException("offline"));
        assertThrows(IllegalStateException.class,()->service.importPlots(GeoJsonPlotParserTest.file(GeoJsonPlotParserTest.feature("A")),1));
        verify(connection).rollback();verifyNoInteractions(redis);
    }
    @Test public void cacheFailureAfterCommitReturnsWarningRatherThanPretendingRollback() throws Exception {
        doThrow(new IllegalStateException("redis offline")).when(redis).setCacheGeoValue(anyString(),any(),any());
        var result=service.importPlots(GeoJsonPlotParserTest.file(GeoJsonPlotParserTest.feature("A")),1);
        assertEquals(1,result.getImportedCount());assertEquals(1,result.getWarnings().size());verify(connection).commit();verify(connection,never()).rollback();
    }
    @Test public void actorCannotBeProvidedAsUntrustedFileMetadata() {
        var f=GeoJsonPlotParserTest.feature("A");((com.fasterxml.jackson.databind.node.ObjectNode)f.get("properties")).put("createBy","999");
        service.importPlots(GeoJsonPlotParserTest.file(f),42);
        verify(mapper).insertPlot(anyLong(),any(),anyString(),anyString(),eq("42"));
    }
}