package com.agri.cms.ai.agent.mapper;

import com.agri.cms.ai.agent.AgentAlert;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 异常工单 Mapper（A-14），路由到 PostgreSQL 业务库。
 *
 * @author agrinav
 */
@DS("pg")
public interface AgentAlertMapper extends BaseMapper<AgentAlert> {
}
