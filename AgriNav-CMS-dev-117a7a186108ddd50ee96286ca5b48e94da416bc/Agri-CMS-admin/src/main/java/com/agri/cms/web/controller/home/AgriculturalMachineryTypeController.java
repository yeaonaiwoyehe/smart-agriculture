package com.agri.cms.web.controller.home;

import cn.hutool.core.text.escape.XmlEscape;
import cn.hutool.core.util.ObjectUtil;
import com.agri.cms.common.constant.Constants;
import com.agri.cms.common.core.controller.BaseController;
import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.system.domain.AgriculturalMachineryType;
import com.agri.cms.system.service.AgriculturalMachineryTypeService;
import com.agri.cms.system.service.impl.SysUserServiceImpl;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.aspectj.bridge.IMessage;
import org.aspectj.weaver.loadtime.Aj;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * @author 姚锦伦
 */
@RestController
@RequestMapping("/agriculturalMachineryType")
@Tag(name = "农机类型")
public class AgriculturalMachineryTypeController extends BaseController {
    @Autowired
    private AgriculturalMachineryTypeService agriculturalMachineryTypeService;
    @Autowired
    private SysUserServiceImpl sysUserService;

    /**
     * 展示列表
     *
     * @param pageNum
     * @param pageSize
     * @return IPage<AgriculturalMachineryType>
     * @author 姚锦伦
     * @since 2024/10/22
     */
    @Operation(summary = "查询农机类型列表")
    @GetMapping("/list")
    // 农机类型筛选登录即可访问，放开权限（原 home:agriculturalMachineryType:list 在 sys_menu 中缺失）
    // @PreAuthorize("@ss.hasPermi('home:agriculturalMachineryType:list')")
    public AjaxResult list(@RequestParam(name = "pageNum", defaultValue = "1") int pageNum, @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
        LambdaUpdateWrapper<AgriculturalMachineryType> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AgriculturalMachineryType::getStatus, Constants.NOT_DELETED);
        IPage<AgriculturalMachineryType> page = new Page<>(pageNum, pageSize);
        IPage<AgriculturalMachineryType> result = agriculturalMachineryTypeService.page(page, wrapper);
        result.getRecords().forEach(item -> {
            item.setCreateBy(sysUserService.selectUserById(Long.parseLong(item.getCreateBy())).getUserName());
            if (ObjectUtil.isNotEmpty(item.getUpdateBy())){
                item.setUpdateBy(sysUserService.selectUserById(Long.parseLong(item.getUpdateBy())).getUserName());
            }
        });
        AjaxResult ajaxResult = AjaxResult.success();
        ajaxResult.put("data", result.getRecords())
                .put("total", result.getTotal());
        return ajaxResult;
    }

    /**
     * 新增农机类型
     *
     * @param agriculturalMachineryType
     * @return AjaxResult
     * @author 姚锦伦
     * @since 2024/10/22
     */
    @Operation(summary = "新增农机类型")
    @PostMapping("/add")
    @PreAuthorize("@ss.hasPermi('home:agriculturalMachineryType:add')")
    public AjaxResult add(@RequestBody AgriculturalMachineryType agriculturalMachineryType) {
        String userId = getLoginUser().getUserId().toString();
        agriculturalMachineryType.setCreateBy(userId);
        agriculturalMachineryType.setStatus(Constants.NOT_DELETED);
        agriculturalMachineryType.setCreateTime(LocalDateTime.now());
        agriculturalMachineryTypeService.save(agriculturalMachineryType);
        return AjaxResult.success();
    }

    /**
     * 更新农机类型
     *
     * @param agriculturalMachineryType
     * @return AjaxResult
     * @author 姚锦伦
     * @since 2024/10/22
     */
    @PutMapping
    @Operation(summary = "更新农机类型")
    @PreAuthorize("@ss.hasPermi('home:agriculturalMachineryType:update')")
    public AjaxResult update(@RequestBody AgriculturalMachineryType agriculturalMachineryType) {
        String userId = getLoginUser().getUserId().toString();
        LambdaUpdateWrapper<AgriculturalMachineryType> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AgriculturalMachineryType::getId, agriculturalMachineryType.getId())
                .set(AgriculturalMachineryType::getUpdateTime, LocalDateTime.now())
                .set(AgriculturalMachineryType::getUpdateBy, userId)
                .set(AgriculturalMachineryType::getStatus, agriculturalMachineryType.getStatus())
                .set(AgriculturalMachineryType::getName, agriculturalMachineryType.getName());
        agriculturalMachineryTypeService.update(wrapper);
        return AjaxResult.success();
    }

    /**
     * 删除农机类型
     *
     * @param id
     * @return AjaxResult
     * @author 姚锦伦
     * @since 2024/10/22
     */
    @DeleteMapping("{id}")
    @Operation(summary = "删除农机类型")
    @PreAuthorize("@ss.hasPermi('home:agriculturalMachineryType:delete')")
    public AjaxResult delete(@PathVariable(name = "id") String id) {
        LambdaUpdateWrapper<AgriculturalMachineryType> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AgriculturalMachineryType::getId, id)
                .set(AgriculturalMachineryType::getStatus, Constants.DELETED);
        agriculturalMachineryTypeService.update(wrapper);
        return AjaxResult.success();
    }
}
