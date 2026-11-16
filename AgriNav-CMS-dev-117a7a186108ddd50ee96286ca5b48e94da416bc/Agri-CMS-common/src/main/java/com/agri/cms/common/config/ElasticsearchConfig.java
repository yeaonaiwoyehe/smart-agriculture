package com.agri.cms.common.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.agri.cms.common.config.serializer.PointConverter;
import com.agri.cms.common.config.serializer.PolygonConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.elasticsearch.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions;

import java.io.IOException;
import java.util.List;


/**
 * Elasticsearch配置类 - 适用于ES 7.17.9版本
 * @author mfr
 * @since 2024/11/8 */
@Configuration
public class ElasticsearchConfig implements DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchConfig.class);

    @Value("${spring.elasticsearch.host}")
    private String host;

    @Value("${spring.elasticsearch.port}")
    private Integer port;

    @Value("${spring.elasticsearch.scheme}")
    private String scheme;

    @Value("${spring.elasticsearch.username}")
    private String username;

    @Value("${spring.elasticsearch.password}")
    private String password;

    @Value("${spring.elasticsearch.connection-timeout:30000}")
    private int connectionTimeout;

    @Value("${spring.elasticsearch.socket-timeout:600000}")
    private int socketTimeout;

    @Value("${spring.elasticsearch.pool.max-conn-total:100}")
    private int maxConnTotal;

    @Value("${spring.elasticsearch.pool.max-conn-per-route:40}")
    private int maxConnPerRoute;

    @Value("${spring.elasticsearch.retry.max-retries:3}")
    private int maxRetries;

    @Value("${spring.elasticsearch.retry.initial-backoff:1000}")
    private int initialBackoff;

    private RestHighLevelClient restHighLevelClient;


     @Bean
     public ElasticsearchCustomConversions elasticsearchCustomConversions() {
         return new ElasticsearchCustomConversions(
                 List.of(
                         new PolygonConverter(),
                         new PointConverter()
                 )
         );
     }
    @Bean
    public ElasticsearchClient elasticSearchClient() {
        ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        // 配置认证信息
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(username, password));

        // 创建RestClient
        RestClient restClient = RestClient.builder(
            new HttpHost(host, port))
            .setHttpClientConfigCallback(httpClientBuilder ->
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider))
            .build();

        // 创建Transport
        ElasticsearchTransport transport = new RestClientTransport(
            restClient,
            new JacksonJsonpMapper(mapper)
        );

        return new ElasticsearchClient(transport);
    }

    /**
     * 创建ES客户端
     */
    @Bean
    public RestHighLevelClient client() {
        try {
            // 创建凭据提供程序
            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(username, password));

            // 创建RestClient构建器
            RestClientBuilder builder = RestClient.builder(
                            new HttpHost(host, port, scheme))
                    .setHttpClientConfigCallback(httpClientBuilder -> {
                        // 设置认证信息
                        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                        // 设置连接池
                        httpClientBuilder.setMaxConnTotal(maxConnTotal);
                        httpClientBuilder.setMaxConnPerRoute(maxConnPerRoute);
                        // 配置IO线程
                        httpClientBuilder.setDefaultIOReactorConfig(
                                IOReactorConfig.custom()
                                        .setIoThreadCount(Runtime.getRuntime().availableProcessors())
                                        .setSoKeepAlive(true)
                                        .build()
                        );
                        return httpClientBuilder;
                    })
                    // 设置请求配置
                    .setRequestConfigCallback(requestConfigBuilder ->
                            requestConfigBuilder
                                    .setConnectTimeout(connectionTimeout)
                                    .setSocketTimeout(socketTimeout)
                                    .setConnectionRequestTimeout(connectionTimeout))
                    // 设置失败监听器
                    .setFailureListener(new RestClient.FailureListener() {
                        @Override
                        public void onFailure(Node node) {
                            logger.error("ES node {} failed", node);
                        }
                    })
                    // 设置节点选择器
                    .setNodeSelector(NodeSelector.SKIP_DEDICATED_MASTERS);

            // 创建客户端实例
            restHighLevelClient = new RestHighLevelClient(builder);

            // 验证连接（带重试）
            validateConnectionWithRetry();

            return restHighLevelClient;
        } catch (Exception e) {
            logger.error("Failed to create ES client", e);
            throw new RuntimeException("Could not create ES client", e);
        }
    }

    /**
     * 带重试机制的连接验证
     */
    private void validateConnectionWithRetry() {
        int retryCount = 0;
        while (retryCount < maxRetries) {
            try {
                boolean ping = restHighLevelClient.ping(RequestOptions.DEFAULT);
                logger.info("ES connection test result: {}", ping);
                if (ping) {
                    return;
                }
            } catch (IOException e) {
                retryCount++;
                if (retryCount == maxRetries) {
                    logger.error("Failed to connect to ES after {} retries", maxRetries, e);
                    break;
                }
                // 指数退避重试
                long waitTime = initialBackoff * (long) Math.pow(2, retryCount - 1);
                logger.warn("Retry attempt {} of {}, waiting {}ms", retryCount, maxRetries, waitTime);
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /**
     * 执行带重试的ES操作
     */
    public <T> T executeWithRetry(ESOperation<T> operation) throws IOException {
        int retryCount = 0;
        IOException lastException = null;

        while (retryCount < maxRetries) {
            try {
                return operation.execute(restHighLevelClient);
            } catch (IOException e) {
                lastException = e;
                retryCount++;
                if (retryCount == maxRetries) {
                    break;
                }
                // 指数退避重试
                long waitTime = initialBackoff * (long) Math.pow(2, retryCount - 1);
                logger.warn("Operation failed, retry attempt {} of {}, waiting {}ms",
                        retryCount, maxRetries, waitTime);
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Retry interrupted", e);
                }
            }
        }

        throw new IOException("Operation failed after " + maxRetries + " retries", lastException);
    }

    /**
     * ES操作接口
     */
    @FunctionalInterface
    public interface ESOperation<T> {
        T execute(RestHighLevelClient client) throws IOException;
    }

    /**
     * 销毁时关闭客户端
     */
    @Override
    public void destroy() {
        try {
            if (restHighLevelClient != null) {
                logger.info("Closing Elasticsearch client");
                restHighLevelClient.close();
            }
        } catch (IOException e) {
            logger.error("Error closing Elasticsearch client", e);
        }
    }
}