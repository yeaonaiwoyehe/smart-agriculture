import * as api from '@/services/api'
import { runAgent } from '@/services/agentEngine'
import { tools } from '@/mock'

const sleep = (ms) => new Promise((r) => setTimeout(r, ms))
const toolName = (id) => tools.find((t) => t.id === id)?.name || id
const num = (n, d = 1) => Number(n || 0).toFixed(d)

const STEPS = {
  land: ['校验用户身份与数据范围', '解析地块与作业状态条件', '查询地块主数据 T01', '汇总面积、单价与待作业情况'],
  machine: ['校验用户身份与数据范围', '解析农机与状态条件', '查询农机实时状态 T02', '统计在线率与作业分布'],
  anomaly: ['校验用户身份与数据范围', '解析单据与异常类型', '查询作业单轨迹与异常 T06/T07', '生成可追溯的异常研判'],
  settlement: ['校验用户身份与数据范围', '解析结算周期与单据', '查询结算单与扣减明细 T08', '解释金额组成与依据'],
  todo: ['校验用户身份与数据范围', '汇总跨域待办', '查询异常、已截止与待核对数据', '生成处置优先级建议']
}

function classify(message, context) {
  const m = message || ''
  // 页面上下文优先：同一业务页内的追问不应被其他模块的关键词抢走
  if (context.key === 'lands' && /地块|面积|权属|单价|亩|幅宽|等级/.test(m)) return 'land'
  if (context.key === 'settlement' && /结算|金额|扣减|单价|付款/.test(m)) return 'settlement'
  if (/待处理|待办|风险|积压/.test(m)) return 'todo'
  if (/异常|轨迹|超界|研判|停留/.test(m)) return 'anomaly'
  if (/结算|金额|扣减|单价|费用|付款/.test(m)) return 'settlement'
  if (/农机|在线|闲置|作业情况|机手/.test(m)) return 'machine'
  if (/地块|面积|权属|亩|边界/.test(m)) return 'land'
  if (context.key === 'lands' && /这个|该|当前/.test(m)) return 'land'
  if (context.key === 'settlement' && /这笔|该笔|这个|当前/.test(m)) return 'settlement'
  return null
}

function pickLand(lands, message, context) {
  const byName = lands.find((l) => message.includes(l.name) || message.includes(l.code))
  if (byName) return byName
  if (context.queryId) {
    const byId = lands.find((l) => l.id === context.queryId)
    if (byId) return byId
  }
  return context.key === 'lands' ? lands[0] : null
}

/**
 * 全局悬浮助手：页面上下文优先，其余交给 MA-05 综合运营 Agent。
 * 事件协议与 agentEngine 保持一致（plan / step / tool-start / tool-end / reply）。
 */
export async function runAssistant({ agentId = 'MA-05', message, role = 'admin', context = {}, emit = () => {} }) {
  const kind = classify(message, context)
  if (!kind) return runAgent({ agentId, message, role, emit })

  const steps = STEPS[kind].map((text) => ({ text, status: 'pending' }))
  emit({ type: 'plan', steps })

  const mark = async (index, status = 'done', delay = 420) => {
    steps[index].status = status
    emit({ type: 'step', index, steps })
    await sleep(delay)
  }
  const tool = async (index, toolId, payload, result, delay = 560) => {
    emit({ type: 'tool-start', index, toolId, name: toolName(toolId), payload })
    await sleep(delay)
    emit({ type: 'tool-end', index, toolId, name: toolName(toolId), payload, result })
    steps[index].tool = { toolId, name: toolName(toolId) }
  }

  await mark(0, 'done', 280)
  await mark(1, 'done', 240)

  const handler = { land: runLand, machine: runMachine, anomaly: runAnomaly, settlement: runSettlement, todo: runTodo }[kind]
  const reply = await handler({ message, context, role, tool, mark, steps })
  emit({ type: 'reply', reply })
  return reply
}

