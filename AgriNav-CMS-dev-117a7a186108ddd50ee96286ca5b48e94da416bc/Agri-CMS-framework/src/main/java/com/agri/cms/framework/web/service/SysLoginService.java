package com.agri.cms.framework.web.service;

import com.agri.cms.common.annotation.DAU;
import com.agri.cms.common.constant.CacheConstants;
import com.agri.cms.common.constant.Constants;
import com.agri.cms.common.constant.EncryptConstants;
import com.agri.cms.common.constant.UserConstants;
import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.common.core.domain.entity.SysUser;
import com.agri.cms.common.core.domain.model.LoginUser;
import com.agri.cms.common.core.redis.RedisCache;
import com.agri.cms.common.exception.ServiceException;
import com.agri.cms.common.exception.user.BlackListException;
import com.agri.cms.common.exception.user.CaptchaExpireException;
import com.agri.cms.common.exception.user.UserNotExistsException;
import com.agri.cms.common.exception.user.UserPasswordNotMatchException;
import com.agri.cms.common.utils.DateUtils;
import com.agri.cms.common.utils.MessageUtils;
import com.agri.cms.common.utils.StringUtils;
import com.agri.cms.common.utils.ip.IpUtils;
import com.agri.cms.common.utils.spring.SpringUtils;
import com.agri.cms.framework.manager.AsyncManager;
import com.agri.cms.framework.manager.factory.AsyncFactory;
import com.agri.cms.framework.security.context.AuthenticationContextHolder;
import com.agri.cms.system.domain.SysUserWechat;
import com.agri.cms.system.service.*;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthRequest;
import me.zhyd.oauth.utils.AuthStateUtils;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import oshi.jna.platform.mac.SystemB;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 登录校验方法
 *
 * @author ruoyi
 */
@Service
public class SysLoginService {

    @Autowired
    private TokenService tokenService;


    @Resource
    private CaptchaService captchaService;


    @Resource
    private AuthenticationManager authenticationManager;

    @Resource
    private RedisCache redisCache;


    @Autowired
    private ISysUserService userService;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private IEncryptService encryptService;

    @Resource
    private AuthRequest webAuthRequest;


    @Resource
    private SysWechatService sysWechatService;
    @Autowired
    private FarmWorkService farmWorkService;
    @Autowired
    private AgriculturalMachineryService agriculturalMachineryService;
    @Autowired
    private InfluxDBEffectiveTrajectoryService effectiveTrajectoryService;
    @Autowired
    private IOriginalTrajectoryService originalTrajectoryService;
    @Autowired
    private LandPlotService landPlotService;
    @Autowired
    private InfluxDBEffectiveTrajectoryService influxDBEffectiveTrajectoryService;


    /**
     * 登录验证
     *
     * @param username 用户名
     * @param password 密码
     * @param code     验证码
     * @return 结果
     */
    public AjaxResult login(String username, String password, String code) throws Exception {
        // 验证码校验
        try {
            captchaService.validate(username, code);
        } catch (Exception e) {
            if (e instanceof CaptchaExpireException) {
                AsyncManager.me().execute(AsyncFactory
                        .recordLogininfor(username, Constants.LOGIN_FAIL,
                                MessageUtils.message("user.jcaptcha.expire")));
            } else {
                AsyncManager.me().execute(AsyncFactory.
                        recordLogininfor(username, Constants.LOGIN_FAIL,
                                MessageUtils.message("user.jcaptcha.error")));
            }
            throw e;
        }
        return ((SysLoginService) AopContext.currentProxy()).defaultLogin(username, password);
    }

