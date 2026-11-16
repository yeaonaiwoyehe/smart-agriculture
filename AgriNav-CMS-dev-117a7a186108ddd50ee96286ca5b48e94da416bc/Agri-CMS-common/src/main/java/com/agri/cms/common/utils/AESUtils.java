package com.agri.cms.common.utils;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * AES加密工具类
 *
 * @author mfr
 * @since 2024/11/7
 */
public class AESUtils {
    private static final String KEY_ALGORITHM = "AES";
    private static final String DEFAULT_CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int IV_LENGTH = 16; // AES IV长度固定为16字节

    /**
     * 生成AES密钥
     */
    public static String generateKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(KEY_ALGORITHM);
        keyGenerator.init(256, new SecureRandom());
        SecretKey secretKey = keyGenerator.generateKey();
        return Base64.encodeBase64String(secretKey.getEncoded());
    }

    /**
     * 生成随机IV
     */
    private static byte[] generateIV() {
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    /**
     * AES加密
     * 返回格式: Base64(IV + 密文)
     */
    public static String encrypt(String content, String key) throws Exception {
        // 生成随机IV
        byte[] iv = generateIV();

        Cipher cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        SecretKeySpec keySpec = new SecretKeySpec(Base64.decodeBase64(key), KEY_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

        // 加密
        byte[] encrypted = cipher.doFinal(content.getBytes(StandardCharsets.UTF_8));

        // 拼接IV和密文
        byte[] combined = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

        return Base64.encodeBase64URLSafeString(combined);
    }

    /**
     * AES解密
     * 输入格式: Base64(IV + 密文)
     */
    public static String decrypt(String content, String key) throws Exception {
        // Base64解码
        byte[] combined = Base64.decodeBase64(content);
        if (combined.length < IV_LENGTH) {
            throw new IllegalArgumentException("Invalid encrypted data");
        }

        // 提取IV
        byte[] iv = new byte[IV_LENGTH];
        System.arraycopy(combined, 0, iv, 0, IV_LENGTH);

        // 提取密文
        byte[] encrypted = new byte[combined.length - IV_LENGTH];
        System.arraycopy(combined, IV_LENGTH, encrypted, 0, encrypted.length);

        // 解密
        Cipher cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        SecretKeySpec keySpec = new SecretKeySpec(Base64.decodeBase64(key), KEY_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

        byte[] decrypted = cipher.doFinal(encrypted);
        return new String(decrypted, StandardCharsets.UTF_8);
    }
}