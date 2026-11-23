package com.agri.cms.ai.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 大模型对话补全请求体，对应 POST /chat/completions 的请求 JSON。
 *
 * 空字段不序列化，普通对话与工具调用复用同一结构。
 *
 * @param model       模型名称，如 deepseek-chat
 * @param messages    对话消息列表（可含历史消息）
 * @param temperature 采样温度
 * @param stream      是否流式返回，本项目取 false（一次性返回）
 * @param tools       可供调用的工具定义，无工具时为 null
 * @param toolChoice  工具调用策略：none / auto / required
 * @author agrinav
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatCompletionRequest(
        String model,
        List<ChatMessage> messages,
        Double temperature,
        Boolean stream,
        List<ToolDefinition> tools,
        @JsonProperty("tool_choice") String toolChoice
) {

    /**
     * 构造一个非流式、无工具的对话请求。
     */
    public static ChatCompletionRequest of(String model, List<ChatMessage> messages, double temperature) {
        return new ChatCompletionRequest(model, messages, temperature, false, null, null);
    }

    /**
     * 构造一个带工具的非流式请求，工具策略默认 auto。
     */
    public static ChatCompletionRequest withTools(String model, List<ChatMessage> messages,
                                                  double temperature, List<ToolDefinition> tools) {
        return new ChatCompletionRequest(model, messages, temperature, false, tools, "auto");
    }
}
