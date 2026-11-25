import { defineStore } from 'pinia'
import type { AgentMessage, AgentSession } from '@/types'
import * as agentApi from '@/api/agent'
import { errorMessage } from '@/utils/ui'

interface AgentState {
  session: AgentSession | null
  thinking: boolean
  executing: boolean
  error: string
}

let seq = 0
function msgId(): string {
  seq += 1
  return `m${Date.now()}${seq}`
}

export const useAgentStore = defineStore('agent', {
  state: (): AgentState => ({
    session: null,
    thinking: false,
    executing: false,
    error: ''
  }),
  getters: {
    messages: (state) => state.session?.messages || [],
    busy: (state) => state.thinking || state.executing
  },
  actions: {
    ensureSession() {
      if (!this.session) {
        this.session = agentApi.createSession()
      }
      return this.session
    },
    newSession() {
      this.session = agentApi.createSession()
      this.thinking = false
      this.executing = false
      this.error = ''
    },
    loadSession(id: string) {
      const found = agentApi.getSession(id)
      if (found) {
        this.session = JSON.parse(JSON.stringify(found)) as AgentSession
        return true
      }
      return false
    },
    persist() {
      if (this.session) agentApi.saveSession(this.session)
    },
    push(partial: Omit<AgentMessage, 'id' | 'time'>) {
      this.ensureSession()
      this.session!.messages.push({ id: msgId(), time: Date.now(), ...partial } as AgentMessage)
    },
    async send(text: string) {
      const content = text.trim()
      if (!content || this.busy) return
      this.ensureSession()
      this.push({ role: 'user', text: content })
      this.thinking = true
      this.error = ''
      try {
        const reply = await agentApi.askAgent(content)
        if (reply.tool) {
          this.push({
            role: 'agent',
            text: '',
            toolCall: {
              name: reply.tool.name,
              label: reply.tool.label,
              params: reply.tool.params,
              status: 'success'
            }
          })
        }
        this.push({
          role: 'agent',
          text: reply.text,
          card: reply.card,
          pendingConfirm: reply.confirm
            ? {
                action: reply.confirm.action,
                label: reply.confirm.label,
                summary: reply.confirm.summary
              }
            : undefined
        })
        this.persist()
      } catch (e) {
        this.error = errorMessage(e, '助手暂时不可用')
        this.push({ role: 'agent', text: `抱歉，${this.error}。` })
      } finally {
        this.thinking = false
        this.persist()
      }
    },
    /** 执行待确认操作，返回后端/演示层结果 */
    async confirm(messageId: string, extraParams: Record<string, any> = {}) {
      const target = this.messages.find((m) => m.id === messageId)
      if (!target || !target.pendingConfirm || this.busy) return null
      const action = target.pendingConfirm.action
      target.pendingConfirm = undefined
      this.push({ role: 'user', text: '确认执行' })
      this.executing = true
      this.error = ''
      try {
        const result = await agentApi.executeAgentAction(
          action as Parameters<typeof agentApi.executeAgentAction>[0],
          extraParams
        )
        this.push({ role: 'agent', text: result.text, card: result.card })
        this.persist()
        return result
      } catch (e) {
        this.error = errorMessage(e)
        this.push({ role: 'agent', text: `执行失败：${this.error}` })
        this.persist()
        return null
      } finally {
        this.executing = false
        this.persist()
      }
    },
    cancelConfirm(messageId: string) {
      const target = this.messages.find((m) => m.id === messageId)
      if (target) target.pendingConfirm = undefined
      this.push({ role: 'user', text: '取消' })
      this.push({ role: 'agent', text: '已取消本次操作，没有产生任何变更。' })
      this.persist()
    }
  }
})
