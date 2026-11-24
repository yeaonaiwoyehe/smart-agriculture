package com.agri.cms.ai.agent;

import com.agri.cms.ai.audit.AgentAuditLog;
import com.agri.cms.ai.audit.mapper.AgentAuditLogMapper;
import com.agri.cms.common.core.domain.entity.SysRole;
import com.agri.cms.common.core.domain.entity.SysUser;
import com.agri.cms.system.mapper.SysRoleMapper;
import com.agri.cms.system.mapper.SysUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 权限审计服务（A-20 MP-06）。
 *
 * 组装用户角色分配、角色数据范围与越权告警：从智能体审计记录中识别
 * 权限类拒绝，检测无有效角色的用户。只做审计分析，不修改权限配置。
 *
 * @author agrinav
 */
@Service
public class PermissionAuditService {

    private static final Logger log = LoggerFactory.getLogger(PermissionAuditService.class);

    /** 越权类关键词，用于在审计文本中识别权限拒绝 */
    private static final String[] DENY_KEYWORDS = {"无权", "不属于您", "没有权限", "权限不足"};

    private final SysRoleMapper roleMapper;
    private final SysUserMapper userMapper;
    private final AgentAuditLogMapper auditLogMapper;

    public PermissionAuditService(SysRoleMapper roleMapper,
                                  SysUserMapper userMapper,
                                  AgentAuditLogMapper auditLogMapper) {
        this.roleMapper = roleMapper;
        this.userMapper = userMapper;
        this.auditLogMapper = auditLogMapper;
    }

    /**
     * 生成权限审计结果。
     */
    public PermissionAuditResult audit() {
        List<SysUser> users = userMapper.selectUserList(new SysUser());
        List<SysRole> roles = roleMapper.selectRoleList(new SysRole());

        Map<Long, Long> roleUserCount = new HashMap<>();
        List<PermissionAuditResult.UserPermission> userPermissions = new ArrayList<>();
        long usersWithoutRole = 0;

        for (SysUser user : users) {
            List<SysRole> userRoles = roleMapper.selectRolePermissionByUserId(user.getUserId());
            if (userRoles == null || userRoles.isEmpty()) {
                usersWithoutRole++;
                userPermissions.add(new PermissionAuditResult.UserPermission(
                        user.getUserId(), user.getUserName(), user.getNickName(),
                        null, null, null, null, null, false,
                        "无有效角色，可能存在失效的角色关联，登录后仅具备基础能力。"));
                continue;
            }
            SysRole role = userRoles.get(0);
            roleUserCount.merge(role.getRoleId(), 1L, Long::sum);
            userPermissions.add(new PermissionAuditResult.UserPermission(
                    user.getUserId(), user.getUserName(), user.getNickName(),
                    role.getRoleId(), role.getRoleName(), role.getRoleKey(),
                    role.getDataScope(), scopeName(role.getDataScope()), true,
                    "角色配置正常。"));
        }

        List<PermissionAuditResult.RoleView> roleViews = new ArrayList<>();
        for (SysRole role : roles) {
            roleViews.add(new PermissionAuditResult.RoleView(
                    role.getRoleId(), role.getRoleName(), role.getRoleKey(),
                    role.getDataScope(), scopeName(role.getDataScope()),
                    roleUserCount.getOrDefault(role.getRoleId(), 0L)));
        }

        List<PermissionAuditResult.Violation> violations = buildViolations();
        List<String> risks = buildRisks(usersWithoutRole, violations.size());

        PermissionAuditResult.Summary summary = new PermissionAuditResult.Summary(
                users.size(), roles.size(), usersWithoutRole, violations.size());

        return new PermissionAuditResult(summary, userPermissions, roleViews,
                violations, risks);
    }

    /**
     * 从审计记录中识别越权拒绝。
     */
    private List<PermissionAuditResult.Violation> buildViolations() {
        List<AgentAuditLog> all = auditLogMapper.selectList(null);
        List<PermissionAuditResult.Violation> result = new ArrayList<>();

        for (AgentAuditLog logEntry : all) {
            String text = safe(logEntry.getErrorMsg()) + " " + safe(logEntry.getReply());
            if (containsDenyKeyword(text)) {
                result.add(new PermissionAuditResult.Violation(
                        logEntry.getUserId(), logEntry.getUserName(),
                        logEntry.getToolName(), logEntry.getStatus(),
                        firstNonBlank(logEntry.getErrorMsg(), logEntry.getReply()),
                        logEntry.getCreateTime()));
            }
        }
        return result;
    }

    /**
     * 风险提示。
     */
    private List<String> buildRisks(long usersWithoutRole, long violations) {
        List<String> risks = new ArrayList<>();
        if (usersWithoutRole > 0) {
            risks.add("存在 " + usersWithoutRole + " 个无有效角色的用户，建议核对并补授角色或清理失效关联。");
        }
        if (violations > 0) {
            risks.add("检测到 " + violations + " 次越权访问尝试，均已被拦截，建议关注相关账号行为。");
        }
        risks.add("本审计仅提供只读分析，不修改任何角色、数据范围或用户权限配置。");
        return risks;
    }

    private boolean containsDenyKeyword(String text) {
        for (String keyword : DENY_KEYWORDS) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String scopeName(String dataScope) {
        if (dataScope == null) {
            return "未知";
        }
        return switch (dataScope) {
            case "1" -> "全部数据";
            case "2" -> "自定义数据";
            case "3" -> "本部门数据";
            case "4" -> "本部门及以下数据";
            case "5" -> "仅本人数据";
            default -> "未知";
        };
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String firstNonBlank(String a, String b) {
        return (a != null && !a.isBlank()) ? a : b;
    }
}
