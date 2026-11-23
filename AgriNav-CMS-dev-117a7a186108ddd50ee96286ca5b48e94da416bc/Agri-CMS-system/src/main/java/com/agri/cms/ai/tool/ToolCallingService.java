package com.agri.cms.ai.tool;

import com.agri.cms.ai.client.LlmClient;
import com.agri.cms.ai.model.ChatCompletionResponse;
import com.agri.cms.ai.model.ChatMessage;
import com.agri.cms.ai.model.ToolCall;
import com.agri.cms.ai.model.ToolDefinition;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 工具调用编排服务（A-05）。
 *
 * 在大模型与业务工具之间完成一轮闭环：
 * 1. 携带工具清单请求大模型，模型自主决定直接回答或调用工具；
 * 2. 读工具：参数校验通过后直接执行，结果回传模型生成自然语言；
 * 3. 写工具：参数校验通过后不立即执行，返回确认令牌，由用户二次确认；
 * 4. 确认接口凭令牌执行写工具，生成回执编号并返回。
 *
 * @author agrinav
 */
@Service
public class ToolCallingService {

    private static final Logger log = LoggerFactory.getLogger(ToolCallingService.class);

    private final LlmClient llmClient;
    private final ToolRegistry toolRegistry;
    private final ToolArgumentValidator argumentValidator;
    private final ConfirmationManager confirmationManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ToolCallingService(LlmClient llmClient,
                              ToolRegistry toolRegistry,
                              ToolArgumentValidator argumentValidator,
                              ConfirmationManager confirmationManager) {
        this.llmClient = llmClient;
        this.toolRegistry = toolRegistry;
        this.argumentValidator = argumentValidator;
        this.confirmationManager = confirmationManager;
    }

    /**
     * 处理用户消息，可携带系统提示词。
     */
    public ToolConversation handle(String systemPrompt, String userMessage) {
        List<ChatMessage> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(ChatMessage.system(systemPrompt));
        }
        messages.add(ChatMessage.user(userMessage));
        return run(messages);
    }

    /**
     * 用户确认后执行写操作，返回执行结果与回执编号。
     */
    public ToolConversation confirm(String confirmToken) {
        PendingConfirmation pending = confirmationManager.consume(confirmToken);
        if (pending == null) {
            return ToolConversation.failed("确认令牌无效或已过期，请重新发起操作");
        }
        AgentTool tool = toolRegistry.getTool(pending.toolName());
        if (tool == null) {
            return ToolConversation.failed("待执行的工具已不存在");
        }

        ToolResult result = tool.execute(pending.arguments());
        if (!result.success()) {
            return ToolConversation.failed("操作未成功：" + result.errorMessage());
        }
        log.info("写操作确认执行完成：{} 回执={}", tool.name(), result.receiptNo());
        return new ToolConversation("REPLY", result.content(), null, null, result.receiptNo());
    }

    /**
     * 执行一次带工具的模型交互。
     */
    private ToolConversation run(List<ChatMessage> messages) {
        List<ToolDefinition> tools = toolRegistry.listDefinitions();
        ChatCompletionResponse response = llmClient.exchange(messages, tools);
        if (response == null) {
            return ToolConversation.failed("智能助手暂时无法响应，请稍后再试");
        }

        ChatMessage message = response.choices().get(0).message();
        List<ToolCall> toolCalls = message.toolCalls();
        if (toolCalls == null || toolCalls.isEmpty()) {
            // 模型未调用工具，直接返回文本
            return ToolConversation.reply(message.content());
        }

        // 保留 assistant 的工具调用消息，便于后续回传
        messages.add(ChatMessage.assistant(toolCalls));
        ToolCall call = toolCalls.get(0);

        AgentTool tool = toolRegistry.getTool(call.function().name());
        if (tool == null) {
            return ToolConversation.failed("模型请求了未知工具：" + call.function().name());
        }

        Map<String, Object> arguments = parseArguments(call.function().arguments());
        List<String> missing = argumentValidator.validate(tool, arguments);
        if (!missing.isEmpty()) {
            return ToolConversation.needParams("缺少必要信息：" + String.join("、", missing)
                    + "，请补充后再试");
        }

        if (tool.isWriteOperation()) {
            // 写操作：登记待确认，等待用户二次确认
            String summary = buildSummary(tool, arguments);
            String token = confirmationManager
                    .register(PendingConfirmation.of(tool.name(), arguments, summary));
            log.info("写操作等待确认：{} 确认令牌={}", tool.name(), token);
            return ToolConversation.needConfirm(token, summary);
        }

        // 读操作：直接执行并把结果回传模型
        ToolResult result = tool.execute(arguments);
        return finalizeWithToolResult(messages, call.id(), result);
    }

    /**
     * 把工具结果作为 tool 消息回传，再请模型生成自然语言；模型不可用时兜底返回工具原文。
     */
    private ToolConversation finalizeWithToolResult(List<ChatMessage> messages,
                                                    String toolCallId, ToolResult result) {
        String toolContent = result.success()
                ? result.content()
                : "工具执行失败：" + result.errorMessage();
        messages.add(ChatMessage.tool(toolCallId, toolContent));

        ChatCompletionResponse finalResponse = llmClient.exchange(messages, null);
        if (finalResponse == null) {
            return ToolConversation.reply(toolContent);
        }
        return ToolConversation.reply(finalResponse.choices().get(0).message().content());
    }

    /**
     * 解析模型给出的工具参数 JSON，解析失败返回空 Map，由校验环节触发缺参提示。
     */
    private Map<String, Object> parseArguments(String argumentsJson) {
        try {
            return objectMapper.readValue(argumentsJson, Map.class);
        } catch (Exception e) {
            log.warn("工具参数 JSON 解析失败：{}，原文：{}", e.getMessage(), argumentsJson);
            return Map.of();
        }
    }

    /**
     * 生成写操作的确认摘要。
     */
    private String buildSummary(AgentTool tool, Map<String, Object> arguments) {
        return "请确认执行「" + tool.name() + "」，参数：" + arguments;
    }
}
