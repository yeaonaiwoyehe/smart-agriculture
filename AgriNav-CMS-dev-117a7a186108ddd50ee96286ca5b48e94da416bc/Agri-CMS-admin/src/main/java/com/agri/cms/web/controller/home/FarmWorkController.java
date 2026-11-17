package com.agri.cms.web.controller.home;


import com.agri.cms.common.core.controller.BaseController;
import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.system.domain.dto.FarmWorkDTO;
import com.agri.cms.system.service.FarmWorkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @author 姚锦伦
 */
@RestController
@RequestMapping("/farmWork")
@Tag(name = "寻找农机")
public class FarmWorkController extends BaseController {
    @Autowired
    private FarmWorkService farmWorkService;

    /**
     * 新增寻找农机
     *
     * @param farmWorkDTO
     * @return AjaxResult
     * @author 姚锦伦
     * @since 2024/10/21
     */
    @Operation(summary = "新增寻找农机")
    @PostMapping("/add")
    @PreAuthorize("@ss.hasPermi('home:farmWork:add')")
    public AjaxResult add(@Valid @RequestBody FarmWorkDTO farmWorkDTO) {
        //判断截止时间是否大于当前时间
        if (farmWorkDTO.getEndTime().isBefore(LocalDateTime.now())) {
            return AjaxResult.error("截止时间不得小于当前时间");
        }
        farmWorkService.insert(farmWorkDTO, getLoginUser().getUserId().toString());
        return AjaxResult.success();
    }

    /**
     * 下架寻找农机
     *
     * @param id
     * @return AjaxResult
     * @author 姚锦伦
     * @since 2024/10/21
     */
    @Operation(summary = "下架寻找农机")
    @DeleteMapping("{id}")
    @PreAuthorize("@ss.hasPermi('home:farmWork:delete')")
    public AjaxResult delete(@PathVariable String id) {
        farmWorkService.deleteById(id);
        return AjaxResult.success();
    }

    /**
     * 修改寻找农机
     *
     * @param farmWorkDTO
     * @return AjaxResult
     * @author 姚锦伦
     * @since 2024/10/21
     */
    @PutMapping
    @Operation(summary = "更新寻找农机")
    @PreAuthorize("@ss.hasPermi('home:farmWork:update')")
    public AjaxResult update(@Valid @RequestBody FarmWorkDTO farmWorkDTO) {
        //判断截止时间是否大于当前时间
        if (farmWorkDTO.getEndTime().isBefore(LocalDateTime.now())) {
            return AjaxResult.error("截止时间不得小于当前时间");
        }
        farmWorkService.update(farmWorkDTO, getLoginUser().getUserId().toString());
        return AjaxResult.success();
    }

    @Operation(summary = "查询寻找农机")
    @PostMapping("/list")
    // 首页农活列表属于登录即可浏览的公共信息，原 home:farmWork:list 权限在 sys_menu 中缺失，普通用户会被 403 拦截，故放开查询
    // @PreAuthorize("@ss.hasPermi('home:farmWork:list')")
    public AjaxResult list(@RequestBody FarmWorkDTO farmWorkDTO,
                           @RequestParam(defaultValue = "1") int pageNum,
                           @RequestParam(defaultValue = "6") int pageSize) {
        if (getLoginUser().getUser().isAdmin()) {
            farmWorkDTO.setMyFarmWork(false);
        }
        return farmWorkService.list(farmWorkDTO, getUserId().toString(), pageNum, pageSize);
    }

    @DeleteMapping("/signOut/{username}")
    @Operation(summary = "注销该用户的地块")
    @PreAuthorize("@ss.hasPermi('home:farmWork:signout')")
    public AjaxResult deleteByUsername(@PathVariable String username) {
        farmWorkService.deleteByUsername(username);
        return AjaxResult.success();
    }
}
