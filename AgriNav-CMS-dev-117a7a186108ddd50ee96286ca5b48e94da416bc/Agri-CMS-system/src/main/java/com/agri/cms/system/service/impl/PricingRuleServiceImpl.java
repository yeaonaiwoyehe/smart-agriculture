package com.agri.cms.system.service.impl;

import com.agri.cms.system.domain.PricingRule;
import com.agri.cms.system.mapper.PricingRuleMapper;
import com.agri.cms.system.service.PricingRuleService;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 作业单价规则 Service 实现
 */
@Service
@DS("pg")
public class PricingRuleServiceImpl implements PricingRuleService {

    @Resource
    private PricingRuleMapper pricingRuleMapper;

    @Override
    public List<PricingRule> selectList(PricingRule query) {
        LambdaQueryWrapper<PricingRule> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (query.getRuleName() != null && !query.getRuleName().isEmpty()) {
                wrapper.like(PricingRule::getRuleName, query.getRuleName());
            }
            if (query.getWorkType() != null) {
                wrapper.eq(PricingRule::getWorkType, query.getWorkType());
            }
            if (query.getStatus() != null) {
                wrapper.eq(PricingRule::getStatus, query.getStatus());
            }
        }
        wrapper.orderByAsc(PricingRule::getWorkType).orderByAsc(PricingRule::getMinArea);
        return pricingRuleMapper.selectList(wrapper);
    }

    @Override
    public PricingRule selectById(Long id) {
        return pricingRuleMapper.selectById(id);
    }

    @Override
    public void insert(PricingRule rule, String username) {
        rule.setCreateBy(username);
        rule.setCreateTime(LocalDateTime.now());
        if (rule.getStatus() == null) {
            rule.setStatus((short) 0);
        }
        if (rule.getMinArea() == null) {
            rule.setMinArea(java.math.BigDecimal.ZERO);
        }
        pricingRuleMapper.insert(rule);
    }

    @Override
    public void update(PricingRule rule, String username) {
        rule.setUpdateBy(username);
        rule.setUpdateTime(LocalDateTime.now());
        pricingRuleMapper.updateById(rule);
    }

    @Override
    public void deleteByIds(Long[] ids) {
        pricingRuleMapper.deleteBatchIds(Arrays.asList(ids));
    }
}
