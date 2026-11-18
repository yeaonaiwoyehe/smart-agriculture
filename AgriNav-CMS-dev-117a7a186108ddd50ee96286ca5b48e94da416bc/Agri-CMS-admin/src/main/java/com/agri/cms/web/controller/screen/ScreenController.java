package com.agri.cms.web.controller.screen;


import cn.hutool.core.collection.CollectionUtil;
import com.agri.cms.system.domain.vo.*;
import com.agri.cms.system.service.impl.ScreenServiceImpl;
import com.agri.cms.common.core.domain.AjaxResult;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.service.annotation.GetExchange;

import java.util.Collections;
import java.util.List;


/**
 * @author 李鸿麟
 * 大屏展示Controller
 */
@RestController
@RequestMapping("/screen")
public class ScreenController {

    @Resource
    private ScreenServiceImpl screenService;


    /**
     * 此方法用于返回作物的工作情况
     *
     * @return List<ScreenStatistics>
     * @author 李鸿麟
     * @since 2024/10/29
     */
    @GetMapping("/getScreenStatistics")
    @PreAuthorize("@ss.hasPermi('system:screen:display')")
    public List<ScreenStatisticsVO> getScreenStatistics() {
        List<ScreenStatisticsVO> screenStatistics = screenService.getFieldWorkStatistics();
        if (CollectionUtil.isNotEmpty(screenStatistics)) {
            return screenStatistics;
        }
        return Collections.emptyList();
    }

    /**
     * 此方法用于返回农机的相关信息
     *
     * @return List<AgriculturalMachinery>
     * @author 李鸿麟
     * @since 2024/10/29
     */
    @GetMapping("/getFarmWorkInfo")
    @PreAuthorize("@ss.hasPermi('system:screen:display')")
    public List<ScreenTaskVO> getFarmWorkInfo() {
        return screenService.getFarmWorkStatistics();
    }

    /**
     * 此方法用于返回活跃用户的百分比
     *
     * @return List<LoginUser>
     * @author 李鸿麟
     * @since 2024/10/29
     */
    @GetMapping("/getActiveUserPercentage")
    @PreAuthorize("@ss.hasPermi('system:screen:display')")
    public ScreenPercentVO getActiveUserPercentage() {
        return screenService.getUserStatistics();
    }

    /**
     * 此方法用于返回已完成任务的百分比
     *
     * @return ScreenPercentVO
     * @author 李鸿麟
     * @since 2024/11/10
     */
    @GetMapping("/getFinishedFarmWorkPercentage")
    @PreAuthorize("@ss.hasPermi('system:screen:display')")
    public ScreenPercentVO getFinishedFarmWorkPercentage() {
        return screenService.getFarmWorkTaskStatistics();
    }


    /**
     * 此方法用于返回全国各省的地块数量
     *
     * @return List<ProvinceVO>
     * @author 李鸿麟
     * @since 2024/11/11
     */
    @GetMapping("/getLandPlotsWithinProvince")
    @PreAuthorize("@ss.hasPermi('system:screen:display')")
    public List<ProvinceVO> getLandPlotsWithinProvince() {
        return screenService.getLandPlotsWithinProvince();
    }

    /**
     * 此方法用于返回发布最多农活的省份
     *
     * @return ProvinceVO
     * @author 李鸿麟
     * @since 2024/11/11
     */
    @GetMapping("/getMostFarmWorkProvince")
    @PreAuthorize("@ss.hasPermi('system:screen:display')")
    public ProvinceVO getMostFarmWorkProvince() {
        return screenService.getProvinceWithMostFarmWork();
    }


    /**
     * 此方法用于返回工作面积最大的田地操作
     *
     * @return ProvinceVO
     * @author 李鸿麟
     * @since 2024/11/11
     */
    @GetMapping("/getBiggestFieldWorkType")
    @PreAuthorize("@ss.hasPermi('system:screen:display')")
    public ScreenStatisticsVO getBiggestFieldWorkType() {
        return screenService.getFieldWorkTypeWithBiggestArea();
    }

