package com.agri.cms.ai.agent.mapper;

import com.agri.cms.ai.agent.AgentTaskClaim;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 任务认领 Mapper（A-11），路由到 PostgreSQL 业务库。
 *
 * @author agrinav
 */
@DS("pg")
public interface AgentTaskClaimMapper extends BaseMapper<AgentTaskClaim> {
}
