package com.agri.cms.system.mapper;

import com.agri.cms.system.domain.LandPlot;
import com.agri.cms.system.domain.vo.HeatmapDataVO;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;


import java.util.List;
import java.util.Map;

/**
 * @author mfr
 * @since 2024/10/22
 */
@Mapper
@DS("pg")
public interface LandPlotMapper extends BaseMapper<LandPlot> {

    List<LandPlot> queryByConditions(Map<String, Object> conditions);

    Long countByConditions(Map<String, Object> conditions);

    String queryBoundaryByLocation(String location);

    List<String> queryPointByLocation(String location);

    @MapKey("id")
    List<HeatmapDataVO> queryHeatmapData();

    @Select("SELECT SUM(land_area) FROM land_plot WHERE is_deleted != 1")
    Float sumLandArea();

    void subtractArea(Long landPlotId, Float area);

    @Select("SELECT DISTINCT create_by FROM land_plot")
    IPage<String> selectUserIdsPage(Page<?> page);
}
