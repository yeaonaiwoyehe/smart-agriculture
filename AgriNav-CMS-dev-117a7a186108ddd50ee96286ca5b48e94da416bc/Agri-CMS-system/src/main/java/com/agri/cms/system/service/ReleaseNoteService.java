package com.agri.cms.system.service;

import com.agri.cms.system.domain.ReleaseNote;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @author : simon
 * @description :
 * @last : 2024-11-18 21:09
 * Copyright (c) 2024. 保留所有权利。
 * @since : 2024-11-18 21:09
 */
public interface ReleaseNoteService extends IService<ReleaseNote> {
    /**
     * 获取发布记录详情，包含变更日志
     */
    ReleaseNote getDetailById(String id);
}
