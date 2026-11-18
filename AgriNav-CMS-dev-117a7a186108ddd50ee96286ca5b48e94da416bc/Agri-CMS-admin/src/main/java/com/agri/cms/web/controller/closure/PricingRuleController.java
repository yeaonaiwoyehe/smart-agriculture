package com.agri.cms.web.controller.closure;

import com.agri.cms.common.core.controller.BaseController;
import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.common.core.page.TableDataInfo;
import com.agri.cms.system.domain.PricingRule;
import com.agri.cms.system.service.PricingRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 作业单价规则 Controller
 */
@RestController
@RequestMapping("/pricingRule")
@Tag(name = "作业单价规则")
public class PricingRuleController extends BaseController {

    private final PricingRuleService pricingRuleService;

    public PricingRuleController(PricingRuleService pricingRuleService) {
        this.pricingRuleService = pricingRuleService;
    }

    @Operation(summary = "查询单价规则列表")
    @GetMapping("/list")
    public TableDataInfo list(PricingRule query) {
        startPage();
        List<PricingRule> list = pricingRuleService.selectList(query);
        return getDataTable(list);
    }

    @Operation(summary = "查询单价规则详情")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(pricingRuleService.selectById(id));
    }

    @Operation(summary = "新增单价规则")
    @PostMapping
    @PreAuthorize("@ss.hasPermi('agri:pricingRule:add')")
    public AjaxResult add(@RequestBody PricingRule rule) {
        pricingRuleService.insert(rule, getUsername());
        return success();
    }

    @Operation(summary = "修改单价规则")
    @PutMapping
    @PreAuthorize("@ss.hasPermi('agri:pricingRule:edit')")
    public AjaxResult edit(@RequestBody PricingRule rule) {
        pricingRuleService.update(rule, getUsername());
        return success();
    }

    @Operation(summary = "删除单价规则")
    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPermi('agri:pricingRule:remove')")
    public AjaxResult remove(@PathVariable Long[] ids) {
        pricingRuleService.deleteByIds(ids);
        return success();
    }
}
