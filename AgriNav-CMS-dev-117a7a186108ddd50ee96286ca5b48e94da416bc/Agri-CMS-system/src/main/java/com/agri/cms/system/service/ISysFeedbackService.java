package com.agri.cms.system.service;

import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.system.domain.Feedback;
import com.agri.cms.system.domain.dto.FeedbackDTO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.stereotype.Service;


public interface ISysFeedbackService extends IService<Feedback> {
    AjaxResult getMyFeedback(int pageNum, int pageSize, String username);

    AjaxResult queryData(Page<Feedback> page, FeedbackDTO feedback);
}
