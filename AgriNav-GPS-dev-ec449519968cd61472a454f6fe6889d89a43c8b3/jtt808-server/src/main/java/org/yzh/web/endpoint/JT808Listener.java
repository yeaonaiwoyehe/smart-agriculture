package org.yzh.web.endpoint;

import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.yzh.protocol.t808.T0200;
import org.yzh.protocol.t808.T0801;
import org.yzh.web.service.RemoteFileService;
import org.yzh.web.service.SearchEngineService;
import org.yzh.web.trajectory.service.IEffectiveTrajectoryService;
import org.yzh.web.trajectory.service.IOriginalTrajectoryService;

import java.util.List;

/**
 * JT808 消息处理器（方案 B：去 Kafka，由 AsyncQueueService 通过本地线程池直接调用）
 */
@Component
public class JT808Listener {

    private static final Logger log = LoggerFactory.getLogger(JT808Listener.class);

    @Resource
    private IEffectiveTrajectoryService effectiveTrajectoryService;

    @Resource
    private IOriginalTrajectoryService originalTrajectoryService;

    @Resource
    private RemoteFileService remoteFileService;

    @Resource
    private SearchEngineService searchEngineService;

    /**
     * 处理位置信息：原始点写 ES，有效点判定后写 PostgreSQL 暂存表
     */
    public void savePoint(List<T0200> messages) {
        originalTrajectoryService.saveOriginalPoints(messages);
        effectiveTrajectoryService.insertBatch(messages);
    }

    /**
     * 处理多媒体数据：转存文件并在 ES 中建立索引
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveMediaFile(List<T0801> messages) throws Exception {
        for (T0801 message : messages) {
            searchEngineService.save(remoteFileService.uploadMediaFile(message));
        }
    }
}
