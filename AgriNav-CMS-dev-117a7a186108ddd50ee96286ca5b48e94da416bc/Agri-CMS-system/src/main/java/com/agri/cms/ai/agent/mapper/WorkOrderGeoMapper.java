package com.agri.cms.ai.agent.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 作业空间判断 Mapper（A-12，B-09 点是否在多边形内）。
 *
 * 基于 PostGIS ST_Contains 判断某个定位点是否落在地块边界多边形内，
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
    @Select("SELECT COUNT(*) FROM land_plot "
            + "WHERE id = #{plotId} "
            + "AND is_deleted != 1 "
            + "AND ST_Contains(boundary, ST_SetSRID(ST_MakePoint(#{longitude}, #{latitude}), 4326))")
    Integer countPointInside(@Param("plotId") Long plotId,
                             @Param("longitude") Double longitude,
                             @Param("latitude") Double latitude);
}
