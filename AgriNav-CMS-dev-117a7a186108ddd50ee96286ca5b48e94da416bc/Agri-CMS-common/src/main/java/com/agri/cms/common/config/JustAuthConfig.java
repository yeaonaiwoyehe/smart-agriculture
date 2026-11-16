package com.agri.cms.common.config;

import com.agri.cms.common.annotation.Log;
import com.agri.cms.common.enums.BusinessType;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.request.AuthRequest;
import me.zhyd.oauth.request.AuthWeChatOpenRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author simon
 * @since 2024/10/21
 */
@Configuration
public class JustAuthConfig {


    @Value("${justAuth.web.wechat.client-id}")
    private String webClientId;
    @Value("${justAuth.web.wechat.client-secret}")
    private String webClientSecret;
    @Value("${justAuth.mobile.wechat.client-id}")
    private String mobileClientId;
    @Value("${justAuth.mobile.wechat.client-secret}")
    private String mobileClientSecret;
    @Value("${justAuth.web.wechat.redirect-uri}")
    private String redirectUri;

    @Bean
    public AuthRequest mobileAuthRequest() {
        return new AuthWeChatOpenRequest(AuthConfig.builder()
                .clientId(mobileClientId)
                .clientSecret(mobileClientSecret)
                .redirectUri(redirectUri)
                .ignoreCheckState(true)
                .build());
    }

    @Bean
    public AuthRequest webAuthRequest() {
        return new AuthWeChatOpenRequest(AuthConfig.builder()
                .clientId(webClientId)
                .clientSecret(webClientSecret)
                .redirectUri(redirectUri)
                .ignoreCheckState(true)
                .build());
    }
}
