package com.agri.cms.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * 加密配置
 *
 * @author mfr
 * @since 2024/11/7
 */
@Configuration
public class EncryptConfig {

    // 需要加密的路径
    private static final List<String> ENCRYPT_PATHS = Arrays.asList(
//        "/system/user/profile",
//        "/system/config",
//        "/captcha","/validate","/validateQR","/captchaQR",
//        "/crop/**","/fieldWork/**","/landAddress/**","/landPlot/**"
            "/**"
            // 所有接口都加密
    );

    // 不需要加密的路径(优先级高于ENCRYPT_PATHS)
    private static final List<String> EXCLUDE_PATHS = Arrays.asList(
//            "/render",
//             "/validate*",
//            "/login/**",
//            "/login*",
//            "/logout",
//            "/captchaImage",
//            "/register",
//            // 一些公共接口
//            "/common/**",
//            // 文件上传下载等接口
//            "/profile/**"  ,
//            // 密钥交换接口,请求数据本身就是已经加密过的,不需要再次加密
//            "/system/key/**",
//            "/getInfo",
//            "/getRouters",
//            "/captcha*",
//            "/signOut*",
//            "/screen/**",
//            "/system/user/bind"
            "/**"
    );

    @Bean
    public List<String> encryptPaths() {
        return ENCRYPT_PATHS;
    }

    @Bean
    public List<String> excludePaths() {
        return EXCLUDE_PATHS;
    }

    /**
     * 判断请求路径是否需要加密
     * 规则: 排除路径优先级高于加密路径
     */
    public static boolean isEncryptPath(String path) {
        // 1. 先检查是否在排除列表中(优先级高)
        for (String excludePath : EXCLUDE_PATHS) {
            if (pathMatch(path, excludePath)) {
                // 在排除列表中,不需要加密
                return false;
            }
        }

        // 2. 再检查是否在加密列表中(优先级低)
        for (String encryptPath : ENCRYPT_PATHS) {
            if (pathMatch(path, encryptPath)) {
                // 在加密列表中且不在排除列表中,需要加密
                return true;
            }
        }

        // 默认不加密
        return false;
    }

    /**
     * 路径匹配(支持通配符 **)
     */
    private static boolean pathMatch(String path, String pattern) {
        // 处理 /** 和 /xxx/** 的情况
        if (pattern.endsWith("/**")) {
            String basePattern = pattern.substring(0, pattern.length() - 2);
            return path.startsWith(basePattern);
        }
        // 处理 /xxx* 的情况（单个*通配符）
        if (pattern.endsWith("*") && !pattern.endsWith("/**")) {
            String basePattern = pattern.substring(0, pattern.length() - 1);
            // 确保匹配的是同一层级的路径（不包含额外的/）
            return path.startsWith(basePattern) && !path.substring(basePattern.length()).contains("/");
        }
        // 处理精确匹配
        return path.equals(pattern);
    }
} 
