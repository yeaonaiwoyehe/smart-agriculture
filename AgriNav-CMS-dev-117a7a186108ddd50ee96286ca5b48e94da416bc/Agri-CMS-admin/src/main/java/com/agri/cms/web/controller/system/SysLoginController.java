package com.agri.cms.web.controller.system;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.PhoneUtil;
import cn.hutool.extra.tokenizer.TokenizerException;
import com.agri.cms.common.annotation.DAU;
import com.agri.cms.common.constant.Constants;
import com.agri.cms.common.constant.EncryptConstants;
import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.common.core.domain.entity.SysMenu;
import com.agri.cms.common.core.domain.entity.SysUser;
import com.agri.cms.common.core.domain.model.LoginUser;
import com.agri.cms.common.utils.SecurityUtils;
import com.agri.cms.framework.web.service.SysLoginService;
import com.agri.cms.framework.web.service.SysPermissionService;
import com.agri.cms.framework.web.service.TokenService;
import com.agri.cms.system.service.IEncryptService;
import com.agri.cms.system.service.ISysMenuService;
import com.agri.cms.system.service.ISysUserService;
import com.agri.cms.web.controller.field.LandPlotController;
import com.agri.cms.web.controller.home.AgriculturalMachineryController;
import com.agri.cms.web.controller.home.FarmWorkController;
import com.agri.cms.web.controller.trajectory.EffectiveTrajectoryController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthRequest;
import me.zhyd.oauth.utils.AuthStateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 登录验证
 *
 * @author ruoyi
 */
@Tag(name = "用户登录验证")
@RestController
public class SysLoginController {
    @Autowired
    private SysLoginService loginService;
    @Autowired
    private ISysUserService userService;

    @Autowired
    private ISysMenuService menuService;

    @Autowired
    private SysPermissionService permissionService;

    @Resource
    AuthRequest webAuthRequest;
    @Resource
    AuthRequest mobileAuthRequest;

    @Resource
    private ISysUserService sysUserService;

    @Resource
    private TokenService tokenService;

    @Resource
    private IEncryptService encryptService;


    /**
     * 手机号登录注册
     *
     * @param phoneNumber : 手机号
     * @param captcha     : 验证码
     * @return token
     * @author simon
     * @since 2024年9月7日12:00:50
     */
    @PostMapping("/login")
    @Operation(summary = "手机号登录注册")
    public AjaxResult loginOrRegisterByMobile(
            @RequestParam(value = "phoneNumber") @NotEmpty(message = "手机号不得为空") String phoneNumber,
            @RequestParam(value = "captcha", required = false) String captcha) throws Exception {
        if (!PhoneUtil.isPhone(phoneNumber)) {
            throw new Exception("手机号格式不正确");
        }
        AjaxResult ajax = AjaxResult.success();
        SysUser user = new SysUser();
        user.setPhonenumber(phoneNumber);
        sysUserService.preLoginInsertUser(user);
        AjaxResult login = loginService.login(user.getPhonenumber(),
                user.getPhonenumber().substring(user.getPhonenumber()
                        .length() - Constants.PASSWORD_INTERCEPTION), captcha);

        ajax.putAll(login);
        return ajax;
    }

    @PostMapping("/loginQR")
    public AjaxResult loginByQRCode(HttpServletRequest request) {
        return loginService.loginQR(request);
    }

    @RequestMapping("/render")
    public void render(HttpServletResponse response) throws IOException {
        loginService.render(response);
    }

    @GetMapping("/loginOAuth")
    public AjaxResult loginOAuth(AuthCallback callback) {
        AuthUser user = (AuthUser) webAuthRequest.login(callback).getData();
        return loginService.loginOAuth(user);
    }

    @GetMapping("/loginWechatMobile")
    public AjaxResult loginWechatMobile(AuthCallback callback) {
        AuthUser user = (AuthUser) mobileAuthRequest.login(callback).getData();
        return loginService.loginOAuth(user);
    }

    @Operation(summary = "免密登录")
    @GetMapping("/loginWithoutSecret")
    public AjaxResult loginWithoutSecret(HttpServletRequest request) {
        LoginUser loginUser = tokenService.getLoginUser(request);
        if(ObjectUtil.isNull(loginUser)){
            throw new TokenizerException("登录凭证已失效，请重新登录");
        }
        tokenService.verifyToken(loginUser);
        return AjaxResult.success().put(Constants.TOKEN,tokenService.getToken(request))
                .put(EncryptConstants.RSA_PUBLIC_KEY_NAME,encryptService.getRSAPublicKey());
    }

    /**
     * 获取用户信息
     *
     * @return 用户信息
     */
    @Operation(summary = "获取当前用户信息")
    @GetMapping("getInfo")
    public AjaxResult getInfo() {
        SysUser user = SecurityUtils.getLoginUser().getUser();
        // 角色集合
        Set<String> roles = permissionService.getRolePermission(user);
        // 权限集合
        Set<String> permissions = permissionService.getMenuPermission(user);
        AjaxResult ajax = AjaxResult.success();
        ajax.put("user", user);
        ajax.put("roles", roles);
        ajax.put("permissions", permissions);
        ajax.put("departmentNames", userService.selectDeptNameById(user.getDeptId()));
        return ajax;
    }

    /**
     * 获取路由信息
     *
     * @return 路由信息
     */
    @Operation(summary = "获取路由信息")
    @GetMapping("getRouters")
    public AjaxResult getRouters() {
        Long userId = SecurityUtils.getUserId();
        List<SysMenu> menus = menuService.selectMenuTreeByUserId(userId);
        return AjaxResult.success(menuService.buildMenus(menus));
    }

    @DeleteMapping("/signOut/{username}")
    @Operation(summary = "注销用户")
    public AjaxResult signOut(@PathVariable("username") String username) {
        loginService.signOut(username);
        return AjaxResult.success();
    }
}
