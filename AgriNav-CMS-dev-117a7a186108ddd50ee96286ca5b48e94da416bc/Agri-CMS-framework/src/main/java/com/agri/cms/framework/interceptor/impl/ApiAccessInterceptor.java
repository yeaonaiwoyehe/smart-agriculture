package com.agri.cms.framework.interceptor.impl;

import com.agri.cms.framework.manager.AsyncManager;
import com.agri.cms.system.service.ApiMetricsService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.TimerTask;

/**
 * @author : simon
 * @description :
 * @last : 2024-12-17 14:22
 * Copyright (c) 2024. 保留所有权利。
 * @since : 2024-12-17 14:22
 */
@Component
public class ApiAccessInterceptor implements HandlerInterceptor {

    private final ThreadLocal<Long> startTime = new ThreadLocal<>();
    @Resource
    private ApiMetricsService apiMetricsService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        startTime.set(System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
            final long executionTime = System.currentTimeMillis() - startTime.get();
            final String uri = request.getRequestURI();
            final String method = request.getMethod();

            // 使用 AsyncManager 异步记录
            AsyncManager.me().execute(new TimerTask() {
                @Override
                public void run() {
                    apiMetricsService.recordApiAccess(uri, method, executionTime);
                }
            });
            startTime.remove();
        }
    }
