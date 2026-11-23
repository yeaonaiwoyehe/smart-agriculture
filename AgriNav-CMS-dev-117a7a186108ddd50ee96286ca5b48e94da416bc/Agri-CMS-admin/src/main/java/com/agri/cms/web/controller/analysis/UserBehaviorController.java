package com.agri.cms.web.controller.analysis;
import io.swagger.v3.oas.annotations.tags.Tag;


import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.system.domain.dto.UserActivityProfileDTO;
import com.agri.cms.system.domain.dto.UserBehaviorAnalysisDTO;
import com.agri.cms.system.domain.dto.UserRiskProfileDTO;
import com.agri.cms.system.mapper.AgriculturalMachineryInfoMapper;
import com.agri.cms.system.mapper.FarmWorkInfoMapper;
import com.agri.cms.system.mapper.LandPlotMapper;
import com.agri.cms.system.mapper.TrajectoryInfoMapper;
import com.agri.cms.system.service.IUserBehaviorAnalysisService;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author 李鸿麟
 */
@RestController
@RequestMapping("/analysis/user")
@Tag(name = "用户行为分析")
public class UserBehaviorController {
    @Resource
    private IUserBehaviorAnalysisService userBehaviorAnalysisService;

    /**
     * 此方法用于返回某个用户的活跃度信息
     * @param userName 用户名
     * @return AjaxResult
     * @author 李鸿麟
     * @since 2024/12/17     */
    @GetMapping("/one")
    public AjaxResult analyzeUserActivity(@Nonnull @RequestParam String userName) {
        return AjaxResult.success(userBehaviorAnalysisService.analyzeUserActivity(userName));
    }

    /**
     * 此方法用于返回所有用户的行为分析档案
     * @return AjaxResult
     * @author 李鸿麟
     * @since 2024/12/17     */
    @GetMapping("/overview")
    public AjaxResult getOverview() {
        return AjaxResult.success(userBehaviorAnalysisService.analyzeAllUsersBehavior());
    }

    /**
     * 此方法用于返回前N个活跃用户信息
     * @return AjaxResult
     * @author 李鸿麟
     * @since 2024/12/17     */
    @GetMapping("/top-active")
    public AjaxResult getTopActiveUsers() {
        return AjaxResult.success(userBehaviorAnalysisService.getTopActiveUsers(10));
    }

    /**
     * 此方法用于返回高风险用户信息
     * @return AjaxResult
     * @author 李鸿麟
     * @since 2024/12/17     */
    @GetMapping("/high-risk")
    public AjaxResult getHighRiskUsers() {
        return AjaxResult.success(userBehaviorAnalysisService.getHighRiskUsers());
    }

    /**
     * 此方法用于返回某个用户的用户画像
     * @param userId 用户ID
     * @return AjaxResult
     * @author 李鸿麟
     * @since 2024/12/17     */
    @GetMapping("/authenticity")
    public AjaxResult analyzeOneUserAuthenticity(@RequestParam String userId) {
        return AjaxResult.success(userBehaviorAnalysisService.analyzeUserAuthenticity(userId));
    }

    /**
     * 此方法用于返回所有用户的用户画像
     * @return AjaxResult
     * @author 李鸿麟
     * @since 2024/12/17     */
    @GetMapping("/authenticity/all")
    public AjaxResult analyzeAllUsersAuthenticity() {
        return AjaxResult.success(userBehaviorAnalysisService.analyzeAllUsersAuthenticity());
    }
}
