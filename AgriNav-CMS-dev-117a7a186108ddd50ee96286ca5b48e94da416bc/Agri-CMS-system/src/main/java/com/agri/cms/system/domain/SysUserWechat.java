package com.agri.cms.system.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * @author : simon
 * @description :
 * @last : 2024-10-27 21:05
 * Copyright (c) 2024. 保留所有权利。
 * @since : 2024-10-27 21:05
 */
@TableName("sys_user_wechat")
public class SysUserWechat {
    private Long userId;
    @TableField("wechat_uuid")
    private String wechatId;

    private String token;

    public SysUserWechat() {
    }

    public SysUserWechat(Long userId, String wechatId, String token) {
        this.userId = userId;
        this.wechatId = wechatId;
        this.token = token;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getWechatId() {
        return wechatId;
    }

    public void setWechatId(String wechatId) {
        this.wechatId = wechatId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
