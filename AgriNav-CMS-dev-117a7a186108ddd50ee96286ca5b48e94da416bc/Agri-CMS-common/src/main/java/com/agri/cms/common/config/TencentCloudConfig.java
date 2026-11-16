package com.agri.cms.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 腾讯云配置.
 *
 * @author simon
 * @since 2023/05/16
 */

@Configuration
@ConfigurationProperties(prefix = "tencentcloud")
public class TencentCloudConfig {

    /**
     * 腾讯云账户信息
     */
    private Account account = new Account();

    /**
     * 腾讯云短信服务信息
     */
    private Sms sms = new Sms();

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public Sms getSms() {
        return sms;
    }

    public void setSms(Sms sms) {
        this.sms = sms;
    }

    public static class Account {

        /**
         * 腾讯云账户密钥对
         */
        private String secretId;

        /**
         * 腾讯云账户密钥对
         */
        private String secretKey;

        public String getSecretId() {
            return secretId;
        }

        public void setSecretId(String secretId) {
            this.secretId = secretId;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }
    }

    public static class Sms {

        /**
         * 短信应用 ID
         */
        private String sdkAppId;

        /**
         * 短信签名内容
         */
        private String signName;

        /**
         * 短信模板 ID
         */
        private String templateId;


        public String getSdkAppId() {
            return sdkAppId;
        }

        public void setSdkAppId(String sdkAppId) {
            this.sdkAppId = sdkAppId;
        }

        public String getSignName() {
            return signName;
        }

        public void setSignName(String signName) {
            this.signName = signName;
        }

        public String getTemplateId() {
            return templateId;
        }

        public void setTemplateId(String templateId) {
            this.templateId = templateId;
        }
    }
}
