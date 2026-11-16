package com.agri.cms.web.controller.monitor;

import com.agri.cms.common.core.controller.BaseController;
import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.system.service.ApiMetricsService;
import com.influxdb.client.domain.Duration;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * @author : simon
 * @description :
 * @last : 2024-12-17 15:35
 * Copyright (c) 2024. 保留所有权利。
 * @since : 2024-12-17 15:35
 */
@RestController
@RequestMapping("/monitor/api")
public class ApiStatsController extends BaseController {


    @Resource
    private ApiMetricsService apiMetricsService;


    @GetMapping("/hourly")
    public AjaxResult getHourlyStats() {
//        return apiMetricsService.getHourlyStats();
        return success();
    }

    @GetMapping
    public AjaxResult getOverallStats() {
        return success(apiMetricsService.getOverallStats());
    }


}