    @DAU("#p0")
    public AjaxResult defaultLogin(String username, String password) {
        AjaxResult result = AjaxResult.success();
        // 登录前置校验
        loginPreCheck(username, password);
        // 用户验证
        Authentication authentication = null;
        try {
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username, password);
            AuthenticationContextHolder.setContext(authenticationToken);
            // 该方法会去调用UserDetailsServiceImpl.loadUserByUsername
            authentication = authenticationManager.authenticate(authenticationToken);
        } catch (Exception e) {
            if (e instanceof BadCredentialsException) {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.password.not.match")));
                throw new UserPasswordNotMatchException();
            } else {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, e.getMessage()));
                throw new ServiceException(e.getMessage());
            }
        } finally {
            AuthenticationContextHolder.clearContext();
        }
        AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_SUCCESS, MessageUtils.message("user.login.success")));
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        recordLoginInfo(loginUser.getUserId());
        // 生成token
        result.put(Constants.TOKEN,tokenService.createToken(loginUser));
        // 生成公钥
        String publicKey = encryptService.getRSAPublicKey();
        result.put(EncryptConstants.RSA_PUBLIC_KEY_NAME, publicKey);
        return result;
    }


    /**
     * 登录前置校验
     *
     * @param username 用户名
     * @param password 用户密码
     */
    public void loginPreCheck(String username, String password) {
        // 用户名或密码为空 错误
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("not.null")));
            throw new UserNotExistsException();
        }
        // 密码如果不在指定范围内 错误
        if (password.length() < UserConstants.PASSWORD_MIN_LENGTH
                || password.length() > UserConstants.PASSWORD_MAX_LENGTH) {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.password.not.match")));
            throw new UserPasswordNotMatchException();
        }
        // 用户名不在指定范围内 错误
        if (username.length() < UserConstants.USERNAME_MIN_LENGTH
                || username.length() > UserConstants.USERNAME_MAX_LENGTH) {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.password.not.match")));
            throw new UserPasswordNotMatchException();
        }
        // IP黑名单校验
        String blackStr = configService.selectConfigByKey("sys.login.blackIPList");
        if (IpUtils.isMatchedIp(blackStr, IpUtils.getIpAddr())) {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("login.blocked")));
            throw new BlackListException();
        }
    }

    /**
     * 记录登录信息
     *
     * @param userId 用户ID
     */
    public void recordLoginInfo(Long userId) {
        SysUser sysUser = new SysUser();
        sysUser.setUserId(userId);
        sysUser.setLoginIp(IpUtils.getIpAddr());
        sysUser.setLoginDate(DateUtils.getNowDate());
        userService.updateUserProfile(sysUser);
    }

    /**
     * 扫码登录
     *
     * @param request
     * @return AjaxResult
     * @author simon
     * @since 2024/10/22
     */

    public AjaxResult loginQR(HttpServletRequest request) {
        String uuid = request.getParameter("uuid");
        if (redisCache.getExpire(CacheConstants.QR_CODE_KEY + uuid) <= 0) {
            return AjaxResult.warn("二维码已过期，请重新登录");
        }
        tokenService.verifyToken(tokenService.getLoginUser(request));
        redisCache.setCacheObject(CacheConstants.QR_CODE_KEY + uuid, tokenService.getToken(request),
                Constants.CAPTCHA_EXPIRATION, TimeUnit.MINUTES);
        return AjaxResult.success();
    }

    public void render(HttpServletResponse response) throws IOException {
        response.sendRedirect(webAuthRequest.authorize(AuthStateUtils.createState()));
    }

    public AjaxResult loginOAuth(AuthUser details) {
        if (sysWechatService.exists(new LambdaQueryWrapper<SysUserWechat>()
                .eq(SysUserWechat::getWechatId, details.getUuid()))) {
            SysUser user = userService.selectUserById(
                    sysWechatService
                            .getOne(new LambdaQueryWrapper<SysUserWechat>()
                                    .eq(SysUserWechat::getWechatId, details.getUuid()))
                            .getUserId());
            sysWechatService.lambdaUpdate()
                    .set(SysUserWechat::getToken, details.getToken().getAccessToken())
                    .eq(SysUserWechat::getWechatId, details.getUuid())
                    .update();
            return ((SysLoginService) AopContext.currentProxy()).defaultLogin(user.getPhonenumber(),
                    user.getPhonenumber().substring(user.getPhonenumber()
                            .length() - Constants.PASSWORD_INTERCEPTION));
        } else {
            return AjaxResult.warn("登录失败，请先绑定手机号").put(Constants.USER_DETAILS, details);
        }
    }

    @DSTransactional(rollbackFor = Exception.class)
    public void signOut(String username) {
        try {
            //删除微信绑定
            sysWechatService.lambdaUpdate().eq(SysUserWechat::getUserId, userService.selectUserByUserName(username).getUserId()).remove();
            //注销农机
            agriculturalMachineryService.deleteByUsername(username);
            //注销农活
            farmWorkService.deleteByUsername(username);
            //注销轨迹
            influxDBEffectiveTrajectoryService.loginOut(username);
            //注销地块
            landPlotService.deleteByUsername(username);
            //注销es
            originalTrajectoryService.deleteByUserName(username);
            //注销用户
            userService.deleteByUserName(username);
        } catch (Exception e) {
            throw new RuntimeException("注销失败", e);
        }
    }
}
