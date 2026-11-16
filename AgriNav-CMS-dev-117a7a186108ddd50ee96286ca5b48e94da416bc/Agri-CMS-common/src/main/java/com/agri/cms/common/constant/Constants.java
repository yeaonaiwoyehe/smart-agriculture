package com.agri.cms.common.constant;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.agri.cms.common.exception.SmsException;
import io.jsonwebtoken.Claims;
import org.springframework.security.web.PortResolverImpl;

/**
 * 通用常量信息
 *
 * @author ruoyi
 */
public class Constants {
    /**
     * UTF-8 字符集
     */
    public static final String UTF8 = "UTF-8";

    /**
     * GBK 字符集
     */
    public static final String GBK = "GBK";

    /**
     * 系统语言
     */
    public static final Locale DEFAULT_LOCALE = Locale.SIMPLIFIED_CHINESE;

    /**
     * www主域
     */
    public static final String WWW = "www.";

    /**
     * http请求
     */
    public static final String HTTP = "http://";

    /**
     * https请求
     */
    public static final String HTTPS = "https://";

    /**
     * 通用成功标识
     */
    public static final String SUCCESS = "0";

    /**
     * 通用失败标识
     */
    public static final String FAIL = "1";

    /**
     * 登录成功
     */
    public static final String LOGIN_SUCCESS = "Success";

    /**
     * 注销
     */
    public static final String LOGOUT = "Logout";

    /**
     * 注册
     */
    public static final String REGISTER = "Register";

    /**
     * 登录失败
     */
    public static final String LOGIN_FAIL = "Error";

    /**
     * 所有权限标识
     */
    public static final String ALL_PERMISSION = "*:*:*";

    /**
     * 管理员角色权限标识
     */
    public static final String SUPER_ADMIN = "admin";

    /**
     * 角色权限分隔符
     */
    public static final String ROLE_DELIMETER = ",";

    /**
     * 权限标识分隔符
     */
    public static final String PERMISSION_DELIMETER = ",";

    /**
     * 验证码有效期（分钟）
     */
    public static final Integer CAPTCHA_EXPIRATION = 3;

    /**
     * 令牌
     */
    public static final String TOKEN = "token";

    /**
     * 令牌前缀
     */
    public static final String TOKEN_PREFIX = "Bearer ";

    /**
     * 令牌前缀
     */
    public static final String LOGIN_USER_KEY = "login_user_key";

    /**
     * 用户ID
     */
    public static final String JWT_USERID = "userid";

    /**
     * 用户名称
     */
    public static final String JWT_USERNAME = Claims.SUBJECT;

    public static final String USERNAME = "智航";

    /**
     * 用户名称截取
     */
    public static final Integer USERNAME_INTERCEPTION = 4;
    /**
     * 密码截取
     */
    public static final Integer PASSWORD_INTERCEPTION = 8;

    // 删除标记

    public static final Integer DELETED = 1;
    public static final Integer CHECKED = 1;
    public static final Integer NOT_CHECKED = 0;

    public static final Integer VISIBLE = 0;
    public static final Integer INVISIBLE = 1;

    /**
     * 未删除
     */
    public static final Integer NOT_DELETED = 0;
    /**
     * 用户头像
     */
    public static final String JWT_AVATAR = "avatar";

    /**
     * 创建时间
     */
    public static final String JWT_CREATED = "created";

    /**
     * 用户权限
     */
    public static final String JWT_AUTHORITIES = "authorities";

    /**
     * 资源映射路径 前缀
     */
    public static final String RESOURCE_PREFIX = "/profile";

    /**
     * RMI 远程方法调用
     */
    public static final String LOOKUP_RMI = "rmi:";

    /**
     * LDAP 远程方法调用
     */
    public static final String LOOKUP_LDAP = "ldap:";

    /**
     * LDAPS 远程方法调用
     */
    public static final String LOOKUP_LDAPS = "ldaps:";

