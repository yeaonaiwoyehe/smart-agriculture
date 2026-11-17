package org.yzh.web.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.yzh.protocol.t808.T0200;
import org.yzh.protocol.t808.T0801;
import org.yzh.web.endpoint.JT808Listener;

import java.util.List;

/**
 * 异步队列服务（方案 B：去 Kafka）
 * 原 Kafka 仅承担"自己发自己收"的异步缓冲，这里改为进程内线程池直接调用处理器。
 *
 * @author simon
 */
@Service
public class AsyncQueueService {

    private static final Logger log = LoggerFactory.getLogger(AsyncQueueService.class);

    @Resource
    private JT808Listener jt808Listener;

    private ThreadPoolTaskExecutor executor;

    @PostConstruct
    public void init() {
        executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(2000);
        executor.setThreadNamePrefix("gps-async-");
        // 队列满时由上报线程自己执行，形成背压，避免丢数据
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        executor.initialize();
    }

    /**
     * 异步处理位置信息（原始点写 ES、有效点写 PG 暂存表）
     */
    public void sendLocationInfo(T0200 message) {
        executor.execute(() -> {
            try {
                jt808Listener.savePoint(List.of(message));
            } catch (Exception e) {
                log.error("位置信息处理失败：{}", message, e);
            }
        });
    }

    /**
     * 异步处理多媒体数据（转存文件并写 ES 索引）
     */
    public void sendMedia(T0801 message) {
        executor.execute(() -> {
            try {
                jt808Listener.saveMediaFile(List.of(message));
            } catch (Exception e) {
                log.error("多媒体数据处理失败：{}", message, e);
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
    }
}
