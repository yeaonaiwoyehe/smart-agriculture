package com.agri.cms.web.controller.common;

import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.system.service.CaptchaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 验证码操作处理
 *
 * @author ruoyi
 */
@Tag(name = "验证码操作处理")
@RestController
public class CaptchaController {


    @Resource
    private CaptchaService captchaService;


    /**
     * @param phoneNumber 手机号
     * @return ajaxResult 发送是否成功
     * @author simon
     * @since 2024年9月7日12:11:59
     */

    @Operation(summary = "给指定手机号发送验证码，有效时长3min")
    @GetMapping("/captcha")
    public AjaxResult getCode(@RequestParam("phoneNumber") String phoneNumber) throws Exception {
        return captchaService.getCode(phoneNumber);
    }

    @Operation(summary = "验证验证码是否正确")
    @GetMapping("/validate")
    public AjaxResult validate(
            @RequestParam("phoneNumber") String phoneNumber,
            @RequestParam("captcha") String captcha) throws Exception {
        captchaService.validate(phoneNumber, captcha);
        return AjaxResult.success();
    }

    @GetMapping("/validateQR")
    public AjaxResult validate(@RequestParam("uuid") String uuid) {
        return captchaService.validate(uuid);
    }

    /**
     * 生成验证码
     */
    @GetMapping("/captchaQR")
    public AjaxResult getCode(HttpServletResponse response) throws Exception {
        AjaxResult ajax = AjaxResult.success();
        ajax.putAll(captchaService.getImage(response));
        return ajax;
    }

}
