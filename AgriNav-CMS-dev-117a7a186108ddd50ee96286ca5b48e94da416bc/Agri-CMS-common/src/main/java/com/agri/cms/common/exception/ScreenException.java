package com.agri.cms.common.exception;

/**
 * 大屏异常基类
 * @author 李鸿麟
 */
public class ScreenException extends RuntimeException{

    /**状态码**/
    private Integer code;

    /**异常信息**/
    private String message;


    public ScreenException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
}
