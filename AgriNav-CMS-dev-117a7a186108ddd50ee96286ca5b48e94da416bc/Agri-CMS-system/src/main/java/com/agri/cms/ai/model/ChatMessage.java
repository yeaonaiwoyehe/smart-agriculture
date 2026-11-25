package com.agri.cms.ai.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 一条对话消息，对应 OpenAI/DeepSeek 接口中的 message 对象。
 *
 * role 取值：
 * system（设定角色与规则）、user（用户发言）、assistant（模型回复，可携带 tool_calls）、
 * tool（工具执行结果，需带 tool_call_id）。
 *
 * 空字段不序列化，保证普通对话与工具调用复用同一结构、输出 JSON 干净。
 *
 * @param role       角色
 * @param content    文本内容
 * @param toolCalls  assistant 请求调用的工具列表
 * @param toolCallId tool 消息所对应的工具调用编号
 * @author agrinav
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatMessage(
        String role,
        String content,
        @JsonProperty("tool_calls") List<ToolCall> toolCalls,
        @JsonProperty("tool_call_id") String toolCallId
) {

    public static ChatMessage system(String content) {
        return new ChatMessage("system", content, null, null);
    }

    public static ChatMessage user(String content) {
        return new ChatMessage("user", content, null, null);
    }

    public static ChatMessage assistant(String content) {
        return new ChatMessage("assistant", content, null, null);
    }

    /**
     * 一条请求调用工具的 assistant 消息，content 为空、携带 tool_calls。
     */
    public static ChatMessage assistant(List<ToolCall> toolCalls) {
        return new ChatMessage("assistant", null, toolCalls, null);
    }

    /**
     * 一条工具执行结果消息，关联对应的工具调用编号。
     */
    public static ChatMessage tool(String toolCallId, String content) {
        return new ChatMessage("tool", content, null, toolCallId);
    }
}
