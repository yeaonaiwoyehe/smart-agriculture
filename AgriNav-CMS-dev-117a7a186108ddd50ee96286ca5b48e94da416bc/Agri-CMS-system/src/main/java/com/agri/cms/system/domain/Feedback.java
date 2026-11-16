package com.agri.cms.system.domain;

import com.agri.cms.common.core.domain.entity.SysDictData;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.annotation.JSONType;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

@TableName("sys_feedback")
public class Feedback {
    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String content;
    private String username;
    private String phoneNumber;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime commitTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime checkedTime;
    private Integer isChecked;
    private String checkedBy;

    private String reply;
    @TableField(exist = false)
    private List<SysDictData> labels;

    public String getId() {
        return id;
    }

    public Feedback() {
    }


    public String getReply() {
        return reply;
    }

    public Feedback setReply(String reply) {
        this.reply = reply;
        return this;
    }

    public Feedback setId(String id) {
        this.id = id;
        return this;
    }

    public String getContent() {
        return content;
    }

    public Feedback setContent(String content) {
        this.content = content;
        return this;
    }


    public String getUsername() {
        return username;
    }

    public Feedback setUsername(String username) {
        this.username = username;
        return this;
    }

    public List<SysDictData> getLabels() {
        return labels;
    }

    public Feedback setLabels(List<SysDictData> labels) {
        this.labels = labels;
        return this;
    }

    public LocalDateTime getCheckedTime() {
        return checkedTime;
    }

    public Feedback setCheckedTime(LocalDateTime checkedTime) {
        this.checkedTime = checkedTime;
        return this;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public Feedback setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        return this;
    }

    public LocalDateTime getCommitTime() {
        return commitTime;
    }

    public Feedback setCommitTime(LocalDateTime commitTime) {
        this.commitTime = commitTime;
        return this;
    }

    public Integer getIsChecked() {
        return isChecked;
    }

    public Feedback setIsChecked(Integer isChecked) {
        this.isChecked = isChecked;
        return this;
    }

    public String getCheckedBy() {
        return checkedBy;
    }

    public Feedback setCheckedBy(String checkedBy) {
        this.checkedBy = checkedBy;
        return this;
    }
}
