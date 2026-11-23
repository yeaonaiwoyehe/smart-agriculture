package com.agri.cms.system.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 单选派单请求（A-19，MP-02）。
 *
 * @author agrinav
 */
public class DispatchRequest {

    /** 要派发的农活 ID（farm_work.id） */
    private String farmWorkId;

    /** 被派机手用户 ID */
    private Long assigneeId;

    /** 完成时限（截止时间） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deadline;

    /** 备注（可选） */
    private String remark;

    public String getFarmWorkId() {
        return farmWorkId;
    }

    public void setFarmWorkId(String farmWorkId) {
        this.farmWorkId = farmWorkId;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
