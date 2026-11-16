package com.agri.cms.common.annotation;


import java.lang.annotation.*;


/**
 * @author 李鸿麟
 * 统计日活用户注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DAU {
    /**
     * SpEL表达式，用于获取用户ID
     */
    String value() default "";
}
