package com.agri.cms.ai.model;

import java.util.List;

/**
 * 大模型对话补全请求体，对应 POST /chat/completions 的请求 JSON。
 *
 * @param model        模型名称，如 deepseek-chat
 * @param messages     对话消息列表（可含历史消息）
 * @param temperature  采样温度
 * @param stream       是否流式返回，本项目 A-01 先取 false（一次性返回）
 * @author agrinav
 */
public record ChatCompletionRequest(
        String model,
        List<ChatMessage> messages,
        Double temperature,
        Boolean stream
) {

    /**
     * 构造一个非流式对话请求。
     */
    public static ChatCompletionRequest of(String model, List<ChatMessage> messages, double temperature) {
        return new ChatCompletionRequest(model, messages, temperature, false);
    }
}