    /**
     * 此方法用于返回工作次数最多的田地操作
     *
     * @return ProvinceVO
     * @author 李鸿麟
     * @since 2024/11/11
     */
    @GetMapping("/getMostFieldWorkType")
    @PreAuthorize("@ss.hasPermi('system:screen:display')")
    public ScreenStatisticsVO getMostFieldWorkType() {
        return screenService.getFieldWorkTypeWithBiggestCountNum();
    }

    /**
     * 此方法用于返回最热门的作物
     *
     * @return ProvinceVO
     * @author 李鸿麟
     * @since 2024/11/11
     */
    @GetMapping("/getMostCrop")
    @PreAuthorize("@ss.hasPermi('system:screen:display')")
    public ScreenStatisticsVO getMostCrop() {
        return screenService.getHottestCrop();
    }


    /**
     * 此方法用于返回各个作物的工作总亩数
     * @return List<ProvinceVO>
     * @author 李鸿麟
     * @since 2024/11/12
     */
    @GetMapping("/getCropSituation")
    @PreAuthorize("@ss.hasPermi('system:screen:display')")
    public List<ProvinceVO> getCropSituation() {
        return screenService.getCropDetails();
    }

    /**
     * 此方法用于返回每个月的轨迹数量
     * @return List<ProvinceVO>
     * @author 李鸿麟
     * @since 2024/11/12     */
    @GetMapping("/getTrajectoryNumByMonth")
    @PreAuthorize("@ss.hasPermi('system:screen:display')")
    public List<ProvinceVO> getTrajectoryNumByMonth() {
        return screenService.getTrajectoryCountsByMonth();
    }


    /**
     * 此方法用于返回每月的地块数量
     * @return List<ProvinceVO>
     * @author 李鸿麟
     * @since 2024/11/12     */
    @GetMapping("/getLandPlotNumByMonth")
    @PreAuthorize("@ss.hasPermi('system:screen:display')")
    public List<ProvinceVO> getLandPlotNumByMonth() {
        return screenService.getLandPlotCountsByMonth();
    }

    /**
     * 此方法用于返回地块总面积
     * @return LandPlotAreaVO
     * @author 李鸿麟
     * @since 2024/11/15     */
    @GetMapping("/getLandPlotsTotalArea")
    @PreAuthorize("@ss.hasPermi('system:screen:display')")
    public LandPlotAreaVO getLandPlotsTotalArea() {
        return screenService.countLandPlotsTotalArea();
    }

    /**
     * 此方法用于返回全国省份的地块数量
     * @return List<ProvinceVO>
     * @author 李鸿麟
     * @since 2024/11/24     */
    @GetMapping("/getLandPlotCountByProvice")
    @PreAuthorize("@ss.hasPermi('system:screen:display')")
    public List<ProvinceVO> getLandPlotCountInProvince() {
        return screenService.getLandPlotsWithinProvince();
    }


    /**
     * 此方法用于返回某个省份下属行政区划详细的地块数量
     * @param provinceCode 省份代码
     * @return List<ProvinceVO>
     * @author 李鸿麟
     * @since 2024/11/22     */
    @GetMapping("/getLandPlotCountInDetailedProvince")
    @PreAuthorize("@ss.hasPermi('system:screen:display')")
    public List<ProvinceVO> getLandPlotCountInDetailedProvince(@RequestParam @NotNull(message = "传入省份编码不可为空") String provinceCode) {
        return screenService.getOneProvinceLandPlotCounts(provinceCode);
    }

    /**
     * 手动触发大屏统计刷新
     * 定时任务默认每天凌晨 1 点执行 updateScreenStatistics；此端点用于演示或运维时立即重建
     * screen_statistics 表与 Redis 缓存（screen:statistics:list）。
     *
     * @return AjaxResult
     */
    @GetMapping("/refresh")
    @PreAuthorize("@ss.hasPermi('system:screen:display')")
    public AjaxResult refreshScreenStatistics() {
        screenService.updateScreenStatistics();
        return AjaxResult.success("大屏统计刷新成功");
    }
}
