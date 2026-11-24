package com.agri.cms.system.mapper;

import com.agri.cms.system.domain.TrajectoryInfo;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
@DS("pg")
public interface TrajectoryPlaybackMapper {
    @Select("SELECT * FROM public.trajectory_info WHERE trajectory_info_id=#{id} AND (status IS NULL OR status<>2)")
    TrajectoryInfo order(@Param("id") String id);

    @Select("""
        SELECT * FROM public.trajectory_info WHERE phone_number=#{phone}
        AND (status IS NULL OR status<>2)
        AND start_time <= #{end} AND COALESCE(end_time,#{end}) >= #{start}
        ORDER BY start_time,trajectory_info_id LIMIT 201
        """)
    List<TrajectoryInfo> orders(@Param("phone") String phone,@Param("start") LocalDateTime start,@Param("end") LocalDateTime end);

    record Archive(String geometry,String times) { }
    @Select("SELECT ST_AsText(points) AS geometry,times FROM public.trajectory_points WHERE trajectory_id=#{id}")
    Archive archive(@Param("id") String id);
}
