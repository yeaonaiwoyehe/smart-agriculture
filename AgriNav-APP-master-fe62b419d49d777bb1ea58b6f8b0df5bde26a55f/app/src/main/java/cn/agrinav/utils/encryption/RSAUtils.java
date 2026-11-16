package cn.agrinav.utils.encryption;

import android.util.Base64;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.Cipher;


/**
 * @author 11230
 */
public class RSAUtils {

    private static final String KEY_ALGORITHM = "RSA";
    private static final String TRANSFORMATION = "RSA/ECB/PKCS1Padding";
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
        return Base64.encodeToString(publicKey.getEncoded(), Base64.DEFAULT);
    }

    /**
     * 获取私钥字符串
     */
    public static String getPrivateKeyStr(KeyPair keyPair) {
        PrivateKey privateKey = keyPair.getPrivate();
        return Base64.encodeToString(privateKey.getEncoded(), Base64.DEFAULT);
    }

    /**
     * 使用公钥加密
     */
    public static String encrypt(String content, String publicKeyStr) throws Exception {
        PublicKey publicKey = getPublicKey(publicKeyStr);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encrypted = cipher.doFinal(content.getBytes());
        return Base64.encodeToString(encrypted, Base64.DEFAULT);
    }

    /**
     * 使用私钥解密
     */
    public static String decrypt(String content, String privateKeyStr) throws Exception {
        PrivateKey privateKey = getPrivateKey(privateKeyStr);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decoded = Base64.decode(content, Base64.DEFAULT);
        byte[] decrypted = cipher.doFinal(decoded);
        return new String(decrypted);
    }

    /**
     * 将Base64编码的公钥字符串转换为PublicKey对象
     */
    private static PublicKey getPublicKey(String publicKeyStr) throws Exception {
        byte[] keyBytes = Base64.decode(publicKeyStr, Base64.DEFAULT);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        return keyFactory.generatePublic(keySpec);
    }

    /**
     * 将Base64编码的私钥字符串转换为PrivateKey对象
     */
    private static PrivateKey getPrivateKey(String privateKeyStr) throws Exception {
        byte[] keyBytes = Base64.decode(privateKeyStr, Base64.DEFAULT);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        return keyFactory.generatePrivate(keySpec);
    }
}