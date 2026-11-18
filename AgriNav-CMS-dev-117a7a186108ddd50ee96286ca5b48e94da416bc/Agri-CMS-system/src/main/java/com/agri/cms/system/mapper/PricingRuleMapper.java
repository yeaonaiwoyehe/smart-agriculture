package com.agri.cms.system.mapper;

import com.agri.cms.system.domain.PricingRule;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 作业单价规则 Mapper
 */
@DS("pg")
public interface PricingRuleMapper extends BaseMapper<PricingRule> {
}
