package com.agri.cms.web.controller.ai;

import com.agri.cms.ai.agent.AgentAlert;
import com.agri.cms.ai.agent.AlertService;
import com.agri.cms.ai.agent.AlertSubmitResult;
import com.agri.cms.common.core.controller.BaseController;
import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.common.core.page.TableDataInfo;
import com.agri.cms.common.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 异常工单接口（A-14，AG-04 异常上报）。
 *
 * 提供异常工单的分页查询、详情、提交与管理端处理。普通机手仅能查看本人工单，
 * 处理接口仅管理员可用。
 *
 * @author agrinav
 */
@RestController
@RequestMapping("/agent/alert")
@Tag(name = "异常工单")
public class AgentAlertController extends BaseController {

    private final AlertService alertService;

    public AgentAlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @Operation(summary = "分页查询异常工单（管理员全部/机手本人）")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(required = false) String status) {
        Long userId = SecurityUtils.getUserId();
        boolean isAdmin = SecurityUtils.isAdmin(userId);
        startPage();
        List<AgentAlert> list = isAdmin
                ? alertService.listAll(status)
                : alertService.listMyAlerts(String.valueOf(userId), status);
        return getDataTable(list);
    }

    @Operation(summary = "查询异常工单详情")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        Long userId = SecurityUtils.getUserId();
        AgentAlert alert = alertService.getForUser(id, userId, SecurityUtils.isAdmin(userId));
        if (alert == null) {
            return error("未找到该工单，或该工单不属于您。");
        }
        return success(alert);
    }

    @Operation(summary = "提交异常工单")
    @PostMapping
    public AjaxResult submit(@RequestBody AgentAlert body) {
        Long userId = SecurityUtils.getUserId();
        AlertSubmitResult result = alertService.submit(
                body.getAlertType(), body.getDescription(), body.getWorkOrderId(),
                body.getPlotId(), body.getLongitude(), body.getLatitude(),
                body.getOccurTime(), String.valueOf(userId),
                SecurityUtils.getUsername(),
                SecurityUtils.getLoginUser().getUser().getNickName());
        if (!result.success()) {
            return error(result.message());
        }
        return AjaxResult.success(result.message(), result);
    }

    @Operation(summary = "处理异常工单（管理员）")
    @PreAuthorize("@ss.hasRole('admin')")
    @PutMapping("/{id}/handle")
    public AjaxResult handle(@PathVariable Long id,
                             @RequestParam String status,
                             @RequestParam(required = false) String handleResult) {
        Long userId = SecurityUtils.getUserId();
        AgentAlert alert = alertService.handle(id, status, handleResult,
                String.valueOf(userId), SecurityUtils.getUsername());
        if (alert == null) {
            return error("未找到该工单。");
        }
        return success(alert);
    }
}
