package com.agri.cms.system.dao;

import com.agri.cms.system.domain.record.LandPlotRecord;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author : simon
 * @description :
 * @last : 2024-12-17 08:23
 * Copyright (c) 2024. 保留所有权利。
 * @since : 2024-12-17 08:23
 */
@Repository
public interface LandPlotRepository extends ElasticsearchRepository<LandPlotRecord, String> {
    List<LandPlotRecord> findByLandId(String landId);
}
