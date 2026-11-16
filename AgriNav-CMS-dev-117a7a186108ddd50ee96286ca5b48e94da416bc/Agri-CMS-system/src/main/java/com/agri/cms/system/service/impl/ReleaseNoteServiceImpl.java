package com.agri.cms.system.service.impl;

import com.agri.cms.system.domain.ChangeLog;
import com.agri.cms.system.domain.ReleaseNote;
import com.agri.cms.system.mapper.ReleaseNoteMapper;
import com.agri.cms.system.service.ChangeLogService;
import com.agri.cms.system.service.ReleaseNoteService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author : simon
 * @description :
 * @last : 2024-11-18 21:10
 * Copyright (c) 2024. 保留所有权利。
 * @since : 2024-11-18 21:10
 */
@Service
public class ReleaseNoteServiceImpl
        extends ServiceImpl<ReleaseNoteMapper, ReleaseNote>
        implements ReleaseNoteService {

    @Resource
    private ChangeLogService changeLogService;

    @Override
    public ReleaseNote getDetailById(String id) {
        // 获取发布记录基本信息
        ReleaseNote releaseNote = this.getById(id);
        if (releaseNote != null) {
            // 查询关联的变更日志列表
            List<ChangeLog> changeLogs = changeLogService.list(
                new LambdaQueryWrapper<ChangeLog>()
                    .eq(ChangeLog::getReleaseNoteId, id)
                    .orderByAsc(ChangeLog::getSortOrder)
            );
            releaseNote.setChangeLogs(changeLogs);
        }
        return releaseNote;
    }
}
