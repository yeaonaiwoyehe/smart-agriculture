package com.agri.cms.common.exception.user;

/**
 * @author : simon
 * @description :
 * @last : 2024-12-11 08:02
 * Copyright (c) 2024. 保留所有权利。
 * @since : 2024-12-11 08:02
 */
public class CaptchaRepeatException extends UserException{

    private static final long serialVersionUID = 1L;
    public CaptchaRepeatException() {
        super("user.jcaptcha.repeat", null);
    }
}
