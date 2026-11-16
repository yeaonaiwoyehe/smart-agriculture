package com.agri.cms.system.service;

import com.agri.cms.system.domain.UserAuthenticityResult;
import com.agri.cms.system.domain.dto.UserActivityProfileDTO;
import com.agri.cms.system.domain.dto.UserBehaviorAnalysisDTO;
import com.agri.cms.system.domain.dto.UserRiskProfileDTO;

import java.util.List;
import java.util.Map;

/**
 * @author 李鸿麟
 * 用户分析
 */
public interface IUserBehaviorAnalysisService {
    UserActivityProfileDTO analyzeUserActivity(String userName);
    UserRiskProfileDTO assessUserRisk(String userName);
    UserBehaviorAnalysisDTO analyzeAllUsersBehavior();
    List<UserActivityProfileDTO> getTopActiveUsers(int limit);
    List<UserRiskProfileDTO> getHighRiskUsers();
    UserAuthenticityResult analyzeUserAuthenticity(String userId);
    Map<String, UserAuthenticityResult> analyzeAllUsersAuthenticity();
}
