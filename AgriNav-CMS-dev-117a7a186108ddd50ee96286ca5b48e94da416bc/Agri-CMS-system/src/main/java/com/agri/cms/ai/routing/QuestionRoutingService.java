package com.agri.cms.ai.routing;

import com.agri.cms.ai.client.LlmClient;
import com.agri.cms.ai.conversation.AgentConversation;
import com.agri.cms.ai.conversation.ConversationService;
import com.agri.cms.ai.intent.IntentRecognitionService;
import com.agri.cms.ai.intent.IntentResult;
import com.agri.cms.ai.knowledge.KnowledgeQaService;
import com.agri.cms.ai.tool.ToolCallingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 问答路由编排服务（A-07，A-10 接入会话留存）。
 *
 * 先做意图识别，再把问题分发到对应链路：
 * 业务数据问答走工具调用，制度知识问答走知识库，闲聊直接对话，未知走兜底；
 * 每条回答都带明确的来源标注。同时把每一轮问与答归入会话留存，
 * 前端可凭 conversationId 续接同一会话。
 *
 * @author agrinav
 */
@Service
public class QuestionRoutingService {

    private static final Logger log = LoggerFactory.getLogger(QuestionRoutingService.class);

    private final IntentRecognitionService intentRecognitionService;
    private final ToolCallingService toolCallingService;
    private final KnowledgeQaService knowledgeQaService;
    private final LlmClient llmClient;
    private final ConversationService conversationService;

    public QuestionRoutingService(IntentRecognitionService intentRecognitionService,
                                  ToolCallingService toolCallingService,
                                  KnowledgeQaService knowledgeQaService,
                                  LlmClient llmClient,
                                  ConversationService conversationService) {
        this.intentRecognitionService = intentRecognitionService;
        this.toolCallingService = toolCallingService;
        this.knowledgeQaService = knowledgeQaService;
        this.llmClient = llmClient;
        this.conversationService = conversationService;
    }

    /**
     * 路由处理用户问题，不限制所属端，自动新建会话。
     */
    public RoutedAnswer route(String message) {
        return route(message, null, null);
    }

    /**
     * 路由处理用户问题，自动新建会话。
     */
    public RoutedAnswer route(String message, String side) {
        return route(message, side, null);
    }

    /**
     * 路由处理用户问题，并归入指定会话。
     *
     * @param message        用户问题
     * @param side           所属端 ag / ma，可空
     * @param conversationId 会话 ID，为空则新建会话
     */
    public RoutedAnswer route(String message, String side, Long conversationId) {
        long start = System.currentTimeMillis();
        if (message == null || message.isBlank()) {
            return RoutedAnswer.unknown("请输入你的问题。");
        }

        // 1. 续接或新建会话，先留存用户提问
        AgentConversation conversation =
                conversationService.getOrCreateConversation(conversationId, message, side);
        conversationService.saveUserMessage(conversation.getId(), message);

        // 2. 意图识别 + 路由分发，得到带来源标注的回答
        RoutedAnswer answer = doRoute(message, side);

        // 3. 留存助手回答并刷新会话计数，最后把会话 ID 与耗时回传给前端
        long costMs = System.currentTimeMillis() - start;
        conversationService.saveAssistantMessage(conversation.getId(), answer);
        conversationService.touchAfterRound(conversation.getId());
        return answer.withConversation(conversation.getId(), costMs);
    }

    /**
     * 意图识别并按路由类型分发到对应链路。
     */
    private RoutedAnswer doRoute(String message, String side) {
        IntentResult intent = intentRecognitionService.recognize(message, side);
        QueryRouteType routeType = QueryRouteType.fromIntent(intent.intent());
        log.info("问答路由：意图={} 路由类型={}", intent.intent().getCode(), routeType.getCode());

        return switch (routeType) {
            case BUSINESS_DATA ->
                    RoutedAnswer.fromTool(toolCallingService.handle(null, message));
            case KNOWLEDGE ->
                    RoutedAnswer.knowledge(knowledgeQaService.answer(message));
            case CHITCHAT ->
                    RoutedAnswer.chitchat(llmClient.chat(message));
            case UNKNOWN ->
                    RoutedAnswer.unknown("抱歉，我暂时无法理解这个问题，你可以换个说法，或联系人工客服。");
        };
    }
}
