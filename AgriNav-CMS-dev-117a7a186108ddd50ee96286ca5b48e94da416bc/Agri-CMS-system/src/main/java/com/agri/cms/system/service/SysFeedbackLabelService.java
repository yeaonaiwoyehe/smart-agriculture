package com.agri.cms.system.service;

import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.system.domain.Feedback;
import com.agri.cms.system.domain.SysFeedbackLabel;
import com.agri.cms.system.domain.dto.FeedbackDTO;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * @author : simon
 * @description :
 * @last : 2024-11-10 12:14
 * Copyright (c) 2024. 保留所有权利。
 * @since : 2024-11-10 12:14
 */
public interface SysFeedbackLabelService extends IService<SysFeedbackLabel> {
    /**
     * 根据ID获取反馈标签
     * @param feedbackId    反馈ID
     * @return List<SysFeedbackLabel>
     * @author simon
     * @since 2024/11/10
     */
    List<SysFeedbackLabel> listByFeedbackId(String feedbackId);
}
