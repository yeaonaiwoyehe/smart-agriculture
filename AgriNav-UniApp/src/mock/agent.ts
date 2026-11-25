import type { AgentCard, AgentSession, Task, WorkOrder } from '@/types'
import { MockError, delay, fmtDateTime, getDb, maybeFail, saveDb } from './db'
import { confirmSettlement, finishWork, startWork } from './work'

export interface AgentConfirm {
  action: 'startWork' | 'finishWork' | 'submitException'
  label: string
  summary: string
  params: Record<string, any>
}

export interface AgentReply {
  tool?: { name: string; label: string; params: Record<string, any> }
  text: string
  card?: AgentCard
  confirm?: AgentConfirm
  link?: string
}

export const AGENT_SUGGESTIONS = [
  '今天有哪些任务？',
  '我现在作业多久了？',
  '帮我开始作业',
  '为什么这单结算被扣了？',
  '上报一个农机故障'
]

function taskCard(task: Task): AgentCard {
  return {
    type: 'task',
    title: `${task.plot.plotName} · ${task.workTypeName}`,
    fields: [
      { label: '任务编号', value: task.taskNo },
      { label: '作业面积', value: `${task.area} 亩` },
      { label: '作业单价', value: `${task.unitPrice} 元/亩` },
      { label: '截止时间', value: task.deadline },
      { label: '地块地址', value: task.plot.address }
    ],
    link: `/pages/task/detail?id=${task.id}`
  }
}

function workFields(order: WorkOrder) {
  const durationMin = order.endTime
    ? Math.max(
        0,
        Math.round(
          (new Date(order.endTime.replace(' ', 'T')).getTime() -
            new Date(order.startTime.replace(' ', 'T')).getTime()) /
            60000
        )
      )
    : Math.max(0, Math.round((Date.now() - new Date(order.startTime.replace(' ', 'T')).getTime()) / 60000))
  return [
    { label: '作业单号', value: order.trajectoryInfoId },
    { label: '地块', value: order.plotName },
    { label: '作业类型', value: order.workTypeName },
    { label: '开始时间', value: order.startTime },
    { label: '已作业时长', value: `${durationMin} 分钟` },
    { label: '作业面积', value: order.area ? `${order.area} 亩` : '核算中' }
  ]
}

function includesAny(text: string, words: string[]): boolean {
  return words.some((w) => text.includes(w))
}

function pickClaimedTask(): Task | undefined {
  const db = getDb()
  return db.tasks.find((t) => t.status === 'claimed')
}

function pickActiveWork(): WorkOrder | undefined {
  return getDb().workOrders.find((o) => o.status === 1 || o.status === 2)
}

