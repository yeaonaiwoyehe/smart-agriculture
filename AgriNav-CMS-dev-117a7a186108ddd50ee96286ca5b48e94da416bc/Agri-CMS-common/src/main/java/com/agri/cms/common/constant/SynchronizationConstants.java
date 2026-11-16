package com.agri.cms.common.constant;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author : simon
 * @description :
 * @last : 2024-11-20 21:54
 * Copyright (c) 2024. 保留所有权利。
 * @since : 2024-11-20 21:54
 */
public class SynchronizationConstants {

    public static final String CHANGE_LOG_TOPIC = "sync-mysql.ry-vue.sys_change_log";
    public static final String LOGIN_INFO_TOPIC = "sync-mysql.ry-vue.sys_logininfor";
    public static final String NOTIFICATION_TOPIC = "sync-mysql.ry-vue.sys_notification";

    public static final String DEFAULT_CONTAINER = "debeziumListenerContainerFactory";


    public enum OperationType {
        //操作类型枚举类 ： 新增、修改、删除、替换
        INSERT("c"),
        UPDATE("u"),
        DELETE("d"),
        REPLACE("r");

    private final String typeName;

    OperationType(String typeName) {
        this.typeName = typeName;
    }

        public String getTypeName() {
            return typeName;
        }

        @Override
    public String toString() {
        return this.typeName;
    }
    }
}
