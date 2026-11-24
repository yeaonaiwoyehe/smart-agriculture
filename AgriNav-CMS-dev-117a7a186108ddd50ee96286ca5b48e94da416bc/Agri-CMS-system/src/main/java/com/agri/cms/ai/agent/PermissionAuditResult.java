package com.agri.cms.ai.agent;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 权限审计结果（A-20 MP-06）。
 *
 * 汇总用户角色分配、角色数据范围配置与越权告警，全部基于真实的
 * sys_role / sys_user_role 与智能体审计数据，只做审计分析，不修改权限配置。
 *
 * @param summary     审计摘要
 * @param userPermissions 用户权限分配
 * @param roles       角色及其数据范围
 * @param violations  越权告警
 * @param risks       风险提示
 * @author agrinav
 */
public record PermissionAuditResult(
        Summary summary,
        List<UserPermission> userPermissions,
        List<RoleView> roles,
        List<Violation> violations,
        List<String> risks
) {

    /**
     * 审计摘要。
     */
    public record Summary(
            long userCount,
            long roleCount,
            long usersWithoutRole,
            long violationCount
    ) {
    }

    /**
     * 用户权限分配。
     */
    public record UserPermission(
            Long userId,
            String userName,
            String nickName,
            Long roleId,
            String roleName,
            String roleKey,
            String dataScope,
            String dataScopeName,
            boolean validRole,
            String risk
    ) {
    }

    /**
     * 角色数据范围视图。
     */
    public record RoleView(
            Long roleId,
            String roleName,
            String roleKey,
            String dataScope,
            String dataScopeName,
            long userCount
    ) {
    }

    /**
     * 越权告警。
     */
    public record Violation(
            String userId,
            String userName,
            String toolName,
            String status,
            String detail,
            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createTime
    ) {
    }
}
