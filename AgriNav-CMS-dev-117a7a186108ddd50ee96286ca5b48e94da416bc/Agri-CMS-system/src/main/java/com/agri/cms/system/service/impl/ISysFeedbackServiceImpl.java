package com.agri.cms.system.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.system.domain.Feedback;
import com.agri.cms.system.domain.SysFeedbackLabel;
import com.agri.cms.system.domain.dto.FeedbackDTO;
import com.agri.cms.system.mapper.SysFeedbackMapper;
import com.agri.cms.system.service.ISysDictDataService;
import com.agri.cms.system.service.ISysFeedbackService;
import com.agri.cms.system.service.SysFeedbackLabelService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ISysFeedbackServiceImpl extends ServiceImpl<SysFeedbackMapper, Feedback> implements ISysFeedbackService {
    @Resource
    private SysFeedbackLabelService sysFeedbackLabelService;
    @Resource
    private ISysDictDataService dictDataService;

    @Override
    public AjaxResult getMyFeedback(int pageNum, int pageSize, String username) {
        AjaxResult ajaxResult = AjaxResult.success();
        LambdaQueryWrapper<Feedback> queryWrapper = new LambdaQueryWrapper<>();
        // 查询当前用户的反馈
        queryWrapper.eq(Feedback::getPhoneNumber, username)
                .orderByDesc(Feedback::getCommitTime);
        Page<Feedback> resultPage = this.page(Page.of(pageNum, pageSize), queryWrapper);
        List<Feedback> processedList = resultPage.getRecords().stream().peek(fb-> {
            Long[] labelCodes = sysFeedbackLabelService.listByFeedbackId(fb.getId())
                    .stream()
                    .map(SysFeedbackLabel::getLabelCode)
                    .toArray(Long[]::new);
            if (ObjectUtil.isNotEmpty(labelCodes)) {
                fb.setLabels(dictDataService.selectDictDataByIds(labelCodes));
            }
        }).toList();
        //设置total
        ajaxResult.put("total", resultPage.getTotal());
        //设置data
        ajaxResult.put("data", processedList);
        return ajaxResult;
    }

    @Override
    public AjaxResult queryData(Page<Feedback> page, FeedbackDTO feedback) {
        Page<Feedback> resultPage = this.page(page,
                new LambdaQueryWrapper<Feedback>()
                        .and(ObjectUtil.isNotEmpty(feedback.getLabels()), wrapper ->
                                wrapper.inSql(Feedback::getId,
                                        "SELECT feedback_id FROM sys_feedback_label " +
                                                "WHERE label_code in (" +
                                                feedback.getLabels().stream()
                                                        .map(label -> label.getDictCode().toString())
                                                        .collect(Collectors.joining(",")) +
                                                ") GROUP BY feedback_id " +
                                                "HAVING COUNT(DISTINCT label_code) = " + feedback.getLabels().size()
                                )
                        )
                        .eq(ObjectUtil.isNotEmpty(feedback.getId()), Feedback::getId, feedback.getId())
                        .eq(ObjectUtil.isNotEmpty(feedback.getUsername()), Feedback::getUsername, feedback.getUsername())
                        .like(ObjectUtil.isNotEmpty(feedback.getContent()), Feedback::getContent, feedback.getContent())
                        .eq(ObjectUtil.isNotEmpty(feedback.getPhoneNumber()), Feedback::getPhoneNumber, feedback.getPhoneNumber())
                        .eq(ObjectUtil.isNotEmpty(feedback.getIsChecked()), Feedback::getIsChecked, feedback.getIsChecked())
                        .orderByDesc(Feedback::getCommitTime)
                        .between(ObjectUtil.isNotEmpty(feedback.getStartCommitTime()) && ObjectUtil.isNotEmpty(feedback.getEndCommitTime()),
                                Feedback::getCommitTime, feedback.getStartCommitTime(), feedback.getEndCommitTime())
                        .between(ObjectUtil.isNotEmpty(feedback.getStartCheckedTime()) && ObjectUtil.isNotEmpty(feedback.getEndCheckedTime()),
                                Feedback::getCheckedTime, feedback.getStartCheckedTime(), feedback.getEndCheckedTime())
        );

        // 处理查询结果
        List<Feedback> processedList = resultPage.getRecords().stream().peek(fb-> {
            Long[] labelCodes = sysFeedbackLabelService.listByFeedbackId(fb.getId())
                    .stream()
                    .map(SysFeedbackLabel::getLabelCode)
                    .toArray(Long[]::new);
            if (ObjectUtil.isNotEmpty(labelCodes)) {
                fb.setLabels(dictDataService.selectDictDataByIds(labelCodes));
            }
        }).toList();

        // 创建一个新的 AjaxResult，直接设置 data 和 total
        AjaxResult ajax = AjaxResult.success();
        ajax.put("data", processedList);
        ajax.put("total", resultPage.getTotal());
        return ajax;
    }
}
