package com.agri.cms.web.controller.field;


import com.agri.cms.common.constant.Constants;
import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.system.domain.LandAddress;
import com.agri.cms.system.service.impl.LandAddressServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * @author 李鸿麟
 * @description: 田地详细地址Controller
 */
@Tag(name = "田块详细地址相关API")
@RestController
@RequestMapping("/landAddress")
public class LandAddressController {

    @Resource
    private LandAddressServiceImpl landAddressService;

    @Operation(summary = "获取省份列表")
    @GetMapping("/getProvince")
    @PreAuthorize("@ss.hasPermi('field:address:check')")
    public AjaxResult getProvinces() {
        QueryWrapper<LandAddress> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(Constants.AREA_LEVEL, Constants.PROVINCE);
        return AjaxResult.success(landAddressService.list(queryWrapper));
    }

    @Operation(summary = "获取城市列表")
    @PostMapping("/getCity")
    @PreAuthorize("@ss.hasPermi('field:address:check')")
    public AjaxResult getCities(@RequestBody @NotNull LandAddress landAddress) {
        QueryWrapper<LandAddress> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(Constants.SENIOR_CODE, landAddress.getCode());
        queryWrapper.eq(Constants.AREA_LEVEL, Constants.CITY);
        return AjaxResult.success(landAddressService.list(queryWrapper));
    }

    @Operation(summary = "获取区县列表")
    @PostMapping("/getDistrict")
    @PreAuthorize("@ss.hasPermi('field:address:check')")
    public AjaxResult getDistricts(@RequestBody @NotNull LandAddress landAddress) {
        QueryWrapper<LandAddress> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(Constants.SENIOR_CODE, landAddress.getCode());
        queryWrapper.eq(Constants.AREA_LEVEL, Constants.DISTRICT);
        return AjaxResult.success(landAddressService.list(queryWrapper));
    }

    @Operation(summary = "获取乡镇列表")
    @PostMapping("/getVillage")
    @PreAuthorize("@ss.hasPermi('field:address:check')")
    public AjaxResult getVillages(@RequestBody @NotNull LandAddress landAddress) {
        QueryWrapper<LandAddress> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(Constants.SENIOR_CODE, landAddress.getCode());
        queryWrapper.eq(Constants.AREA_LEVEL, Constants.VILLAGE);
        return AjaxResult.success(landAddressService.list(queryWrapper));
    }

    @Operation(summary = "获取村镇列表")
    @PostMapping("/getHamlet")
    @PreAuthorize("@ss.hasPermi('field:address:check')")
    public AjaxResult getHamlets(@RequestBody @NotNull LandAddress landAddress) {
        QueryWrapper<LandAddress> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(Constants.SENIOR_CODE, landAddress.getCode());
        queryWrapper.eq(Constants.AREA_LEVEL, Constants.HAMLET);
        return AjaxResult.success(landAddressService.list(queryWrapper));
    }

}
