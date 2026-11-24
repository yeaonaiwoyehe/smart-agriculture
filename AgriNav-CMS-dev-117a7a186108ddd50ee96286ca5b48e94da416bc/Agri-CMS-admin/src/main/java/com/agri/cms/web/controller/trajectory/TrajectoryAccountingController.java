package com.agri.cms.web.controller.trajectory;

import com.agri.cms.common.core.controller.BaseController;
import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.common.utils.SecurityUtils;
import com.agri.cms.system.service.accounting.AccountingModels.PreviewRequest;
import com.agri.cms.system.service.accounting.TrajectoryAccountingService;
import com.agri.cms.system.service.boundary.BoundaryQueryService.Actor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/trajectory/accounting")
@Tag(name = "B-10 幅宽面积核算")
public class TrajectoryAccountingController extends BaseController {
    private final TrajectoryAccountingService service;
    public TrajectoryAccountingController(TrajectoryAccountingService service) { this.service = service; }
    private Actor actor() {
        return new Actor(getUserId(), getUsername(), SecurityUtils.isAdmin(getUserId()) || SecurityUtils.hasRole("admin"));
    }
    @PostMapping("/preview")
    @Operation(summary = "按地块、厘米幅宽及分段轨迹只读试算面积，返回去重与裁剪扣减明细")
    public AjaxResult preview(@RequestBody PreviewRequest body) { return success(service.preview(body, actor())); }
    @GetMapping
    @Operation(summary = "按作业单已记录的幅宽及归档轨迹只读重算，不回填历史面积")
    public AjaxResult archived(@RequestParam String trajectoryId) { return success(service.archived(trajectoryId, actor())); }
}
