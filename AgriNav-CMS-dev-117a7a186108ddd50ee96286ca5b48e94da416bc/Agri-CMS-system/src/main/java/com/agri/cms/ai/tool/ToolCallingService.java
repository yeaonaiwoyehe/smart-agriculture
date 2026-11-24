package com.agri.cms.ai.tool;

import com.agri.cms.ai.audit.AgentAuditLog;
import com.agri.cms.ai.audit.AuditLogService;
import com.agri.cms.ai.client.LlmClient;
import com.agri.cms.ai.model.ChatCompletionResponse;
import com.agri.cms.ai.model.ChatMessage;
import com.agri.cms.ai.model.ToolCall;
import com.agri.cms.ai.model.ToolDefinition;
import com.agri.cms.ai.prompt.PromptTemplateManager;
import com.agri.cms.common.utils.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 工具调用编排服务（A-05），并在 A-06 接入指令审计。
 *
 * 在大模型与业务工具之间完成一轮闭环：
 * 1. 携带工具清单请求大模型，模型自主决定直接回答或调用工具；
 * 2. 读工具：参数校验通过后直接执行，结果回传模型生成自然语言；
 * 3. 写工具：参数校验通过后不立即执行，返回确认令牌，由用户二次确认；
 * 4. 确认接口凭令牌执行写工具，生成回执编号并返回。
 *
 * 每一轮处理（含确认）都会异步写入审计表，可追溯用户、指令、工具与结果。
 *
 * @author agrinav
 */
@Service
public class ToolCallingService {

    private static final Logger log = LoggerFactory.getLogger(ToolCallingService.class);

    /** 默认工具调度提示词：用户未显式提供系统提示词时加载，强化口语意图识别 */
    private static final String DEFAULT_TOOL_PROMPT_PATH = "prompts/common/tool-orchestration.txt";

    private final LlmClient llmClient;
    private final ToolRegistry toolRegistry;
    private final ToolArgumentValidator argumentValidator;
    private final ConfirmationManager confirmationManager;
    private final AuditLogService auditLogService;
    private final ToolAccessControl accessControl;
    private final PromptTemplateManager promptTemplateManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ToolCallingService(LlmClient llmClient,
                              ToolRegistry toolRegistry,
                              ToolArgumentValidator argumentValidator,
                              ConfirmationManager confirmationManager,
                              AuditLogService auditLogService,
                              ToolAccessControl accessControl,
                              PromptTemplateManager promptTemplateManager) {
        this.llmClient = llmClient;
        this.toolRegistry = toolRegistry;
        this.argumentValidator = argumentValidator;
        this.confirmationManager = confirmationManager;
        this.auditLogService = auditLogService;
        this.accessControl = accessControl;
        this.promptTemplateManager = promptTemplateManager;
    }

    /**
     * 处理用户消息，可携带系统提示词。
     */
    public ToolConversation handle(String systemPrompt, String userMessage) {
        long startNanos = System.nanoTime();
        List<ChatMessage> messages = new ArrayList<>();
        // 未显式提供系统提示词时，加载默认工具调度提示词，让纯自然语言也能准确选工具
        String effectivePrompt = (systemPrompt != null && !systemPrompt.isBlank())
                ? systemPrompt
                : promptTemplateManager.renderTemplate(DEFAULT_TOOL_PROMPT_PATH, Map.of());
        if (effectivePrompt != null && !effectivePrompt.isBlank()) {
            messages.add(ChatMessage.system(effectivePrompt));
        }
        messages.add(ChatMessage.user(userMessage));

        ToolConversation result = run(messages);
        writeAudit(userMessage, result, elapsedMillis(startNanos));
        return result;
    }