    /**
     * 自动识别json对象白名单配置（仅允许解析的包名，范围越小越安全）
     */
    public static final String[] JSON_WHITELIST_STR = {"org.springframework", "com.agri.cms"};

    /**
     * 定时任务白名单配置（仅允许访问的包名，如其他需要可以自行添加）
     */
    public static final String[] JOB_WHITELIST_STR = {"com.agri.cms.quartz.task"};

    /**
     * 定时任务违规的字符
     */
    public static final String[] JOB_ERROR_STR = {"java.net.URL", "javax.naming.InitialContext", "org.yaml.snakeyaml",
            "org.springframework", "org.apache", "com.agri.cms.common.utils.file", "com.agri.cms.common.config", "com.agri.cms.generator"};
    public static final String MSG_SUCCESS = "OK";
    public static final String MSG = "msg";

    /**
     * Postgres数据库表示是否可用的数据
     */
    public static final Boolean IS_AVAILABLE = Boolean.TRUE;
    public static final Boolean IS_DELETED = Boolean.FALSE;

    public static final String IS_STILL_AVAILABLE = "is_deleted";


    /**
     * 田块地址详细信息所需常量
     */

    public static final String AREA_LEVEL = "level";
    public static final String SENIOR_CODE = "pcode";
    public static final String CHINA = "中国";
    public static final int PROVINCE = 1;
    public static final int CITY = 2;
    public static final int DISTRICT = 3;
    public static final int VILLAGE = 4;
    public static final int HAMLET = 5;

    /**
     * 页面查询
     */
    public static final int PAGE_SIZE = 6;
    public static final int PAGE_NUM = 1;
    public static final int PAGE_SIZE_DEFAULT = 10;

    /**
     * 大屏相关常量
     */
    //redis缓存key
    public static final String SCREEN_REDIS_KEY = "screen:statistics:list";

    //刷新时间
    public static final int SCREEN_REFRESH_TIME = 24;

    //逻辑删除
    public static final int AGRICULTURAL_MACHINERY_DELETE_STATUS = 2;

    public static final int FARM_WORK_DELETE_STATUS_STATEMENT = 2;

    public static final String AGRICULTURAL_MACHINERY_FINISHED_STATUS = "1";
    public static final String AGRICULTURAL_MACHINERY_UNFINISHED_STATUS = "0";

    public static final String AGRICULTURAL_MACHINERY_FINISHED_STATEMENT = "已完成";

    public static final String AGRICULTURAL_MACHINERY_UNFINISHED_STATEMENT = "等待完成";
    public static final String HUBEI_CODE = "420000000000";
    public static final String XINJIANG_CODE = "650000000000";
    public static final String HAINAN_CODE = "460000000000";

    public static final String SPECIAL_DISCTRICT = "县级行政区划";

    public static final String EVERY_MONTH_TRAJECTORY_COUNT = "every_month_trajectory_count:";

    public static final String LANDPLOTS_TOTAL_AREA = "land_plots_area:";

    public static final String EVERY_MONTH_LANDPLOT_COUNT = "every_month_land_plot_count:";


    /**
     * 田块相关常量
     */
    public static final String LAND_PLOT_GEO_KEY = "landPlots";
    public static final String CODE = "code";
    public static final Integer CODE_SUCCESS = 200;
    public static final int AVAILABLE = 0;
    public static final int NO_LONGER_AVAILABLE = 1;
    public static final String MATH = "math";
    public static final String CHAR = "char";
    public static final String CODE_OK = "Ok";
    public static final String USER_DETAILS = "userDetails";
    public static final String TEST_ACCOUNT = "18888888888";
    public static final String FEEDBACK_LABEL = "sys_feedback_type";

    public static final String DELIMITER = "¡";
    public static final Integer WGS84_SRID = 4326;
    public static final String QRCODE = "qrcode";
    public static final String UUID = "uuid";
    public static final Integer NOT_READ = 0;
    public static final Object READ = 1;
    public static final String DEBEZIUM_TOPIC_MAPPING = "debezium_topic";
    public static final Long DAY_HOUR = 24L;


