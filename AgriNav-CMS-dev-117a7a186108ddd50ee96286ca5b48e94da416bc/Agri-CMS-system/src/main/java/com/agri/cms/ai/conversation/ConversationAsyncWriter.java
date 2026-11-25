package com.agri.cms.ai.conversation;

import com.agri.cms.ai.conversation.mapper.AgentConversationMapper;
import com.agri.cms.ai.conversation.mapper.AgentConversationMessageMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 会话留存异步写入器（A-10）。
 *
 * 独立成组件，保证 @Async 经过 Spring 代理生效；会话与消息的落库、
 * 会话计数更新都在这里执行，任何失败仅记录日志，不影响问答主链路。
 *
 * @author agrinav
 */
@Component
public class ConversationAsyncWriter {

    private static final Logger log = LoggerFactory.getLogger(ConversationAsyncWriter.class);

    private final AgentConversationMapper conversationMapper;
    private final AgentConversationMessageMapper messageMapper;

    public ConversationAsyncWriter(AgentConversationMapper conversationMapper,
                                   AgentConversationMessageMapper messageMapper) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
    }

    /**
     * 异步插入新会话。
     */
    @Async("taskExecutor")
    public void saveConversation(AgentConversation conversation) {
        try {
            conversationMapper.insert(conversation);
            log.debug("会话已落表：id={} title={}", conversation.getId(), conversation.getTitle());
        } catch (Exception e) {
            log.error("会话落表失败：{}", e.getMessage(), e);
        }
    }

    /**
     * 异步插入一条消息。
     */
    @Async("taskExecutor")
    public void saveMessage(AgentConversationMessage message) {
        try {
            messageMapper.insert(message);
            log.debug("会话消息已落表：conversationId={} role={}", message.getConversationId(), message.getRole());
        } catch (Exception e) {
            log.error("会话消息落表失败：{}", e.getMessage(), e);
        }
    }

    /**
     * 原子自增会话消息数，并刷新最近消息时间。
     * 用 SQL 自增避免"读旧值再写回"的并发覆盖。
     */
    @Async("taskExecutor")
    public void increaseCount(Long conversationId, int added, LocalDateTime lastMessageTime) {
        try {
            LambdaUpdateWrapper<AgentConversation> wrapper = new LambdaUpdateWrapper<AgentConversation>()
                    .eq(AgentConversation::getId, conversationId)
                    .setSql("message_count = message_count + " + added)
                    .set(AgentConversation::getLastMessageTime, lastMessageTime);
            conversationMapper.update(null, wrapper);
        } catch (Exception e) {
            log.error("会话计数更新失败：conversationId={} {}", conversationId, e.getMessage(), e);
        }
    }
}
