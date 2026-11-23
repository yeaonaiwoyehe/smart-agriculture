package com.agri.cms.ai.routing;

import com.agri.cms.ai.client.LlmClient;
import com.agri.cms.ai.intent.IntentRecognitionService;
import com.agri.cms.ai.intent.IntentResult;
import com.agri.cms.ai.knowledge.KnowledgeQaService;
import com.agri.cms.ai.tool.ToolCallingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 问答路由编排服务（A-07）。
 *
 * 先做意图识别，再把问题分发到对应链路：
 * 业务数据问答走工具调用，制度知识问答走知识库，闲聊直接对话，未知走兜底；
 * 每条回答都带明确的来源标注，实现两类问答的分离与可追溯。
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

    public QuestionRoutingService(IntentRecognitionService intentRecognitionService,
                                  ToolCallingService toolCallingService,
                                  KnowledgeQaService knowledgeQaService,
                                  LlmClient llmClient) {
        this.intentRecognitionService = intentRecognitionService;
        this.toolCallingService = toolCallingService;
        this.knowledgeQaService = knowledgeQaService;
        this.llmClient = llmClient;
    }

    /**
     * 路由处理用户问题，不限制所属端。
     */
    public RoutedAnswer route(String message) {
        return route(message, null);
    }

    /**
     * 路由处理用户问题。
     *
     * @param message 用户问题
     * @param side    所属端 ag / ma，可空
     */
    public RoutedAnswer route(String message, String side) {
        if (message == null || message.isBlank()) {
            return RoutedAnswer.unknown("请输入你的问题。");
        }

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
