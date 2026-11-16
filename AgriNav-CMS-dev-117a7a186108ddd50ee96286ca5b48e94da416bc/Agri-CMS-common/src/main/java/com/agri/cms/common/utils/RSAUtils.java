package com.agri.cms.common.utils;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * RSA加密解密工具类
 *
 * @author mfr
 * @since 2024/11/7
 */
public class RSAUtils {
    private static final String KEY_ALGORITHM = "RSA";
    private static final int KEY_SIZE = 2048;

    /**
     * 生成RSA密钥对
     */
    public static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance(KEY_ALGORITHM);
        generator.initialize(KEY_SIZE, new SecureRandom());
        return generator.generateKeyPair();
    }

    /**
     * 获取公钥字符串
     */
    public static String getPublicKeyStr(KeyPair keyPair) {
        PublicKey publicKey = keyPair.getPublic();
        return Base64.encodeBase64String(publicKey.getEncoded());
    }

    /**
     * 获取私钥字符串
     */
    public static String getPrivateKeyStr(KeyPair keyPair) {
        PrivateKey privateKey = keyPair.getPrivate();
        return Base64.encodeBase64String(privateKey.getEncoded());
    }

    /**
     * 使用公钥加密
     */
    public static String encrypt(String content, String publicKeyStr) throws Exception {
        PublicKey publicKey = getPublicKey(publicKeyStr);
        Cipher cipher = Cipher.getInstance(KEY_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encrypted = cipher.doFinal(content.getBytes());
        return Base64.encodeBase64String(encrypted);
    }

    /**
     * 使用私钥解密
     */
    public static String decrypt(String content, String privateKeyStr) throws Exception {
        System.out.println("私钥解密前：" + content);
        PrivateKey privateKey = getPrivateKey(privateKeyStr);
        Cipher cipher = Cipher.getInstance(KEY_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decoded = Base64.decodeBase64(content);
        byte[] decrypted = cipher.doFinal(decoded);
        return new String(decrypted);
    }

    /**
     * 将Base64编码的公钥字符串转换为PublicKey对象
     */
    private static PublicKey getPublicKey(String publicKeyStr) throws Exception {
        byte[] keyBytes = Base64.decodeBase64(publicKeyStr);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        return keyFactory.generatePublic(keySpec);
    }

    /**
     * 将Base64编码的私钥字符串转换为PrivateKey对象
     */
    private static PrivateKey getPrivateKey(String privateKeyStr) throws Exception {
        byte[] keyBytes = Base64.decodeBase64(privateKeyStr);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        return keyFactory.generatePrivate(keySpec);
    }
}