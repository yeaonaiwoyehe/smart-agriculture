package com.agri.cms.system.service;

import com.agri.cms.system.domain.ScreenStatistics;
import com.agri.cms.system.domain.vo.*;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * @author 李鸿麟
 * 大屏展示接口nm,.lnjnj
 */

public interface IScreenService extends IService<ScreenStatistics> {
    List<ScreenStatisticsVO> getFieldWorkStatistics();

    List<ScreenTaskVO> getFarmWorkStatistics();

    ScreenPercentVO getUserStatistics();

    ScreenPercentVO getFarmWorkTaskStatistics();

    ProvinceVO getProvinceWithMostFarmWork();

    ScreenStatisticsVO getFieldWorkTypeWithBiggestArea();

    ScreenStatisticsVO getFieldWorkTypeWithBiggestCountNum();

    ScreenStatisticsVO getHottestCrop();

    List<ProvinceVO> getCropDetails();

    List<ProvinceVO> getTrajectoryCountsByMonth();

    List<ProvinceVO> getLandPlotCountsByMonth();

    LandPlotAreaVO countLandPlotsTotalArea();

    List<ProvinceVO> getOneProvinceLandPlotCounts(String provinceCode);
}

