package com.agri.cms.system.service.impl;

import com.agri.cms.common.annotation.Log;
import com.agri.cms.common.constant.CacheConstants;
import com.agri.cms.common.constant.Constants;
import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.common.core.redis.RedisCache;
import com.agri.cms.common.enums.BusinessType;
import com.agri.cms.common.exception.user.CaptchaException;
import com.agri.cms.common.exception.user.CaptchaExpireException;
import com.agri.cms.common.exception.user.CaptchaRepeatException;
import com.agri.cms.common.service.QRCodeService;
import com.agri.cms.common.utils.SmsUtils;
import com.agri.cms.common.utils.StringUtils;
import com.agri.cms.common.utils.sign.Base64;
import com.agri.cms.common.utils.uuid.IdUtils;
import com.agri.cms.system.service.CaptchaService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FastByteArrayOutputStream;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * @author simon
 * @since 2024/9/21
 */
@Service
public class CaptchaServiceImpl implements CaptchaService {

    @Resource
    private SmsUtils smsUtils;

    @Autowired
    private RedisCache redisCache;


    @Resource
    QRCodeService qrCodeService;


    private static final Logger log = LoggerFactory.getLogger(CaptchaServiceImpl.class);

    /**
     * 短信演示模式开关：开启后不调用腾讯云真实短信，验证码直接写日志并在接口返回
     */
    @Value("${sms.mock.enabled:false}")
    private boolean mockEnabled;

    @Override
//    @Log(title = "发送验证码", businessType = BusinessType.OTHER)
    public AjaxResult getCode(String phoneNumber) throws Exception {
        //随机生成一个六位随机数
        Integer random = ThreadLocalRandom.current().nextInt(100000, 999999);
        String verifyKey = CacheConstants.CAPTCHA_CODE_KEY + phoneNumber;
        if (redisCache.hasKey(verifyKey)){
            throw new CaptchaRepeatException();
        }
        if (mockEnabled) {
            // 演示模式：不真实发送短信，直接把验证码存入 Redis，并在接口/日志中返回，便于本机演示登录
            redisCache.setCacheObject(verifyKey, random, Constants.CAPTCHA_EXPIRATION, TimeUnit.MINUTES);
            log.warn("【演示模式】手机号 {} 的验证码为 {}（未真实发送短信）", phoneNumber, random);
            AjaxResult mockResult = AjaxResult.success("演示模式，验证码已生成（未真实发送短信）");
            mockResult.put("mockCode", random);
            return mockResult;
        }
        try {
            AjaxResult result = smsUtils.sendSms(new String[]{random.toString()}, new String[]{phoneNumber});
            if (Constants.CODE_SUCCESS.equals(result.get(Constants.CODE))) {
                redisCache.setCacheObject(verifyKey, random, Constants.CAPTCHA_EXPIRATION, TimeUnit.MINUTES);
            }
            return result;
        } catch (Exception e) {
            log.error("向{}发送验证码失败,原因为{}", phoneNumber, e.getMessage());
            throw e;
        }
    }

    @Override
    public void validate(String phoneNumber, String captcha) throws CaptchaExpireException, CaptchaException {
        String verifyKey = CacheConstants.CAPTCHA_CODE_KEY + phoneNumber;
        Integer captchaCode = redisCache.getCacheObject(verifyKey);
        if (captchaCode == null) {
            throw new CaptchaExpireException();
        }
        if (!captcha.equalsIgnoreCase(String.valueOf(captchaCode))) {
            throw new CaptchaException();
        }
        if(!phoneNumber.equals(Constants.TEST_ACCOUNT)){
            redisCache.deleteObject(verifyKey);
        }
    }

    @Override
    public AjaxResult validate(String uuid) {
        String token = redisCache.getCacheObject(CacheConstants.QR_CODE_KEY + uuid);
        if (redisCache.getExpire(CacheConstants.QR_CODE_KEY + uuid) < 0) {
            return AjaxResult.error("二维码已失效");
        }
        if (StringUtils.isEmpty(token)) {
            return new AjaxResult(203,"尚未登录");
            }
        redisCache.deleteObject(CacheConstants.QR_CODE_KEY + uuid);
        return AjaxResult.success().put(Constants.TOKEN,token);
    }

    @Override
    public AjaxResult getImage(HttpServletResponse response) throws Exception {
        AjaxResult result = AjaxResult.success();
        String uuid = IdUtils.simpleUUID();
        String verifyKey = CacheConstants.QR_CODE_KEY + uuid;
        redisCache.setCacheObject(verifyKey, null, Constants.CAPTCHA_EXPIRATION, TimeUnit.MINUTES);
        try (FastByteArrayOutputStream os = new FastByteArrayOutputStream()) {
            qrCodeService.generateQRCode(uuid, os);
            result.put(Constants.QRCODE, Base64.encode(os.toByteArray()));
        }
        result.put(Constants.UUID, uuid);
        return result;
    }
}
