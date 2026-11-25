import * as api from '@/services/api'
import { workorders, agents, tools } from '@/mock'

const sleep = (ms) => new Promise((r) => setTimeout(r, ms))
const toolName = (id) => tools.find((t) => t.id === id)?.name || id
const fmt = (n, d = 1) => Number(n).toFixed(d)

export const QUICK_ASKS = {
  'MA-01': ['为河东片区生成今日派单建议', '哪些农机目前闲置？', '批量生成明日调度方案'],
  'MA-02': ['研判 ZY-20261123-002 为什么面积偏小', '最近有哪些超界作业？', '解释异常停留的判定依据'],
  'MA-03': ['合作农机的结算周期是多久？', '质量不达标如何扣减？', '跨片区调度需要谁确认？'],
  'MA-04': ['本月哪个片区作业效率最高？', '最近七天作业面积变化', '哪些任务存在超期风险？'],
  'MA-05': ['汇总本周异常并生成复核清单', '生成本周运营简报', '协调调度与结算待办']
}

const AGENT_GREETING = {
  'MA-01': '我是调度建议 Agent。我会结合待作业地块、农机实时位置与历史效率生成派单建议，所有派单都需要你确认后才会执行。',
  'MA-02': '我是异常研判 Agent。我只会基于轨迹点位、边界与核算结果解释异常，不会估算面积或直接修改复核状态。',
  'MA-03': '我是制度与口径问答 Agent。回答会标注制度名称、版本与条款号，并只检索你有权限访问的知识域。',
  'MA-04': '我是数据分析 Agent。所有数字来自统计接口，我会标注统计区间与口径。',
  'MA-05': '我是综合运营 Agent。我可以编排调度、核算、结算等多个工具完成复杂任务，写操作会先请你确认。'
}

export function agentGreeting(agentId) {
  return AGENT_GREETING[agentId] || AGENT_GREETING['MA-05']
}

function plan(agentId, message, role) {
  const base = [
    { text: `识别意图并校验权限（角色：${roleLabel(role)}）`, status: 'pending' },
    { text: '解析业务实体与查询参数', status: 'pending' }
  ]
  const tail = {
    'MA-01': [
      { text: '查询待作业地块 T03', status: 'pending' },
      { text: '查询农机实时位置与作业状态 T02', status: 'pending' },
      { text: '查询近 30 天历史作业效率 T04', status: 'pending' },
      { text: '综合时限、距离与效率生成调度建议', status: 'pending' }
    ],
    'MA-02': [
      { text: '查询作业单与轨迹 T06', status: 'pending' },
      { text: '查询异常记录与扣减项 T07', status: 'pending' },
      { text: '定位异常轨迹点位并比对地块边界', status: 'pending' },
      { text: '生成可追溯的异常研判结论', status: 'pending' }
    ],
    'MA-03': [
      { text: '计算可访问知识域（服务端权限过滤）', status: 'pending' },
      { text: '检索制度知识库 T10', status: 'pending' },
      { text: '提取原文片段与条款号', status: 'pending' }
    ],
    'MA-04': [
      { text: '解析统计指标、维度与时间区间', status: 'pending' },
      { text: '调用统计报表接口 T09', status: 'pending' },
      { text: '生成表格与图表结果', status: 'pending' }
    ],
    'MA-05': [
      { text: '拆解复杂任务为可执行子步骤', status: 'pending' },
      { text: '查询异常作业单 T07', status: 'pending' },
      { text: '查询结算待办 T08', status: 'pending' },
      { text: '汇总生成协同处置清单', status: 'pending' }
    ]
  }[agentId] || [{ text: '生成回答', status: 'pending' }]
  return [...base, ...tail]
}

function roleLabel(role) {
  return { admin: '公司管理员', leader: '片区生产队长', finance: '财务人员', operator: '农机手' }[role] || '未知'
}

/**
 * 以事件流方式执行 Agent，模拟 LangGraph 工作流。
 * emit(event): step / tool / reply
 */
export async function runAgent({ agentId, message, role = 'admin', emit = () => {} }) {
  const steps = plan(agentId, message, role)
  emit({ type: 'plan', steps })

  const mark = async (index, status = 'done', delay = 520) => {
    steps[index].status = status
    emit({ type: 'step', index, steps })
    await sleep(delay)
  }

  const tool = async (index, toolId, payload, result, delay = 640) => {
    emit({ type: 'tool-start', index, toolId, name: toolName(toolId), payload })
    await sleep(delay)
    emit({ type: 'tool-end', index, toolId, name: toolName(toolId), payload, result })
    steps[index].tool = { toolId, name: toolName(toolId), payload, result }
  }

  await mark(0, 'done', 420)
  await mark(1, 'done', 380)

  let reply
  if (agentId === 'MA-01') reply = await runDispatch(steps, mark, tool, role, message)
  else if (agentId === 'MA-02') reply = await runAnomaly(steps, mark, tool, role, message)
  else if (agentId === 'MA-03') reply = await runKnowledge(steps, mark, tool, role, message)
  else if (agentId === 'MA-04') reply = await runAnalysis(steps, mark, tool, role, message)
  else reply = await runOrchestrator(steps, mark, tool, role, message)

  emit({ type: 'reply', reply })
  return reply
}

