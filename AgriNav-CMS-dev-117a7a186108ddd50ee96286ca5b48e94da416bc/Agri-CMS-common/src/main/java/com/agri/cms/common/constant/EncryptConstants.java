package com.agri.cms.common.constant;

/**
 * 加密常量
 *
 * @author mfr
 * @since 2024/11/7
 */
public class EncryptConstants {

    /**
     * 加密请求头
     */
    public static final String SOURCE_HEADER = "source";

    /**
     * 客户端类型
     */
    public static final String SOURCE_WEB = "Web";
    public static final String SOURCE_APP = "App";

    /**
     * Redis key前缀
     */
    public static final String RSA_PUBLIC_KEY_PREFIX = "rsa:public:";
    public static final String RSA_PRIVATE_KEY_PREFIX = "rsa:private:";
    public static final String AES_KEY_PREFIX = "aes:key:%s:%d";  // source:userId

    // 密钥过期时间（秒）（30天）
    public static final long KEY_EXPIRE_TIME = 30 * 24 * 60 * 60;

    public static final String RSA_PUBLIC_KEY_NAME = "rsa_public_key";

    public static final String RSA_PRIVATE_KEY_NAME = "rsa_private_key";
}