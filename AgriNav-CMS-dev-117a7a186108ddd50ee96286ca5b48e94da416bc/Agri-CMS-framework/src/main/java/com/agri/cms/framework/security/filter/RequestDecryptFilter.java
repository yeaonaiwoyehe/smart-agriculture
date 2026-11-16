package com.agri.cms.framework.security.filter;

import com.agri.cms.common.config.EncryptConfig;
import com.agri.cms.common.constant.EncryptConstants;
import com.agri.cms.common.constant.OrderConstants;
import com.agri.cms.common.utils.SecurityUtils;
import com.alibaba.fastjson2.JSON;
import org.springframework.core.annotation.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.apache.commons.io.IOUtils;

import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.support.StandardMultipartHttpServletRequest;

import java.util.Map;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.agri.cms.system.service.IEncryptService;

/**
 * 请求解密过滤器
 *
 * @author mfr
 * @since 2024/11/6
 */
@Component
//@Order(OrderConstants.ORDER_STATUS_5)
//@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestDecryptFilter extends OncePerRequestFilter {

    @Autowired
    private IEncryptService encryptService;

    private static final Logger logger = LoggerFactory.getLogger(RequestDecryptFilter.class);


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // 1. 判断是否需要解密
        String path = request.getRequestURI();
        if (!EncryptConfig.isEncryptPath(path)) {
            chain.doFilter(request, response);
            return;
        }
        System.out.println("path: " + path);
        System.out.println("需要加密");

        try {
            // 2. 获取当前用户ID和来源source
            Long userId = SecurityUtils.getUserId();
            String source = request.getHeader(EncryptConstants.SOURCE_HEADER);

            // 3. 根据请求方法处理解密
            HttpServletRequest processedRequest = request;
            switch (request.getMethod()) {
                case "GET":
                    processedRequest = handleGetRequest(request, userId, source);
                    break;
                case "POST":
                    processedRequest = handlePostRequest(request, userId, source);
                    break;
                case "PUT":
                    processedRequest = handlePutRequest(request, userId, source);
                    break;
                case "DELETE":
                    processedRequest = handleDeleteRequest(request, userId, source);
                    break;
            }

            // 4. 继续过滤器链
            chain.doFilter(processedRequest, response);

        } catch (Exception e) {
            logger.error("请求解密失败", e);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Request decryption failed: " + e.getMessage());
        }
    }

    /**
     * 处理GET请求解密
     */
    private HttpServletRequest handleGetRequest(HttpServletRequest request, Long userId, String source) throws Exception {
        Map<String, String[]> parameterMap = request.getParameterMap();
        if (parameterMap.isEmpty()) {
            return request;
        }

        // 创建新的参数Map
        Map<String, String[]> decryptedParams = new HashMap<>();

        // 解密所有参数
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            String[] values = entry.getValue();
            if (values != null) {
                for (String value : values) {
                    try {
                        // 假设解密后的值是一个JSON字符串
                        String decryptedValue = encryptService.aesDecrypt(value, userId, source);
                        // 将解密后的JSON字符串解析为Map
                        Map<String, Object> jsonMap = JSON.parseObject(decryptedValue, Map.class);
                        // 将解析后的键值对放入decryptedParams
                        for (Map.Entry<String, Object> jsonEntry : jsonMap.entrySet()) {
                            decryptedParams.put(jsonEntry.getKey(), new String[]{jsonEntry.getValue().toString()});
                        }
                    } catch (Exception e) {
                        logger.error("解密GET参数[{}]失败", entry.getKey(), e);
                        decryptedParams.put(entry.getKey(), values); // 保留原始值以防解密失败
                    }
                }
            }
        }

        // 创建包装请求
        return new HttpServletRequestWrapper(request) {
            @Override
            public String getParameter(String name) {
                String[] values = decryptedParams.get(name);
                return values != null && values.length > 0 ? values[0] : null;
            }

            @Override
            public Map<String, String[]> getParameterMap() {
                return decryptedParams;
            }

            @Override
            public String[] getParameterValues(String name) {
                return decryptedParams.get(name);
            }
        };
    }

    /**
     * 处理POST请求解密
     */
    private HttpServletRequest handlePostRequest(HttpServletRequest request, Long userId, String source) throws Exception {
        String contentType = request.getContentType();
        
        // 1. 处理 JSON 请求体
        if (contentType != null && contentType.contains(MediaType.APPLICATION_JSON_VALUE)) {
            HttpServletRequest jsonRequest = handleJsonRequest(request, userId, source);
            
            // 2. 如果有 URL 参数，创建包装请求同时处理 URL 参数和请求体
            if (!request.getParameterMap().isEmpty()) {
                final String decryptedBody = IOUtils.toString(jsonRequest.getInputStream(), StandardCharsets.UTF_8);
                
                return new RequestWrapper(request, decryptedBody) {
                    @Override
                    public String getParameter(String name) {
                        String value = super.getParameter(name);
                        if (value != null) {
                            // 对于分页参数等数字类型，直接返回原值
                            if (name.equals("pageNum") || name.equals("pageSize") || 
                                name.equals("id") || name.equals("status") ||
                                name.equals("current") || name.equals("size")) {
                                return value;
                            }
                            try {
                                // 尝试解析参数值是否为加密数据
                                if (value.matches("^[A-Za-z0-9+/=]+$") && value.length() % 4 == 0) {
                                    return encryptService.aesDecrypt(value, userId, source);
                                }
                            } catch (Exception e) {
                                logger.debug("参数[{}]解密失败，使用原始值", name);
                            }
                        }
                        return value;
                    }

                    @Override
                    public Map<String, String[]> getParameterMap() {
                        Map<String, String[]> originalMap = super.getParameterMap();
                        Map<String, String[]> decryptedMap = new HashMap<>();
                        
                        for (Map.Entry<String, String[]> entry : originalMap.entrySet()) {
                            String name = entry.getKey();
                            String[] values = entry.getValue();
                            
                            // 对于分页参数等数字类型，直接使用原值
                            if (name.equals("pageNum") || name.equals("pageSize") || 
                                name.equals("id") || name.equals("status")) {
                                decryptedMap.put(name, values);
                                continue;
                            }
                            
                            if (values != null) {
                                String[] decryptedValues = new String[values.length];
                                for (int i = 0; i < values.length; i++) {
                                    try {
                                        if (values[i].matches("^[A-Za-z0-9+/=]+$") && values[i].length() % 4 == 0) {
                                            decryptedValues[i] = encryptService.aesDecrypt(values[i], userId, source);
                                        } else {
                                            decryptedValues[i] = values[i];
                                        }
                                    } catch (Exception e) {
                                        decryptedValues[i] = values[i];
                                        logger.debug("参数数组[{}]解密失败，使用原始值", entry.getKey());
                                    }
                                }
                                decryptedMap.put(name, decryptedValues);
                            }
                        }
                        return decryptedMap;
                    }

                    @Override
                    public String[] getParameterValues(String name) {
                        String[] values = super.getParameterValues(name);
                        // 对于分页参数等数字类型，直接返回原值
                        if (name.equals("pageNum") || name.equals("pageSize") || 
                            name.equals("id") || name.equals("status")) {
                            return values;
                        }
                        
                        if (values != null) {
                            String[] decryptedValues = new String[values.length];
                            for (int i = 0; i < values.length; i++) {
                                try {
                                    if (values[i].matches("^[A-Za-z0-9+/=]+$") && values[i].length() % 4 == 0) {
                                        decryptedValues[i] = encryptService.aesDecrypt(values[i], userId, source);
                                    } else {
                                        decryptedValues[i] = values[i];
                                    }
                                } catch (Exception e) {
                                    decryptedValues[i] = values[i];
                                    logger.debug("参数数组[{}]解密失败，使用原始值", name);
                                }
                            }
                            return decryptedValues;
                        }
                        return values;
                    }
                };
            }
            return jsonRequest;
        } 
        // 3. 处理 FormData 请求
        else if (contentType != null && contentType.contains(MediaType.MULTIPART_FORM_DATA_VALUE)) {
            return handleMultipartRequest(request, userId, source);
        }
        
        return request;
    }

    /**
     * 处理PUT请求解密
     */
    private HttpServletRequest handlePutRequest(HttpServletRequest request, Long userId, String source) throws Exception {
        return handlePostRequest(request, userId, source);
    }

    /**
     * 处理DELETE请求解密
     */
    private HttpServletRequest handleDeleteRequest(HttpServletRequest request, Long userId, String source) throws Exception {
        return handleGetRequest(request, userId, source);
    }

    /**
     * 处理JSON请求体解密
     */
    private HttpServletRequest handleJsonRequest(HttpServletRequest request, Long userId, String source) throws Exception {
        String encryptedBody = IOUtils.toString(request.getInputStream(), StandardCharsets.UTF_8);
        //输出当前请求的路径
        System.out.println("path: " + request.getRequestURI());
        String decryptedBody = encryptService.aesDecrypt(encryptedBody, userId, source);
        return new RequestWrapper(request, decryptedBody);
    }

    /**
     * 处理multipart/form-data请求解密
     */
    private HttpServletRequest handleMultipartRequest(HttpServletRequest request, Long userId, String source) throws Exception {
        MultipartHttpServletRequest multipartRequest;
        if (request instanceof MultipartHttpServletRequest) {
            multipartRequest = (MultipartHttpServletRequest) request;
        } else {
            multipartRequest = new StandardMultipartHttpServletRequest(request);
        }

        return new StandardMultipartHttpServletRequest(request) {
            @Override
            public Map<String, String[]> getParameterMap() {
                Map<String, String[]> parameterMap = new HashMap<>(super.getParameterMap());
                try {
                    for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
                        String[] values = entry.getValue();
                        if (values != null) {
                            for (int i = 0; i < values.length; i++) {
                                values[i] = encryptService.aesDecrypt(values[i], userId, source);
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error("解密请求参数失败", e);
                }
                return parameterMap;
            }

            @Override
            public String getParameter(String name) {
                String value = super.getParameter(name);
                if (value != null) {
                    try {
                        return encryptService.aesDecrypt(value, userId, source);
                    } catch (Exception e) {
                        logger.error("解密参数[{}]失败", name, e);
                    }
                }
                return value;
            }

            @Override
            public String[] getParameterValues(String name) {
                String[] values = super.getParameterValues(name);
                if (values != null) {
                    for (int i = 0; i < values.length; i++) {
                        try {
                            values[i] = encryptService.aesDecrypt(values[i], userId, source);
                        } catch (Exception e) {
                            logger.error("解密参数数组[{}]失败", name, e);
                        }
                    }
                }
                return values;
            }
        };
    }
}