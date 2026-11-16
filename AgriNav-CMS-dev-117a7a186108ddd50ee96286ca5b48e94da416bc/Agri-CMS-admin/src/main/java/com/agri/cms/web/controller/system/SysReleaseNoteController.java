package com.agri.cms.web.controller.system;

import com.agri.cms.common.constant.Constants;
import com.agri.cms.common.utils.StringUtils;
import com.agri.cms.system.domain.ReleaseNote;
import com.agri.cms.system.service.ReleaseNoteService;
import com.agri.cms.common.core.controller.BaseController;
import com.agri.cms.common.core.domain.AjaxResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

/**
 * 发布说明控制器，用于管理软件发布说明信息
 * @author simon
 */
@PreAuthorize("@ss.hasRole('manager')")
@RestController
@RequestMapping("/system/releaseNote")
public class SysReleaseNoteController extends BaseController {

    @Resource
    private ReleaseNoteService releaseNoteService;

    /**
     * 添加发布说明信息
     * @param releaseNote 发布说明对象，包含要添加的发布说明信息
     * @return 返回操作结果的AjaxResult对象
     */
    @PostMapping
    public AjaxResult add(@RequestBody @Valid ReleaseNote releaseNote) {
        releaseNote.setCreatedBy(getUsername());
        return toAjax(releaseNoteService.save(releaseNote));
    }

    /**
     * 获取发布说明列表
     * @return 返回包含发布说明列表的AjaxResult对象
     */
    @GetMapping("/list")
    public AjaxResult list(@RequestParam(value = "version", required = false) String version ) {
        return success(StringUtils.isNotEmpty(version)?releaseNoteService.list(
                new LambdaQueryWrapper<ReleaseNote>()
                        .like(ReleaseNote::getVersion, version)
                        .eq(ReleaseNote::getDeleted, Constants.NOT_DELETED)
                        .orderByDesc(ReleaseNote::getCreatedAt)
        ):releaseNoteService.list()
        );
    }

    /**
     * 根据ID获取发布说明详细信息
     * @param id 发布说明的ID，用于查询特定的发布说明信息
     * @return 返回包含发布说明详细信息或错误信息的AjaxResult对象
     */
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable @NotBlank(message = "发布记录ID不能为空") String id) {
        ReleaseNote releaseNote = releaseNoteService.getDetailById(id);
        return releaseNote != null ? success(releaseNote) : error("找不到指定的发布记录");
    }

    /**
     * 编辑发布说明信息
     * @param releaseNote 发布说明对象，包含要更新的发布说明信息
     * @return 返回操作结果的AjaxResult对象
     */
    @PutMapping
    public AjaxResult edit(@RequestBody @Valid ReleaseNote releaseNote) {
        return toAjax(releaseNoteService.updateById(releaseNote));
    }

    /**
     * 删除发布说明信息
     * @param id 发布说明的ID，用于指定要删除的发布说明
     * @return 返回操作结果的AjaxResult对象
     */
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable @NotBlank(message = "发布记录ID不能为空") String id) {
        return toAjax(releaseNoteService.removeById(id));
    }
}
