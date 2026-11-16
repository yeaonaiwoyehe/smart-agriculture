package com.agri.cms.system.service;

import com.agri.cms.system.domain.record.LocationStats;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author : simon
 * @description :
 * @last : 2024-11-17 17:26
 * Copyright (c) 2024. 保留所有权利。
 * @since : 2024-11-17 17:26
 */

public interface IUserStaticService {
    Long getUserStatistics();

    Long getUserLoginCount(LocalDateTime startTime, LocalDateTime endTime);

    List<LocationStats> getLocationStats();

}
