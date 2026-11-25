import type { AgentConfirm, AgentReply } from '@/mock/agent'
import * as mockAgent from '@/mock/agent'
import type { AgentSession } from '@/types'

/**
 * Agent 接口（后端尚未提供）。
 * 当前为本地规则引擎演示，接入真实后端后在此替换为 SSE / HTTP 调用。
 */
export async function askAgent(text: string): Promise<AgentReply> {
  return mockAgent.agentAnswer(text)
}

export async function executeAgentAction(
  action: AgentConfirm['action'],
  params: Record<string, any>
): Promise<{ text: string; card?: AgentReply['card']; link?: string }> {
  return mockAgent.agentExecute(action, params)
}

export async function speechToText(): Promise<string> {
  return mockAgent.mockSpeechToText()
}

export const AGENT_SUGGESTIONS = mockAgent.AGENT_SUGGESTIONS

export function createSession(): AgentSession {
  return mockAgent.createSession()
}

export function listSessions(): AgentSession[] {
  return mockAgent.listSessions()
}

export function getSession(id: string): AgentSession | undefined {
  return mockAgent.getSession(id)
}

export function saveSession(session: AgentSession): void {
  mockAgent.saveSession(session)
}

export function deleteSession(id: string): void {
  mockAgent.deleteSession(id)
}

export type { AgentReply, AgentConfirm }