    /**
     * 用户确认后执行写操作，返回执行结果与回执编号。
     */
    public ToolConversation confirm(String confirmToken) {
        long startNanos = System.nanoTime();
        PendingConfirmation pending = confirmationManager.consume(confirmToken);
        if (pending == null) {
            ToolConversation result = ToolConversation.failed("确认令牌无效或已过期，请重新发起操作");
            writeAudit("确认执行写操作（令牌已失效）", result, elapsedMillis(startNanos));
            return result;
        }

        AgentTool tool = toolRegistry.getTool(pending.toolName());
        if (tool == null) {
            ToolConversation result = ToolConversation.failed("待执行的工具已不存在");
            writeAudit(pending.summary(), result, elapsedMillis(startNanos));
            return result;
        }

        // 确认执行前再次校验权限，防止越权确认
        if (!accessControl.canAccess(tool)) {
            ToolConversation result = ToolConversation.failed(accessControl.denyReason(tool));
            writeAudit(pending.summary(), result, elapsedMillis(startNanos));
            return result;
        }

        ToolResult result = tool.execute(pending.arguments());
        if (!result.success()) {
            ToolConversation conv = ToolConversation.failed(
                    "操作未成功：" + result.errorMessage())
                    .withTool(tool.name(), writeArguments(pending.arguments()));
            writeAudit(pending.summary(), conv, elapsedMillis(startNanos));
            return conv;
        }

        log.info("写操作确认执行完成：{} 回执={}", tool.name(), result.receiptNo());
        ToolConversation conv = new ToolConversation(
                "REPLY", result.content(), null, null,
                result.receiptNo(), tool.name(), writeArguments(pending.arguments()));
        writeAudit(pending.summary(), conv, elapsedMillis(startNanos));
        return conv;
    }

    /**
     * 执行一次带工具的模型交互。
     */
    private ToolConversation run(List<ChatMessage> messages) {
        // 只把当前用户有权限的工具下发给大模型，模型看不到无权工具
        List<AgentTool> accessible = accessControl.accessibleTools(toolRegistry.listTools());
        List<ToolDefinition> tools = toolRegistry.toDefinitions(accessible);
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
        String rawArguments = call.function().arguments();

        AgentTool tool = toolRegistry.getTool(call.function().name());
        if (tool == null) {
            return ToolConversation.failed("模型请求了未知工具：" + call.function().name());
        }

        Map<String, Object> arguments = parseArguments(rawArguments);

        // 执行前权限兜底校验：即使模型经工具清单过滤，也防止直接调用越权工具
        if (!accessControl.canAccess(tool)) {
            log.warn("越权工具调用被拒绝：用户={} 工具={}", SecurityUtils.getUsername(), tool.name());
            return ToolConversation.failed(accessControl.denyReason(tool));
        }

        List<String> missing = argumentValidator.validate(tool, arguments);
        if (!missing.isEmpty()) {
            return ToolConversation.needParams("缺少必要信息：" + String.join("、", missing)
                    + "，请补充后再试").withTool(tool.name(), rawArguments);
        }

        if (tool.isWriteOperation()) {
            // 写操作：登记待确认，等待用户二次确认
            String summary = buildSummary(tool, arguments);
            String token = confirmationManager
                    .register(PendingConfirmation.of(tool.name(), arguments, summary));
            log.info("写操作等待确认：{} 确认令牌={}", tool.name(), token);
            return ToolConversation.needConfirm(token, summary).withTool(tool.name(), rawArguments);
        }

        // 读操作：直接执行并把结果回传模型
        ToolResult result = tool.execute(arguments);
        ToolConversation conversation = finalizeWithToolResult(messages, call.id(), result);
        return conversation.withTool(tool.name(), rawArguments);
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
     * 统一构建并异步写入审计记录，审计本身异常不影响主流程。
     */
    private void writeAudit(String userMessage, ToolConversation result, long costMs) {
        try {
            AgentAuditLog auditLog = new AgentAuditLog();
            auditLog.setMessage(userMessage);
            auditLog.setStatus(result.status());
            auditLog.setReply(result.reply());
            auditLog.setReceiptNo(result.receiptNo());
            auditLog.setToolName(result.toolName());
            auditLog.setToolArguments(result.toolArguments());
            if (result.summary() != null) {
                auditLog.setToolResult(result.summary());
            }
            if ("FAILED".equals(result.status())) {
                auditLog.setErrorMsg(result.reply());
            }
            auditLog.setCostMs(costMs);
            auditLogService.record(auditLog);
        } catch (Exception e) {
            log.warn("写入审计记录时发生异常：{}", e.getMessage());
        }
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
     * 把参数 Map 序列化为 JSON 字符串用于审计，失败时返回兜底文本。
     */
    private String writeArguments(Map<String, Object> arguments) {
        try {
            return objectMapper.writeValueAsString(arguments);
        } catch (Exception e) {
            return String.valueOf(arguments);
        }
    }

    /**
     * 生成写操作的确认摘要。
     */
    private String buildSummary(AgentTool tool, Map<String, Object> arguments) {
        return "请确认执行「" + tool.name() + "」，参数：" + arguments;
    }

    private long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }
}
