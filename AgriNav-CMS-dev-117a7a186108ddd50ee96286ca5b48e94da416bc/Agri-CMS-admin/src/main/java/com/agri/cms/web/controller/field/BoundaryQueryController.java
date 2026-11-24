package com.agri.cms.web.controller.field;
import com.agri.cms.common.core.controller.BaseController;
import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.common.utils.SecurityUtils;
import com.agri.cms.system.domain.vo.Point;
import com.agri.cms.system.service.boundary.BoundaryQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/landPlot/boundary")
@Tag(name="B-09 地块边界判断与裁剪")
public class BoundaryQueryController extends BaseController {
    private final BoundaryQueryService service;
    public BoundaryQueryController(BoundaryQueryService service) {this.service=service;}
    private BoundaryQueryService.Actor actor() {return new BoundaryQueryService.Actor(getUserId(),getUsername(),SecurityUtils.isAdmin(getUserId())||SecurityUtils.hasRole("admin"));}
    @GetMapping("/check")
    @Operation(summary="只读定位判断：INSIDE、BOUNDARY、OUTSIDE、EXCLUDED、UNAVAILABLE")
    public AjaxResult check(@RequestParam Long plotId,@RequestParam Double longitude,@RequestParam Double latitude) {
        return success(service.check(plotId,longitude,latitude,actor()));
    }
    public record ClipRequest(Long plotId,List<Point> points) { }
    @PostMapping("/clip")
    @Operation(summary="只读轨迹裁剪，返回地块内可作业线段及米制长度，不写入核算结果")
    public AjaxResult clip(@RequestBody ClipRequest body) {return success(service.clip(body.plotId(),body.points(),actor()));}
}
