package com.agri.cms.common.utils;

import com.agri.cms.common.config.TencentCloudConfig;
import com.agri.cms.common.constant.Constants;
import com.agri.cms.common.constant.HttpStatus;
import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.common.exception.SmsException;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.sms.v20210111.SmsClient;
import com.tencentcloudapi.sms.v20210111.models.SendSmsRequest;
import com.tencentcloudapi.sms.v20210111.models.SendSmsResponse;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 短信工具类.
 *
 * @author simon
 * @since 2023/05/16
 */
@SuppressWarnings(value = {"unused"})
@Component
public class SmsUtils {

    @Resource
    private TencentCloudConfig config;

    private SmsUtils() {
    }

    /**
     * 发送短信.
     *
     * @return 发送结果
     * @throws TencentCloudSDKException TencentCloudSDKException 异常
     */
    public AjaxResult sendSms(
            String[] code,
            String[] phoneNumber
    ) throws TencentCloudSDKException {
        AjaxResult result = AjaxResult.success();
        Credential cred = new Credential(config.getAccount().getSecretId(),
                config.getAccount().getSecretKey());
        HttpProfile httpProfile = new HttpProfile();
        httpProfile.setReqMethod("POST");
        httpProfile.setConnTimeout(60);
        httpProfile.setEndpoint("sms.tencentcloudapi.com");

        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setSignMethod("HmacSHA256");
        clientProfile.setHttpProfile(httpProfile);
        SmsClient client = new SmsClient(cred, "ap-beijing", clientProfile);
        SendSmsRequest req = new SendSmsRequest();

        req.setSmsSdkAppId(config.getSms().getSdkAppId());
        req.setSignName(config.getSms().getSignName());
        req.setTemplateId(config.getSms().getTemplateId());
        req.setTemplateParamSet(code);
        req.setPhoneNumberSet(phoneNumber);

        SendSmsResponse sendSmsResponse = client.SendSms(req);
        sendSmsResponse.getSendStatusSet()[0].getCode();
        if (Constants.CODE_OK.equals(sendSmsResponse.getSendStatusSet()[0].getCode())) {
            result.put(Constants.CODE, HttpStatus.SUCCESS);
            result.put(Constants.MSG, "发送成功");
            return result;
        } else {
            throw Constants.smsExceptionMap.containsKey(sendSmsResponse.getSendStatusSet()[0].getCode()) ?
                    Constants.smsExceptionMap.get(sendSmsResponse.getSendStatusSet()[0].getCode()) :
                    new SmsException("短信服务出现异常，请您稍后再发");
        }
    }
}
