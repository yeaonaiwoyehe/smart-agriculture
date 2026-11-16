package org.yzh.web.endpoint;

import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.yzh.protocol.t808.T0200;
import org.yzh.protocol.t808.T0801;
import org.yzh.protocol.t808.T8103;
import org.yzh.web.model.DTO.T0200DTO;
import org.yzh.web.model.entity.MediaFile;
import org.yzh.web.service.RemoteFileService;
import org.yzh.web.service.SearchEngineService;
import org.yzh.web.trajectory.service.IEffectiveTrajectoryService;
import org.yzh.web.trajectory.service.IOriginalTrajectoryService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JT808 消息监听器
 * 负责接收并处理不同类型的 JT808 消息，包括批量处理功能。
 * 目前支持 T0200、T0801 消息类型。
 *
 * @author simon
 * @since 2024/9/23
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
     * 批量接收 T0200 消息并处理
     * @param messages 消息列表
     */
    @KafkaListener(
            topicPartitions = @TopicPartition(topic = "T0200", partitions = {"0"}),
            containerFactory = "t0200ListenerContainerFactory"
    )
    public void savePoint(List<T0200> messages)  {
        originalTrajectoryService.saveOriginalPoints(messages);
        effectiveTrajectoryService.insertBatch(messages);
    }

    /**
     * 从kafka中读出T0801消息，获取其中的Packet转成jpg格式存进MinIO并在ES中建立外置索引
     * @param messages T0801消息列表
     * @throws Exception 回滚条件
     * @author simon
     * @since 2024/11/17
     */
    @KafkaListener(
            topicPartitions = @TopicPartition(topic = "T0801", partitions = {"0"}),
            containerFactory = "t0801ListenerContainerFactory"
    )
    @Transactional(rollbackFor = Exception.class)
    public void saveMediaFile(List<T0801> messages) throws Exception {
        for (T0801 message : messages){
            searchEngineService.save(remoteFileService.uploadMediaFile(message));
        }
    }
}
