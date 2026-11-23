package com.agri.cms.web.controller.system;

import cn.hutool.core.util.PhoneUtil;
import com.agri.cms.common.annotation.Log;
import com.agri.cms.common.constant.Constants;
import com.agri.cms.common.core.controller.BaseController;
import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.common.core.domain.entity.SysUser;
import com.agri.cms.common.core.domain.model.LoginUser;
import com.agri.cms.common.core.redis.RedisCache;
import com.agri.cms.common.enums.BusinessType;
import com.agri.cms.common.utils.SecurityUtils;
import com.agri.cms.common.utils.StringUtils;
import com.agri.cms.common.utils.file.FileUploadUtils;
import com.agri.cms.framework.web.service.SysLoginService;
import com.agri.cms.framework.web.service.TokenService;
import com.agri.cms.system.service.CaptchaService;
import com.agri.cms.system.service.ISysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 个人信息 业务处理
 *
 * @author ruoyi
 */

@Tag(name = "个人信息", description = "个人信息业务处理")
@RestController
@RequestMapping("/system/user/profile")
public class SysProfileController extends BaseController {
    @Resource
    private SysLoginService sysLoginService;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private TokenService tokenService;

    @Resource
    private CaptchaService captchaService;

    @Resource
    private RedisCache redisCache;


    /**
     * 个人信息
     */
    @GetMapping
    @Operation(summary = "获取个人信息")
    public AjaxResult profile() {
        LoginUser loginUser = getLoginUser();
        SysUser user = loginUser.getUser();
        AjaxResult ajax = AjaxResult.success(user);
        ajax.put("roleGroup", userService.selectUserRoleGroup(loginUser.getUsername()));
        ajax.put("postGroup", userService.selectUserPostGroup(loginUser.getUsername()));
        ajax.put("departmentName", userService.selectDeptNameById(user.getDeptId()));
        return ajax;
    }

    /**
     * 修改用户
     */
    @Log(title = "个人信息", businessType = BusinessType.UPDATE)
    @Operation(summary = "修改用户")
    @PutMapping
    public AjaxResult updateProfile(@RequestBody SysUser user) {
        LoginUser loginUser = getLoginUser();
        SysUser currentUser = loginUser.getUser();
        currentUser.setNickName(user.getNickName());
        currentUser.setEmail(user.getEmail());
        currentUser.setPhonenumber(user.getPhonenumber());
        currentUser.setSex(user.getSex());
        if (StringUtils.isNotEmpty(user.getPhonenumber()) && !userService.checkPhoneUnique(currentUser)) {
            return error("修改用户'" + loginUser.getUsername() + "'失败，手机号码已存在");
        }
        if (StringUtils.isNotEmpty(user.getEmail()) && !userService.checkEmailUnique(currentUser)) {
            return error("修改用户'" + loginUser.getUsername() + "'失败，邮箱账号已存在");
        }
        if (userService.updateUserProfile(currentUser) > 0) {
            // 更新缓存用户信息
            tokenService.setLoginUser(loginUser);
            return success();
        }
        return error("修改个人信息异常，请联系管理员");
    }


    /**
     * 重置密码
     */
    @Log(title = "个人信息", businessType = BusinessType.UPDATE)
    @Operation(summary = "重置密码")
    @PutMapping("/updatePwd")
    public AjaxResult updatePwd(String oldPassword, String newPassword) {
        LoginUser loginUser = getLoginUser();
        String userName = loginUser.getUsername();
        String password = loginUser.getPassword();
        if (!SecurityUtils.matchesPassword(oldPassword, password)) {
            return error("修改密码失败，旧密码错误");
        }
        if (SecurityUtils.matchesPassword(newPassword, password)) {
            return error("新密码不能与旧密码相同");
        }
        newPassword = SecurityUtils.encryptPassword(newPassword);
        if (userService.resetUserPwd(userName, newPassword) > 0) {
            // 更新缓存用户密码
            loginUser.getUser().setPassword(newPassword);
            tokenService.setLoginUser(loginUser);
            return success();
        }
        return error("修改密码异常，请联系管理员");
    }

    /**
     * 头像上传
     */
    @Log(title = "用户头像", businessType = BusinessType.UPDATE)
    @Operation(summary = "更新头像")
    @PostMapping("/avatar")
    public AjaxResult avatar(@RequestParam("avatarfile") MultipartFile file) throws Exception {
        if (!file.isEmpty()) {
            LoginUser loginUser = getLoginUser();
            //头像上传到minio
            String avatar = FileUploadUtils.uploadMinio(file, FileUploadUtils.AVATAR_PATH);
            if (userService.updateUserAvatar(loginUser.getUsername(), avatar)) {
                AjaxResult ajax = AjaxResult.success();
                ajax.put("imgUrl", avatar);
                // 更新缓存用户头像
                loginUser.getUser().setAvatar(avatar);
                tokenService.setLoginUser(loginUser);
                return ajax;
            }
        }
        return error("上传图片异常，请联系管理员");
    }

    @Log(title = "用户手机号", businessType = BusinessType.UPDATE)
    @Operation(summary = "修改手机号")
    @PutMapping("/phoneNumber")
    public AjaxResult updatePhoneNumber(
            @RequestParam(value = "phoneNumber") @NotEmpty String phoneNumber,
            @RequestParam(value = "captcha") @NotEmpty String captcha)
            throws Exception {
        if (!PhoneUtil.isPhone(phoneNumber)) {
            return error("手机号格式不正确");
        }
        //获取当前登录的账户
        LoginUser user = getLoginUser();
        //校验目标手机号验证码
        captchaService.validate(phoneNumber, captcha);
        tokenService.delLoginUser(user.getToken());
        if (userService.updatePhoneNumber(user, phoneNumber)) {
            redisCache.deleteObject(user.getToken());

            return sysLoginService.defaultLogin(phoneNumber,
                    phoneNumber.substring(phoneNumber.length()
                            - Constants.PASSWORD_INTERCEPTION));
        } else {
            return error("修改失败，请联系管理员!");
        }
    }
}