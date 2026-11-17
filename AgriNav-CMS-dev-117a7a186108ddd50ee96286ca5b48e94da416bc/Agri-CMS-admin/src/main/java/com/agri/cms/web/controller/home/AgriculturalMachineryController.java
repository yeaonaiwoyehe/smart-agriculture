package com.agri.cms.web.controller.home;


import com.agri.cms.common.core.controller.BaseController;
import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.system.domain.dto.AgriculturalMachineryDTO;
import com.agri.cms.system.service.AgriculturalMachineryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author 姚锦伦
 */
@RestController
@Tag(name = "农机找活")
@RequestMapping("/agriculturalMachinery")
public class AgriculturalMachineryController extends BaseController {
    @Autowired
    private AgriculturalMachineryService agriculturalMachineryService;

    private static final String BUCKET_NAME = "agricultural-machinery";

    /**
     * 插入农机找活信息
     *
     * @param agriculturalMachineryDTO
     * @param image
     * @return AjaxResult
     * @author 姚锦伦
     * @since 2024/10/18
     */
    @PostMapping("/add")
    @Operation(summary = "新增农机找活信息")
    @PreAuthorize("@ss.hasPermi('home:agriculturalMachinery:add')")
    public AjaxResult insert(@RequestParam("agriculturalMachinery") String agriculturalMachineryDTO, @RequestParam(required = false) List<MultipartFile> image) {
        return agriculturalMachineryService.add(agriculturalMachineryDTO,image,getLoginUser().getUserId().toString());
    }

    /**
     * 将该农机找活置为已下架
     *
     * @param id
     * @return AjaxResult
     * @author 姚锦伦
     * @since 2024/10/18
     */
    @DeleteMapping("{id}")
    @Operation(summary = "下架农机找活信息")
    @PreAuthorize("@ss.hasPermi('home:agriculturalMachinery:remove')")
    public AjaxResult delete(@PathVariable(name = "id") String id) {
        agriculturalMachineryService.delete(id);
        return AjaxResult.success();
    }

    /**
     * 查询农机找活的信息
     *
     * @param agriculturalMachineryDTO
     * @param image
     * @return AjaxResult
     * @author 姚锦伦
     * @since 2024/10/21
     */
    @PutMapping
    @Operation(summary = "更新农机找活信息")
    @PreAuthorize("@ss.hasPermi('home:agriculturalMachinery:update')")
    public AjaxResult update(@RequestParam("agriculturalMachinery") String agriculturalMachineryDTO, @RequestParam(required = false) List<MultipartFile> image) {
        return agriculturalMachineryService.updateAgriculturalMachinery(agriculturalMachineryDTO,image,getLoginUser().getUserId().toString());
    }

    @PostMapping("/list")
    // 首页农机列表属于登录即可浏览的公共信息，原 home:agriculturalMachinery:list 权限在 sys_menu 中缺失，普通用户会被 403 拦截，故放开查询
    // @PreAuthorize("@ss.hasPermi('home:agriculturalMachinery:list')")
    @Operation(summary = "查询农机找活信息")
    public AjaxResult list(@RequestBody AgriculturalMachineryDTO agriculturalMachineryDTO,
                           @RequestParam(defaultValue = "1") int pageNum,
                           @RequestParam(defaultValue = "6") int pageSize) {
        if (getLoginUser().getUser().isAdmin()) {
            agriculturalMachineryDTO.setMyAgriculturalMachinery(false);
        }
        return agriculturalMachineryService.list(agriculturalMachineryDTO, getLoginUser().getUserId().toString(), pageNum, pageSize);
    }

    @DeleteMapping("/signOut/{username}")
    @Operation(summary = "注销该用户的农机找活信息")
    @PreAuthorize("@ss.hasPermi('home:agriculturalMachinery:signout')")
    public AjaxResult deleteByUsername(@PathVariable String username) {
        agriculturalMachineryService.deleteByUsername(username);
        return AjaxResult.success();
    }
}
