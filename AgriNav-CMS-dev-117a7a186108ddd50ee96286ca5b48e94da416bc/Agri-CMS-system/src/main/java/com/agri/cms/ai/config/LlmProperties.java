package com.agri.cms.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 大模型接入配置。
 *
 * 对应 application-dev.yml 中以 llm 开头的配置段，敏感的 api-key
 * 通过环境变量 DEEPSEEK_API_KEY 注入，不写死在代码或仓库里。
 *
 * @author agrinav
 */
@ConfigurationProperties(prefix = "llm")
public class LlmProperties {

    /** 接口基础地址，DeepSeek 官方为 https://api.deepseek.com */
    private String baseUrl = "https://api.deepseek.com";

    /** API Key，从环境变量 DEEPSEEK_API_KEY 读取，默认空（未配置时不发起真实调用） */
    private String apiKey = "";

    /** 模型名称，DeepSeek 对话模型为 deepseek-chat */
    private String model = "deepseek-chat";

    /** 建立连接超时（秒） */
    private long connectTimeout = 10;

    /** 读取响应超时（秒），大模型生成可能较慢，默认 60 秒 */
    private long readTimeout = 60;

    /** 采样温度，0~2，越高越随机，默认 0.7 */
    private double temperature = 0.7;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public long getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(long connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public long getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(long readTimeout) {
        this.readTimeout = readTimeout;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }
}
