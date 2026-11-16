package com.agri.cms.system.domain.dto;

import com.agri.cms.common.enums.ActivityLevel;
import com.agri.cms.common.enums.RiskLevel;

import java.util.List;
import java.util.Map;

/**
 * @author 李鸿麟
 * 用户批量分析DTO
 */
public class UserBehaviorAnalysisDTO {
    private Map<ActivityLevel, List<UserActivityProfileDTO>> userActivityProfiles;
    private Map<RiskLevel, List<UserRiskProfileDTO>> userRiskProfiles;
    private Map<String, Integer> activityLevelDistribution;
    private Map<String, Integer> riskLevelDistribution;
    private List<String> highRiskUsers;
    private List<String> mostActiveUsers;

    public Map<ActivityLevel, List<UserActivityProfileDTO>> getUserActivityProfiles() {
        return userActivityProfiles;
    }

    public void setUserActivityProfiles(Map<ActivityLevel, List<UserActivityProfileDTO>> userActivityProfiles) {
        this.userActivityProfiles = userActivityProfiles;
    }

    public Map<RiskLevel, List<UserRiskProfileDTO>> getUserRiskProfiles() {
        return userRiskProfiles;
    }

    public void setUserRiskProfiles(Map<RiskLevel, List<UserRiskProfileDTO>> userRiskProfiles) {
        this.userRiskProfiles = userRiskProfiles;
    }

    public Map<String, Integer> getActivityLevelDistribution() {
        return activityLevelDistribution;
    }

    public void setActivityLevelDistribution(Map<String, Integer> activityLevelDistribution) {
        this.activityLevelDistribution = activityLevelDistribution;
    }

    public Map<String, Integer> getRiskLevelDistribution() {
        return riskLevelDistribution;
    }

    public void setRiskLevelDistribution(Map<String, Integer> riskLevelDistribution) {
        this.riskLevelDistribution = riskLevelDistribution;
    }

    public List<String> getHighRiskUsers() {
        return highRiskUsers;
    }

    public void setHighRiskUsers(List<String> highRiskUsers) {
        this.highRiskUsers = highRiskUsers;
    }

    public List<String> getMostActiveUsers() {
        return mostActiveUsers;
    }

    public void setMostActiveUsers(List<String> mostActiveUsers) {
        this.mostActiveUsers = mostActiveUsers;
    }
}
