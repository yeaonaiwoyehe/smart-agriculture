package com.agri.cms.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * @author : simon
 * @description :
 * @last : 2024-11-18 21:02
 * Copyright (c) 2024. 保留所有权利。
 * @since : 2024-11-18 21:02
 */
@TableName("sys_change_log")
public class ChangeLog {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    @NotBlank(message = "关联的发布记录ID不能为空")
    private String releaseNoteId;

    @NotBlank(message = "变更类型不能为空")
    @Size(max = 20, message = "变更类型长度不能超过20个字符")
    private String type;

    @NotBlank(message = "变更内容不能为空")
    @Size(max = 500, message = "变更内容长度不能超过500个字符")
    private String content;

    @NotNull(message = "排序号不能为空")
    private Integer sortOrder;

    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String deleted;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getReleaseNoteId() {
        return releaseNoteId;
    }

    public void setReleaseNoteId(String releaseNoteId) {
        this.releaseNoteId = releaseNoteId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getDeleted() {
        return deleted;
    }

    public void setDeleted(String deleted) {
        this.deleted = deleted;
    }
}
