package com.agri.cms.common.exception;

import com.agri.cms.common.exception.base.BaseException;
import com.agri.cms.common.exception.user.UserException;

public class SmsException extends BaseException {

    private static final long serialVersionUID = 1L;

    public SmsException(String code, Object[] args) {
        super(code, args);
    }

    public SmsException(String defaultMessage) {
        super(defaultMessage);
    }
}
