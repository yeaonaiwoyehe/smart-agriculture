package com.agri.cms.system.service.impl;

import com.agri.cms.system.domain.TrajectoryInfo;
import com.agri.cms.system.domain.vo.SortMessage;
import com.agri.cms.system.mapper.TrajectoryInfoMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** 直接捕获旧查询入口的 SQL 条件，防止把已完成状态误当成已删除。 */
public class TrajectoryListStatusTest {
    @Test public void bothLegacyListsKeepCompletedRecordsAndExcludeOnlyDeleted() throws Exception {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(),"status-test"),TrajectoryInfo.class);
        var mapper=mock(TrajectoryInfoMapper.class);var seen=new ArrayList<String>();
        when(mapper.selectPage(any(Page.class),any())).thenAnswer(call->{
            LambdaQueryWrapper<TrajectoryInfo> filter=call.getArgument(1);
            String sql=filter.getSqlSegment();assertTrue(sql.contains("status IS NULL"));assertTrue(sql.contains("status <>"));
            assertTrue(filter.getParamNameValuePairs().containsValue(2));assertFalse(sql.contains("status ="));seen.add(sql);
            return new Page<TrajectoryInfo>(1,10,0);
        });
        var service=new TrajectoryPointsServiceImpl();var field=TrajectoryPointsServiceImpl.class.getDeclaredField("trajectoryInfoMapper");field.setAccessible(true);field.set(service,mapper);
        service.queryAll("15888888888",1,10);service.queryByConditions(new SortMessage(),"15888888888",1,10);assertEquals(2,seen.size());
    }
}
