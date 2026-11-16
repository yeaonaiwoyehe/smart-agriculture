package com.agri.cms.web.controller.common;

import cn.hutool.core.lang.Assert;
import com.agri.cms.common.annotation.Anonymous;
import com.agri.cms.common.constant.CacheConstants;
import com.agri.cms.common.core.controller.BaseController;
import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.common.core.redis.RedisCache;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

/**
 * @author : simon
 * @description :
 * @last : 2024-12-03 20:59
 * Copyright (c) 2024. 保留所有权利。
 * @since : 2024-12-03 20:59
 */
@RestController
@RequestMapping("/version")
@Anonymous
public class VersionCodeController extends BaseController {


    @Resource
    private RedisCache redisCache;

    @GetMapping
    public AjaxResult getVersionCode(String version){
        Integer currentVersion = redisCache.getCacheMapValue(CacheConstants.SYS_VERSION_CODE, version);
        String latestVersion = redisCache.getCacheObject(CacheConstants.SYS_LATEST_VERSION);
        Integer latestVersionCode = redisCache.getCacheMapValue(CacheConstants.SYS_VERSION_CODE, latestVersion);
        return Objects.requireNonNull(success()
                        .put("version", latestVersion))
                .put("update", latestVersionCode > currentVersion);
    }

    @PostMapping
    @PreAuthorize("@ss.hasRole('admin')")
    public AjaxResult updateVersionCode(
            @RequestParam @NotBlank String version,
            @RequestParam(defaultValue = "true", required = false) boolean update

    ){
        redisCache.setCacheObject(CacheConstants.SYS_LATEST_VERSION, version);
        redisCache.setCacheMapValue(CacheConstants.SYS_VERSION_CODE, version,
                (Integer)redisCache.getCacheMapLastValue(CacheConstants.SYS_VERSION_CODE)+(update?1:0));
        return success();
    }
}
