package com.agri.cms.web.controller.system;

import com.agri.cms.common.annotation.Anonymous;
import com.agri.cms.common.core.controller.BaseController;
import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.system.domain.ChangeLog;
import com.agri.cms.system.domain.ReleaseNote;
import com.agri.cms.system.service.ChangeLogService;
import com.agri.cms.system.service.ReleaseNoteService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @author : simon
 * @description : 更新日志
 * @last : 2024-11-17 21:05
 * Copyright (c) 2024. 保留所有权利。
 * @since : 2024-11-17 21:05
 */
@Validated
@RestController
@RequestMapping("/system/changelog")
public class SysChangeLogController extends BaseController {

    @Resource
    private ChangeLogService changeLogService;

    @Resource
    private ReleaseNoteService releaseNoteService;

//    @PreAuthorize("@ss.hasPermi('system:changelog:add')")
    @PostMapping
    public AjaxResult add(@Valid @RequestBody ChangeLog changeLog) {
        // 验证关联的ReleaseNote是否存在
        ReleaseNote releaseNote = releaseNoteService.getById(changeLog.getReleaseNoteId());
        if (releaseNote == null) {
            return error("关联的发布记录不存在");
        }
        changeLog.setCreatedBy(getUsername());
        return toAjax(changeLogService.save(changeLog));
    }

//    @PreAuthorize("@ss.hasPermi('system:changelog:list')")
    @GetMapping("/list/{releaseNoteId}")
    public AjaxResult list(@NotBlank(message = "发布记录ID不能为空") 
                          @PathVariable String releaseNoteId) {
        // 验证ReleaseNote是否存在
        if (releaseNoteService.getById(releaseNoteId) == null) {
            return error("发布记录不存在");
        }
        return success(changeLogService.list(new LambdaQueryWrapper<ChangeLog>()
                .eq(ChangeLog::getReleaseNoteId, releaseNoteId)));
    }

//    @PreAuthorize("@ss.hasPermi('system:changelog:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@NotBlank(message = "变更日志ID不能为空") 
                             @PathVariable String id) {
        return success(changeLogService.getById(id));
    }

//    @PreAuthorize("@ss.hasPermi('system:changelog:edit')")
    @PutMapping
    public AjaxResult edit(@Valid @RequestBody ChangeLog changeLog) {
        // 验证ID是否存在
        if (changeLog.getId() == null) {
            return error("变更日志ID不能为空");
        }
        
        // 验证关联的ReleaseNote是否存在
        if (releaseNoteService.getById(changeLog.getReleaseNoteId()) == null) {
            return error("关联的发布记录不存在");
        }
        return toAjax(changeLogService.updateById(changeLog));
    }

//    @PreAuthorize("@ss.hasPermi('system:changelog:remove')")
    @DeleteMapping("/{id}")
    public AjaxResult remove(@NotBlank(message = "变更日志ID不能为空") 
                            @PathVariable String id) {
        return toAjax(changeLogService.removeById(id));
    }
}
