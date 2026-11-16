package com.agri.cms.common.config.serializer;

import com.agri.cms.common.annotation.Log;
import com.agri.cms.common.constant.Constants;
import com.agri.cms.common.core.domain.DebeziumMessage;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.TypeReference;
import org.apache.commons.lang3.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * @author : simon
 * @description : kafka消息反序列化
 * @last : 2024-11-18 17:03
 * Copyright (c) 2024. 保留所有权利。
 * @since : 2024-11-18 17:03
 */



public class DebeziumMessageDeserializer<T> implements Deserializer<DebeziumMessage<T>> {

    Logger log = LoggerFactory.getLogger(DebeziumMessageDeserializer.class);
    private final Map<String, Class<?>> topicTypeMapping;

    public DebeziumMessageDeserializer() {
        this.topicTypeMapping = new HashMap<>();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void configure(Map<String, ?> configs, boolean isKey) {
        Map<String, String> topicMappings = (Map<String, String>) configs.get(Constants.DEBEZIUM_TOPIC_MAPPING);
        if (topicMappings != null) {
            for (Map.Entry<String, String> entry : topicMappings.entrySet()) {
                try {
                    Class<?> type = Class.forName(entry.getValue());
                    topicTypeMapping.put(entry.getKey(), type);
                } catch (Exception e) {
                    throw new SerializationException("Debezium消息序列化失败: " + entry.getValue(), e);
                }
            }
        }
    }

    @Override
    public DebeziumMessage<T> deserialize(String topic, byte[] data) {
        try {
            if (data == null) {
                return null;
            }

            String jsonStr = new String(data, StandardCharsets.UTF_8);
            Class<?> targetType = topicTypeMapping.get(topic);
            if (targetType == null) {
                targetType = topicTypeMapping.entrySet().stream()
                    .filter(entry -> topic.contains(entry.getKey()))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElseThrow(() -> new SerializationException("找不到topic对应的类型映射: " + topic));
            }

            Type genericType = new TypeReference<DebeziumMessage<T>>(targetType) {}.getType();

            return JSON.parseObject(
                    jsonStr,
                    genericType,
                    JSONReader.Feature.SupportSmartMatch,
                    JSONReader.Feature.FieldBased,
                    JSONReader.Feature.UseNativeObject,
                    JSONReader.Feature.SupportArrayToBean,
                    JSONReader.Feature.TrimString
            );

        } catch (Exception e) {
            log.error("一次失败的消息解析",e);
            throw new SerializationException("Debezium消息格式不匹配: " + topic, e);
        }
    }

    @Override
    public void close() {
        topicTypeMapping.clear();
    }
}