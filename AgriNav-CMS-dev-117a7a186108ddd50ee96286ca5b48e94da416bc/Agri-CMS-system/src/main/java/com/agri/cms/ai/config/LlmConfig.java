package com.agri.cms.ai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * 大模型相关 Bean 配置。
 *
 * 构建一个预填好基础地址、认证头和超时时间的 RestClient，
 * 业务代码直接注入即可调用，无需每次重复设置。
 *
 * @author agrinav
 */
@Configuration
@EnableConfigurationProperties(LlmProperties.class)
public class LlmConfig {

    @Bean
    public RestClient llmRestClient(LlmProperties properties) {
        // 简单的 JDK 原生 HTTP 工厂，用于设置连接和读取超时，A-02 会在此基础上加重试与兜底
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) properties.getConnectTimeout() * 1000);
        factory.setReadTimeout((int) properties.getReadTimeout() * 1000);

        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(factory)
                .defaultHeader("Authorization", "Bearer " + properties.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
