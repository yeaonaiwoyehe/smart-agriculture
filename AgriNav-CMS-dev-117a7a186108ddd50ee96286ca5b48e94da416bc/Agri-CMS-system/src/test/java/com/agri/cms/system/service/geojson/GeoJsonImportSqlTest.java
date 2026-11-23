package com.agri.cms.system.service.geojson;

import com.agri.cms.system.mapper.GeoJsonLandPlotImportMapper;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

/** 校验 MyBatis 注解 SQL、批量参数和 record 属性绑定，不执行数据库语句。 */
public class GeoJsonImportSqlTest {
    private Configuration configuration() {
        Configuration config=new Configuration();config.addMapper(GeoJsonLandPlotImportMapper.class);return config;
    }
    @Test public void duplicateLookupUsesBoundParametersInsteadOfInterpolatedCodes() {
        Configuration config=configuration();String value="A' OR 1=1 --";
        BoundSql sql=config.getMappedStatement(GeoJsonLandPlotImportMapper.class.getName()+".existingCodes")
                .getBoundSql(Map.of("codes",List.of(value,"B")));
        assertEquals(2,sql.getParameterMappings().size());assertFalse(sql.getSql().contains(value));
        assertEquals(value,sql.getAdditionalParameter(sql.getParameterMappings().get(0).getProperty()));
    }
    @Test public void plotInsertBindsRecordPropertiesAndComputesAreaInMetres() {
        Configuration config=configuration();ParsedGeoJsonPlot plot=new GeoJsonPlotParser()
                .parse(GeoJsonPlotParserTest.file(GeoJsonPlotParserTest.feature("A")),true).getPlots().get(0);
        Map<String,Object> params=new HashMap<>();params.put("plot",plot);params.put("id",1L);
        params.put("wkt",plot.geometry().toText());params.put("centre",plot.geometry().getInteriorPoint().toText());params.put("userId","42");
        BoundSql sql=config.getMappedStatement(GeoJsonLandPlotImportMapper.class.getName()+".insertPlot").getBoundSql(params);
        assertTrue(sql.getSql().contains("::geography"));assertTrue(sql.getSql().contains("0.0015"));
        assertTrue(sql.getSql().contains("4326"));
        for(var mapping:sql.getParameterMappings()) assertNotNull(config.newMetaObject(params).getValue(mapping.getProperty()));
    }
    @Test public void boundaryUsesOutwardRoundingAndAParameterizedPlotId() {
        Configuration config=configuration();BoundSql sql=config.getMappedStatement(GeoJsonLandPlotImportMapper.class.getName()+".insertBoundary")
                .getBoundSql(Map.of("id",1L));
        assertTrue(sql.getSql().contains("floor("));assertTrue(sql.getSql().contains("ceil("));
        assertEquals("id",sql.getParameterMappings().get(0).getProperty());
    }
    @Test public void sampleFilePassesTheSameParser() throws Exception {
        java.nio.file.Path path=java.nio.file.Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while(path!=null&&!java.nio.file.Files.exists(path.resolve("database/samples/devb-land-plots.geojson")))path=path.getParent();
        assertNotNull("Cannot find repository sample",path);
        try(var input=java.nio.file.Files.newInputStream(path.resolve("database/samples/devb-land-plots.geojson"))) {
            assertEquals(0,new GeoJsonPlotParser().parse(input,true).getInvalidCount());
        }
    }
}