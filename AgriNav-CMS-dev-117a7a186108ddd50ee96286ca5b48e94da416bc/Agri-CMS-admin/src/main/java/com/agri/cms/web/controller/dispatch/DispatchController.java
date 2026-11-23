package com.agri.cms.web.controller.dispatch;

import com.agri.cms.common.core.controller.BaseController;
import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.common.core.page.TableDataInfo;
import com.agri.cms.system.domain.WorkDispatch;
import com.agri.cms.system.domain.dto.BatchDispatchRequest;
import com.agri.cms.system.domain.dto.DispatchRequest;
import com.agri.cms.system.service.DispatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 作业调度 Controller（A-19，MP-02）。
 *
 * 单选/批量派单、派单列表与详情、接单/完成/取消、超时标记与完成率统计。
 * 派单与取消为管理员操作；接单、完成由被派机手本人执行。
 *
 * @author agrinav
 */
@RestController
@RequestMapping("/dispatch")
@Tag(name = "作业调度派单")
public class DispatchController extends BaseController {

    private final DispatchService dispatchService;

    public DispatchController(DispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    @Operation(summary = "单选派单")
    @PostMapping
    @PreAuthorize("@ss.hasRole('admin')")
    public AjaxResult dispatchOne(@RequestBody DispatchRequest request) {
        WorkDispatch result = dispatchService.dispatchOne(request, String.valueOf(getUserId()));
        return AjaxResult.success("派单成功", result);
    }

    @Operation(summary = "批量派单")
    @PostMapping("/batch")
    @PreAuthorize("@ss.hasRole('admin')")
    public AjaxResult dispatchBatch(@RequestBody BatchDispatchRequest request) {
        List<WorkDispatch> result = dispatchService.dispatchBatch(request, String.valueOf(getUserId()));
        return AjaxResult.success("批量派单成功，共 " + result.size() + " 单", result);
    }

    @Operation(summary = "派单列表")
    @GetMapping("/list")
    public TableDataInfo list(WorkDispatch query) {
        startPage();
        List<WorkDispatch> list = dispatchService.selectList(query);
        return getDataTable(list);
    }

    @Operation(summary = "派单详情")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(dispatchService.selectById(id));
    }

    @Operation(summary = "机手接单")
    @PutMapping("/{id}/accept")
    public AjaxResult accept(@PathVariable Long id) {
        dispatchService.accept(id, String.valueOf(getUserId()));
        return success();
    }

    @Operation(summary = "完成派单")
    @PutMapping("/{id}/complete")
    public AjaxResult complete(@PathVariable Long id) {
        dispatchService.complete(id, String.valueOf(getUserId()));
        return success();
    }

    @Operation(summary = "取消派单")
    @PutMapping("/{id}/cancel")
    @PreAuthorize("@ss.hasRole('admin')")
    public AjaxResult cancel(@PathVariable Long id) {
        dispatchService.cancel(id, String.valueOf(getUserId()));
        return success();
    }

    @Operation(summary = "手动刷新超时标记")
    @PostMapping("/timeout")
    public AjaxResult markTimeout() {
        int count = dispatchService.markTimeout();
        return AjaxResult.success("已标记超时派单 " + count + " 条", count);
    }

    @Operation(summary = "完成率统计")
    @GetMapping("/stats")
    public AjaxResult stats() {
        return success(dispatchService.completionStats());
    }
}
