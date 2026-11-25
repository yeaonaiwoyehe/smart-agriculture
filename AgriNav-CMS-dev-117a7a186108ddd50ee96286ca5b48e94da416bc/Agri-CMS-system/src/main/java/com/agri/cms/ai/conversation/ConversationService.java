package com.agri.cms.ai.conversation;

import com.agri.cms.ai.conversation.mapper.AgentConversationMapper;
import com.agri.cms.ai.routing.RoutedAnswer;
import com.agri.cms.common.utils.SecurityUtils;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 智能体会话留存服务（A-10）。
 *
 * 在主线程先捕获当前登录用户与时间（异步线程无法访问 SecurityContext），
 * 负责会话的新建/续接、用户消息与助手消息的留存，以及会话计数更新；
 * 实际落库交由 {@link ConversationAsyncWriter} 异步执行，不增加用户等待。
 *
 * @author agrinav
 */
@Service
public class ConversationService {

    private static final Logger log = LoggerFactory.getLogger(ConversationService.class);

    /** 会话与回答留存天数（任务要求不少于 180 天）。 */
    private static final int RETAIN_DAYS = 180;

    /** 会话标题最多保留的问题字符数。 */
    private static final int TITLE_MAX_LEN = 20;

    private final AgentConversationMapper conversationMapper;
    private final ConversationAsyncWriter asyncWriter;

    public ConversationService(AgentConversationMapper conversationMapper,
                               ConversationAsyncWriter asyncWriter) {
        this.conversationMapper = conversationMapper;
        this.asyncWriter = asyncWriter;
    }

    /**
     * 按会话 ID 续接已有会话；ID 为空或查不到时，以当前问题为起点新建会话。
     *
     * @param conversationId 前端透传的会话 ID，可空
     * @param firstMessage   本轮用户问题（新建会话时用于生成标题）
     * @param side           所属端 ag / ma，可空
     * @return 续接的或新建的会话
     */
    public AgentConversation getOrCreateConversation(Long conversationId, String firstMessage, String side) {
        if (conversationId != null) {
            AgentConversation existing = tryFind(conversationId);
            if (existing != null) {
                return existing;
            }
            log.info("会话 {} 不存在，改为新建会话", conversationId);
        }
        return createConversation(firstMessage, side);
    }

    /**
     * 留存一条用户提问消息。
     */
    public void saveUserMessage(Long conversationId, String content) {
        AgentConversationMessage message = new AgentConversationMessage();
        message.setConversationId(conversationId);
        message.setRole("user");
        message.setContent(content);
        message.setCreateTime(LocalDateTime.now());
        asyncWriter.saveMessage(message);
    }

    /**
     * 留存一条助手回答消息，从路由结果提取路由类型、来源、状态、工具、回执与业务单号。
     */
    public void saveAssistantMessage(Long conversationId, RoutedAnswer answer) {
        AgentConversationMessage message = new AgentConversationMessage();
        message.setConversationId(conversationId);
        message.setRole("assistant");
        message.setContent(answer.answer());
        message.setRouteType(answer.routeType());
        message.setSourceLabel(answer.source());
        message.setStatus(answer.status());
        message.setConfirmToken(answer.confirmToken());
        message.setCostMs(answer.costMs());
        message.setCreateTime(LocalDateTime.now());

        // 回执号作为当前可跳转的业务关联；后续业务工具完善后再补充作业单/结算单号。
        if (answer.receiptNo() != null && !answer.receiptNo().isBlank()) {
            message.setToolName(answer.toolName());
            message.setReceiptNo(answer.receiptNo());
            message.setBizRefNo(answer.receiptNo());
            message.setBizRefType("回执");
        }
        asyncWriter.saveMessage(message);
    }

    /**
     * 一轮问答结束后，原子自增消息数（一问一答共 2 条）并刷新最近消息时间。
     */
    public void touchAfterRound(Long conversationId) {
        asyncWriter.increaseCount(conversationId, 2, LocalDateTime.now());
    }

    /**
     * 主键查询会话，任何异常返回 null，由调用方走新建逻辑。
     */
    private AgentConversation tryFind(Long conversationId) {
        try {
            return conversationMapper.selectById(conversationId);
        } catch (Exception e) {
            log.warn("查询会话失败：id={} {}", conversationId, e.getMessage());
            return null;
        }
    }

    /**
     * 新建会话：主线程捕获用户、生成标题与留存到期时间，再异步落表。
     */
    private AgentConversation createConversation(String firstMessage, String side) {
        LocalDateTime now = LocalDateTime.now();
        AgentConversation conversation = new AgentConversation();
        // 主线程提前定下雪花 ID：异步 insert 才回填 ID，若不提前生成，
        // 紧接着保存的第一条消息会拿到 null 的会话 ID 而落表失败。
        conversation.setId(IdWorker.getId());
        conversation.setSide(side);
        conversation.setTitle(buildTitle(firstMessage));
        conversation.setMessageCount(0);
        conversation.setCreateTime(now);
        conversation.setLastMessageTime(now);
        conversation.setExpireTime(now.plusDays(RETAIN_DAYS));
        fillCurrentUser(conversation);
        asyncWriter.saveConversation(conversation);
        return conversation;
    }

    /**
     * 取问题前若干字作为会话标题。
     */
    private String buildTitle(String firstMessage) {
        if (firstMessage == null) {
            return "新会话";
        }
        String normalized = firstMessage.trim().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) {
            return "新会话";
        }
        return normalized.length() > TITLE_MAX_LEN
                ? normalized.substring(0, TITLE_MAX_LEN) + "…"
                : normalized;
    }

    /**
     * 捕获当前登录用户；未登录场景用匿名兜底，不阻断留存。
     */
    private void fillCurrentUser(AgentConversation conversation) {
        try {
            var loginUser = SecurityUtils.getLoginUser();
            conversation.setUserId(String.valueOf(loginUser.getUserId()));
            conversation.setUserName(loginUser.getUsername());
            conversation.setNickName(loginUser.getUser().getNickName());
        } catch (Exception e) {
            log.debug("会话留存时未获取到登录用户，按匿名处理：{}", e.getMessage());
            conversation.setUserName("anonymous");
        }
    }
}
