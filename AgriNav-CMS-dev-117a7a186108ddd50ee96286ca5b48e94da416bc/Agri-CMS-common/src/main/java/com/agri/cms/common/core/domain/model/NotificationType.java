package com.agri.cms.common.core.domain.model;

import com.baomidou.mybatisplus.annotation.TableField;
import org.springframework.boot.autoconfigure.flyway.FlywayProperties;

/**
 * @author : simon
 * @description :
 * @last : 2024-11-30 15:50
 * Copyright (c) 2024. 保留所有权利。
 * @since : 2024-11-30 15:50
 */
public enum NotificationType {
    /**
     * 农事通知
     */
    FARM_WORK("FARM_WORK","pg","","farm_work","您的农事工作已完成"),
    /**
     * 农事记录
     */
    FARM_WORK_NOTIFICATION("FARM_WORK_NOTIFICATION","","pg","farm_work_notification","您的农机找活已收到委托"),
    /**
     * 农事记录
     */
    FARM_MACHINERY_NOTIFICATION("FARM_MACHINERY_NOTIFICATION","","pg","find_work","有人接收了您的找活委托"),

    SYSTEM("SYSTEM","ms","sys_notice","","您收到一条系统通知"),

    FEEDBACK_REPLY("FEEDBACK_REPLY","ms","com.agri.cms.system.domain.Feedback","sys_feedback","您的反馈得到了回复");

    @TableField("type_name")
    private final String typeName;

    @TableField("relation_db")
    private final String relationDB;

    @TableField(exist = false)
    private final String relationClass;

    @TableField("relation_name")
    private final String relationName;
    @TableField("content")
    private final String defaultContent;

    NotificationType(String typeName,String relationDB,String relationClass
            ,String relationName,String defaultContent) {
        this.typeName = typeName;
        this.relationDB = relationDB;
        this.relationName = relationName;
        this.defaultContent = defaultContent;
        this.relationClass = relationClass;
    }

    public String getDefaultContent() {
        return defaultContent;
    }

    public String getTypeName() {
        return typeName;
    }

    public String getRelationDB() {
        return relationDB;
    }

    public String getRelationName() {
        return relationName;
    }

    public String getRelationClass() {
        return relationClass;
    }

    public String getTitle(){
        return "您获取到一条%s消息".formatted(typeName);
    }
    @Override
    public String toString() {
        return typeName;
    }
}
