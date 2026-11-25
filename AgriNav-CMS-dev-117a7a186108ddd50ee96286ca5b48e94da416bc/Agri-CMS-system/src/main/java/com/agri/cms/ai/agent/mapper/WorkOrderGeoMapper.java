package com.agri.cms.ai.agent.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 作业空间判断 Mapper（A-12，B-09 点是否在多边形内）。
 *
 * 基于 PostGIS ST_Covers 判断，外边界与空洞边缘计为边界；排除区及其边缘不计。
 * 供开始/结束作业的超界提醒与独立的超界查询使用。
 *
 * @author agrinav
 */
@Mapper
@DS("pg")
public interface WorkOrderGeoMapper {

    /**
     * 统计定位点是否在指定地块边界内。
     *
     * @param plotId    地块 ID
     * @param longitude 定位经度
     * @param latitude  定位纬度
     * @return 命中行数（1 表示在界内，0 表示超界或地块不存在）
     */
    @Select("""
        SELECT COALESCE((SELECT CASE
          WHEN p.boundary IS NULL OR ST_IsEmpty(p.boundary) OR NOT ST_IsValid(p.boundary)
            OR ST_SRID(p.boundary)<>4326 THEN 'UNAVAILABLE'
          WHEN p.land_type='插花地' AND EXISTS (SELECT 1 FROM public.land_plot_exclude e WHERE e.land_plot_id=p.id AND e.is_deleted=0
            AND (e.boundary IS NULL OR ST_IsEmpty(e.boundary) OR NOT ST_IsValid(e.boundary) OR ST_SRID(e.boundary)<>4326)) THEN 'UNAVAILABLE'
          WHEN NOT ST_Covers(p.boundary,ST_SetSRID(ST_MakePoint(#{longitude},#{latitude}),4326)) THEN 'OUTSIDE'
          WHEN p.land_type='插花地' AND EXISTS (SELECT 1 FROM public.land_plot_exclude e WHERE e.land_plot_id=p.id AND e.is_deleted=0
            AND ST_Covers(e.boundary,ST_SetSRID(ST_MakePoint(#{longitude},#{latitude}),4326))) THEN 'EXCLUDED'
          WHEN ST_Intersects(ST_Boundary(p.boundary),ST_SetSRID(ST_MakePoint(#{longitude},#{latitude}),4326)) THEN 'BOUNDARY'
          ELSE 'INSIDE' END FROM public.land_plot p WHERE p.id=#{plotId} AND p.is_deleted=0),'UNAVAILABLE')
        """)
    String pointState(@Param("plotId") Long plotId,@Param("longitude") Double longitude,@Param("latitude") Double latitude);

    /** 保留 A-12 既有调用签名，坏边界与数据源失败不伪装成界外。 */
    default Integer countPointInside(Long plotId,Double longitude,Double latitude) {
        com.agri.cms.system.service.boundary.BoundaryCoordinates.validate(longitude,latitude);
        String state=pointState(plotId,longitude,latitude);
        if(state==null||!java.util.Set.of("INSIDE","BOUNDARY","OUTSIDE","EXCLUDED").contains(state))throw new IllegalArgumentException("地块边界缺失或无效，暂时无法核对是否超界");
        return state.equals("INSIDE")||state.equals("BOUNDARY")?1:0;
    }
}
