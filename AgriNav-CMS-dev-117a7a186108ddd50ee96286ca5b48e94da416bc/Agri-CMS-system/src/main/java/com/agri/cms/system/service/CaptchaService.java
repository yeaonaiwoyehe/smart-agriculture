package com.agri.cms.system.service;

import com.agri.cms.common.core.domain.AjaxResult;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

/**
 * @author simon
 * @since 2024/9/21
 */
public interface CaptchaService {

    /**
     * 获取验证码
     *
     * @param phoneNumber
     * @return Map<String, Object>
     * @throws Exception e
     * @author simon
     * @since 2024/9/21
     */
    AjaxResult getCode(String phoneNumber) throws Exception;

    /**
     * 校验验证码
     *
     * @param phoneNumber
     * @param captcha
     * @throws Exception
     * @author simon
     * @since 2024/9/21
     */
    void validate(String phoneNumber, String captcha) throws Exception;

    AjaxResult validate(String uuid);

    /**
     * 获取验证码图片
     *
     * @return Map<String, Object>
     * @author simon
     * @since 2024/10/21
     */

    AjaxResult getImage(HttpServletResponse response) throws Exception;

}
