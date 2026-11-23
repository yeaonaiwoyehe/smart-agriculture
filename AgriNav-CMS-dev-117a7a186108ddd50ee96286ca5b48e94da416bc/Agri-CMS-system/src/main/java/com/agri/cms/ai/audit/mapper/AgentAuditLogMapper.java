package com.agri.cms.ai.audit.mapper;

import com.agri.cms.ai.audit.AgentAuditLog;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 智能体指令审计 Mapper（A-06），路由到 PostgreSQL 业务库。
 *
 * @author agrinav
 */
@DS("pg")
public interface AgentAuditLogMapper extends BaseMapper<AgentAuditLog> {
}
