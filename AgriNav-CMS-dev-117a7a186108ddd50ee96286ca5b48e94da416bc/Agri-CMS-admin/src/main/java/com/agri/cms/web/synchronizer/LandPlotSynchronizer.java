package com.agri.cms.web.synchronizer;

import com.agri.cms.common.constant.CacheConstants;
import com.agri.cms.common.constant.Constants;
import com.agri.cms.common.core.redis.RedisCache;
import com.agri.cms.system.domain.LandPlot;
import com.agri.cms.system.service.LandPlotService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.data.geo.Point;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author : simon
 * @description :
 * @last : 2024-12-25 21:27
 * Copyright (c) 2024. 保留所有权利。
 * @since : 2024-12-25 21:27
 */
@RestController
public class LandPlotSynchronizer {


    @Resource
    private LandPlotService landPlotService;


    @Resource
    private RedisCache redisCache;

//    @PreAuthorize("@ss.hasRole('admin')")
    @PutMapping("/refreshLandPlots")
    @Scheduled(cron = "0 0 0 * * ?")
    public void synchronize() {
        landPlotService.list(
                new LambdaQueryWrapper<LandPlot>()
                        .eq(LandPlot::getIsDeleted, Constants.NOT_DELETED)
                )
                .forEach(landPlot -> redisCache.setCacheGeoValue(Constants.LAND_PLOT_GEO_KEY
                        , new Point(landPlot.getCenterPoint().getY(),landPlot.getCenterPoint().getX())
                        , landPlot.getId()));
    }



}