async function runLand({ message, context, role, tool, mark, steps }) {
  const lands = await api.listLands()
  await tool(2, 'T01', { scope: role === 'leader' ? '河东片区' : '全公司' }, { count: lands.length })
  await mark(2)

  const focus = /面积|权属|单价|幅宽|等级|这个|该/.test(message) ? pickLand(lands, message, context) : null
  let content
  let table

  if (focus) {
    const onlyPrice = /单价/.test(message) && !/面积|权属|等级|幅宽/.test(message)
    const onlyArea = /面积/.test(message) && !/单价|权属|等级|幅宽/.test(message)
    if (onlyPrice) {
      content = `已结合当前页面「${context.title || '地块管理'}」：地块 <b>${focus.name}</b>（${focus.code}）作业单价为 ` +
        `<b>${focus.price} 元/亩</b>（${focus.grade} · ${focus.crop}）。单价依据地块等级与作物类型配置，取自地块主数据 T01。`
    } else if (onlyArea) {
      content = `已结合当前页面「${context.title || '地块管理'}」：地块 <b>${focus.name}</b>（${focus.code}）面积 <b>${focus.area} 亩</b>，` +
        `位于 ${focus.district} ${focus.village}，当前状态 ${focus.status}。面积取自地块主数据 T01，未做估算。`
    } else {
      content = `已结合当前页面「${context.title || '地块管理'}」：地块 <b>${focus.name}</b>（${focus.code}）面积 <b>${focus.area} 亩</b>，` +
        `权属 ${focus.owner || focus.village || '村集体'}，单价 <b>${focus.price} 元/亩</b>，标准幅宽 ${focus.width} 米，等级 ${focus.grade}，` +
        `作物 ${focus.crop}，当前状态 ${focus.status}。以上均取自地块主数据 T01，未做估算。`
    }
    table = {
      columns: ['指标', '数值'],
      rows: [
        ['所属片区 / 行政村', `${focus.district} / ${focus.village}`],
        ['面积', `${focus.area} 亩`],
        ['单价 / 标准幅宽', `${focus.price} 元/亩 / ${focus.width} 米`],
        ['地块等级 / 作物', `${focus.grade} / ${focus.crop}`],
        ['历史作业次数', `${focus.workCount || 0} 次`]
      ]
    }
  } else {
    const pending = lands.filter((l) => l.status === '待作业')
    const rows = (pending.length ? pending : lands).slice(0, 6)
    content = `当前数据范围内共有 <b>${pending.length || lands.length} 个</b>待作业地块，合计 <b>${num((pending.length ? pending : lands).reduce((a, b) => a + b.area, 0))} 亩</b>。` +
      (pending.length ? '建议优先处理临近时限的地块，可在调度管理中一键生成派单建议。' : '暂无待作业地块，任务均已派发。')
    table = {
      columns: ['地块', '片区', '面积', '作物', '状态', '单价'],
      rows: rows.map((l) => [l.name, l.district, `${l.area} 亩`, l.crop, l.status, `${l.price} 元/亩`])
    }
  }

  await mark(3, 'done', 460)
  return { content, steps, artifacts: { table, links: [{ text: '打开地块管理', route: '/lands' }] } }
}

async function runMachine({ message, context, role, tool, mark, steps }) {
  const machines = await api.listMachines()
  await tool(2, 'T02', { scope: role === 'leader' ? '河东片区' : '全公司' }, { count: machines.length })
  await mark(2)

  const online = machines.filter((m) => m.online)
  const working = machines.filter((m) => m.workStatus === '作业中')
  const idle = online.filter((m) => m.workStatus !== '作业中')
  const rate = machines.length ? ((online.length / machines.length) * 100).toFixed(1) : '0.0'
  const content = `当前在线农机 <b>${online.length}/${machines.length} 台</b>，在线率 <b>${rate}%</b>；作业中 <b>${working.length} 台</b>，` +
    `待机 <b>${idle.length} 台</b>${idle.length ? `（${idle.map((m) => m.code).join('、')}）` : ''}。` +
    `页面「${context.title || '农机与农机手'}」的数据范围已按你的角色收敛。`
  await mark(3, 'done', 460)
  return {
    content,
    steps,
    artifacts: {
      table: {
        columns: ['农机', '型号', '片区', '状态', '效率', '当前地块'],
        rows: machines.slice(0, 6).map((m) => [m.code, m.model, m.district, m.workStatus, `${m.efficiency} 亩/小时`, m.landId || '—'])
      },
      links: [{ text: '打开农机管理', route: '/machines' }, { text: '前往实时监控', route: '/monitor' }]
    }
  }
}

async function runAnomaly({ message, context, tool, mark, steps }) {
  const list = await api.listWorkorders()
  const abnormal = list.filter((w) => w.status === '异常' || (w.anomalies && w.anomalies.length))
  await tool(2, 'T06', { scope: '异常/待复核', context: context.title || '—' }, { count: abnormal.length })
  await mark(2)

  const focus = abnormal[0]
  if (!focus) {
    await mark(3, 'done', 420)
    return { content: '当前范围内没有异常作业单，最近核算结果均在允许误差内。', steps, artifacts: { links: [{ text: '打开核算列表', route: '/workorders' }] } }
  }
  const order = await api.getWorkorder(focus.id)
  await tool(3, 'T07', { workorderId: focus.id }, { anomalies: order?.anomalies?.length || 0 })
  await mark(3)

  const a = order?.anomalies || []
  const lines = a.slice(0, 3).map((x) => `· <b>${x.type}</b>｜${x.at}（轨迹点 #${x.point}）：${x.desc}`).join('<br/>')
  const content = `当前共 <b>${abnormal.length} 份</b>异常作业单。以 <b>${focus.id}</b>（${focus.landName}）为例：GPS 轨迹面积 ${order?.gpsArea} 亩，` +
    `有效面积 ${order?.effectiveArea} 亩，差异 ${order?.diffRate}%。<br/>${lines || '异常点位待补充。'}<br/>` +
    '以上判断来自轨迹点位与地块边界裁剪结果，复核结论需你确认后才会写入。'
  return {
    content,
    steps,
    artifacts: {
      table: {
        columns: ['异常类型', '发生时间', '轨迹点', '影响'],
        rows: a.map((x) => [x.type, x.at, `#${x.point}`, x.area ? `扣除 ${x.area} 亩` : `${x.minutes} 分钟`])
      },
      links: [{ text: '打开作业单详情', route: '/workorders' }]
    }
  }
}

