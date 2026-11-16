package com.agri.cms.common.constant;

/**
 * 缓存的key 常量
 *
 * @author ruoyi
 */
public class CacheConstants {
    /**
     * 登录用户 redis key
     */
    public static final String LOGIN_TOKEN_KEY = "login_tokens:";

    /**
     * 验证码 redis key
     */
    public static final String CAPTCHA_CODE_KEY = "captcha_codes:";

    /**
     * 参数管理 cache key
     */
    public static final String SYS_CONFIG_KEY = "sys_config:";

    /**
     * 字典管理 cache key
     */
    public static final String SYS_DICT_KEY = "sys_dict:";

    /**
     * 防重提交 redis key
     */
    public static final String REPEAT_SUBMIT_KEY = "repeat_submit:";

    /**
     * 限流 redis key
     */
    public static final String RATE_LIMIT_KEY = "rate_limit:";

    /**
     * 登录账户密码错误次数 redis key
     */
    public static final String PWD_ERR_CNT_KEY = "pwd_err_cnt:";
    public static final String QR_CODE_KEY = "qr_codes:";

    /**jps
     * 日活用户前缀
     */
    public static final String DAU_KEY_PREFIX = "sys:dau:";

    /**
     * 大屏农机任务前缀
     */
    public static final String AGRICULTURE_MACHINERY_STATISTICS_KEY = "screen_agriculture_machinery_task:";

    /**
     * 大屏用户情况前缀
     */
    public static final String USER_STATISTICS_KEY = "screen_user_statistics:";

    /**
     * 大屏任务完成情况前缀
     */
    public static final String TASK_STATISTICS_KEY = "screen_task_statistics:";
    public static final String LAND_PLOT_COUNT_BY_PROVINCE = "screen_land_plot_count_by_service:";
    public static final String PROVINCE_WITH_MOST_FARM_WORK = "screen_province_with_most_farm_work";
    public static final String LOGIN_LOCATION = "login-location:";
    public static final String CHANGE_LOG_CACHE_KEY = "change_log_";

    public static final String SYSTEM_NOTICE_CACHE_KEY = "sys_notice:";
    public static final String SYSTEM_NOTICE_READ_CACHE_KEY = "sys_notice_read:";


    public static final String SYS_VERSION_CODE = "sys_version";
    public static final String SYS_LATEST_VERSION = "sys_latest_version";
}
