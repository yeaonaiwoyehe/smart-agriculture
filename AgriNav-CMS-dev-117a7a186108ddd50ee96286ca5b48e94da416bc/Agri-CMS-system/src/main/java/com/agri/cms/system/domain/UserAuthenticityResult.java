package com.agri.cms.system.domain;


import java.util.List;

/**
 * @author 李鸿麟
 * 用户真实性分析类
 */
public class UserAuthenticityResult {
    // 用户ID
    private String userId;
    // 圈地行为得分
    private double landMarkingScore;
    // 轨迹质量得分
    private double trajectoryScore;
    // 农活发布得分
    private double farmWorkScore;
    // 农机发布得分
    private double machineryScore;
    // 综合真实度得分
    private double overallScore;
    // 用户标签
    private List<String> userTags;
    // 用户类型（真实农户/非农户）
    private String userType;

    private String userName;
    private String phoneNumber;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public double getLandMarkingScore() {
        return landMarkingScore;
    }

    public void setLandMarkingScore(double landMarkingScore) {
        this.landMarkingScore = landMarkingScore;
    }

    public double getTrajectoryScore() {
        return trajectoryScore;
    }

    public void setTrajectoryScore(double trajectoryScore) {
        this.trajectoryScore = trajectoryScore;
    }

    public double getFarmWorkScore() {
        return farmWorkScore;
    }

    public void setFarmWorkScore(double farmWorkScore) {
        this.farmWorkScore = farmWorkScore;
    }

    public double getMachineryScore() {
        return machineryScore;
    }

    public void setMachineryScore(double machineryScore) {
        this.machineryScore = machineryScore;
    }

    public double getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(double overallScore) {
        this.overallScore = overallScore;
    }

    public List<String> getUserTags() {
        return userTags;
    }

    public void setUserTags(List<String> userTags) {
        this.userTags = userTags;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }
}
