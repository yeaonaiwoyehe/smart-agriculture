package com.agri.cms.system.service.impl;


import com.agri.cms.common.constant.EncryptConstants;
import com.agri.cms.common.utils.AESUtils;
import com.agri.cms.common.utils.RSAUtils;
import com.agri.cms.system.service.IEncryptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.util.concurrent.TimeUnit;

/**
 * 密钥实现类
 *
 * @author mfr
 * @since 2024/11/7
 */
@Service
public class EncryptServiceImpl implements IEncryptService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public String generateRSAKey() {
        try {
            // 生成新的RSA密钥对
            KeyPair keyPair = RSAUtils.generateKeyPair();
            String publicKey = RSAUtils.getPublicKeyStr(keyPair);
            String privateKey = RSAUtils.getPrivateKeyStr(keyPair);

            // 存储密钥对
            redisTemplate.opsForValue().set(EncryptConstants.RSA_PUBLIC_KEY_PREFIX, publicKey, EncryptConstants.KEY_EXPIRE_TIME, TimeUnit.SECONDS);
            redisTemplate.opsForValue().set(EncryptConstants.RSA_PRIVATE_KEY_PREFIX, privateKey, EncryptConstants.KEY_EXPIRE_TIME, TimeUnit.SECONDS);

            return publicKey;
        } catch (Exception e) {
            throw new RuntimeException("生成RSA密钥对失败", e);
        }
    }

    @Override
    public String getRSAPublicKey() {
        String publicKey = redisTemplate.opsForValue().get(EncryptConstants.RSA_PUBLIC_KEY_PREFIX);
        if (publicKey == null) {
            publicKey = generateRSAKey();
        }
        return publicKey;
    }

    @Override
    public String getRSAPrivateKey() {
        return redisTemplate.opsForValue().get(EncryptConstants.RSA_PRIVATE_KEY_PREFIX);
    }

    @Override
    public String rsaEncrypt(String content, String publicKey) throws Exception {
        return RSAUtils.encrypt(content, publicKey);
    }

    @Override
    public String rsaDecrypt(String encryptedContent) throws Exception {
        String privateKey = getRSAPrivateKey();
        if (privateKey == null) {
            throw new RuntimeException("RSA私钥不存在");
        }
        return RSAUtils.decrypt(encryptedContent, privateKey);
    }


    @Override
    public boolean saveAESKey(Long userId, String encryptedAesKey, String source) {
        try {
            // 获取RSA私钥并解密AES密钥
            String aesKey = rsaDecrypt(encryptedAesKey);

            // 生成key
            String redisKey = String.format(EncryptConstants.AES_KEY_PREFIX, source, userId);

            // 存储AES密钥
            redisTemplate.opsForValue().set(redisKey, aesKey, EncryptConstants.KEY_EXPIRE_TIME, TimeUnit.SECONDS);

//            // 删除RSA密钥对(可选,看具体需求)
//            redisTemplate.delete(RSA_PRIVATE_KEY_PREFIX + userId);
//            redisTemplate.delete(RSA_PUBLIC_KEY_PREFIX + userId);

            return true;
        } catch (Exception e) {
            throw new RuntimeException("保存AES密钥失败", e);
        }
    }

    @Override
    public String getAESKey(Long userId, String source) {
        String redisKey = String.format(EncryptConstants.AES_KEY_PREFIX, source, userId);
        String aesKey = redisTemplate.opsForValue().get(redisKey);

        return aesKey;
    }

    @Override
    public String aesEncrypt(String content, Long userId, String source) throws Exception {
        String aesKey = getAESKey(userId, source);
        if (aesKey == null) {
            throw new RuntimeException("AES密钥不存在");
        }
        return AESUtils.encrypt(content, aesKey);
    }

    @Override
    public String aesDecrypt(String encryptedContent, Long userId, String source) throws Exception {
        String aesKey = getAESKey(userId, source);
        if (aesKey == null) {
            throw new RuntimeException("AES密钥不存在");
        }
        return AESUtils.decrypt(encryptedContent, aesKey);
    }
}