package com.agri.cms.web.synchronizer;

import com.agri.cms.common.constant.CacheConstants;
import com.agri.cms.common.constant.SynchronizationConstants;
import com.agri.cms.common.core.domain.DebeziumMessage;
import com.agri.cms.common.core.redis.RedisCache;
import com.agri.cms.system.domain.SysLogininfor;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.stereotype.Component;

/**
 * 登录信息同步器，用于同步MySQL数据库中的登录信息到Redis
 * @author simon
 * @since 2024/11/22 */
@Component
public class LoginInfoSynchronizer {

    /**
     * Redis缓存对象，用于操作Redis缓存
     */
    @Resource
    private RedisCache redisCache;

    /**
     * 监听MySQL的login_info表，将登录地点信息同步到Redis中
     *
     * @param message seaTunnel消息，包含从MySQL中获取的数据
     */
    @KafkaListener(
            topicPartitions = @TopicPartition(
                    topic = SynchronizationConstants.LOGIN_INFO_TOPIC,
                    partitions = {"0"} ),
            containerFactory = SynchronizationConstants.DEFAULT_CONTAINER
    )
    public void synchronize(DebeziumMessage<SysLogininfor> message){

        // 处理消息中的每个登录信息，提取登录地点并更新到Redis缓存中
        redisCache.incrementCacheObject(CacheConstants.LOGIN_LOCATION, message.getAfter().getLoginLocation());
    }

}
