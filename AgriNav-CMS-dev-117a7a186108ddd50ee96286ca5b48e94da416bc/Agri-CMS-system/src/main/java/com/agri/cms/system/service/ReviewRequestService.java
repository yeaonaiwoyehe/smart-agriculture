package com.agri.cms.system.service;

import com.agri.cms.system.domain.ReviewRequest;

import java.util.List;

/**
 * 复核申请 Service（机手异议 -> 队长初核 -> 管理员终审）
 */
public interface ReviewRequestService {

    /**
     * 查询复核申请列表
     */
    List<ReviewRequest> selectList(ReviewRequest query);

    /**
     * 根据ID查询复核申请
     */
    ReviewRequest selectById(Long id);

    /**
     * 机手提交复核申请
     */
    void submit(ReviewRequest request);

    /**
     * 队长初核
     * @param pass true 通过（流转终审） false 驳回（终态）
     */
    void initialReview(Long id, boolean pass, String comment, String reviewer);

    /**
     * 管理员终审
     * @param pass true 通过（终态） false 驳回（终态）
     */
    void finalReview(Long id, boolean pass, String comment, String reviewer);
}
