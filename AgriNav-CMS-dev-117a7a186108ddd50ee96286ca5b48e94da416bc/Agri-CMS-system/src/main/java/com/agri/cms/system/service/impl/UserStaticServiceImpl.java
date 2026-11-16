package com.agri.cms.system.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.agri.cms.common.constant.CacheConstants;
import com.agri.cms.common.core.redis.RedisCache;
import com.agri.cms.system.domain.record.LocationStats;
import com.agri.cms.system.service.ISysLogininforService;
import com.agri.cms.system.service.IUserStaticService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author : simon
 * @description :
 * @last : 2024-11-17 17:27
 * Copyright (c) 2024. 保留所有权利。
 * @since : 2024-11-17 17:27
 */
@Service
public class UserStaticServiceImpl implements IUserStaticService {


    @Resource
    private RedisCache redisCache;


    @Resource
    private ISysLogininforService iSysLogininforService;
    @Override
    public Long getUserStatistics() {
        return redisCache.pfcount(CacheConstants.DAU_KEY_PREFIX);
    }

    @Override
    public Long getUserLoginCount(LocalDateTime startTime, LocalDateTime endTime) {
        return ObjectUtil.isNotEmpty(startTime)&&ObjectUtil.isNotEmpty(endTime)?
                iSysLogininforService.getLoginCount(startTime, endTime):
                iSysLogininforService.getLoginCount();
    }

    @Override
    public List<LocationStats> getLocationStats() {
        Map<String, Object> cacheMap = redisCache.getCacheMap(CacheConstants.LOGIN_LOCATION);
        long total = cacheMap.values().stream()
                .mapToLong(count -> Long.parseLong(count.toString()))
                .sum();
        return cacheMap.entrySet().stream()
                    .map(entry -> {
                        long count = Long.parseLong(entry.getValue().toString());
                        double percentage = total > 0 ? (double) count / total * 100 : 0;
                        return new LocationStats(
                            entry.getKey(),
                            count,
                            String.format("%.2f%%", percentage)
                        );
                    })
                    .sorted((a, b) -> Long.compare(b.visitCount(), a.visitCount()))
                    .collect(Collectors.toList());
    }
}
