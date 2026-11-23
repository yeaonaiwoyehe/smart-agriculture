package com.agri.cms.ai.tool;

import com.agri.cms.common.core.domain.model.LoginUser;
import com.agri.cms.common.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 智能体工具访问控制（A-09，权限过滤）。
 *
 * 基于当前登录用户的角色与权限，判断其能否调用某个工具，并过滤出该用户
 * 可见的工具清单。管理员（admin）拥有全部工具；普通用户只能使用非管理员
 * 专用、且满足权限标识的工具。任何越权调用都会被拒绝并在审计中留痕。
 *
 * @author agrinav
 */
@Component
public class ToolAccessControl {

    private static final Logger log = LoggerFactory.getLogger(ToolAccessControl.class);

    /**
     * 当前登录用户能否访问指定工具。
     */
    public boolean canAccess(AgentTool tool) {
        if (tool == null) {
            return false;
        }
        LoginUser loginUser = SecurityUtils.getLoginUser();

        // 管理员放行全部工具
        if (isAdministrator(loginUser)) {
            return true;
        }

        // 仅管理员工具，普通用户拒绝
        if (tool.isAdminOnly()) {
            return false;
        }

        // 声明了权限标识的，按权限校验；未声明则登录即可用
        String permission = tool.requiredPermission();
        if (permission == null || permission.isBlank()) {
            return true;
        }
        return SecurityUtils.hasPermi(loginUser.getPermissions(), permission);
    }

    /**
     * 过滤出当前用户可访问的工具，保持原顺序。
     */
    public List<AgentTool> accessibleTools(List<AgentTool> allTools) {
        if (allTools == null || allTools.isEmpty()) {
            return List.of();
        }
        return allTools.stream().filter(this::canAccess).toList();
    }

    /**
     * 返回工具被拒绝的原因，用于审计与用户提示。
     */
    public String denyReason(AgentTool tool) {
        if (tool != null && tool.isAdminOnly()) {
            return "该功能仅管理员可使用，您当前账号无权调用「" + tool.name() + "」。";
        }
        return "您没有调用工具「" + (tool == null ? "未知" : tool.name()) + "」的权限。";
    }

    private boolean isAdministrator(LoginUser loginUser) {
        if (loginUser == null || loginUser.getUserId() == null) {
            return false;
        }
        if (SecurityUtils.isAdmin(loginUser.getUserId())) {
            return true;
        }
        try {
            return SecurityUtils.hasRole("admin");
        } catch (Exception e) {
            log.debug("判断管理员角色时发生异常：{}", e.getMessage());
            return false;
        }
    }
}
