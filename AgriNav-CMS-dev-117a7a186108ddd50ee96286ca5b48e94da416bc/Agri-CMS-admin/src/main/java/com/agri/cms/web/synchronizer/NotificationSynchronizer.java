package com.agri.cms.web.synchronizer;

import com.agri.cms.common.constant.CacheConstants;
import com.agri.cms.common.constant.SynchronizationConstants;
import com.agri.cms.common.core.domain.DebeziumMessage;
import com.agri.cms.common.core.domain.model.Notification;
import com.agri.cms.common.core.redis.RedisCache;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.stereotype.Component;

import java.util.Set;


/**
 * @author : simon
 * @description : 站内信发布器，监听
 * @last : 2024-11-20 16:52
 * Copyright (c) 2024. 保留所有权利。
 * @since : 2024-11-20 16:52
 */
@Component
public class NotificationSynchronizer {

    private final Logger log = LoggerFactory.getLogger(NotificationSynchronizer.class);


    @Resource
    private RedisCache redisCache;

    @KafkaListener(
            topicPartitions = @TopicPartition(
                    topic = SynchronizationConstants.NOTIFICATION_TOPIC,
                    partitions = {"0"} ),
            containerFactory = SynchronizationConstants.DEFAULT_CONTAINER
    )
    public void onMessage(DebeziumMessage<Notification> message) {
        try {
            //如果新增
            if (SynchronizationConstants.OperationType.INSERT.toString().equals(
                    message.getOp()
            )){
                //添加到全部通知对应的缓存Set中
                redisCache.setCacheSet(
                        CacheConstants.SYSTEM_NOTICE_CACHE_KEY + message.getAfter().getReceiverId(),
                        Set.of(message.getAfter().getId()));
                //如果更新
            }else if (SynchronizationConstants.OperationType.UPDATE.toString().equals(
                    message.getOp()
            )){
                //添加到已读通知对应的缓存Set中
                redisCache.setCacheSet(
                        CacheConstants.SYSTEM_NOTICE_READ_CACHE_KEY + message.getAfter().getReceiverId(),
                        Set.of(message.getAfter().getId()));
            }
        }catch (Exception e){
            log.error("同步通知失败",e);
        }
    }
}
