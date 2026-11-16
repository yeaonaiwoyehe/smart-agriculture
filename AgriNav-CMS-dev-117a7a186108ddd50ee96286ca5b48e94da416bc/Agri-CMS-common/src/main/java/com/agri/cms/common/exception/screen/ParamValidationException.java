package com.agri.cms.common.exception.screen;

import com.agri.cms.common.exception.ScreenException;

/**
 * 大屏参数校验异常
 * @author 李鸿麟
 */
public class ParamValidationException extends ScreenException {
    public ParamValidationException(Integer code, String message) {
        super(code, message);
    }
}
