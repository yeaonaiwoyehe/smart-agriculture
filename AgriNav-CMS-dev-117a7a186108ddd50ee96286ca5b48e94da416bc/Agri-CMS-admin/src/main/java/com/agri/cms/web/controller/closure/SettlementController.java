package com.agri.cms.web.controller.closure;

import com.agri.cms.common.core.controller.BaseController;
import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.common.core.page.TableDataInfo;
import com.agri.cms.common.utils.poi.ExcelUtil;
import com.agri.cms.system.domain.Settlement;
import com.agri.cms.system.domain.vo.SettlementExportVO;
import com.agri.cms.system.service.SettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.util.List;

/**
 * 结算单 Controller
 */
@RestController
@RequestMapping("/settlement")
@Tag(name = "结算管理")
public class SettlementController extends BaseController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @Operation(summary = "查询结算单列表")
    @GetMapping("/list")
    public TableDataInfo list(Settlement query) {
        startPage();
        return getDataTable(settlementService.selectList(query));
    }

    @Operation(summary = "查询结算单详情")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(settlementService.selectById(id));
    }

    @Operation(summary = "查询结算单明细")
    @GetMapping("/items/{id}")
    public AjaxResult getItems(@PathVariable Long id) {
        return success(settlementService.selectItems(id));
    }

    @Operation(summary = "生成结算单")
    @PostMapping("/generate")
    @PreAuthorize("@ss.hasPermi('agri:settlement:generate')")
    public AjaxResult generate(@RequestParam Short periodType,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd,
                               @RequestParam String phoneNumber) {
        Settlement settlement = settlementService.generate(periodType, periodStart, periodEnd,
                phoneNumber, getUsername());
        return success(settlement);
    }

    @Operation(summary = "更新结算单状态")
    @PutMapping("/status")
    @PreAuthorize("@ss.hasPermi('agri:settlement:edit')")
    public AjaxResult updateStatus(@RequestParam Long id, @RequestParam Short status) {
        settlementService.updateStatus(id, status);
        return success();
    }

    @Operation(summary = "导出结算明细Excel")
    @PostMapping("/export/{id}")
    @PreAuthorize("@ss.hasPermi('agri:settlement:export')")
    public void export(@PathVariable Long id, HttpServletResponse response) {
        List<SettlementExportVO> data = settlementService.buildExportData(id);
        ExcelUtil<SettlementExportVO> util = new ExcelUtil<>(SettlementExportVO.class);
        util.exportExcel(response, data, "结算明细");
    }
}
