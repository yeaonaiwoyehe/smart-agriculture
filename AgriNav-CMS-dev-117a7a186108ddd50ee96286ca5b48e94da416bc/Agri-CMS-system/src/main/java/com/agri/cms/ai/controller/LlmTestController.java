package com.agri.cms.ai.controller;

import com.agri.cms.ai.client.LlmClient;
import com.agri.cms.ai.intent.IntentRecognitionService;
import com.agri.cms.ai.intent.IntentResult;
import com.agri.cms.ai.prompt.AgentType;
import com.agri.cms.ai.prompt.PromptTemplateManager;
import com.agri.cms.common.core.controller.BaseController;
import com.agri.cms.common.core.domain.AjaxResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 大模型对话测试接口。
 *
 * /llm/chat：不携带系统提示词的原始对话（A-01）；
 * /llm/agent：按指定智能体加载系统提示词后对话（A-03）；
 * /llm/intent：输出结构化意图与参数，缺参时返回追问话术（A-04）。
 *
 * @author agrinav
 */
@RestController
@RequestMapping("/llm")
public class LlmTestController extends BaseController {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy年MM月dd日");

    private final LlmClient llmClient;
    private final PromptTemplateManager promptTemplateManager;
    private final IntentRecognitionService intentRecognitionService;

    public LlmTestController(LlmClient llmClient,
                             PromptTemplateManager promptTemplateManager,
                             IntentRecognitionService intentRecognitionService) {
        this.llmClient = llmClient;
        this.promptTemplateManager = promptTemplateManager;
        this.intentRecognitionService = intentRecognitionService;
    }

    /**
     * 原始对话：不携带系统提示词，保持 A-01 链路验证方式。
     */
    @GetMapping("/chat")
    public AjaxResult chat(@RequestParam String message) {
        return AjaxResult.success("操作成功", llmClient.chat(message));
    }

    /**
     * 智能体对话：根据 agent 编号加载对应系统提示词，再与用户消息一起发给大模型。
     */
    @GetMapping("/agent")
    public AjaxResult agent(@RequestParam String agent, @RequestParam String message) {
        AgentType type = AgentType.fromCode(agent);
        if (type == null) {
            return AjaxResult.error("未知的智能体编号：" + agent);
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("current_date", LocalDate.now().format(DATE_FORMATTER));
        variables.put("user_name", resolveDisplayName());
        // RAG 尚未建设（A-08），先给制度问答一个占位说明，避免提示词出现空引用
        if (type == AgentType.MA_03) {
            variables.put("retrieved_context", "（暂无可用的制度资料）");
        }

        String systemPrompt = promptTemplateManager.getSystemPrompt(type, variables);
        return AjaxResult.success("操作成功", llmClient.chat(systemPrompt, message));
    }

    /**
     * 意图识别：返回结构化意图、参数；缺参时附带一次追问话术。
     */
    @GetMapping("/intent")
    public AjaxResult intent(@RequestParam String message,
                             @RequestParam(required = false) String side) {
        IntentResult result = intentRecognitionService.recognize(message, side);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("intent", result.intent().getCode());
        data.put("intentName", result.intent().getDisplayName());
        data.put("confidence", result.confidence());
        data.put("parameters", result.parameters());
        data.put("missingParams", result.missingParams());
        data.put("needFollowUp", result.needFollowUp());
        if (result.needFollowUp()) {
            data.put("followUpQuestion", intentRecognitionService.buildFollowUpQuestion(result));
        }
        return AjaxResult.success("操作成功", data);
    }

    /**
     * 取当前登录用户昵称，取不到时用通用称呼兜底。
     */
    private String resolveDisplayName() {
        try {
            String nickName = getLoginUser().getUser().getNickName();
            return (nickName == null || nickName.isBlank()) ? "用户" : nickName;
        } catch (Exception e) {
            return "用户";
        }
    }
}
