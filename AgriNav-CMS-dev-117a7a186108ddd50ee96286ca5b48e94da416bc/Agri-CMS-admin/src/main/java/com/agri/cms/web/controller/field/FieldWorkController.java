package com.agri.cms.web.controller.field;

import com.agri.cms.common.constant.Constants;
import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.system.domain.FieldWork;
import com.agri.cms.system.domain.WorkType;
import com.agri.cms.system.service.WorkTypeService;
import com.agri.cms.system.service.impl.CropWorkServiceImpl;
import com.agri.cms.system.service.impl.FieldWorkServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author 李鸿麟
 * @description: 田地工作类型Controller
 */
@Tag(name = "田地工作类型相关API")
@RestController
@RequestMapping("/fieldWork")
public class FieldWorkController {
    @Resource
    FieldWorkServiceImpl fieldWorkService;
    @Resource
    WorkTypeService workTypeService;

    @Resource
    CropWorkServiceImpl cropWorkService;

    @GetMapping("/getWork")
    @PreAuthorize("@ss.hasPermi('field:work:check')")
    @Operation(summary = "查看数据库中可用的工作类型列表")
    public AjaxResult getFieldWorks(@RequestParam @NotNull Long cropId) {
        return fieldWorkService.getAvailableFieldWorks(cropId);
    }

    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('field:work:check')")
    @Operation(summary = "获取全部可用工作类型")
    public AjaxResult getWorkByCropId() {
        return AjaxResult.success(fieldWorkService.list());
    }


    @PostMapping("/addWork")
    @PreAuthorize(("@ss.hasPermi('field:work:add')"))
    @Operation(summary = "添加新的工作类型")
    public AjaxResult addWork(@RequestBody @NotNull FieldWork work, @RequestBody @Nullable Long cropId) {
        return fieldWorkService.addWork(work, cropId);
    }

    @DeleteMapping("/deleteWork")
    @PreAuthorize("@ss.hasPermi('field:work:delete')")
    @Operation(summary = "删除工作类型")
    public AjaxResult deleteWork(@RequestParam @NotNull Long workId) {
        return fieldWorkService.deleteWork(workId);
    }

    @PostMapping("/updateWork")
    @PreAuthorize("@ss.hasPermi('field:work:update')")
    @Operation(summary = "修改工作类型")
    public AjaxResult updateWork(@RequestBody @NotNull FieldWork work) {
        return fieldWorkService.updateWork(work);
    }

    @GetMapping("/listUnique")
    @Operation(summary = "查询所有唯一的工作类型")
    @PreAuthorize("@ss.hasPermi('field:work:check')")
    public AjaxResult listUniqueWorkType() {
        LambdaQueryWrapper<WorkType> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WorkType::getDelete, false);
        List<WorkType> result = workTypeService.list(queryWrapper);
        return AjaxResult.success(result);
    }
}
