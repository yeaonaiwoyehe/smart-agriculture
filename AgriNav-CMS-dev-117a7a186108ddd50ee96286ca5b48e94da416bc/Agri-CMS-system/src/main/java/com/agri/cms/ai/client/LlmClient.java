package com.agri.cms.ai.client;

import com.agri.cms.ai.config.LlmProperties;
import com.agri.cms.ai.model.ChatCompletionRequest;
import com.agri.cms.ai.model.ChatCompletionResponse;
import com.agri.cms.ai.model.ChatMessage;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * 大模型客户端：对 RestClient 调用做一层业务封装。
 *
 * 上层只需要传消息、拿回复文本，不关心 URL、请求体结构和响应解析。
 * A-02 会在这一层补充超时之外的重试、限流和异常兜底。
 *
 * @author agrinav
 */
@Component
public class LlmClient {

    private final RestClient llmRestClient;
    private final LlmProperties properties;

    public LlmClient(RestClient llmRestClient, LlmProperties properties) {
        this.llmRestClient = llmRestClient;
        this.properties = properties;
    }

    /**
     * 单轮对话：只传一句用户消息，返回模型回复文本。
     *
     * @param userMessage 用户输入
     * @return 模型回复文本
     */
    public String chat(String userMessage) {
        return chat(List.of(ChatMessage.user(userMessage)));
    }

    /**
     * 多轮对话：传入完整消息列表（可含 system 设定和历史消息），返回模型回复文本。
     *
     * @param messages 消息列表
     * @return 模型回复文本
     */
    public String chat(List<ChatMessage> messages) {
        ChatCompletionRequest request = ChatCompletionRequest.of(
                properties.getModel(), messages, properties.getTemperature());

        ChatCompletionResponse response = llmRestClient.post()
                .uri("/chat/completions")
                .body(request)
                .retrieve()
                .body(ChatCompletionResponse.class);

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new IllegalStateException("大模型返回内容为空");
        }
        return response.choices().get(0).message().content();
    }
}