/** 意图识别 + 工具调用（演示实现，真实 Agent 需后端支持） */
export async function agentAnswer(text: string): Promise<AgentReply> {
  await delay(400, 900)
  const q = text.trim()
  const db = getDb()

  if (!q) {
    return { text: '请告诉我你想做什么，例如「今天有哪些任务」。' }
  }

  // 开工
  if (includesAny(q, ['开工', '开始作业', '开始干活'])) {
    const task = pickClaimedTask()
    if (!task) {
      return { text: '当前没有可开工的任务。请先到任务中心接收任务，再开始作业。' }
    }
    return {
      tool: { name: 'startWork', label: '准备开工', params: { taskId: task.id } },
      text: `已找到待作业任务：${task.plot.plotName}（${task.workTypeName}，${task.area} 亩）。开工后将开始记录作业时长与轨迹。`,
      confirm: {
        action: 'startWork',
        label: '确认开工',
        summary: `${task.plot.plotName} / ${task.workTypeName} / ${task.area} 亩 / 单价 ${task.unitPrice} 元/亩`,
        params: { taskId: task.id }
      }
    }
  }

  // 完工
  if (includesAny(q, ['完工', '结束作业', '收工', '完成作业'])) {
    const order = pickActiveWork()
    if (!order) {
      return { text: '当前没有进行中的作业，无需结束。' }
    }
    if (order.status !== 1) {
      return { text: `作业单 ${order.trajectoryInfoId} 正在核算中，请稍候。` }
    }
    return {
      tool: { name: 'finishWork', label: '准备完工', params: { orderId: order.trajectoryInfoId } },
      text: `当前作业单 ${order.trajectoryInfoId}（${order.plotName}）正在进行，确认结束后将提交核算。`,
      confirm: {
        action: 'finishWork',
        label: '确认结束作业',
        summary: `${order.plotName} / ${order.workTypeName} / 开始于 ${order.startTime}`,
        params: { orderId: order.trajectoryInfoId }
      }
    }
  }

  // 作业状态
  if (includesAny(q, ['作业状态', '进度', '多久', '作业了多长', '正在作业'])) {
    const order = pickActiveWork()
    if (!order) {
      return { text: '当前没有进行中的作业。今天还有待作业任务，可以直接开工。' }
    }
    return {
      tool: { name: 'queryWorkStatus', label: '查询作业状态', params: {} },
      text:
        order.status === 1
          ? `你正在 ${order.plotName} 进行${order.workTypeName}作业，作业单号 ${order.trajectoryInfoId}。`
          : `作业单 ${order.trajectoryInfoId} 处于待核算状态，面积核算完成后会生成结算明细。`,
      card: { type: 'work', title: '作业单状态', fields: workFields(order), link: `/pages/work/detail?id=${encodeURIComponent(order.trajectoryInfoId)}` }
    }
  }

  // 扣减原因（需先于「结算」判断，避免「为什么结算被扣了」被误路由）
  if (includesAny(q, ['扣', '少了', '不够', '明细', '对不上'])) {
    const withDeduction = db.settlements.find((s) =>
      (s.items || []).some((i) => i.deductionReason)
    )
    if (!withDeduction) {
      return { text: '最近的结算明细中没有扣减记录。' }
    }
    const item = (withDeduction.items || []).find((i) => i.deductionReason)
    return {
      tool: { name: 'explainDeduction', label: '查询扣减原因', params: { settlementId: withDeduction.settlementId } },
      text: `结算单 ${withDeduction.settlementNo} 中确实存在扣减，原因是：${item?.deductionReason}。如不认可，可在结算详情提交复核。`,
      card: {
        type: 'settlement',
        title: '扣减明细',
        fields: [
          { label: '地块', value: item?.plotName || '--' },
          { label: '扣减金额', value: `${item?.deduction ?? 0} 元` },
          { label: '扣减原因', value: item?.deductionReason || '--' }
        ],
        link: `/pages/settlement/detail?id=${withDeduction.settlementId}`
      }
    }
  }

  // 结算 / 收入
  if (includesAny(q, ['结算', '收入', '多少钱', '工资', '到账'])) {
    const pending = db.settlements.filter((s) => s.status === 'pending')
    const total = pending.reduce((sum, s) => sum + s.totalAmount, 0)
    const latest = db.settlements[0]
    return {
      tool: { name: 'querySettlement', label: '查询结算', params: {} },
      text: `你当前有 ${pending.length} 张待结算单，合计 ${total.toFixed(2)} 元。最近一张结算单 ${
        latest ? latest.settlementNo : '--'
      }。`,
      card: {
        type: 'settlement',
        title: '结算概览',
        fields: [
          { label: '待结算金额', value: `${total.toFixed(2)} 元` },
          { label: '待结算单数', value: `${pending.length} 张` },
          { label: '最近结算周期', value: latest ? `${latest.periodStart} 至 ${latest.periodEnd}` : '--' }
        ],
        link: '/pages/settlement/index'
      }
    }
  }

  // 上报异常
  if (includesAny(q, ['异常', '故障', '报修', '坏了', '上报'])) {
    return {
      tool: { name: 'prepareException', label: '准备异常上报', params: {} },
      text: '我可以帮你发起异常上报。提交前需要你补充问题描述和现场照片，确认后进入上报页面。',
      confirm: {
        action: 'submitException',
        label: '去填写上报',
        summary: '将跳转到异常上报页面，填写类型、描述并上传照片后提交',
        params: {}
      }
    }
  }

  // 导航
  if (includesAny(q, ['导航', '怎么走', '位置', '地图', '地块在哪'])) {
    const task = pickClaimedTask() || db.tasks.find((t) => t.status === 'working')
    if (!task) {
      return { text: '当前没有待作业任务，暂时没有可导航的地块。' }
    }
    return {
      tool: { name: 'navigateToPlot', label: '打开地块地图', params: { plotId: task.plot.plotId } },
      text: `已为你打开 ${task.plot.plotName} 的地块位置，可直接查看边界与轨迹。`,
      link: `/pages/map/index?plotId=${task.plot.plotId}`
    }
  }

  // 今日任务
  if (includesAny(q, ['任务', '待办', '今天做什么', '今日'])) {
    const list = db.tasks.filter((t) => t.status === 'claimed' || t.status === 'working')
    if (!list.length) {
      return {
        tool: { name: 'queryTodayTasks', label: '查询今日任务', params: {} },
        text: '今天暂时没有待作业任务，可以到任务中心看看新发布的任务。',
        card: { type: 'tip', title: '暂无待办任务', fields: [], link: '/pages/task/index' }
      }
    }
    return {
      tool: { name: 'queryTodayTasks', label: '查询今日任务', params: {} },
      text: `你今天有 ${list.length} 个待办任务，最近的是 ${list[0].plot.plotName}（${list[0].workTypeName}，${list[0].area} 亩）。`,
      card: taskCard(list[0])
    }
  }

  // FAQ / 帮助
  if (includesAny(q, ['怎么用', '如何', '帮助', '什么是', '说明'])) {
    return {
      tool: { name: 'searchFaq', label: '搜索帮助', params: {} },
      text:
        '常用操作：①任务中心接收任务；②作业页开始作业，系统自动计时并记录轨迹；③结束作业后等待核算；④在结算中心查看面积与金额。若遇设备或地块问题，可在异常上报中提交工单。'
    }
  }

  return {
    text:
      '我可以帮你查询今日任务、作业状态、结算明细，也可以发起开工、完工和异常上报。你可以试试说「今天有哪些任务」或「帮我开始作业」。'
  }
}

