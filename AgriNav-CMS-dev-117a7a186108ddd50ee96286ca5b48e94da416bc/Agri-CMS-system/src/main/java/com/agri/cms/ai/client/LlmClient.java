package com.agri.cms.ai.client;

import com.agri.cms.ai.config.LlmProperties;
import com.agri.cms.ai.model.ChatCompletionRequest;
import com.agri.cms.ai.model.ChatCompletionResponse;
import com.agri.cms.ai.model.ChatMessage;
import com.agri.cms.ai.model.ToolDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * 大模型客户端：在 RestClient 之上做一层健壮性封装。
 *
 * 提供能力：
 * 1. 限流：Semaphore 控制最大并发调用数，避免把接口打爆；
 * 2. 重试：网络/超时、429、5xx 等可恢复错误自动重试，带退避间隔；
 * 3. 兜底：重试耗尽或不可重试错误时，文本对话返回固定友好提示，结构化调用返回 null；
 * 4. 日志：记录每次调用耗时、成功/失败与重试次数，便于排查。
 *
 * 普通对话用 {@link #chat}，工具调用框架用 {@link #exchange} 拿完整响应。
 *
 * @author agrinav
 */
@Component
public class LlmClient {

    private static final Logger log = LoggerFactory.getLogger(LlmClient.class);

    private final RestClient llmRestClient;
    private final LlmProperties properties;

    /** 并发限流器，许可数由 maxConcurrent 决定 */
    private final Semaphore rateLimiter;

    public LlmClient(RestClient llmRestClient, LlmProperties properties) {
        this.llmRestClient = llmRestClient;
        this.properties = properties;
        this.rateLimiter = new Semaphore(properties.getMaxConcurrent());
    }

    /**
     * 单轮对话。
     */
    public String chat(String userMessage) {
        return chat(List.of(ChatMessage.user(userMessage)));
    }

    /**
     * 带系统提示词的单轮对话：在用户消息前插入一条 system 消息（A-03）。
     */
    public String chat(String systemPrompt, String userMessage) {
        List<ChatMessage> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(ChatMessage.system(systemPrompt));
        }
        messages.add(ChatMessage.user(userMessage));
        return chat(messages);
    }

    /**
     * 多轮对话：返回模型回复文本；任何异常都不抛出，最终失败返回兜底提示。
     */
    public String chat(List<ChatMessage> messages) {
        ChatCompletionResponse response = invokeWithGuard(messages, null);
        if (response == null) {
            return properties.getFallbackMessage();
        }
        String content = response.choices().get(0).message().content();
        if (content == null || content.isBlank()) {
            return properties.getFallbackMessage();
        }
        return content;
    }

    /**
     * 带工具的结构化调用：返回完整响应（可能含 tool_calls）；最终失败返回 null。
     */
    public ChatCompletionResponse exchange(List<ChatMessage> messages, List<ToolDefinition> tools) {
        return invokeWithGuard(messages, tools);
    }

    /**
     * 统一的限流 + 重试 + 日志骨架，普通对话与工具调用共用。
     */
    private ChatCompletionResponse invokeWithGuard(List<ChatMessage> messages,
                                                   List<ToolDefinition> tools) {
        long start = System.currentTimeMillis();
        boolean acquired = false;
        try {
            acquired = rateLimiter.tryAcquire(
                    properties.getAcquireTimeoutMillis(), TimeUnit.MILLISECONDS);
            if (!acquired) {
                log.warn("LLM 限流：等待 {}ms 仍未获得许可，放弃本次调用",
                        properties.getAcquireTimeoutMillis());
                return null;
            }

            Exception lastError = null;
            for (int attempt = 0; attempt <= properties.getMaxRetries(); attempt++) {
                try {
                    ChatCompletionResponse response = doExchange(messages, tools);
                    log.info("LLM 调用成功，耗时 {}ms", System.currentTimeMillis() - start);
                    return response;
                } catch (Exception e) {
                    lastError = e;
                    if (!isRetryable(e) || attempt == properties.getMaxRetries()) {
                        break;
                    }
                    log.warn("LLM 第 {} 次调用失败（{}），{}ms 后重试",
                            attempt + 1, reason(e), properties.getRetryBackoffMillis());
                    sleepBackoff();
                }
            }

            log.error("LLM 调用最终失败，已重试 {} 次，总耗时 {}ms，错误：{}",
                    properties.getMaxRetries(), System.currentTimeMillis() - start,
                    reason(lastError), lastError);
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("LLM 限流等待被中断，放弃本次调用");
            return null;
        } finally {
            if (acquired) {
                rateLimiter.release();
            }
        }
    }

    /**
     * 真正发起一次 HTTP 调用并返回完整响应（不做重试，重试由上层控制）。
     */
    private ChatCompletionResponse doExchange(List<ChatMessage> messages,
                                              List<ToolDefinition> tools) {
        ChatCompletionRequest request = (tools == null || tools.isEmpty())
                ? ChatCompletionRequest.of(properties.getModel(), messages, properties.getTemperature())
                : ChatCompletionRequest.withTools(properties.getModel(), messages,
                        properties.getTemperature(), tools);

        ChatCompletionResponse response = llmRestClient.post()
                .uri("/chat/completions")
                .body(request)
                .retrieve()
                .body(ChatCompletionResponse.class);

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new IllegalStateException("大模型返回内容为空");
        }
        return response;
    }

    /**
     * 判断异常是否值得重试：
     * 网络/IO/超时（ResourceAccessException）、429 限流、5xx 服务端错误可重试；
     * 400/401/403 等客户端错误是请求本身有问题，重试无意义。
     */
    private boolean isRetryable(Exception e) {
        if (e instanceof ResourceAccessException) {
            return true;
        }
        if (e instanceof HttpStatusCodeException hse) {
            int status = hse.getStatusCode().value();
            return status == 429 || status >= 500;
        }
        return false;
    }

    /**
     * 提取用于日志的简短错误原因。
     */
    private String reason(Exception e) {
        if (e instanceof HttpStatusCodeException hse) {
            return "HTTP " + hse.getStatusCode().value();
        }
        return e.getClass().getSimpleName();
    }

    /**
     * 重试前退避等待，被中断时恢复中断标志。
     */
    private void sleepBackoff() {
        try {
            Thread.sleep(properties.getRetryBackoffMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
