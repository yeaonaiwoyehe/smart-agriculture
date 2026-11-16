package com.agri.cms.system.service;

/**
 * 加密服务接口
 *
 * @author mfr
 * @since 2024/11/7
 */
public interface IEncryptService {
    /**
     * 生成RSA密钥对并返回公钥
     *
     * @return RSA公钥
     */
    String generateRSAKey();

    /**
     * 获取RSA公钥
     *
     * @return RSA公钥
     */
    String getRSAPublicKey();

    /**
     * 获取RSA私钥
     *
     * @return RSA私钥
     */
    String getRSAPrivateKey();

    /**
     * RSA加密
     *
     * @param content   待加密内容
     * @param publicKey RSA公钥
     * @return 加密后的内容
     */
    String rsaEncrypt(String content, String publicKey) throws Exception;

    /**
     * RSA解密
     *
     * @param encryptedContent 加密的内容
     * @return 解密后的内容
     */
    String rsaDecrypt(String encryptedContent) throws Exception;

    /**
     * 保存用户的AES密钥(RSA加密后的)
     *
     * @param source          来源
     * @param userId          用户ID
     * @param encryptedAesKey RSA加密后的AES密钥
     * @return 是否保存成功
     */
    boolean saveAESKey(Long userId, String encryptedAesKey, String source);

    /**
     * 获取用户的AES密钥
     *
     * @param source 来源
     * @param userId 用户ID
     * @return AES密钥
     */
    String getAESKey(Long userId, String source);

    /**
     * AES加密
     *
     * @param source  来源
     * @param content 待加密内容
     * @param userId  用户ID
     * @return 加密后的内容
     */
    String aesEncrypt(String content, Long userId, String source) throws Exception;

    /**
     * AES解密
     *
     * @param source           来源
     * @param encryptedContent 加密的内容
     * @param userId           用户ID
     * @return 解密后的内容
     */
    String aesDecrypt(String encryptedContent, Long userId, String source) throws Exception;
} 