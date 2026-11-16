package com.agri.cms.system.service;

import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.system.domain.LandPlot;
import com.agri.cms.system.domain.dto.LandPlotQueryDTO;
import com.agri.cms.system.domain.record.LandPlotRecord;
import com.agri.cms.system.domain.vo.HeatmapDataVO;
import com.agri.cms.system.domain.vo.LandPlotVO;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author simon
 * @since 2024/9/19
 */
@DS("pg")
public interface LandPlotService extends IService<LandPlot> {

    /**
     * 根据用户id或部门id查询地块信息
     *
     * @return List<LandPlotRecord>
     * @author simon
     * @since 2024/9/20
     */
    List<LandPlot> getLandPlotByCreatorName(Long userId, Long current, Long size);


    /**
     * 更新地块信息
     *
     * @return Map<String, Object>
     * @author simon
     * @since 2024/9/20
     */
    AjaxResult updateLandPlot(LandPlotVO landPlot, MultipartFile file);


    /**
     * 新增地块信息
     *
     * @return Map<String, Object>
     * @author simon
     * @since 2024/9/20
     */
    AjaxResult addLandPlot(LandPlotVO landPlot, MultipartFile file);

    /**
     * 根据经纬度查询地块信息
     *
     * @return List<LandPlotRecord>
     * @author simon
     * @since 2024/9/22
     */
    List<LandPlot> checkByCoordinate(Double longitude, Double latitude, Double distance);

    /**
     * 根据条件查询地块信息
     *
     * @return List<LandPlotRecord>
     * @author mfr
     * @since 2024/10/21
     */
    AjaxResult queryByConditions(LandPlotQueryDTO landPlotQueryDTO);

    /**
     * 获取热力图数据
     *
     * @return List<Map < String, Object>>
     * @author mfr
     * @since 2024/10/21
     */
    List<HeatmapDataVO> getHeatmapData();


    AjaxResult getArea(Long address, int level);

    double getAreaByAddress(String address);

    void deleteByUsername(String username);

    void deleteById(String id);

    /**
     * 获取删除的历史地块
     * @return List<LandPlotRecord>
     * @author simon
     * @since 2024/12/7
     */

    List<LandPlot> getHistoryLandPlots(Page<LandPlot> page, Long userId, String username);

    /**
     * 恢复删除的地块
     * @param id 地块id
     * @author simon
     * @since 2024/12/7
     */
    void restoreLandPlot(Long id);
}
