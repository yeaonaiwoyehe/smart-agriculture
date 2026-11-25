package com.agri.cms.web.controller.system;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.agri.cms.common.constant.Constants;
import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.system.domain.AppPermission;
import com.agri.cms.system.service.IAppPermissionService;
import com.agri.cms.system.service.ISysRoleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * @author 李鸿麟
 * 本Controller用于给安卓端返回组件的可见性信息
 */
@RestController
@RequestMapping("/permission")
@Tag(name = "APP权限管理")
public class SysPermissionController {

    @Resource
    private IAppPermissionService appPermissionService;

    /**
     * 该方法返回当前APP内可以看到的组件
     * @return AjaxResult
     * @author 李鸿麟
     * @since 2024/12/11     */
    @GetMapping("/check")
    @PreAuthorize("@ss.hasPermi('app:permission:check')")
    @Operation(summary = "返回当前的所有组件")
    public AjaxResult check(){
        return AjaxResult.success(appPermissionService.checkPermission());
    }

    @GetMapping("/list")
    //@PreAuthorize("@ss.hasPermi('app:permission:list')")
    @Operation(summary = "返回所有的组件")
    public AjaxResult list() {
        LambdaQueryWrapper<AppPermission> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.ne(AppPermission::getIsDeleted, Constants.DELETED);
//        lambdaQueryWrapper.ne(AppPermission::getIsVisible, Constants.INVISIBLE);
        return AjaxResult.success(appPermissionService.list(lambdaQueryWrapper));
    }

    @GetMapping("/get/{id}")
    //@PreAuthorize("@ss.hasPermi('app:permission:get')")
    @Operation(summary = "根据id获取组件")
    public AjaxResult get(@PathVariable("id") Long id) {
        return AjaxResult.success(appPermissionService.getComponentById(id));
    }

    /**
     * 此方法用于更新组件的可见性
     * @param permission 要修改的组件对象
     * @return AjaxResult
     * @author 李鸿麟
     * @since 2024/12/11     */
    @PostMapping("/update")
    //@PreAuthorize("@ss.hasPermi('app:permission:update')")
    @Operation(summary = "更新组件是否可见")
    public AjaxResult update(@RequestBody @NotNull AppPermission permission) {
        int i = appPermissionService.updatePermission(permission);
        if (i == 0) {
            return AjaxResult.error("操作失败");
        }
        return AjaxResult.success("操作成功");
    }


    @PostMapping("/add")
    //@PreAuthorize("@ss.hasPermi('app:permission:add')")
    @Operation(summary = "添加组件")
    public AjaxResult add(@RequestBody @NotNull AppPermission permission) {
        int i = appPermissionService.addPermission(permission);
        if (i == 0) {
            return AjaxResult.error("操作失败");
        }
        return AjaxResult.success("操作成功");
    }

    @DeleteMapping("/delete")
    //@PreAuthorize("@ss.hasPermi('app:permission:delete')")
    @Operation(summary = "删除组件")
    public AjaxResult delete(@RequestBody @NotNull AppPermission permission) {
        int i = appPermissionService.deletePermission(permission);
        if (i == 0) {
            return AjaxResult.error("操作失败");
        }
        return AjaxResult.success("操作成功");
    }

}
