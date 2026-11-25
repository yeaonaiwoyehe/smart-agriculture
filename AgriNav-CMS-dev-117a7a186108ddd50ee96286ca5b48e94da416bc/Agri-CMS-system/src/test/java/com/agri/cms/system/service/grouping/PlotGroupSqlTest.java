package com.agri.cms.system.service.grouping;

import com.agri.cms.system.domain.LandPlot;
import com.agri.cms.system.domain.dto.LandPlotQueryDTO;
import com.agri.cms.system.mapper.LandPlotGroupingMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.session.Configuration;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class PlotGroupSqlTest {
    private final Configuration config=new Configuration();
    public PlotGroupSqlTest() { config.setMapUnderscoreToCamelCase(true);config.addMapper(LandPlotGroupingMapper.class); }
    @Test public void statsCountOnlyOwnedActivePlotsAndKeepUnassignedRows() {
        var sql=config.getMappedStatement(LandPlotGroupingMapper.class.getName()+".summary").getBoundSql(Map.of("type","REGION","userId","42")).getSql();
        assertTrue(sql.contains("COUNT(lp.id)"));assertTrue(sql.contains("lp.is_deleted=0"));
        assertTrue(sql.contains("NOT EXISTS"));assertEquals(2,sql.split("lp.create_by=",-1).length-1);
    }
    @Test public void assignmentsBindEachIdAndConflictKeyIncludesCategory() {
        String code="NORTH";
        var sql=config.getMappedStatement(LandPlotGroupingMapper.class.getName()+".assign")
                .getBoundSql(Map.of("ids",List.of(1L,2L),"type","REGION","code",code,"userId","42"));
        assertEquals(8,sql.getParameterMappings().size());assertFalse(sql.getSql().contains(code));
        assertTrue(sql.getSql().contains("ON CONFLICT (land_plot_id,group_type)"));
    }
    @Test public void ownerAndIdsAreBoundDuringLockAndRead() {
        for(String method:List.of("lockOwnedPlots","assignments")) {
            var sql=config.getMappedStatement(LandPlotGroupingMapper.class.getName()+"."+method)
                    .getBoundSql(Map.of("ids",List.of(1L,2L),"userId","42"));
            assertTrue(sql.getSql().contains("create_by=?"));assertEquals(3,sql.getParameterMappings().size());
        }
    }
    @Test public void groupFiltersUseBoundValuesAndDoNotAlterOldQueries() {
        var dto=new LandPlotQueryDTO();var query=new LambdaQueryWrapper<LandPlot>();
        PlotGroupFilter.apply(query,dto);assertTrue(query.getSqlSegment().isEmpty());
        dto.setGroupType("REGION");dto.setGroupCode("NORTH");PlotGroupFilter.apply(query,dto);
        assertTrue(query.getSqlSegment().contains("EXISTS"));assertFalse(query.getSqlSegment().contains("NORTH"));
        assertTrue(query.getParamNameValuePairs().containsValue("NORTH"));
    }
    @Test public void unassignedFilterTargetsOneCategoryAndRejectsContradictions() {
        var dto=new LandPlotQueryDTO();dto.setGroupType("VILLAGE");dto.setUngrouped(true);
        var query=new LambdaQueryWrapper<LandPlot>();PlotGroupFilter.apply(query,dto);assertTrue(query.getSqlSegment().contains("NOT EXISTS"));
        assertTrue(query.getParamNameValuePairs().containsValue("VILLAGE"));
        dto.setGroupCode("123456789012");assertThrows(IllegalArgumentException.class,()->PlotGroupFilter.apply(new LambdaQueryWrapper<>(),dto));
    }
}
