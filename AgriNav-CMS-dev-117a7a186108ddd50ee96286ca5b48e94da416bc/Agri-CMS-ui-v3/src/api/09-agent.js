/**
 * 09-智能体（10 个接口）
 * 依据 AgriNav接口清单.md 自动生成，方法与路径逐条对应。
 */
import { get, post, put, del, patch } from '@/utils/request'

export const getAgentAuditById = (id, ...rest) => get(`/agent/audit/${encodeURIComponent(id)}`, ...rest)
export const getAgentAuditList = (...args) => get('/agent/audit/list', ...args)
export const getAgentConversationById = (id, ...rest) => get(`/agent/conversation/${encodeURIComponent(id)}`, ...rest)
export const getAgentConversationList = (...args) => get('/agent/conversation/list', ...args)
export const getLlmAgent = (...args) => get('/llm/agent', ...args)
export const getLlmChat = (...args) => get('/llm/chat', ...args)
export const getLlmIntent = (...args) => get('/llm/intent', ...args)
export const getLlmRoute = (...args) => get('/llm/route', ...args)
export const getLlmTool = (...args) => get('/llm/tool', ...args)
export const getLlmToolConfirm = (...args) => get('/llm/tool/confirm', ...args)

export const agentApi = {
  getAgentAuditById,
  getAgentAuditList,
  getAgentConversationById,
  getAgentConversationList,
  getLlmAgent,
  getLlmChat,
  getLlmIntent,
  getLlmRoute,
  getLlmTool,
  getLlmToolConfirm,
}
export default agentApi
