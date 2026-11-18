package com.agri.cms.system.service.impl;

import com.agri.cms.system.domain.ReviewRequest;
import com.agri.cms.system.domain.TrajectoryInfo;
import com.agri.cms.system.mapper.ReviewRequestMapper;
import com.agri.cms.system.mapper.TrajectoryInfoMapper;
import com.agri.cms.system.service.ReviewRequestService;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 复核申请 Service 实现
 */
@Service
@DS("pg")
public class ReviewRequestServiceImpl implements ReviewRequestService {

    @Resource
    private ReviewRequestMapper reviewRequestMapper;

    @Resource
    private TrajectoryInfoMapper trajectoryInfoMapper;

    @Override
    public List<ReviewRequest> selectList(ReviewRequest query) {
        LambdaQueryWrapper<ReviewRequest> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (query.getReviewNo() != null && !query.getReviewNo().isEmpty()) {
                wrapper.like(ReviewRequest::getReviewNo, query.getReviewNo());
            }
            if (query.getPhoneNumber() != null && !query.getPhoneNumber().isEmpty()) {
                wrapper.eq(ReviewRequest::getPhoneNumber, query.getPhoneNumber());
            }
            if (query.getStatus() != null) {
                wrapper.eq(ReviewRequest::getStatus, query.getStatus());
            }
        }
        wrapper.orderByDesc(ReviewRequest::getCreateTime);
        return reviewRequestMapper.selectList(wrapper);
    }

    @Override
    public ReviewRequest selectById(Long id) {
        return reviewRequestMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(ReviewRequest request) {
        // 依据作业单回填关键信息，保证复核数据与作业单一致
        if (request.getTrajectoryInfoId() != null && !request.getTrajectoryInfoId().isEmpty()) {
            TrajectoryInfo info = trajectoryInfoMapper.selectById(request.getTrajectoryInfoId());
            if (info != null) {
                request.setPhoneNumber(info.getPhoneNumber());
                request.setPlotId(info.getPlotId());
                request.setWorkType(info.getWorkType());
                request.setSystemArea(info.getArea());
            }
        }
        request.setReviewNo(generateReviewNo());
        request.setStatus((short) 0);
        request.setCreateTime(LocalDateTime.now());
        reviewRequestMapper.insert(request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initialReview(Long id, boolean pass, String comment, String reviewer) {
        ReviewRequest request = mustGet(id);
        if (request.getStatus() != 0) {
            throw new IllegalStateException("当前复核单不是待初核状态，无法初核");
        }
        request.setInitialReviewer(reviewer);
        request.setInitialComment(comment);
        request.setInitialTime(LocalDateTime.now());
        request.setUpdateTime(LocalDateTime.now());
        // 初核通过 -> 流转终审；驳回 -> 终态并通知机手
        request.setStatus(pass ? (short) 1 : (short) 2);
        reviewRequestMapper.updateById(request);
        if (!pass) {
            notifyOperator(request);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finalReview(Long id, boolean pass, String comment, String reviewer) {
        ReviewRequest request = mustGet(id);
        if (request.getStatus() != 1) {
            throw new IllegalStateException("当前复核单未通过初核，无法终审");
        }
        request.setFinalReviewer(reviewer);
        request.setFinalComment(comment);
        request.setFinalTime(LocalDateTime.now());
        request.setUpdateTime(LocalDateTime.now());
        // 终审通过/驳回均为终态，并将结果推机手
        request.setStatus(pass ? (short) 3 : (short) 4);
        reviewRequestMapper.updateById(request);
        notifyOperator(request);
    }

    private ReviewRequest mustGet(Long id) {
        ReviewRequest request = reviewRequestMapper.selectById(id);
        if (request == null) {
            throw new IllegalArgumentException("复核单不存在");
        }
        return request;
    }

    private void notifyOperator(ReviewRequest request) {
        reviewRequestMapper.insertNotification(
                UUID.randomUUID().toString(),
                request.getPhoneNumber(),
                String.valueOf(request.getId()));
    }

    private String generateReviewNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int seq = ThreadLocalRandom.current().nextInt(100, 1000);
        return "FH-" + date + "-" + seq;
    }
}
