package com.agri.cms.system.service.playback;

import com.agri.cms.system.mapper.TrajectoryPlaybackMapper;
import org.apache.ibatis.session.Configuration;
import org.junit.Test;
import java.time.LocalDateTime;
import java.util.Map;
import static org.junit.Assert.*;

public class PlaybackSqlTest {
    @Test public void rangesAndIdsAreBoundAndBothStatusesAreQueryable() {
        var config=new Configuration();config.addMapper(TrajectoryPlaybackMapper.class);
        var sql=config.getMappedStatement(TrajectoryPlaybackMapper.class.getName()+".orders").getBoundSql(Map.of("phone","15888888888","start",LocalDateTime.now(),"end",LocalDateTime.now()));
        assertFalse(sql.getSql().contains("15888888888"));assertTrue(sql.getSql().contains("start_time <="));assertFalse(sql.getSql().contains("&lt;"));assertTrue(sql.getSql().contains("LIMIT 201"));assertFalse(sql.getSql().contains("status=0"));
        var archive=config.getMappedStatement(TrajectoryPlaybackMapper.class.getName()+".archive").getBoundSql(Map.of("id","A' OR 1=1"));assertFalse(archive.getSql().contains("OR 1=1"));assertTrue(archive.getSql().contains("ST_AsText(points) AS geometry,times"));
        assertTrue(sql.getSql().contains("status IS NULL OR status<>2"));
        assertTrue(config.getMappedStatement(TrajectoryPlaybackMapper.class.getName()+".order").getBoundSql(Map.of("id","A")).getSql().contains("status IS NULL OR status<>2"));
    }
}
