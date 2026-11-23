package com.agri.cms.ai.controller;

import com.agri.cms.ai.client.LlmClient;
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
import java.util.Map;

/**
 * 大模型对话测试接口。
 *
 * /llm/chat：不携带系统提示词的原始对话，用于确认接入链路（A-01）；
 * /llm/agent：按指定智能体（如 AG-01、MA-03）加载对应的系统提示词后再对话，
 * 用于验证提示词模板集中管理是否生效（A-03）。
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

    public LlmTestController(LlmClient llmClient, PromptTemplateManager promptTemplateManager) {
        this.llmClient = llmClient;
        this.promptTemplateManager = promptTemplateManager;
    }

    /**
     * 原始对话：不携带系统提示词，保持 A-01 链路验证方式。
     */
    @GetMapping("/chat")
    public AjaxResult chat(@RequestParam String message) {
        // 显式用两参数版本，避免 String 被重载匹配到 success(String msg)，确保回复进入 data
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
