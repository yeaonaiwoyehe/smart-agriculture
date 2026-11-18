package com.agri.cms.system.service;

import com.agri.cms.system.domain.PricingRule;

import java.util.List;

/**
 * 作业单价规则 Service
 */
public interface PricingRuleService {

    /**
     * 查询单价规则列表
     */
    List<PricingRule> selectList(PricingRule query);

    /**
     * 根据ID查询单价规则
     */
    PricingRule selectById(Long id);

    /**
     * 新增单价规则
     */
    void insert(PricingRule rule, String username);

    /**
     * 修改单价规则
     */
    void update(PricingRule rule, String username);

    /**
     * 删除单价规则
     */
    void deleteByIds(Long[] ids);
}
