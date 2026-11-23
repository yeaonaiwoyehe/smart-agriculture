package com.agri.cms.ai.conversation.mapper;

import com.agri.cms.ai.conversation.AgentConversationMessage;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 智能体会话消息 Mapper（A-10），路由到 PostgreSQL 业务库。
 *
 * @author agrinav
 */
@DS("pg")
public interface AgentConversationMessageMapper extends BaseMapper<AgentConversationMessage> {
}
