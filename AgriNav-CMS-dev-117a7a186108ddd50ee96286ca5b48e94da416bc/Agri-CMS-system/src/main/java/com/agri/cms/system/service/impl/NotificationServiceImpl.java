package com.agri.cms.system.service.impl;


import com.agri.cms.common.constant.CacheConstants;
import com.agri.cms.common.constant.Constants;
import com.agri.cms.common.core.domain.model.Notification;
import com.agri.cms.common.core.redis.RedisCache;
import com.agri.cms.system.mapper.NotificationMapper;
import com.agri.cms.system.service.NotificationService;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author : simon
 * @description :
 * @last : 2024-11-21 09:42
 * Copyright (c) 2024. 保留所有权利。
 * @since : 2024-11-21 09:42
 */
@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification<?>> implements NotificationService {


    @Resource
    private RedisCache redisCache;



    @Override
    public Boolean read(String id) {
        return update(new LambdaUpdateWrapper<Notification<?>>()
                .eq(Notification::getId, id)
                .set(Notification::getIsRead, Constants.READ)
                .set(Notification::getReadTime, LocalDateTime.now()));
    }

    @Override
    public Boolean deleteById(String username, String id) {
        return redisCache.deleteCacheSetValue(CacheConstants.SYSTEM_NOTICE_CACHE_KEY + username, id)
                && redisCache.deleteCacheSetValue(CacheConstants.SYSTEM_NOTICE_READ_CACHE_KEY + username, id);
    }

    @Override
    public Set<Notification> listUnread(String username) {
        return redisCache.getCacheSetDiff(CacheConstants.SYSTEM_NOTICE_CACHE_KEY + username,
                CacheConstants.SYSTEM_NOTICE_READ_CACHE_KEY+username)
                .stream()
                .map(String::valueOf)
                .map(this::getById)
                .map(this::transfer)
                .collect(Collectors.toSet());
    }

    @Override
    public Boolean clear(String username) {
        return redisCache.deleteObject(CacheConstants.SYSTEM_NOTICE_CACHE_KEY + username)
                && redisCache.deleteObject(CacheConstants.SYSTEM_NOTICE_READ_CACHE_KEY + username);
    }

    @Override
    public Boolean readAll(String username) {
        return update(new LambdaUpdateWrapper<Notification<?>>()
                .set(Notification::getIsRead, Constants.READ)
                .eq(Notification::getReceiverId, username));
    }

    @Override
    public List<Notification> list(String userName) {
        return redisCache.getCacheSet(CacheConstants.SYSTEM_NOTICE_CACHE_KEY + userName)
                .stream()
                .map(String::valueOf)
                .map(this::getById)
                .map(this::transfer)
                .sorted()
                .collect(Collectors.toList());
    }


    @Override
    public Notification transfer(Notification<?> notification){
        try{
            switch (notification.getType().getRelationDB()) {
                case "ms":
                    return relatedToMySQL(notification);
                case "pg":
                    return relatedToPostgreSQL(notification);
                case "redis":

                    break;
                default:
                    break;
            }
            return null;
        }catch (Exception e){
            log.error("转换错误",e);
        }
        return null;
    }
    @SuppressWarnings("unchecked")
    public Notification relatedToMySQL(Notification notification) {
        try {
            Class<?> clazz = Class.forName(notification.getType().getRelationClass());
            Map<String, Object> transfer = getBaseMapper().transfer(notification);
            
            if (transfer != null && !transfer.isEmpty()) {
                Object target = clazz.getDeclaredConstructor().newInstance();
                for (Field field : clazz.getDeclaredFields()) {
                    field.setAccessible(true);
                    // 数据库字段名可能是下划线格式，需要转换
                    Object value = transfer.get(camelToUnderscore(field.getName()));
                    if (value != null) {
                        field.set(target, value);
                    }
                }
                notification.setPayload(target);
            }
            
            return notification;
        } catch (Exception e) {
            log.error("数据转换失败: {}",e);
            return notification;
        }
    }

    // 驼峰转下划线工具方法
    private String camelToUnderscore(String camel) {
        return camel.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    @DS("pg")
    public Notification relatedToPostgreSQL(Notification notification){
        return null;
    }
}
