package com.agri.cms.common.annotation;

import com.agri.cms.common.core.domain.model.NotificationType;

import java.lang.annotation.*;

/**
 * @author : simon
 * @description :
 * @last : 2024-11-30 20:46
 * Copyright (c) 2024. 保留所有权利。
 * @since : 2024-11-30 20:46
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AsyncNotification {
    /**
     * 通知类型
     */
    NotificationType type() default NotificationType.SYSTEM;

    String receiver() default "*";

    String relationIdentity();

}