    public static Map<String, SmsException> smsExceptionMap = Map.ofEntries(
            Map.entry("Ok", new SmsException("sms.send.success", null)),
            Map.entry("ActionOffline", new SmsException("sms.send.actionOffline", null)),
            Map.entry("AuthFailure.InvalidAuthorization", new SmsException("sms.send.invalidAuthorization", null)),
            Map.entry("AuthFailure.InvalidSecretId", new SmsException("sms.send.invalidSecretId", null)),
            Map.entry("AuthFailure.MFAFailure", new SmsException("sms.send.mfaFailure", null)),
            Map.entry("AuthFailure.SecretIdNotFound", new SmsException("sms.send.secretIdNotFound", null)),
            Map.entry("AuthFailure.SignatureExpire", new SmsException("sms.send.signatureExpire", null)),
            Map.entry("AuthFailure.SignatureFailure", new SmsException("sms.send.signatureFailure", null)),
            Map.entry("AuthFailure.TokenFailure", new SmsException("sms.send.tokenFailure", null)),
            Map.entry("AuthFailure.UnauthorizedOperation", new SmsException("sms.send.unauthorizedOperation", null)),
            Map.entry("DryRunOperation", new SmsException("sms.send.dryRunOperation", null)),
            Map.entry("FailedOperation", new SmsException("sms.send.failedOperation", null)),
            Map.entry("InternalError", new SmsException("sms.send.internalError", null)),
            Map.entry("InvalidAction", new SmsException("sms.send.invalidAction", null)),
            Map.entry("InvalidParameter", new SmsException("sms.send.invalidParameter", null)),
            Map.entry("InvalidParameterValue", new SmsException("sms.send.invalidParameterValue", null)),
            Map.entry("InvalidRequest", new SmsException("sms.send.invalidRequest", null)),
            Map.entry("IpInBlacklist", new SmsException("sms.send.ipInBlacklist", null)),
            Map.entry("IpNotInWhitelist", new SmsException("sms.send.ipNotInWhitelist", null)),
            Map.entry("LimitExceeded", new SmsException("sms.send.limitExceeded", null)),
            Map.entry("MissingParameter", new SmsException("sms.send.missingParameter", null)),
            Map.entry("NoSuchProduct", new SmsException("sms.send.noSuchProduct", null)),
            Map.entry("NoSuchVersion", new SmsException("sms.send.noSuchVersion", null)),
            Map.entry("RequestLimitExceeded", new SmsException("sms.send.requestLimitExceeded", null)),
            Map.entry("RequestLimitExceeded.GlobalRegionUinLimitExceeded", new SmsException("sms.send.globalRegionUinLimitExceeded", null)),
            Map.entry("RequestLimitExceeded.IPLimitExceeded", new SmsException("sms.send.ipLimitExceeded", null)),
            Map.entry("RequestLimitExceeded.UinLimitExceeded", new SmsException("sms.send.uinLimitExceeded", null)),
            Map.entry("RequestSizeLimitExceeded", new SmsException("sms.send.requestSizeLimitExceeded", null)),
            Map.entry("ResourceInUse", new SmsException("sms.send.resourceInUse", null)),
            Map.entry("ResourceInsufficient", new SmsException("sms.send.resourceInsufficient", null)),
            Map.entry("ResourceNotFound", new SmsException("sms.send.resourceNotFound", null)),
            Map.entry("ResourceUnavailable", new SmsException("sms.send.resourceUnavailable", null)),
            Map.entry("ResponseSizeLimitExceeded", new SmsException("sms.send.responseSizeLimitExceeded", null)),
            Map.entry("ServiceUnavailable", new SmsException("sms.send.serviceUnavailable", null)),
            Map.entry("UnauthorizedOperation", new SmsException("sms.send.unauthorizedOperation", null)),
            Map.entry("UnknownParameter", new SmsException("sms.send.unknownParameter", null)),
            Map.entry("UnsupportedOperation", new SmsException("sms.send.unsupportedOperation", null)),
            Map.entry("UnsupportedProtocol", new SmsException("sms.send.unsupportedProtocol", null)),
            Map.entry("UnsupportedRegion", new SmsException("sms.send.unsupportedRegion", null)),
            Map.entry("FailedOperation.ContainSensitiveWord", new SmsException("sms.send.containSensitiveWord", null)),
            Map.entry("FailedOperation.FailResolvePacket", new SmsException("sms.send.failResolvePacket", null)),
            Map.entry("FailedOperation.ForbidAddMarketingTemplates", new SmsException("sms.send.forbidAddMarketingTemplates", null)),
            Map.entry("FailedOperation.InsufficientBalanceInSmsPackage", new SmsException("sms.send.insufficientBalanceInSmsPackage", null)),
            Map.entry("FailedOperation.JsonParseFail", new SmsException("sms.send.jsonParseFail", null)),
            Map.entry("FailedOperation.MarketingSendTimeConstraint", new SmsException("sms.send.marketingSendTimeConstraint", null)),
            Map.entry("FailedOperation.MissingSignature", new SmsException("sms.send.missingSignature", null)),
            Map.entry("FailedOperation.MissingSignatureList", new SmsException("sms.send.missingSignatureList", null)),
            Map.entry("FailedOperation.MissingSignatureToModify", new SmsException("sms.send.missingSignatureToModify", null)),
            Map.entry("FailedOperation.MissingTemplateList", new SmsException("sms.send.missingTemplateList", null)),
            Map.entry("FailedOperation.MissingTemplateToModify", new SmsException("sms.send.missingTemplateToModify", null)),
            Map.entry("FailedOperation.NotEnterpriseCertification", new SmsException("sms.send.notEnterpriseCertification", null)),
            Map.entry("FailedOperation.OtherError", new SmsException("sms.send.otherError", null)),
            Map.entry("FailedOperation.ParametersOtherError", new SmsException("sms.send.parametersOtherError", null)),
            Map.entry("FailedOperation.PhoneNumberInBlacklist", new SmsException("sms.send.phoneNumberInBlacklist", null)),
            Map.entry("FailedOperation.PhoneNumberParseFail", new SmsException("sms.send.phoneNumberParseFail", null)),
            Map.entry("FailedOperation.ProhibitSubAccountUse", new SmsException("sms.send.prohibitSubAccountUse", null)),
            Map.entry("FailedOperation.SignIdNotExist", new SmsException("sms.send.signIdNotExist", null)),
            Map.entry("FailedOperation.SignNumberLimit", new SmsException("sms.send.signNumberLimit", null)),
            Map.entry("FailedOperation.SignatureIncorrectOrUnapproved", new SmsException("sms.send.signatureIncorrectOrUnapproved", null)),
            Map.entry("FailedOperation.TemplateAlreadyPassedCheck", new SmsException("sms.send.templateAlreadyPassedCheck", null)),
            Map.entry("FailedOperation.TemplateIdNotExist", new SmsException("sms.send.templateIdNotExist", null)),
            Map.entry("FailedOperation.TemplateIncorrectOrUnapproved", new SmsException("sms.send.templateIncorrectOrUnapproved", null)),
            Map.entry("FailedOperation.TemplateNumberLimit", new SmsException("sms.send.templateNumberLimit", null)),
            Map.entry("FailedOperation.TemplateParamSetNotMatchApprovedTemplate", new SmsException("sms.send.templateParamSetNotMatchApprovedTemplate", null)),
            Map.entry("FailedOperation.TemplateUnapprovedOrNotExist", new SmsException("sms.send.templateUnapprovedOrNotExist", null)),
            Map.entry("InternalError.JsonParseFail", new SmsException("sms.send.jsonParseFail", null)),
            Map.entry("InternalError.OtherError", new SmsException("sms.send.otherError", null)),
            Map.entry("InternalError.ParseBackendResponseFail", new SmsException("sms.send.parseBackendResponseFail", null)),
            Map.entry("InternalError.RequestTimeException", new SmsException("sms.send.requestTimeException", null)),
            Map.entry("InternalError.RestApiInterfaceNotExist", new SmsException("sms.send.restApiInterfaceNotExist", null)),
            Map.entry("InternalError.SendAndRecvFail", new SmsException("sms.send.sendAndRecvFail", null)),
            Map.entry("InternalError.SigFieldMissing", new SmsException("sms.send.sigFieldMissing", null)),
            Map.entry("InternalError.SigVerificationFail", new SmsException("sms.send.sigVerificationFail", null)),
            Map.entry("InternalError.Timeout", new SmsException("sms.send.timeout", null)),
            Map.entry("InternalError.UnknownError", new SmsException("sms.send.unknownError", null)),
            Map.entry("InvalidParameter.AppidAndBizId", new SmsException("sms.send.appidAndBizIdMismatch", null)),
            Map.entry("InvalidParameter.DirtyWordFound", new SmsException("sms.send.dirtyWordFound", null)),
            Map.entry("InvalidParameter.InvalidParameters", new SmsException("sms.send.invalidParameters", null)),
            Map.entry("InvalidParameterValue.BeginTimeVerifyFail", new SmsException("sms.send.beginTimeVerifyFail", null)),
            Map.entry("InvalidParameterValue.ContentLengthLimit", new SmsException("sms.send.contentLengthLimit", null)),
            Map.entry("InvalidParameterValue.EndTimeVerifyFail", new SmsException("sms.send.endTimeVerifyFail", null)),
            Map.entry("InvalidParameterValue.ImageInvalid", new SmsException("sms.send.imageInvalid", null)),
            Map.entry("InvalidParameterValue.IncorrectPhoneNumber", new SmsException("sms.send.incorrectPhoneNumber", null)),
            Map.entry("InvalidParameterValue.InvalidDocumentType", new SmsException("sms.send.invalidDocumentType", null)),
            Map.entry("InvalidParameterValue.InvalidInternational", new SmsException("sms.send.invalidInternational", null)),
            Map.entry("InvalidParameterValue.InvalidSignPurpose", new SmsException("sms.send.invalidSignPurpose", null)),
            Map.entry("InvalidParameterValue.InvalidSmsType", new SmsException("sms.send.invalidSmsType", null)),
            Map.entry("InvalidParameterValue.InvalidStartTime", new SmsException("sms.send.invalidStartTime", null)),
            Map.entry("InvalidParameterValue.InvalidTemplateFormat", new SmsException("sms.send.invalidTemplateFormat", null)),
            Map.entry("InvalidParameterValue.InvalidUsedMethod", new SmsException("sms.send.invalidUsedMethod", null)),
            Map.entry("InvalidParameterValue.LimitVerifyFail", new SmsException("sms.send.limitVerifyFail", null)),
            Map.entry("InvalidParameterValue.MarketingTemplateWithoutUnsubscribe", new SmsException("sms.send.marketingTemplateWithoutUnsubscribe", null)),
            Map.entry("InvalidParameterValue.OffsetVerifyFail", new SmsException("sms.send.offsetVerifyFail", null)),
            Map.entry("InvalidParameterValue.ProhibitedUseUrlInTemplateParameter", new SmsException("sms.send.prohibitedUseUrlInTemplateParameter", null)),
            Map.entry("InvalidParameterValue.SdkAppIdNotExist", new SmsException("sms.send.sdkAppIdNotExist", null)),
            Map.entry("InvalidParameterValue.SignAlreadyPassedCheck", new SmsException("sms.send.signAlreadyPassedCheck", null)),
            Map.entry("InvalidParameterValue.SignExistAndUnapproved", new SmsException("sms.send.signExistAndUnapproved", null)),
            Map.entry("InvalidParameterValue.SignNameLengthTooLong", new SmsException("sms.send.signNameLengthTooLong", null)),
            Map.entry("InvalidParameterValue.TemplateParameterFormatError", new SmsException("sms.send.templateParameterFormatError", null)),
            Map.entry("InvalidParameterValue.TemplateParameterLengthLimit", new SmsException("sms.send.templateParameterLengthLimit", null)),
            Map.entry("InvalidParameterValue.TemplateWithDirtyWords", new SmsException("sms.send.templateWithDirtyWords", null)),
            Map.entry("InvalidParameterValue.UnsupportedTemplateVariable", new SmsException("sms.send.unsupportedTemplateVariable", null)),
            Map.entry("LimitExceeded.AppCountryOrRegionDailyLimit", new SmsException("sms.send.appCountryOrRegionDailyLimit", null)),
            Map.entry("LimitExceeded.AppCountryOrRegionInBlacklist", new SmsException("sms.send.appCountryOrRegionInBlacklist", null)),
            Map.entry("LimitExceeded.AppDailyLimit", new SmsException("sms.send.appDailyLimit", null)),
            Map.entry("LimitExceeded.AppGlobalDailyLimit", new SmsException("sms.send.appGlobalDailyLimit", null)),
            Map.entry("LimitExceeded.AppMainlandChinaDailyLimit", new SmsException("sms.send.appMainlandChinaDailyLimit", null)),
            Map.entry("LimitExceeded.DailyLimit", new SmsException("sms.send.dailyLimit", null)),
            Map.entry("LimitExceeded.DeliveryFrequencyLimit", new SmsException("sms.send.deliveryFrequencyLimit", null)),
            Map.entry("LimitExceeded.PhoneNumberCountLimit", new SmsException("sms.send.phoneNumberCountLimit", null)),
            Map.entry("LimitExceeded.PhoneNumberDailyLimit", new SmsException("sms.send.phoneNumberDailyLimit", null)),
            Map.entry("LimitExceeded.PhoneNumberOneHourLimit", new SmsException("sms.send.phoneNumberOneHourLimit", null)),
            Map.entry("LimitExceeded.PhoneNumberSameContentDailyLimit", new SmsException("sms.send.phoneNumberSameContentDailyLimit", null)),
            Map.entry("LimitExceeded.PhoneNumberThirtySecondLimit", new SmsException("sms.send.phoneNumberThirtySecondLimit", null)),
            Map.entry("MissingParameter.EmptyPhoneNumberSet", new SmsException("sms.send.emptyPhoneNumberSet", null)),
            Map.entry("UnauthorizedOperation.IndividualUserMarketingSmsPermissionDeny", new SmsException("sms.send.individualUserMarketingSmsPermissionDeny", null)),
            Map.entry("UnauthorizedOperation.RequestIpNotInWhitelist", new SmsException("sms.send.requestIpNotInWhitelist", null)),
            Map.entry("UnauthorizedOperation.RequestPermissionDeny", new SmsException("sms.send.requestPermissionDeny", null)),
            Map.entry("UnauthorizedOperation.SdkAppIdIsDisabled", new SmsException("sms.send.sdkAppIdIsDisabled", null)),
            Map.entry("UnauthorizedOperation.ServiceSuspendDueToArrears", new SmsException("sms.send.serviceSuspendDueToArrears", null)),
            Map.entry("UnauthorizedOperation.SmsSdkAppIdVerifyFail", new SmsException("sms.send.smsSdkAppIdVerifyFail", null)),
            Map.entry("UnsupportedOperation.ChineseMainlandTemplateToGlobalPhone", new SmsException("sms.send.chineseMainlandTemplateToGlobalPhone", null)),
            Map.entry("UnsupportedOperation.ContainDomesticAndInternationalPhoneNumber", new SmsException("sms.send.containDomesticAndInternationalPhoneNumber", null)),
            Map.entry("UnsupportedOperation.GlobalTemplateToChineseMainlandPhone", new SmsException("sms.send.globalTemplateToChineseMainlandPhone", null)),
            Map.entry("UnsupportedOperation.UnsupportedRegion", new SmsException("sms.send.unsupportedRegion", null))
    );
    
    public static final String DEFAULT_NOTIFICATION_TITLE = "您收到一条新信息:";
}