async function runDispatch(steps, mark, tool, role, message) {
  // 后端无调度建议/推荐接口，无法基于真实距离与历史效率生成建议。
  // 这里改为读取真实待接单作业需求（FarmWork status=0），如实说明"无匹配建议能力"，
  // 不再用硬编码的建议列表制造"AI 已给出方案"的假象。
  // 查询异常必须向上传递，不能把接口失败误报成“没有可接单需求”。
  const { list: pending } = await api.listTasks({ status: 0 })
  await tool(2, 'T03', { district: role === 'leader' ? '河东片区' : '全公司', status: '可接单' }, { count: pending.length, sample: pending.slice(0, 3).map((t) => t.landName || t.name) })
  await mark(2)
  await tool(3, 'T02', { district: role === 'leader' ? '河东片区' : '全公司' }, { source: 'agriculturalMachinery' })
  await mark(3)
  await mark(4, 'done', 600)

  const content = pending.length
    ? `当前有 <b>${pending.length} 条</b>可接单作业需求（来自 /farmWork/list，status=0）。` +
      '但后端未提供农机位置、机手档案与历史作业效率的关联接口，' +
      '因此我不能给出「哪台农机、哪位机手、多远、多少效率」的匹配建议——这部分属于<b>后端待补充</b>能力。' +
      '请前往调度管理查看作业需求；实际派单能力仍待后端补充。'
    : '当前没有状态为「可接单」的作业需求。若列表为空，请先在农田作业管理发布作业需求。'

  return {
    content,
    steps,
    artifacts: {
      table: {
        columns: ['作业需求', '地块', '面积', '截止时间', '状态'],
        rows: pending.slice(0, 8).map((t) => [
          t.name || t.id,
          (t.landPlotName || []).join('、') || '-',
          `${t.area ?? '-'} 亩`,
          t.endTime || '-',
          t.status === 0 ? '可接单' : t.status === 1 ? '已结束' : '-'
        ])
      },
      links: [{ text: '前往调度管理', route: '/dispatch' }]
    }
  }
}

async function runAnomaly(steps, mark, tool, role, message) {
  const idMatch = message.match(/ZY-\d{8}-\d{3}/)
  const targetId = idMatch ? idMatch[0] : workorders.find((w) => w.status === '异常')?.id
  const order = await api.getWorkorder(targetId)
  await tool(2, 'T06', { workorderId: targetId }, { effectiveArea: order?.effectiveArea, gpsArea: order?.gpsArea, points: order?.pointCount })
  await mark(2)
  await tool(3, 'T07', { workorderId: targetId, type: '全部' }, { anomalies: order?.anomalies?.length || 0 })
  await mark(3)
  await mark(4, 'done', 760)
  await mark(5, 'done', 700)

  if (!order) {
    return { content: '暂时查不到这条作业单记录，请确认单据号。', steps, artifacts: { links: [{ text: '前往核算列表', route: '/workorders' }] } }
  }
  const a = order.anomalies || []
  const lines = a.map((x) => `· <b>${x.type}</b>｜${x.at}（第 ${x.point} 个轨迹点）：${x.desc}`).join('<br/>')
  const content = `作业单 <b>${order.id}</b>（${order.landName}）GPS 轨迹面积 <b>${order.gpsArea} 亩</b>，系统有效面积 <b>${order.effectiveArea} 亩</b>，差异 <b>${order.diffRate}%</b>。<br/>` +
    (a.length ? `共识别 ${a.length} 处异常：<br/>${lines}<br/>以上判断均来自轨迹点位与地块边界裁剪结果，未做任何模型估算。` : '未发现异常轨迹，面积差异在允许误差范围内。')

  return {
    content,
    steps,
    artifacts: {
      table: {
        columns: ['异常类型', '发生时间', '轨迹点', '影响'],
        rows: a.map((x) => [x.type, x.at, `#${x.point}`, x.area ? `扣除 ${x.area} 亩` : `${x.minutes} 分钟`])
      },
      links: [{ text: '打开作业单详情', route: '/workorders' }]
    },
    approval: {
      id: `AP-${Date.now()}`,
      title: '研判结果待复核确认',
      desc: 'Agent 仅提供研判建议，复核状态变更需你确认后由后端接口执行。',
      payload: { workorderId: order.id }
    }
  }
}

