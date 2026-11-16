package org.yzh.web.service;

import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yzh.protocol.basics.JTMessage;
import org.yzh.protocol.t808.T0200;
import org.yzh.protocol.t808.T0801;
import org.yzh.protocol.t808.T8103;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 异步队列服务
 * 提供对KafkaTemplate的定制化封装，处理消息发送的成功与失败回调
 * 
 * @author simon
 * @since 2024/9/23 
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class AsyncQueueService {

    private static final Logger log = LoggerFactory.getLogger(AsyncQueueService.class);

    @Resource
    private KafkaTemplate<String, T0200> t0200KafkaTemplate;
    @Resource
    private KafkaTemplate<String, T0801> t0801KafkaTemplate;

    @Resource
    private KafkaTemplate<String, Object> commonProducer;

    private static final String TOPIC_T0200 = "T0200";
    private static final String TOPIC_T8103 = "T8103";
    private static final String TOPIC_T0801 = "T0801";

    /**
     * 发送位置信息
     * @param message 位置信息消息
     */
    public void sendLocationInfo(T0200 message) {
        sendMessage(TOPIC_T0200, message,t0200KafkaTemplate);
    }

    /**
     * 发送多媒体数据
     * @param message 多媒体数据消息
     * @author simon
     * @since 2024/11/16
     */
    public void sendMedia(T0801 message){
        sendMessage(TOPIC_T0801, message,t0801KafkaTemplate);
    }

    /**
     * 通用发送消息方法，封装了KafkaTemplate的发送逻辑
     * 使用CompletableFuture处理发送结果
     *
     * @param topic   消息主题
     * @param message 发送的消息内容
     */
    private <T extends JTMessage> void sendMessage(String topic, T message,KafkaTemplate<String,T> template) {
        CompletableFuture<SendResult<String,T>> future = template.send(topic, message);
        future.thenAccept(result -> log.info("消息发送成功：主题={}，消息={}", topic, message))
              .exceptionally(ex -> {
                  log.error("消息发送失败：主题={}，消息={}, 原因: {}", topic, message, ex.getMessage());
                  return null;
              });
    }


}
