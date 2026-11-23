package com.agri.cms.ai.conversation.mapper;

import com.agri.cms.ai.conversation.AgentConversation;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 智能体会话 Mapper（A-10），路由到 PostgreSQL 业务库。
 *
 * @author agrinav
 */
@DS("pg")
public interface AgentConversationMapper extends BaseMapper<AgentConversation> {
}
