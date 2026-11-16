package com.agri.cms.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author : simon
 * @description :
 * @last : 2024-11-10 11:56
 * Copyright (c) 2024. 保留所有权利。
 * @since : 2024-11-10 11:56
 */
@TableName("sys_feedback_label")
public class SysFeedbackLabel {
    @TableId(type = IdType.ASSIGN_ID)
    private String id;
    private String feedbackId;
    @JsonProperty("dict_code")
    private Long labelCode;

    public SysFeedbackLabel() {
    }

    public SysFeedbackLabel(String feedbackId, Long labelCode) {
        this.feedbackId = feedbackId;
        this.labelCode = labelCode;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFeedbackId() {
        return feedbackId;
    }

    public void setFeedbackId(String feedbackId) {
        this.feedbackId = feedbackId;
    }

    public Long getLabelCode() {
        return labelCode;
    }

    public void setLabelCode(Long labelCode) {
        this.labelCode = labelCode;
    }
}