async function runSettlement({ message, context, tool, mark, steps }) {
  const list = await api.listStatements()
  await tool(2, 'T08', { period: '2026-11', status: '全部' }, { count: list.length })
  await mark(2)

  const askDetail = /这笔|该笔|为什么|低|扣减|组成|解释/.test(message)
  const focus = askDetail ? (list.find((s) => s.id === context.queryId) || list[0]) : null

  if (focus) {
    const detail = await api.getStatement(focus.id)
    const items = detail?.items || []
    const ded = items.filter((i) => i.deduction > 0)
    const content = `已结合当前页面「${context.title || '内部结算管理'}」：结算单 <b>${focus.id}</b>（${focus.operatorName}，${focus.period}）结算金额 ` +
      `<b>¥ ${Number(focus.amount).toLocaleString('zh-CN')}</b>，作业面积 ${focus.area} 亩，扣减合计 ¥ ${focus.deduction}。` +
      (ded.length
        ? `<br/>扣减原因：${ded.map((i) => `${i.landName} ${i.reason || '按规则扣减'}，扣 ¥ ${i.deduction}`).join('；')}。`
        : '<br/>本单无质量扣减项。') +
      '<br/>金额与单价均来自结算接口，Agent 不参与计算，也不修改正式结算数据。'
    await mark(3, 'done', 460)
    return {
      content,
      steps,
      artifacts: {
        table: {
          columns: ['地块', '作业类型', '面积', '单价', '扣减', '金额'],
          rows: items.map((i) => [i.landName, i.type, `${i.area} 亩`, `${i.price} 元/亩`, `¥ ${i.deduction}`, `¥ ${i.amount.toLocaleString('zh-CN')}`])
        },
        links: [{ text: '打开结算详情', route: '/settlement' }]
      }
    }
  }

  const total = list.reduce((a, b) => a + b.amount, 0)
  const pending = list.filter((s) => s.status === '待核对')
  const content = `本月共 <b>${list.length} 份</b>结算单，结算金额合计 <b>¥ ${total.toLocaleString('zh-CN')}</b>，` +
    `扣减合计 ¥ ${list.reduce((a, b) => a + b.deduction, 0)}，其中待核对 <b>${pending.length} 份</b>。` +
    '可按周、月、季度继续追问具体单号，金额均来自结算接口。'
  await mark(3, 'done', 460)
  return {
    content,
    steps,
    artifacts: {
      table: {
        columns: ['结算单', '机手', '周期', '面积', '扣减', '金额', '状态'],
        rows: list.slice(0, 6).map((s) => [s.id, s.operatorName, s.period, `${s.area} 亩`, `¥ ${s.deduction}`, `¥ ${s.amount.toLocaleString('zh-CN')}`, s.status])
      },
      links: [{ text: '打开内部结算', route: '/settlement' }]
    }
  }
}

async function runTodo({ role, tool, mark, steps }) {
  const [workorders, statements, tasks] = await Promise.all([api.listWorkorders(), api.listStatements(), api.listTaskRows()])
  const abnormal = workorders.filter((w) => w.status === '异常')
  const review = workorders.filter((w) => w.status === '待复核')
  const pendingPay = statements.filter((s) => s.status === '待核对')
  const now = Date.now()
  const overdue = tasks.filter((t) => {
    const value = t.endTime
    const end = Array.isArray(value)
      ? new Date(value[0], (value[1] || 1) - 1, value[2] || 1, value[3] || 0, value[4] || 0, value[5] || 0).getTime()
      : new Date(value).getTime()
    return Number(t.status) === 0 && Number.isFinite(end) && end <= now
  })
  await tool(2, 'T07', { scope: role === 'leader' ? '河东片区' : '全公司' }, { abnormal: abnormal.length, review: review.length })
  await mark(2)
  await mark(3, 'done', 460)

  const content = `当前待处理事项：异常作业单 <b>${abnormal.length} 份</b>、待复核 <b>${review.length} 份</b>、待核对结算 <b>${pendingPay.length} 份</b>、已截止作业需求 <b>${overdue.length} 个</b>。` +
    '建议顺序：先复核异常并处理已截止需求，再核对结算，避免金额二次调整。'
  return {
    content,
    steps,
    artifacts: {
      table: {
        columns: ['类型', '数量', '优先级', '建议动作'],
        rows: [
          ['异常作业单', abnormal.length, '高', '确认轨迹点位后复核'],
          ['待复核作业单', review.length, '高', '核对面积差异'],
          ['已截止作业需求', overdue.length, '中', '调整截止时间或下架'],
          ['待核对结算', pendingPay.length, '中', '核对扣减项后付款']
        ]
      },
      links: [{ text: '去处理异常', route: '/workorders' }, { text: '去核对结算', route: '/settlement' }]
    }
  }
}
