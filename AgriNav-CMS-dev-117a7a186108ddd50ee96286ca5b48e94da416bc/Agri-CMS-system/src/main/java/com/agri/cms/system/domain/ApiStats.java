package com.agri.cms.system.domain;

import java.time.LocalDateTime;

/**
 * @author : simon
 * @description :
 * @last : 2024-12-17 14:39
 * Copyright (c) 2024. 保留所有权利。
 * @since : 2024-12-17 14:39
 */
public class ApiStats {
    private String path;
    private String method;
    private Long count;
    private Double avgTime;
    private LocalDateTime timestamp;

    public ApiStats() {
    }

    public ApiStats(String path, String method, Long count, Double avgTime, LocalDateTime timestamp) {
        this.path = path;
        this.method = method;
        this.count = count;
        this.avgTime = avgTime;
        this.timestamp = timestamp;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public Double getAvgTime() {
        return avgTime;
    }

    public void setAvgTime(Double avgTime) {
        this.avgTime = avgTime;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
