package com.agri.cms.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.time.LocalDateTime;

/**
 * 作业调度派单记录（对应表 work_dispatch，A-19，MP-02）。
 *
 * 记录管理员对农活的单选/批量派单、指定机手与时限，以及待接单、进行中、
 * 已完成、已超时、已取消的全流程状态，用于调度页跟踪与超时标黄。
 *
 * @author agrinav
 */
@TableName("work_dispatch")
public class WorkDispatch {

    /** 状态机：已派单待接单 / 已接单进行中 / 已完成 / 已超时 / 已取消 */
    public static final String STATUS_DISPATCHED = "DISPATCHED";
    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_TIMEOUT = "TIMEOUT";
    public static final String STATUS_CANCELLED = "CANCELLED";

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String dispatchNo;

    private String farmWorkId;

    private String farmWorkName;

    private Long plotId;

    private String workType;

    private String assigneeId;

    private String assigneeName;

    private String assigneeNick;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deadline;

    private String status;

    private String dispatchBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dispatchTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime acceptTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completeTime;

    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDispatchNo() {
        return dispatchNo;
    }

    public void setDispatchNo(String dispatchNo) {
        this.dispatchNo = dispatchNo;
    }

    public String getFarmWorkId() {
        return farmWorkId;
    }

    public void setFarmWorkId(String farmWorkId) {
        this.farmWorkId = farmWorkId;
    }

    public String getFarmWorkName() {
        return farmWorkName;
    }

    public void setFarmWorkName(String farmWorkName) {
        this.farmWorkName = farmWorkName;
    }

    public Long getPlotId() {
        return plotId;
    }

    public void setPlotId(Long plotId) {
        this.plotId = plotId;
    }

    public String getWorkType() {
        return workType;
    }

    public void setWorkType(String workType) {
        this.workType = workType;
    }

    public String getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(String assigneeId) {
        this.assigneeId = assigneeId;
    }

    public String getAssigneeName() {
        return assigneeName;
    }

    public void setAssigneeName(String assigneeName) {
        this.assigneeName = assigneeName;
    }

    public String getAssigneeNick() {
        return assigneeNick;
    }

    public void setAssigneeNick(String assigneeNick) {
        this.assigneeNick = assigneeNick;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDispatchBy() {
        return dispatchBy;
    }

    public void setDispatchBy(String dispatchBy) {
        this.dispatchBy = dispatchBy;
    }

    public LocalDateTime getDispatchTime() {
        return dispatchTime;
    }

    public void setDispatchTime(LocalDateTime dispatchTime) {
        this.dispatchTime = dispatchTime;
    }

    public LocalDateTime getAcceptTime() {
        return acceptTime;
    }

    public void setAcceptTime(LocalDateTime acceptTime) {
        this.acceptTime = acceptTime;
    }

    public LocalDateTime getCompleteTime() {
        return completeTime;
    }

    public void setCompleteTime(LocalDateTime completeTime) {
        this.completeTime = completeTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
