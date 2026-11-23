package com.agri.cms.ai.controller;

import com.agri.cms.ai.client.LlmClient;
import com.agri.cms.common.core.controller.BaseController;
import com.agri.cms.common.core.domain.AjaxResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 大模型对话测试接口（A-01 验证用）。
 *
 * 浏览器或接口工具访问 /llm/chat?message=你好，即可拿到模型回复，
 * 用于确认接入链路是否打通；后续正式智能体接口会在此基础上扩展。
 *
 * @author agrinav
 */
@RestController
@RequestMapping("/llm")
public class LlmTestController extends BaseController {

    private final LlmClient llmClient;

    public LlmTestController(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    @GetMapping("/chat")
    public AjaxResult chat(@RequestParam String message) {
        // 显式用两参数版本，避免 String 被重载匹配到 success(String msg)，确保回复进入 data
        return AjaxResult.success("操作成功", llmClient.chat(message));
    }
}
