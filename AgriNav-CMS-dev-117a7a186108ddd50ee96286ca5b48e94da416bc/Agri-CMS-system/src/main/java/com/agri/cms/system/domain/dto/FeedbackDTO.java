package com.agri.cms.system.domain.dto;

import com.agri.cms.common.core.domain.entity.SysDictData;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author : simon
 * @description :
 * @last : 2024-11-10 11:48
 * Copyright (c) 2024. 保留所有权利。
 * @since : 2024-11-10 11:48
 */
public class FeedbackDTO {
    private String id;
    private String content;
    private String username;
    private String phoneNumber;
    private Integer isChecked;
    private String checkedBy;

    private List<SysDictData> labels;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startCommitTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endCommitTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startCheckedTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endCheckedTime;


    public List<SysDictData> getLabels() {
        return labels;
    }

    public void setLabels(List<SysDictData> labels) {
        this.labels = labels;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Integer getIsChecked() {
        return isChecked;
    }

    public void setIsChecked(Integer isChecked) {
        this.isChecked = isChecked;
    }

    public String getCheckedBy() {
        return checkedBy;
    }

    public void setCheckedBy(String checkedBy) {
        this.checkedBy = checkedBy;
    }

    public LocalDateTime getStartCommitTime() {
        return startCommitTime;
    }

    public void setStartCommitTime(LocalDateTime startCommitTime) {
        this.startCommitTime = startCommitTime;
    }

    public LocalDateTime getEndCommitTime() {
        return endCommitTime;
    }

    public void setEndCommitTime(LocalDateTime endCommitTime) {
        this.endCommitTime = endCommitTime;
    }

    public LocalDateTime getStartCheckedTime() {
        return startCheckedTime;
    }

    public void setStartCheckedTime(LocalDateTime startCheckedTime) {
        this.startCheckedTime = startCheckedTime;
    }

    public LocalDateTime getEndCheckedTime() {
        return endCheckedTime;
    }

    public void setEndCheckedTime(LocalDateTime endCheckedTime) {
        this.endCheckedTime = endCheckedTime;
    }
}