/** 执行确认后的写操作 */
export async function agentExecute(
  action: AgentConfirm['action'],
  params: Record<string, any>
): Promise<{ text: string; card?: AgentCard; link?: string }> {
  maybeFail('agentExecute')
  if (action === 'startWork') {
    const order = await startWork(String(params.taskId))
    return {
      text: `开工成功，作业单号 ${order.trajectoryInfoId}，回执 ${order.receiptNo}。已开始记录作业时长。`,
      card: {
        type: 'receipt',
        title: '开工回执',
        fields: [
          { label: '作业单号', value: order.trajectoryInfoId },
          { label: '地块', value: order.plotName },
          { label: '开始时间', value: order.startTime },
          { label: '回执编号', value: order.receiptNo || '--' }
        ],
        link: `/pages/work/index`
      }
    }
  }

  if (action === 'finishWork') {
    const order = await finishWork(String(params.orderId))
    return {
      text: `已提交完工，作业单 ${order.trajectoryInfoId} 进入核算，预计几秒内返回面积结果。`,
      card: {
        type: 'receipt',
        title: '完工回执',
        fields: [
          { label: '作业单号', value: order.trajectoryInfoId },
          { label: '结束时间', value: order.endTime || '--' },
          { label: '核算面积', value: order.area ? `${order.area} 亩` : '核算中' }
        ],
        link: `/pages/work/detail?id=${encodeURIComponent(order.trajectoryInfoId)}`
      }
    }
  }

  if (action === 'submitException') {
    return {
      text: '请填写异常信息并提交，提交成功后会生成工单编号。',
      link: '/pages/exception/index?from=agent'
    }
  }

  throw new MockError('未知操作')
}

/** 演示：模拟语音识别结果 */
export async function mockSpeechToText(): Promise<string> {
  await delay(900, 1400)
  const samples = [
    '今天有哪些任务',
    '我现在作业多久了',
    '帮我开始作业',
    '为什么这单结算被扣了'
  ]
  return samples[Math.floor(Math.random() * samples.length)]
}

export function createSession(title = '新会话'): AgentSession {
  const db = getDb()
  db.counters.session += 1
  const session: AgentSession = {
    id: `S${Date.now()}${db.counters.session}`,
    title,
    createTime: Date.now(),
    messages: []
  }
  db.agentSessions.unshift(session)
  saveDb(db)
  return session
}

export function listSessions(): AgentSession[] {
  return getDb().agentSessions
}

export function getSession(id: string): AgentSession | undefined {
  return getDb().agentSessions.find((s) => s.id === id)
}

export function saveSession(session: AgentSession): void {
  const db = getDb()
  const idx = db.agentSessions.findIndex((s) => s.id === session.id)
  if (idx >= 0) {
    db.agentSessions[idx] = session
  } else {
    db.agentSessions.unshift(session)
  }
  const firstUser = session.messages.find((m) => m.role === 'user')
  if (firstUser) {
    session.title = firstUser.text.slice(0, 16)
  }
  saveDb(db)
}

export function deleteSession(id: string): void {
  const db = getDb()
  db.agentSessions = db.agentSessions.filter((s) => s.id !== id)
  saveDb(db)
}

export function settleWorkQuietly(trajectoryInfoId: string) {
  return confirmSettlement(trajectoryInfoId)
}

export function nowText(): string {
  return fmtDateTime(new Date())
}
