package com.agri.cms.web.controller.closure;

import com.agri.cms.common.core.controller.BaseController;
import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.common.core.page.TableDataInfo;
import com.agri.cms.system.domain.ReviewRequest;
import com.agri.cms.system.service.ReviewRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 复核申请 Controller（机手异议 -> 队长初核 -> 管理员终审）
 */
@RestController
@RequestMapping("/reviewRequest")
@Tag(name = "复核管理")
public class ReviewRequestController extends BaseController {

    private final ReviewRequestService reviewRequestService;

    public ReviewRequestController(ReviewRequestService reviewRequestService) {
        this.reviewRequestService = reviewRequestService;
    }

    @Operation(summary = "查询复核申请列表")
    @GetMapping("/list")
    public TableDataInfo list(ReviewRequest query) {
        startPage();
        List<ReviewRequest> list = reviewRequestService.selectList(query);
        return getDataTable(list);
    }

    @Operation(summary = "查询复核申请详情")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(reviewRequestService.selectById(id));
    }

    @Operation(summary = "提交复核申请")
    @PostMapping
    public AjaxResult submit(@RequestBody ReviewRequest request) {
        reviewRequestService.submit(request);
        return success();
    }

    @Operation(summary = "队长初核")
    @PutMapping("/initial")
    @PreAuthorize("@ss.hasPermi('agri:reviewRequest:review')")
    public AjaxResult initialReview(@RequestParam Long id,
                                    @RequestParam boolean pass,
                                    @RequestParam(required = false) String comment) {
        reviewRequestService.initialReview(id, pass, comment, getUsername());
        return success();
    }

    @Operation(summary = "管理员终审")
    @PutMapping("/final")
    @PreAuthorize("@ss.hasPermi('agri:reviewRequest:review')")
    public AjaxResult finalReview(@RequestParam Long id,
                                  @RequestParam boolean pass,
                                  @RequestParam(required = false) String comment) {
        reviewRequestService.finalReview(id, pass, comment, getUsername());
        return success();
    }
}
