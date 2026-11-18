package com.agri.cms.system.mapper;

import com.agri.cms.system.domain.Settlement;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 结算单 Mapper
 */
@DS("pg")
public interface SettlementMapper extends BaseMapper<Settlement> {
}
