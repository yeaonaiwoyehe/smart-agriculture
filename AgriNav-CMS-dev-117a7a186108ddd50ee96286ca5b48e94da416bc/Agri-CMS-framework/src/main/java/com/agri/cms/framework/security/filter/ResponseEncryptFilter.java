package com.agri.cms.framework.security.filter;


import com.agri.cms.common.config.EncryptConfig;
import com.agri.cms.common.constant.EncryptConstants;
import com.agri.cms.common.constant.OrderConstants;
import com.agri.cms.common.utils.SecurityUtils;
import com.agri.cms.system.service.IEncryptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 响应加密过滤器
 *
 * @author mfr
 * @since 2024/11/6
 */
@Component
//@Order(OrderConstants.ORDER_STATUS_1)
public class ResponseEncryptFilter extends OncePerRequestFilter {

    @Autowired
    private IEncryptService encryptService;

    private static final Logger logger = LoggerFactory.getLogger(ResponseEncryptFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // 1. 判断是否需要加密
        String path = request.getRequestURI();
        if (!EncryptConfig.isEncryptPath(path)) {
            chain.doFilter(request, response);
            return;
        }
        System.out.println("path: " + path);
        System.out.println("需要加密");
        try {
            // 2. 获取当前用户ID
            Long userId = SecurityUtils.getUserId();

            // 3. 创建响应包装器
            ResponseWrapper responseWrapper = new ResponseWrapper(response);

            // 4. 继续过滤器链
            chain.doFilter(request, responseWrapper);

            // 5. 加密响应数据
            String source = request.getHeader(EncryptConstants.SOURCE_HEADER);
            byte[] responseData = responseWrapper.getResponseData();
            if (responseData.length > 0) {
                String responseStr = new String(responseData, StandardCharsets.UTF_8);
                String encryptedResponse = encryptService.aesEncrypt(responseStr, userId, source);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.getOutputStream().write(encryptedResponse.getBytes(StandardCharsets.UTF_8));
            }

        } catch (Exception e) {
            logger.error("响应加密失败", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Response encryption failed: " + e.getMessage());
        }
    }
}