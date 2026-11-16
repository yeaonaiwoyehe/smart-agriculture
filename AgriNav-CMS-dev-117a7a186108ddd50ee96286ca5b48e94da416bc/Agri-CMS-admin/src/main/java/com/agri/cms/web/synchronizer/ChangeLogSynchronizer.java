package com.agri.cms.web.synchronizer;

import com.agri.cms.common.constant.CacheConstants;
import com.agri.cms.common.constant.SynchronizationConstants;
import com.agri.cms.common.core.domain.DebeziumMessage;
import com.agri.cms.common.core.redis.RedisCache;
import com.agri.cms.system.domain.ChangeLog;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.stereotype.Component;

/**
 * @author : simon
 * @description : 日志同步器
 * @last : 2024-11-18 20:31
 * Copyright (c) 2024. 保留所有权利。
 * @since : 2024-11-18 20:31
 */
@Component
public class ChangeLogSynchronizer {

    /**
     * Redis缓存资源，用于存储同步的日志信息
     */
    @Resource
    private RedisCache redisCache;

    Logger log = LoggerFactory.getLogger(ChangeLogSynchronizer.class);

    /**
     * Kafka监听器，用于消费ChangeLog主题分区0的消息
     * 当有新的日志信息产生时，该方法会被调用以处理消息
     *
     * @param message DebeziumMessage对象，包含ChangeLog的增量日志信息
     */
    @KafkaListener(
            topicPartitions = @TopicPartition(
                    topic = SynchronizationConstants.CHANGE_LOG_TOPIC,
                    partitions = {"0"} ),
            containerFactory = SynchronizationConstants.DEFAULT_CONTAINER
    )
    public void synchronize(DebeziumMessage<ChangeLog> message) {
         //遍历消息中的ChangeLog对象，并将其同步到Redis中
        redisCache.setCacheObject(CacheConstants.CHANGE_LOG_CACHE_KEY + message.getAfter().getType(), message.getAfter());
    }
}
