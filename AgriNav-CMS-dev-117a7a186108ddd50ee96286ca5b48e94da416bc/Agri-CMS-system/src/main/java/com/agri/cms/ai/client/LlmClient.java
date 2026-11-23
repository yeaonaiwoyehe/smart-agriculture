package com.agri.cms.ai.client;

import com.agri.cms.ai.config.LlmProperties;
import com.agri.cms.ai.model.ChatCompletionRequest;
import com.agri.cms.ai.model.ChatCompletionResponse;
import com.agri.cms.ai.model.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * 大模型客户端：在 RestClient 之上做一层健壮性封装（A-02）。
 *
 * 提供四项能力：
 * 1. 限流：Semaphore 控制最大并发调用数，避免把接口打爆；
 * 2. 重试：网络/超时、429、5xx 等可恢复错误自动重试，带退避间隔；
 * 3. 兜底：重试耗尽或不可重试错误时返回固定友好提示，不向调用方抛 500；
 * 4. 日志：记录每次调用耗时、成功/失败与重试次数，便于排查。
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
     * 多轮对话：传入完整消息列表，返回模型回复；任何异常都不会抛出，最终失败返回兜底提示。
     */
    public String chat(List<ChatMessage> messages) {
        long start = System.currentTimeMillis();
        boolean acquired = false;
        try {
            // 1. 限流：在限定时间内争取一个并发许可
            acquired = rateLimiter.tryAcquire(
                    properties.getAcquireTimeoutMillis(), TimeUnit.MILLISECONDS);
            if (!acquired) {
                log.warn("LLM 限流：等待 {}ms 仍未获得许可，直接走兜底", properties.getAcquireTimeoutMillis());
                return properties.getFallbackMessage();
            }

            // 2. 重试循环：首次 + maxRetries 次重试
            Exception lastError = null;
            for (int attempt = 0; attempt <= properties.getMaxRetries(); attempt++) {
                try {
                    String reply = doChat(messages);
                    long cost = System.currentTimeMillis() - start;
                    if (attempt > 0) {
                        log.info("LLM 调用成功（第 {} 次尝试，共重试 {} 次），耗时 {}ms", attempt + 1, attempt, cost);
                    } else {
                        log.info("LLM 调用成功，耗时 {}ms", cost);
                    }
                    return reply;
                } catch (Exception e) {
                    lastError = e;
                    // 不可重试的错误，或已经是最后一次尝试，就退出循环走兜底
                    if (!isRetryable(e) || attempt == properties.getMaxRetries()) {
                        break;
                    }
                    log.warn("LLM 第 {} 次调用失败（{}），{}ms 后重试",
                            attempt + 1, reason(e), properties.getRetryBackoffMillis());
                    sleepBackoff();
                }
            }

            // 3. 重试耗尽，记录错误并兜底
            log.error("LLM 调用最终失败，已重试 {} 次，总耗时 {}ms，错误：{}",
                    properties.getMaxRetries(), System.currentTimeMillis() - start, reason(lastError), lastError);
            return properties.getFallbackMessage();
        } catch (InterruptedException e) {
            // 限流等待被中断，恢复中断标志并兜底
            Thread.currentThread().interrupt();
            log.warn("LLM 限流等待被中断，走兜底");
            return properties.getFallbackMessage();
        } finally {
            if (acquired) {
                rateLimiter.release();
            }
        }
    }

    /**
     * 真正发起一次 HTTP 调用并解析回复文本（不做重试，重试由上层 chat 控制）。
     */
    private String doChat(List<ChatMessage> messages) {
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
        String content = response.choices().get(0).message().content();
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("大模型回复文本为空");
        }
        return content;
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
