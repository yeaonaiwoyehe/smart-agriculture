package com.agri.cms.common.core.domain.model;

import com.agri.cms.common.config.handler.NotificationTypeHandler;
import com.baomidou.mybatisplus.annotation.*;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;

/**
 * @author : simon
 * @description :
 * @last : 2024-11-30 14:10
 * Copyright (c) 2024. 保留所有权利。
 * @since : 2024-11-30 14:10
 */
@TableName("sys_notification")
public class Notification<T> implements Comparable<Notification> {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    @TableField(value = "type_name",typeHandler = NotificationTypeHandler.class)
    private NotificationType type;

    @TableField(exist = false)
    private T payload;


    @TableField("receiver_id")
    private String receiverId;

    @TableField("relation_id")
    private String relationIdentity;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("is_read")
    private Integer isRead;

    @TableField("read_time")
    private LocalDateTime readTime;



    public String getRelationIdentity() {
        return relationIdentity;
    }

    public void setRelationIdentity(String relationIdentity) {
        this.relationIdentity = relationIdentity;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public T getPayload() {
        return payload;
    }

    public void setPayload(T payload) {
        this.payload = payload;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public Integer getIsRead() {
        return isRead;
    }

    public void setIsRead(Integer isRead) {
        this.isRead = isRead;
    }

    public LocalDateTime getReadTime() {
        return readTime;
    }

    public void setReadTime(LocalDateTime readTime) {
        this.readTime = readTime;
    }

    @Override
    public String toString() {
        return "Notification{" +
                "id='" + id + '\'' +
                ", type=" + type +
                ", payload=" + payload +
                ", receiverId='" + receiverId + '\'' +
                ", relationIdentity='" + relationIdentity + '\'' +
                ", createTime=" + createTime +
                ", isRead=" + isRead +
                ", readTime=" + readTime +
                '}';
    }


    @Override
    public int compareTo(@NotNull Notification o) {
        return o.getCreateTime().compareTo(this.getCreateTime());
    }
}
