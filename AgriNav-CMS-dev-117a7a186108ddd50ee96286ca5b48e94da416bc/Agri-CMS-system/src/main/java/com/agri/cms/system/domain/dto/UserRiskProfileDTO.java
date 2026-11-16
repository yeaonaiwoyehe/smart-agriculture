package com.agri.cms.system.domain.dto;

import com.agri.cms.common.enums.RiskLevel;


/**
 * @author 李鸿麟
 * 用户风险DTO
 */
public class UserRiskProfileDTO {
    /** 用户名 */
    private String userName;

    /** 异常登录次数 */
    private int abnormalLoginCount;

    /** 唯一IP数量 */
    private int uniqueIpCount;

    /** 登录失败次数 */
    private int loginFailureCount;

    /** 非正常时间登录次数 */
    private int unusualTimeLoginCount;

    /** IP变化率 */
    private double ipChangingRate;

    /** 风险评分 */
    private int riskScore;

    /** 风险等级 */
    private RiskLevel riskLevel;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Integer getAbnormalLoginCount() {
        return abnormalLoginCount;
    }

    public void setAbnormalLoginCount(Integer abnormalLoginCount) {
        this.abnormalLoginCount = abnormalLoginCount;
    }

    public Integer getUniqueIpCount() {
        return uniqueIpCount;
    }

    public void setUniqueIpCount(Integer uniqueIpCount) {
        this.uniqueIpCount = uniqueIpCount;
    }

    public Integer getLoginFailureCount() {
        return loginFailureCount;
    }

    public void setLoginFailureCount(Integer loginFailureCount) {
        this.loginFailureCount = loginFailureCount;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public void setAbnormalLoginCount(int abnormalLoginCount) {
        this.abnormalLoginCount = abnormalLoginCount;
    }

    public void setUniqueIpCount(int uniqueIpCount) {
        this.uniqueIpCount = uniqueIpCount;
    }

    public void setLoginFailureCount(int loginFailureCount) {
        this.loginFailureCount = loginFailureCount;
    }

    public int getUnusualTimeLoginCount() {
        return unusualTimeLoginCount;
    }

    public void setUnusualTimeLoginCount(int unusualTimeLoginCount) {
        this.unusualTimeLoginCount = unusualTimeLoginCount;
    }

    public double getIpChangingRate() {
        return ipChangingRate;
    }

    public void setIpChangingRate(double ipChangingRate) {
        this.ipChangingRate = ipChangingRate;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
    }
}
