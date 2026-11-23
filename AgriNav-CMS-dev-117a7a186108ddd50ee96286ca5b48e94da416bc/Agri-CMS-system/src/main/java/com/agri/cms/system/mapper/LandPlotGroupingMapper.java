package com.agri.cms.system.mapper;

import com.agri.cms.system.service.grouping.PlotGroupSummary;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
@DS("pg")
public interface LandPlotGroupingMapper {
    @Select("SELECT to_regclass('public.land_plot_group') IS NOT NULL AND to_regclass('public.land_plot_group_assignment') IS NOT NULL")
    boolean isReady();

    @Select("""
        SELECT g.group_type, g.group_code, g.group_name, COUNT(lp.id) AS plot_count,
               COALESCE(SUM(lp.land_area::numeric),0) AS total_area
        FROM public.land_plot_group g
        LEFT JOIN public.land_plot_group_assignment a ON a.group_type=g.group_type AND a.group_code=g.group_code
        LEFT JOIN public.land_plot lp ON lp.id=a.land_plot_id AND lp.is_deleted=0 AND lp.create_by=#{userId}
        WHERE g.group_type=#{type}
        GROUP BY g.group_type,g.group_code,g.group_name
        UNION ALL
        SELECT #{type}, NULL, '未分组', COUNT(*), COALESCE(SUM(lp.land_area::numeric),0)
        FROM public.land_plot lp
        WHERE lp.is_deleted=0 AND lp.create_by=#{userId}
          AND NOT EXISTS (SELECT 1 FROM public.land_plot_group_assignment a WHERE a.land_plot_id=lp.id AND a.group_type=#{type})
        ORDER BY group_code NULLS FIRST
        """)
    List<PlotGroupSummary> summary(@Param("type") String type, @Param("userId") String userId);

    @Insert("""
        INSERT INTO public.land_plot_group(group_type,group_code,group_name,create_by)
        VALUES (#{type},#{code},#{name},#{userId}) ON CONFLICT (group_type,group_code) DO NOTHING
        """)
    int createGroup(@Param("type") String type,@Param("code") String code,@Param("name") String name,@Param("userId") String userId);

    @Select("SELECT group_name FROM public.land_plot_group WHERE group_type=#{type} AND group_code=#{code}")
    String groupName(@Param("type") String type,@Param("code") String code);

    @Select("""
        <script>SELECT id FROM public.land_plot WHERE is_deleted=0 AND create_by=#{userId} AND id IN
        <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>
        ORDER BY id FOR UPDATE</script>
        """)
    List<Long> lockOwnedPlots(@Param("ids") List<Long> ids,@Param("userId") String userId);

    @Insert("""
        <script>INSERT INTO public.land_plot_group_assignment(land_plot_id,group_type,group_code,update_by) VALUES
        <foreach collection='ids' item='id' separator=','>(#{id},#{type},#{code},#{userId})</foreach>
        ON CONFLICT (land_plot_id,group_type) DO UPDATE SET group_code=EXCLUDED.group_code,
            update_by=EXCLUDED.update_by,update_time=CURRENT_TIMESTAMP</script>
        """)
    int assign(@Param("ids") List<Long> ids,@Param("type") String type,@Param("code") String code,@Param("userId") String userId);

    @Delete("""
        <script>DELETE FROM public.land_plot_group_assignment WHERE group_type=#{type} AND land_plot_id IN
        <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>
        """)
    int unassign(@Param("ids") List<Long> ids,@Param("type") String type);

    @Select("""
        <script>SELECT a.land_plot_id::text AS "plotId",a.group_type AS "groupType",a.group_code AS "groupCode",g.group_name AS "groupName"
        FROM public.land_plot_group_assignment a
        JOIN public.land_plot_group g ON g.group_type=a.group_type AND g.group_code=a.group_code
        JOIN public.land_plot lp ON lp.id=a.land_plot_id AND lp.create_by=#{userId} AND lp.is_deleted=0
        WHERE a.land_plot_id IN
        <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>
        """)
    List<Map<String,Object>> assignments(@Param("ids") List<Long> ids,@Param("userId") String userId);
}
