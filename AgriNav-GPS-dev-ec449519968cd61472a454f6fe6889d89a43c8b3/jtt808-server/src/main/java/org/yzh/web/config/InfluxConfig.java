package org.yzh.web.config;

import com.influxdb.client.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
public class InfluxConfig {


    @Value("${spring.influxdb.url}")
    private String url;
    @Value("${spring.influxdb.token}")
    private String token;
    @Value("${spring.influxdb.org}")
    private String org;
    @Value("${spring.influxdb.bucket}")
    private String bucket;
    @Bean
    public InfluxDBClient influxDBClient() {
        return InfluxDBClientFactory.create(influxDBClientOptions());
    }

    @Bean
    public InfluxDBClientOptions influxDBClientOptions() {
        return InfluxDBClientOptions.builder()
                .url(url)
                .authenticateToken(token.toCharArray())
                .org(org)
                .bucket(bucket)
                .build();
    }

    @Bean
    public WriteApi writeApi(InfluxDBClient influxDBClient) {
        // 异步写的API
        return influxDBClient.getWriteApi();
    }
    @Bean
    public QueryApi queryApi(InfluxDBClient influxDBClient){
        return influxDBClient.getQueryApi();
    }

    public void closeInfluxDBClient() {
        if (influxDBClient() != null) {
            influxDBClient().close();
        }
    }
}