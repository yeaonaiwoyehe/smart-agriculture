package com.agri.cms.system.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 批量派单请求（A-19，MP-02）。
 * 多个农活一次性派给同一机手，使用统一时限。
 *
 * @author agrinav
 */
public class BatchDispatchRequest {

    /** 要批量派发的农活 ID 集合 */
    private List<String> farmWorkIds;

    /** 被派机手用户 ID */
    private Long assigneeId;

    /** 完成时限（统一截止时间） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deadline;

    /** 备注（可选） */
    private String remark;

    public List<String> getFarmWorkIds() {
        return farmWorkIds;
    }

    public void setFarmWorkIds(List<String> farmWorkIds) {
        this.farmWorkIds = farmWorkIds;
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
