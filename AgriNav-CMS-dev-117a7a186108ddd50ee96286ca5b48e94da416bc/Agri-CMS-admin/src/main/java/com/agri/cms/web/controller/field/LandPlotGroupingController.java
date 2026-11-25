package com.agri.cms.web.controller.field;

import com.agri.cms.common.core.controller.BaseController;
import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.system.service.grouping.LandPlotGroupingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/landPlot/groups")
@Tag(name="地块行政村与片区分组")
public class LandPlotGroupingController extends BaseController {
    private final LandPlotGroupingService service;
    public LandPlotGroupingController(LandPlotGroupingService service) { this.service=service; }

    @GetMapping
    @Operation(summary="团队分组字典及本人有效地块的分组统计")
    public AjaxResult summary(@RequestParam(defaultValue="VILLAGE") String type) {
        return success(service.summary(type,getUserId()));
    }
    @GetMapping("/assignments")
    @Operation(summary="查询指定本人地块的行政村与片区")
    public AjaxResult assignments(@RequestParam List<String> plotIds) {
        return success(service.assignments(plotIds,getUserId()));
    }
    @PostMapping
    @PreAuthorize("@ss.hasPermi('field:square:update')")
    @Operation(summary="创建行政村或片区字典项，相同编号不得使用不同名称")
    public AjaxResult create(@RequestBody GroupRequest body) {
        service.create(body.type(),body.code(),body.name(),getUserId());return success("分组已保存");
    }
    @PutMapping("/assignment")
    @PreAuthorize("@ss.hasPermi('field:square:update')")
    @Operation(summary="整批归组，groupCode为null时移出该类分组；只允许修改本人有效地块")
    public AjaxResult assign(@RequestBody AssignmentRequest body) {
        service.assign(body.type(),body.groupCode(),body.plotIds(),getUserId());return success("归组已更新");
    }
    public record GroupRequest(String type,String code,String name) { }
    public record AssignmentRequest(String type,String groupCode,List<String> plotIds) { }
}
