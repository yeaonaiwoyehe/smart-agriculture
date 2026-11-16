package com.agri.cms.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @author : simon
 * @description :
 * @last : 2024-12-12 14:51
 * Copyright (c) 2024. 保留所有权利。
 * @since : 2024-12-12 14:51
 */
@Component
@ConfigurationProperties(prefix = "debezium")
public class TopicMappingProperties {
    private Map<String, String> mappings;

    public Map<String, String> getMappings() {
        return mappings;
    }

    public void setMappings(Map<String, String> mappings) {
        this.mappings = mappings;
    }
}
