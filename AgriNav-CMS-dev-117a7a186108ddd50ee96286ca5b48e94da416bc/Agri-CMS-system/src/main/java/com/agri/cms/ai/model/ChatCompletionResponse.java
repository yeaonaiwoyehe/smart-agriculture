package com.agri.cms.ai.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 大模型对话补全响应体，对应 /chat/completions 返回的 JSON。
 *
 * @param id       本次请求的唯一标识
 * @param choices  候选回答列表（非流式通常只有一个）
 * @param usage    token 用量统计
 * @author agrinav
 */
public record ChatCompletionResponse(
        String id,
        List<Choice> choices,
        Usage usage
) {

    /**
     * 一条候选回答。
     *
     * @param index         候选序号
     * @param message       模型生成的消息
     * @param finishReason  结束原因，如 stop（正常结束）
     */
    public record Choice(
            int index,
            ChatMessage message,
            @JsonProperty("finish_reason") String finishReason
    ) {
    }

    /**
     * token 用量。
     */
    public record Usage(
            @JsonProperty("prompt_tokens") int promptTokens,
            @JsonProperty("completion_tokens") int completionTokens,
            @JsonProperty("total_tokens") int totalTokens
    ) {
    }
}
