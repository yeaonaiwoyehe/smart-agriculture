package com.agri.cms.system.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.system.domain.Feedback;
import com.agri.cms.system.domain.SysFeedbackLabel;
import com.agri.cms.system.domain.dto.FeedbackDTO;
import com.agri.cms.system.mapper.SysFeedbackLabelMapper;
import com.agri.cms.system.service.ISysDictDataService;
import com.agri.cms.system.service.ISysFeedbackService;
import com.agri.cms.system.service.SysFeedbackLabelService;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author : simon
 * @description :
 * @last : 2024-11-10 12:16
 * Copyright (c) 2024. 保留所有权利。
 * @since : 2024-11-10 12:16
 */
@Service
@DS("pg")
public class SysFeedbackLabelServiceImpl extends ServiceImpl<SysFeedbackLabelMapper, SysFeedbackLabel> implements SysFeedbackLabelService {
    @Resource
    private ISysDictDataService dictDataService;

    @Override
    public List<SysFeedbackLabel> listByFeedbackId(String feedbackId) {
        return list(new LambdaQueryWrapper<SysFeedbackLabel>().eq(SysFeedbackLabel::getFeedbackId, feedbackId));
    }

}
