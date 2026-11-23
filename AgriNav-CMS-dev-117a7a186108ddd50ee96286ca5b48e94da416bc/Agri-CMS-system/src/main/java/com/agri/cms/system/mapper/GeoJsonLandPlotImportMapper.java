package com.agri.cms.system.mapper;

import com.agri.cms.system.service.geojson.ParsedGeoJsonPlot;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.*;
import java.util.List;

/** 所有空间运算由 PostGIS 完成；面积转亩后写入现有 land_area。 */
@Mapper
@DS("pg")
public interface GeoJsonLandPlotImportMapper {
    @Select("SELECT to_regclass('public.land_plot_import_code') IS NOT NULL")
    boolean hasCodeTable();

    @Select("<script>SELECT plot_code FROM public.land_plot_import_code WHERE plot_code IN "
            + "<foreach collection='codes' item='code' open='(' separator=',' close=')'>#{code}</foreach></script>")
    List<String> existingCodes(@Param("codes") List<String> codes);

    @Insert("""
            INSERT INTO public.land_plot
                (id, land_name, land_type, land_area, is_deleted, address,
                 create_by, create_time, update_by, update_time, measurement_method, boundary, center_point)
            VALUES (#{id}, #{plot.landName}, #{plot.landType},
                    ST_Area(ST_GeomFromText(#{wkt},4326)::geography) * 0.0015,
                    0, #{plot.address}, #{userId}, CURRENT_TIMESTAMP, #{userId}, CURRENT_TIMESTAMP,
                    'GeoJSON', ST_GeomFromText(#{wkt},4326), ST_GeomFromText(#{centre},4326))
            """)
    int insertPlot(@Param("id") long id, @Param("plot") ParsedGeoJsonPlot plot,
                   @Param("wkt") String wkt, @Param("centre") String centre, @Param("userId") String userId);

    @Insert("""
            INSERT INTO public.boundary
                (id,left_down_longitude,left_down_latitude,right_top_longitude,right_top_latitude,land_plot_id,status)
            SELECT id, floor(ST_XMin(Box3D(boundary))*1000000)::bigint,
                       floor(ST_YMin(Box3D(boundary))*1000000)::bigint,
                       ceil(ST_XMax(Box3D(boundary))*1000000)::bigint,
                       ceil(ST_YMax(Box3D(boundary))*1000000)::bigint, id, 0
            FROM public.land_plot WHERE id=#{id}
            """)
    int insertBoundary(@Param("id") long id);

    @Insert("""
            INSERT INTO public.land_plot_import_code(plot_code,land_plot_id,create_by,create_time)
            VALUES (#{code},#{id},#{userId},CURRENT_TIMESTAMP)
            """)
    int insertCode(@Param("code") String code, @Param("id") long id, @Param("userId") String userId);
}