package com.agri.cms.system.mapper;

import com.agri.cms.system.domain.ScreenStatistics;
import com.agri.cms.system.domain.dto.ScreenDTO;
import com.agri.cms.system.domain.vo.ProvinceVO;
import com.agri.cms.system.domain.vo.ScreenStatisticsVO;
import com.agri.cms.system.domain.vo.ScreenTaskVO;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * @author 李鸿麟
 * 大屏展示mapper
 */

@DS("pg")
public interface ScreenMapper extends BaseMapper<ScreenStatistics> {
    @Select("SELECT " +
            "    CAST(ti.work_type AS BIGINT) as work_id, " +
            "    cw.crop_id, " +
            "    SUM(ti.area) as total_area, " +
            "    COUNT(work_id) as work_count  " +
            "FROM trajectory_info ti " +
            "LEFT JOIN crop_work cw ON CAST(ti.work_type AS BIGINT) = cw.work_id " +
            "WHERE ti.status = 0 " +
            "    AND cw.is_available = true " +
            "GROUP BY ti.work_type, cw.crop_id " +
            "ORDER BY work_id, crop_id")
    List<ScreenStatistics> getScreenStatistics();





    @Select("SELECT " +
            "    fw.phone_number as phoneNumber, " +
            "    fw.create_time, " +
            "    fw.name as taskName, " +
            "    fw.status::varchar, " +
            "    lp.address as taskAddress " +
            "    FROM farm_work fw " +
            "    INNER JOIN farm_work_land_plot fwlp ON fw.id::varchar = fwlp.farm_work_id " +
            "    INNER JOIN land_plot lp ON lp.id::varchar = fwlp.land_plot_id" +
            "    WHERE fw.status != '2' ")
    List<ScreenTaskVO> selectScreenList();


    @Select("SELECT DATE_TRUNC('month', created_time) AS month, COUNT(*) AS count " +
            "FROM trajectory_info " +
            "WHERE status != 2 " +
            "GROUP BY month " +
            "ORDER BY month "
            )
    List<Map<String, Object>> countTrajectoriesByMonth();

    @Select("SELECT DATE_TRUNC('month', create_time) AS month, COUNT(*) AS count " +
            "FROM land_plot " +
            "WHERE is_deleted != 1 "+
            "GROUP BY month " +
            "ORDER BY month "
           )
    List<Map<String, Object>> countLanPlotsByMonth();

    @Select("SELECT " +
            "  a.name AS name, " +
            "  COUNT(lp.id) AS value " +
            "FROM " +
            "  address a " +
            "LEFT JOIN " +
            "  land_plot lp ON lp.address LIKE CONCAT('%', a.name, '%') " +
            "  AND lp.is_deleted != 1 " +
            "WHERE " +
            "  a.level = 2 " +
            "  AND a.pcode = CAST(#{provinceCode} AS bigint) " +
            "GROUP BY " +
            "  a.name")
    List<ProvinceVO> countLandPlotCountsInDetailedProvince(String provinceCode);

    @Select("SELECT " +
            "  a.name AS name, " +
            "  COUNT(lp.id) AS value " +
            "FROM " +
            "  address a " +
            "LEFT JOIN " +
            "  land_plot lp ON lp.address LIKE CONCAT('%', a.name, '%') " +
            "  AND lp.is_deleted != 1 " +
            "WHERE " +
            "  a.level = 3 " +
            "AND a.pcode IN (SELECT code FROM address WHERE pcode = CAST(#{provinCode} AS bigint))" +
            "GROUP BY " +
            "  a.name")
    List<ProvinceVO> countLandPlotCountsInMunicipality(String provinceCode);



    @Select("SELECT " +
            "  a.name AS name, " +
            "  COUNT(lp.id) AS value " +
            "FROM " +
            "  address a " +
            "LEFT JOIN " +
            "  land_plot lp ON lp.address LIKE CONCAT('%', a.name, '%') " +
            "  AND lp.is_deleted != 1 " +
            "WHERE " +
            "  a.level = 3 " +
            "AND a.pcode  = CAST(#{provinCode} AS bigint) " +
            "GROUP BY " +
            "  a.name")
    List<ProvinceVO> countLandPlotCountsInHubeiHainanXinjiang(String provinceCode);


    @Select("SELECT " +
            "  a.name AS name, " +
            "  CAST(a.code AS varchar) AS value " +
            "FROM address a " +
            "WHERE " +
            "  a.level = 2 " +
            "  AND a.pcode = CAST(#{provinceCode} AS bigint) ")
    List<ScreenDTO> selectProvinceNamesAndCodes(String provinceCode);
}
