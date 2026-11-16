package org.yzh.web.service;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

@Service
public class RpcService {

    @Value("${cms-server.host}")
    private String cmsServerHost;

    @Value("${cms-server.port}")
    private int cmsServerPort;

    private final CloseableHttpClient httpClient;

    public RpcService() {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(100);
        connectionManager.setDefaultMaxPerRoute(20);

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(5000)
                .setSocketTimeout(5000)
                .build();

        this.httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

    public String sendRemoteRequest(String endpoint) throws IOException {
        String url = "http://" + cmsServerHost + ":" + cmsServerPort + endpoint;
        HttpGet request = new HttpGet(url);
            return httpClient.execute(request, response -> 
                EntityUtils.toString(response.getEntity()));
    }
    public String sendRemoteRequest(String endpoint, String token) throws IOException {
        String url = "http://" + cmsServerHost + ":" + cmsServerPort + endpoint;
        HttpGet request = new HttpGet(url);

        // 如果提供了 Token，则添加到请求头中
        if (token != null && !token.isEmpty()) {
            request.setHeader("Authorization", "Bearer " + token);
        }

        return httpClient.execute(request, response ->
                EntityUtils.toString(response.getEntity()));
    }

    public boolean validatePhoneNumber(String phoneNumber) throws IOException {
        String response = sendRemoteRequest("/system/user/check/" + phoneNumber);
        // 假设CMS返回"true"表示手机号有效，"false"表示无效
        return !Boolean.parseBoolean(response);
    }
}
