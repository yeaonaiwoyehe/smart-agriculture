package com.agri.cms.system.domain.vo;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;


public class SortMessage {
    /**
     * 选择的开始日期
     */

    private String localDate;

    /**
     * 选择的开始时间
     */

    private LocalTime beginTime;

    /**
     * 选择的结束时间
     */

    private LocalTime endTime;


    /**
     * 作业类型
     */

    private String workType;

    /**
     * 作业地址
     */

    private String address;

    /**
     * 填入的手机号
     */
    private String phone;

    public String getLocalDate() {
        return localDate;
    }

    public void setLocalDate(String localDate) {
        this.localDate = localDate;
    }

    public LocalTime getBeginTime() {
        return beginTime;
    }

    public void setBeginTime(LocalTime beginTime) {
        this.beginTime = beginTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getWorkType() {
        return workType;
    }

    public void setWorkType(String workType) {
        this.workType = workType;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }


    public Trajectory convertToTrajectory(SortMessage sortMessage) {

        Trajectory trajectory = new Trajectory();

        // 设置手机号
        trajectory.setPhoneNumber(sortMessage.getPhone());

        // 设置作业类型
        trajectory.setWorkType(sortMessage.getWorkType());

        // 设置位置
        trajectory.setLocation(sortMessage.getAddress());

        // 设置开始时间
        LocalDate startDate = LocalDate.parse(sortMessage.getLocalDate());
        System.out.println(startDate);
        LocalDateTime startDateTime = LocalDateTime.of(startDate, sortMessage.getBeginTime());
        trajectory.setStartTime(startDateTime);

        // 设置结束时间
        LocalDateTime endDateTime = LocalDateTime.of(startDate, sortMessage.getEndTime());
        trajectory.setEndTime(endDateTime);
        System.out.println(trajectory);

        return trajectory;
    }
}