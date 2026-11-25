package com.agri.cms.web.controller.Static;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.agri.cms.common.constant.CacheConstants;
import com.agri.cms.common.core.controller.BaseController;
import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.common.core.redis.RedisCache;
import com.agri.cms.system.domain.record.LocationStats;
import com.agri.cms.system.service.ISysUserService;
import com.agri.cms.system.service.IUserStaticService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author : simon
 * @description :
 * @last : 2024-11-17 17:05
 * Copyright (c) 2024. 保留所有权利。
 * @since : 2024-11-17 17:05
 */
@RestController
@RequestMapping("/static/user")
@Tag(name = "用户统计")
public class UserStaticController extends BaseController {


    @Resource
    private IUserStaticService iUserStaticService;


    @Resource
    private ISysUserService iSysUserService;



    /**
     *  获取用户日活
     * @return AjaxResult
     * @author simon
     * @since 2024/11/17
     */

    @GetMapping("/dau")
    public AjaxResult getUserStatistics(){
        return success(iUserStaticService.getUserStatistics());
    }

    /**
     * 获取用户登录数量
     * @param startTime
     * @param endTime
     * @return AjaxResult
     * @author simon
     * @since 2024/11/17
     */
    @GetMapping("/lc")
    public AjaxResult getUserLoginCount(LocalDateTime startTime, LocalDateTime endTime){
        return success(iUserStaticService.getUserLoginCount(startTime, endTime));
    }

    /**
     * 获取用户注册数量
     * @param startTime
     * @param endTime
     * @return AjaxResult
     * @author simon
     * @since 2024/11/17
     */
    @GetMapping("/rc")
    public AjaxResult getUserRegisterCount(LocalDateTime startTime, LocalDateTime endTime){
        return success(iSysUserService.getUserRegisterCount(startTime, endTime));
    }


    @GetMapping("/location")
    public AjaxResult getUserLocation(){
        return success(iUserStaticService.getLocationStats());
    }

}
