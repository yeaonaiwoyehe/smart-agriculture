package com.agri.cms.web.controller.ai;

import com.agri.cms.ai.agent.PermissionAuditResult;
import com.agri.cms.ai.agent.PermissionAuditService;
import com.agri.cms.common.core.controller.BaseController;
import com.agri.cms.common.core.domain.AjaxResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 权限审计接口（A-20 MP-06）。
 *
 * 提供用户角色分配、角色数据范围配置与越权告警的只读审计，
 * 仅管理员可访问，不修改任何权限配置。
 *
 * @author agrinav
 */
@RestController
@RequestMapping("/permission/audit")
@Tag(name = "权限审计")
public class PermissionAuditController extends BaseController {

    private final PermissionAuditService permissionAuditService;

    public PermissionAuditController(PermissionAuditService permissionAuditService) {
        this.permissionAuditService = permissionAuditService;
    }

    @Operation(summary = "权限审计总览（角色分配/数据范围/越权告警）")
    @GetMapping
    @PreAuthorize("@ss.hasRole('admin')")
    public AjaxResult audit() {
        PermissionAuditResult result = permissionAuditService.audit();
        return AjaxResult.success(result);
    }
}
