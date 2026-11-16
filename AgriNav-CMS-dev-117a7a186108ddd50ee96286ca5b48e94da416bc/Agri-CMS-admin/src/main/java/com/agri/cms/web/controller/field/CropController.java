package com.agri.cms.web.controller.field;

import com.agri.cms.common.constant.Constants;
import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.system.domain.Crop;
import com.agri.cms.system.domain.FieldWork;
import com.agri.cms.system.service.impl.CropServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * @author 李鸿麟
 * @description: 作物Controller
 */

@Tag(name = "作物相关API")
@RestController
@RequestMapping("/crop")
public class CropController {
    @Resource
    CropServiceImpl cropService;


    /**
     * 获取作物列表
     *
     * @return List<Crop>
     * @author 李鸿麟
     * @since 2024/9/22
     */
    @GetMapping("/getCrop")
    @PreAuthorize("@ss.hasPermi('field:crop:check')")
    @Operation(summary = "查看数据库中可用的作物列表")
    public AjaxResult getCrops() {
        QueryWrapper<Crop> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(Constants.IS_STILL_AVAILABLE, Constants.IS_AVAILABLE);
        return AjaxResult.success(cropService.list(queryWrapper));
    }

    @PostMapping("/addCrop")
    @PreAuthorize("@ss.hasPermi('field:crop:add')")
    @Operation(summary = "添加新的作物")
    public AjaxResult addCrop(@RequestBody @NotNull Crop crop) {
        return cropService.addCrop(crop);
    }

    @DeleteMapping("/deleteCrop")
    @PreAuthorize("@ss.hasPermi('field:crop:delete')")
    @Operation(summary = "删除作物")
    public AjaxResult deleteCrop(@RequestParam @NotNull Long cropId) {
        return cropService.deleteCrop(cropId);
    }

    @PostMapping("/updateCrop")
    @PreAuthorize("@ss.hasPermi('field:crop:update')")
    @Operation(summary = "修改作物信息")
    public AjaxResult updateCrop(@RequestBody Crop crop) {
        return cropService.updateCrop(crop);
    }

}