async function runKnowledge(steps, mark, tool, role, message) {
  await mark(2, 'done', 380)
  const hits = await api.searchKnowledge(message, role)
  await tool(3, 'T10', { query: message, category: '按角色过滤' }, { hits: hits.length })
  await mark(3)
  await mark(4, 'done', 620)

  if (!hits.length) {
    return {
      content: '在你的权限范围内没有检索到相关制度条款。请换一种问法，或联系管理员确认知识库是否已上传对应文档。',
      steps,
      artifacts: { links: [{ text: '查看知识库', route: '/agent/knowledge' }] }
    }
  }
  const body = hits.map((h) => `<b>${h.doc}</b>（${h.version}，${h.clause}）：${h.text}`).join('<br/>')
  return {
    content: `根据知识库检索结果：<br/>${body}`,
    steps,
    artifacts: {
      citations: hits.map((h) => ({ doc: h.doc, version: h.version, clause: h.clause, text: h.text, category: h.category })),
      links: [{ text: '打开知识库', route: '/agent/knowledge' }]
    }
  }
}

async function runAnalysis(steps, mark, tool, role, message) {
  const report = await api.getReport('week')
  await tool(2, 'T09', { metric: '作业面积/效率/异常率', dimension: '片区/农机/日期', range: '2026-11-17 ~ 2026-11-23' }, { districts: report.district.length, days: 7 })
  await mark(2)
  await mark(3, 'done', 760)

  const isTrend = message.includes('趋势') || message.includes('变化') || message.includes('七天') || message.includes('7天')
  if (isTrend) {
    return {
      content: `近 7 天作业面积从 <b>${report.trend7.area[0]}</b> 亩增长至 <b>${report.trend7.area[6]}</b> 亩，11-22 达到峰值 1,428 亩；11-23 环比下降 9.9%，主要受北塬片区降雨影响。统计区间：2026-11-17 至 2026-11-23，数据来源：统计报表接口 T09。`,
      steps,
      artifacts: {
        chart: { type: 'line', categories: report.trend7.days, series: [{ name: '作业面积(亩)', data: report.trend7.area }] },
        links: [{ text: '查看统计报表', route: '/reports' }]
      }
    }
  }
  const top = [...report.district].sort((a, b) => b.eff - a.eff)[0]
  return {
    content: `按片区维度统计，<b>${top.name}</b> 平均作业效率最高，为 <b>${top.eff} 亩/小时</b>，完成率 ${top.rate}%。统计区间：2026-11-17 至 2026-11-23，口径：有效作业面积 ÷ 作业时长，数据来自统计报表接口 T09。`,
    steps,
    artifacts: {
      table: {
        columns: ['片区', '作业面积(亩)', '完成率', '平均效率(亩/小时)', '异常数'],
        rows: report.district.map((d) => [d.name, d.area, `${d.rate}%`, d.eff, d.abnormal])
      },
      links: [{ text: '查看统计报表', route: '/reports' }]
    }
  }
}

async function runOrchestrator(steps, mark, tool, role, message) {
  const abnormal = workorders.filter((w) => w.status === '异常' || w.status === '待复核')
  await tool(3, 'T07', { status: '异常/待复核', district: role === 'leader' ? '河东片区' : '全公司' }, { count: abnormal.length })
  await mark(3)
  const statements = await api.listStatements({})
  await tool(4, 'T08', { period: '2026-11 上半月', status: '待核对' }, { count: statements.filter((s) => s.status === '待核对').length })
  await mark(4)
  await mark(5, 'done', 900)

  return {
    content: `已完成跨域编排：当前有 <b>${abnormal.length} 份</b>异常/待复核作业单，其中 <b>${abnormal.filter((w) => w.status === '异常').length} 份</b>涉及面积扣减；结算侧有 <b>${statements.filter((s) => s.status === '待核对').length} 份</b>结算单待核对。建议先完成异常复核，再生成结算，避免金额二次调整。`,
    steps,
    artifacts: {
      table: {
        columns: ['类型', '单据号', '地块/机手', '状态', '处理建议'],
        rows: [
          ...abnormal.slice(0, 4).map((w) => ['异常复核', w.id, w.landName, w.status, '确认轨迹点位后完成复核']),
          ...statements.filter((s) => s.status === '待核对').slice(0, 2).map((s) => ['结算核对', s.id, s.operatorName, s.status, '核对扣减项后提交付款'])
        ]
      },
      links: [
        { text: '去处理异常', route: '/workorders' },
        { text: '去核对结算', route: '/settlement' }
      ]
    }
  }
}

export { agents }
