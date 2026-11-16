package com.agri.cms.system.domain.dto;


import com.agri.cms.common.enums.ActivityLevel;

import java.sql.Date;

/**
 * @author 李鸿麟
 * 用户活跃度DTO
 */
public class UserActivityProfileDTO {
    private String userName;
    private Integer totalLoginCount;
    private Double loginSuccessRate;
    private Integer loginFrequencyScore;
    private Integer loginLocationDiversity;
    private Integer deviceDiversityScore;
    private ActivityLevel activityLevel;
    private Date latestLoginTime;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Integer getTotalLoginCount() {
        return totalLoginCount;
    }

    public void setTotalLoginCount(Integer totalLoginCount) {
        this.totalLoginCount = totalLoginCount;
    }

    public Double getLoginSuccessRate() {
        return loginSuccessRate;
    }

    public void setLoginSuccessRate(Double loginSuccessRate) {
        this.loginSuccessRate = loginSuccessRate;
    }

    public Integer getLoginFrequencyScore() {
        return loginFrequencyScore;
    }

    public void setLoginFrequencyScore(Integer loginFrequencyScore) {
        this.loginFrequencyScore = loginFrequencyScore;
    }

    public Integer getLoginLocationDiversity() {
        return loginLocationDiversity;
    }

    public void setLoginLocationDiversity(Integer loginLocationDiversity) {
        this.loginLocationDiversity = loginLocationDiversity;
    }

    public Integer getDeviceDiversityScore() {
        return deviceDiversityScore;
    }

    public void setDeviceDiversityScore(Integer deviceDiversityScore) {
        this.deviceDiversityScore = deviceDiversityScore;
    }

    public ActivityLevel getActivityLevel() {
        return activityLevel;
    }

    public void setActivityLevel(ActivityLevel activityLevel) {
        this.activityLevel = activityLevel;
    }

    public Date getLatestLoginTime() {
        return latestLoginTime;
    }

    public void setLatestLoginTime(Date latestLoginTime) {
        this.latestLoginTime = latestLoginTime;
    }
}
