package com.agri.cms.system.domain.record;

import java.time.LocalDateTime;

/**
 * @author : simon
 * @description :
 * @last : 2024-11-18 20:23
 * Copyright (c) 2024. 保留所有权利。
 * @since : 2024-11-18 20:23
 */
public record LocationStats(
    String location,      // 地区名称
    long visitCount,      // 访问次数
    String percentage,    // 百分比
    LocalDateTime lastUpdateTime  // 最后更新时间
) {
    public LocationStats(String location, long visitCount, String percentage) {
        this(location, visitCount, percentage, LocalDateTime.now());
    }

    public static LocationStats of(String location, long visitCount, double percentage) {
        return new LocationStats(
            location,
            visitCount,
            String.format("%.2f%%", percentage),
            LocalDateTime.now()
        );
    }

    @Override
    public String toString() {
        return String.format("%s: %d次 (%s)", location, visitCount, percentage);
    }
}