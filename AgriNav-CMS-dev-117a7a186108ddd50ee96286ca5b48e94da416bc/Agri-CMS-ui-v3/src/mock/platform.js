/* 权限、审计、Agent、工具、工作流、知识库 数据 */

export const users = [
  { id: 'U001', name: '王建民', phone: '13800100001', role: 'admin', roleName: '公司管理员', district: '全公司', status: '启用', lastLogin: '2026-11-23 08:12', avatar: '王' },
  { id: 'U002', name: '王强', phone: '13705371101', role: 'leader', roleName: '片区生产队长', district: '河东片区', status: '启用', lastLogin: '2026-11-23 07:40', avatar: '强' },
  { id: 'U003', name: '张海涛', phone: '13705371103', role: 'leader', roleName: '片区生产队长', district: '河西片区', status: '启用', lastLogin: '2026-11-22 19:05', avatar: '涛' },
  { id: 'U004', name: '刘敏', phone: '13900100004', role: 'finance', roleName: '财务人员', district: '全公司', status: '启用', lastLogin: '2026-11-23 08:30', avatar: '敏' },
  { id: 'U005', name: '陈立新', phone: '13705371106', role: 'operator', roleName: '农机手', district: '南滩片区', status: '启用', lastLogin: '2026-11-23 06:55', avatar: '新' },
  { id: 'U006', name: '周文斌', phone: '13705371108', role: 'operator', roleName: '农机手', district: '北塬片区', status: '停用', lastLogin: '2026-11-18 17:20', avatar: '斌' },
  { id: 'U007', name: '孙丽', phone: '13900100007', role: 'finance', roleName: '财务人员', district: '全公司', status: '启用', lastLogin: '2026-11-21 16:44', avatar: '丽' }
]

export const roles = [
  {
    id: 'R01', name: '公司管理员', code: 'admin', scope: '全公司', users: 1,
    perms: ['地块维护', '单价规则', '派单调度', '轨迹查看', '面积复核', '结算导出', '报表分析', '审计日志', 'Agent配置']
  },
  {
    id: 'R02', name: '片区生产队长', code: 'leader', scope: '本片区', users: 2,
    perms: ['地块查看', '派单调度', '轨迹查看', '面积初核', '报表分析']
  },
  {
    id: 'R03', name: '财务人员', code: 'finance', scope: '结算范围', users: 2,
    perms: ['单价规则查看', '结算生成', '结算导出', '报表分析']
  },
  {
    id: 'R04', name: '农机手', code: 'operator', scope: '仅本人', users: 2,
    perms: ['任务认领', '开始/结束作业', '本人轨迹', '本人结算', '发起复核']
  }
]

export const loginLogs = [
  ['王建民', 'admin', '10.20.31.12', 'Chrome 131 / Windows', '成功'],
  ['刘敏', 'finance', '10.20.31.28', 'Edge 131 / Windows', '成功'],
  ['王强', 'leader', '117.136.45.9', '移动端 App', '成功'],
  ['未知账号', '-', '45.12.88.201', 'curl/8.4', '失败'],
  ['张海涛', 'leader', '117.136.45.18', 'Chrome 130 / Windows', '成功'],
  ['周文斌', 'operator', '223.104.9.66', '移动端 App', '成功'],
  ['刘敏', 'finance', '10.20.31.28', 'Edge 131 / Windows', '失败'],
  ['孙丽', 'finance', '10.20.31.31', 'Chrome 131 / macOS', '成功']
].map(([user, role, ip, ua, result], i) => ({
  id: `LG${i + 1}`,
  user, role, ip, ua, result,
  at: `2026-11-23 ${String(7 + i).padStart(2, '0')}:${String((i * 9) % 60).padStart(2, '0')}`,
  location: ip.startsWith('10.') ? '公司内网' : '山东省德州市'
}))

export const operationLogs = [
  ['王建民', '地块数据导入', '地块管理', '批量导入 12 条一地一码矢量数据'],
  ['王建民', '修改单价规则', '内部结算', 'PR05 收割·二类地 62 → 64 元/亩'],
  ['王强', '批量派单', '作业调度', '为河东片区 4 个地块派发旋耕任务'],
  ['刘敏', '生成结算单', '内部结算', '生成 2026-11 上半月结算单 6 份'],
  ['张海涛', '复核作业单', '轨迹与核算', '复核通过 ZY-20261122-001'],
  ['王建民', '调整 Agent 权限', 'Agent 中心', 'MA-01 数据范围调整为全公司'],
  ['刘敏', '导出结算明细', '内部结算', '导出 2026-11 上半月 Excel'],
  ['王强', '撤销任务', '作业调度', '撤销 RW-20261123-012'],
  ['系统', '异常告警', '权限与审计', '检测到越权查询请求，已拦截'],
  ['王建民', '编辑工作流', 'Agent 中心', 'WF-01 新增人工审批节点']
].map(([user, action, module, detail], i) => ({
  id: `OP${i + 1}`,
  user, action, module, detail,
  ip: user === '系统' ? '127.0.0.1' : `10.20.31.${12 + i}`,
  at: `2026-11-23 ${String(8 + i).padStart(2, '0')}:${String((i * 13) % 60).padStart(2, '0')}`,
  result: i === 8 ? '已拦截' : '成功'
}))

export const agents = [
  {
    id: 'MA-01', name: '调度建议 Agent', type: '业务决策辅助', status: '运行中',
    model: 'GLM-4.5', temperature: 0.2, scope: '全公司（按用户角色收敛）',
    desc: '结合待作业地块、农机实时位置、历史效率与时限，生成可解释的派单建议，须人工确认后执行。',
    tools: ['T01', 'T02', 'T03', 'T04', 'T05'],
    prompt: '你是农机调度建议助手。必须先查询地块、农机位置、作业状态与历史效率，再给出建议；不得编造数据；所有派单动作必须等待人工确认。',
    calls: 1284, successRate: 98.6, latency: 2.4, tokens: 1284000, updatedAt: '2026-11-20 14:20'
  },
  {
    id: 'MA-02', name: '异常研判 Agent', type: '风险研判', status: '运行中',
    model: 'Qwen2.5-72B', temperature: 0.1, scope: '本片区 / 全公司',
    desc: '定位异常轨迹点位，解释超界、停留、重复覆盖的判定依据，并给出处理建议。',
    tools: ['T01', 'T06', 'T07'],
    prompt: '你是作业异常研判助手。只能基于轨迹点位、时长、边界与核算结果解释异常，不得估算面积或金额，不得直接修改复核状态。',
    calls: 862, successRate: 97.9, latency: 3.1, tokens: 986000, updatedAt: '2026-11-21 09:10'
  },
  {
    id: 'MA-03', name: '制度与口径问答 Agent', type: '知识问答', status: '运行中',
    model: 'GLM-4.5-Air', temperature: 0.3, scope: '按知识域权限过滤',
    desc: '检索公开制度、内部管理办法与结算规则，回答时附制度名称、版本与条款号。',
    tools: ['T09', 'T10'],
    prompt: '你是制度与结算口径问答助手。回答必须引用知识库原文片段，标注制度名称、版本与条款号；检索不到时明确说明。',
    calls: 2416, successRate: 99.2, latency: 1.8, tokens: 642000, updatedAt: '2026-11-22 16:40'
  },
  {
    id: 'MA-04', name: '数据分析 Agent', type: '数据分析', status: '运行中',
    model: 'Qwen2.5-72B', temperature: 0.2, scope: '全公司 / 本片区',
    desc: '将自然语言问题转为统计查询，输出数字、表格与图表，并标注统计口径与区间。',
    tools: ['T09', 'T03', 'T04'],
    prompt: '你是运营数据分析助手。所有数字必须来自统计接口；输出需标注统计区间与口径；不得推算缺失数据。',
    calls: 538, successRate: 96.8, latency: 2.9, tokens: 712000, updatedAt: '2026-11-22 11:05'
  },
  {
    id: 'MA-05', name: '综合运营 Agent', type: '多工具编排', status: '灰度中',
    model: 'GLM-4.5', temperature: 0.2, scope: '全公司',
    desc: '面向复杂问题自动编排多个业务工具，支持调度、核算、结算跨域协同，写操作强制审批。',
    tools: ['T01', 'T02', 'T03', 'T04', 'T06', 'T08', 'T09'],
    prompt: '你是综合运营助手。复杂任务先拆解为步骤，逐项调用工具；涉及写操作必须请求人工审批；每步输出工具结果摘要。',
    calls: 126, successRate: 94.4, latency: 4.6, tokens: 268000, updatedAt: '2026-11-23 08:00'
  }
]

export const tools = [
  ['T01', '查询地块', '地块管理', '/api/land/query', 'GET', 'landId / district / keyword', '地块档案、面积、单价、边界', '本片区', true, 1824, 0.4],
  ['T02', '查询农机位置', '农机管理', '/api/machine/location', 'GET', 'machineId / district', '经纬度、在线状态、作业状态', '本片区', true, 1620, 0.6],
  ['T03', '查询待作业任务', '作业调度', '/api/task/pending', 'GET', 'district / deadline', '任务列表、时限、优先级', '本片区', true, 986, 0.3],
  ['T04', '查询历史作业效率', '统计分析', '/api/report/machine-efficiency', 'GET', 'machineId / range', '亩/小时、完成率、异常率', '本片区', true, 742, 0.5],
  ['T05', '创建调度建议', '作业调度', '/api/dispatch/suggestion', 'POST', 'landIds / machineId / reason', '建议编号、建议明细', '本片区', true, 318, 2.1],
  ['T06', '查询作业单', '轨迹核算', '/api/workorder/query', 'GET', 'workorderId / landId', '作业单、面积、轨迹、状态', '本片区', true, 1206, 0.4],
  ['T07', '查询异常记录', '轨迹核算', '/api/workorder/anomaly', 'GET', 'workorderId / type', '异常点位、时长、面积扣减', '本片区', true, 654, 0.8],
  ['T08', '查询结算单', '内部结算', '/api/settlement/query', 'GET', 'statementId / period', '结算明细、扣减项、金额', '结算范围', true, 438, 0.5],
  ['T09', '查询统计报表', '统计分析', '/api/report/query', 'GET', 'metric / dimension / range', '统计结果与口径说明', '报表范围', true, 812, 0.7],
  ['T10', '检索制度知识库', '知识库', '/api/knowledge/search', 'POST', 'query / category', '片段、制度名称、条款号', '知识域', true, 2140, 0.3]
].map(([id, name, module, endpoint, method, params, result, scope, enabled, calls, errorRate]) => ({
  id, name, module, endpoint, method, params, result, scope, enabled, calls, errorRate,
  desc: `${name}工具，供 Agent 在工作流中调用，返回结构化业务数据。`,
  updatedAt: '2026-11-18 10:00'
}))

export const workflows = [
  {
    id: 'WF-01', name: '调度建议生成与确认', agent: 'MA-01', status: '启用', runs: 318, successRate: 97.5,
    desc: '用户提出调度需求 → 意图识别 → 查询待作业地块 → 查询农机状态 → 查询历史效率 → 生成方案 → 人工确认 → 派单',
    nodes: [
      { id: 'n1', type: 'start', name: '用户提出调度需求', x: 60, y: 70 },
      { id: 'n2', type: 'intent', name: '意图识别', x: 280, y: 70 },
      { id: 'n3', type: 'tool', name: '查询待作业地块', x: 500, y: 40, tool: 'T03' },
      { id: 'n4', type: 'tool', name: '查询农机状态', x: 500, y: 130, tool: 'T02' },
      { id: 'n5', type: 'tool', name: '查询历史效率', x: 500, y: 220, tool: 'T04' },
      { id: 'n6', type: 'condition', name: '是否满足时限', x: 720, y: 130 },
      { id: 'n7', type: 'agent', name: '生成调度方案', x: 900, y: 130, agent: 'MA-01' },
      { id: 'n8', type: 'approval', name: '人工确认', x: 900, y: 250 },
      { id: 'n9', type: 'tool', name: '调用派单接口', x: 900, y: 370, tool: 'T05' },
      { id: 'n10', type: 'output', name: '返回执行结果', x: 1120, y: 250 },
      { id: 'n11', type: 'exception', name: '异常处理：转人工', x: 720, y: 300 }
    ],
    edges: [
      ['n1', 'n2'], ['n2', 'n3'], ['n2', 'n4'], ['n2', 'n5'], ['n3', 'n6'], ['n4', 'n6'], ['n5', 'n6'],
      ['n6', 'n7'], ['n6', 'n11'], ['n7', 'n8'], ['n8', 'n9'], ['n9', 'n10']
    ]
  },
  {
    id: 'WF-02', name: '异常作业单研判', agent: 'MA-02', status: '启用', runs: 462, successRate: 96.8,
    desc: '接收异常作业单 → 查询轨迹与边界 → 分析越界/停留/重复 → 定位点位 → 输出研判与处理建议 → 人工复核',
    nodes: [
      { id: 'm1', type: 'start', name: '接收异常作业单', x: 60, y: 90 },
      { id: 'm2', type: 'intent', name: '识别异常类型', x: 280, y: 90 },
      { id: 'm3', type: 'tool', name: '查询作业单轨迹', x: 500, y: 40, tool: 'T06' },
      { id: 'm4', type: 'tool', name: '查询异常记录', x: 500, y: 160, tool: 'T07' },
      { id: 'm5', type: 'agent', name: '异常原因研判', x: 720, y: 100, agent: 'MA-02' },
      { id: 'm6', type: 'condition', name: '是否需人工复核', x: 940, y: 100 },
      { id: 'm7', type: 'approval', name: '人工复核', x: 1140, y: 60 },
      { id: 'm8', type: 'output', name: '输出研判报告', x: 1140, y: 180 }
    ],
    edges: [
      ['m1', 'm2'], ['m2', 'm3'], ['m2', 'm4'], ['m3', 'm5'], ['m4', 'm5'], ['m5', 'm6'], ['m6', 'm7'], ['m6', 'm8'], ['m7', 'm8']
    ]
  }
]

export const knowledgeDocs = [
  { id: 'K01', name: '合作农机作业结算管理办法.pdf', category: '内部管理办法', version: 'V2.1', chunks: 64, status: '已向量化', updatedAt: '2026-10-12', uploader: '王建民', roles: ['admin', 'leader', 'finance'], size: '2.4 MB' },
  { id: 'K02', name: '作业面积核算规则.pdf', category: '公开制度', version: 'V2.1', chunks: 48, status: '已向量化', updatedAt: '2026-10-12', uploader: '王建民', roles: ['admin', 'leader', 'finance'], size: '1.8 MB' },
  { id: 'K03', name: '农机调度管理办法.docx', category: '内部管理办法', version: 'V1.4', chunks: 36, status: '已向量化', updatedAt: '2026-09-28', uploader: '王建民', roles: ['admin', 'leader'], size: '860 KB' },
  { id: 'K04', name: '2026 年作业单价表.xlsx', category: '结算与单价规则', version: '2026.11', chunks: 22, status: '已向量化', updatedAt: '2026-11-01', uploader: '刘敏', roles: ['admin', 'finance'], size: '420 KB' },
  { id: 'K05', name: '异常作业判定与处理细则.pdf', category: '公开制度', version: 'V1.2', chunks: 41, status: '已向量化', updatedAt: '2026-08-16', uploader: '王建民', roles: ['admin', 'leader', 'finance'], size: '1.2 MB' },
  { id: 'K06', name: '农机手服务规范.txt', category: '公开制度', version: 'V1.0', chunks: 18, status: '处理中', updatedAt: '2026-11-22', uploader: '王建民', roles: ['admin', 'leader'], size: '36 KB' },
  { id: 'K07', name: '财务付款流程说明.pdf', category: '结算与单价规则', version: 'V1.1', chunks: 27, status: '已向量化', updatedAt: '2026-07-09', uploader: '刘敏', roles: ['admin', 'finance'], size: '980 KB' }
]

export const knowledgeChunks = [
  { doc: '合作农机作业结算管理办法', version: 'V2.1', clause: '第 6 条', category: '内部管理办法', roles: ['admin', 'leader', 'finance'], text: '结算周期为自然月，次月 5 日前出具结算单，7 个工作日内完成付款。' },
  { doc: '合作农机作业结算管理办法', version: 'V2.1', clause: '第 8 条', category: '内部管理办法', roles: ['admin', 'leader', 'finance'], text: '作业质量抽测不达标的，按不合格面积对应金额的 5%—10% 扣减，具体比例由片区生产队长初核、管理员终审确定。' },
  { doc: '作业面积核算规则', version: 'V2.1', clause: '第 4.2 条', category: '公开制度', roles: ['admin', 'leader', 'finance'], text: '轨迹超出地块边界的部分不计入有效作业面积，边界容差为 2 米。' },
  { doc: '作业面积核算规则', version: 'V2.1', clause: '第 4.3 条', category: '公开制度', roles: ['admin', 'leader', 'finance'], text: '同一地块重复覆盖区域仅计一次，去重后按有效作业长度乘以标准幅宽计算面积。' },
  { doc: '作业面积核算规则', version: 'V2.1', clause: '第 4.5 条', category: '公开制度', roles: ['admin', 'leader', 'finance'], text: '单点停留超过 5 分钟的时段不计入作业时长，异常停留需在作业单中标注。' },
  { doc: '农机调度管理办法', version: 'V1.4', clause: '第 3 条', category: '内部管理办法', roles: ['admin', 'leader'], text: '派单应优先考虑农机与地块直线距离、农机当前作业状态及近 30 天历史效率，跨片区调度需管理员确认。' },
  { doc: '2026 年作业单价表', version: '2026.11', clause: '附表 1', category: '结算与单价规则', roles: ['admin', 'finance'], text: '旋耕一类地 45 元/亩，二类地 42 元/亩，三类地 38 元/亩；收割一类地 68 元/亩。' },
  { doc: '财务付款流程说明', version: 'V1.1', clause: '第 2 条', category: '结算与单价规则', roles: ['admin', 'finance'], text: '结算单经财务核对后进入付款队列，付款完成后需回填付款凭证编号。' }
]

const agentLogRows = [
  ['MA-01', '王强', '为河东片区 4 个地块生成派单建议', '成功', 2380, 3120, 5],
  ['MA-02', '王建民', '研判 ZY-20261123-002 超界异常', '成功', 3120, 2480, 3],
  ['MA-03', '刘敏', '合作农机结算周期是多久？', '成功', 1680, 920, 1],
  ['MA-04', '王建民', '本月哪个片区作业效率最高？', '成功', 2940, 1860, 2],
  ['MA-05', '王建民', '汇总本周异常并生成复核清单', '成功', 4620, 5240, 7],
  ['MA-01', '张海涛', '为河西村2号地推荐农机', '成功', 2120, 2680, 4],
  ['MA-02', '张海涛', '为什么 ZY-20261123-004 面积少了 2.3 亩？', '成功', 2860, 2140, 3],
  ['MA-03', '王强', '跨片区调度需要谁确认？', '成功', 1420, 860, 1],
  ['MA-04', '刘敏', '近 7 天结算金额变化', '成功', 3180, 2260, 2],
  ['MA-01', '王建民', '批量生成明日调度方案', '失败', 5240, 3680, 9],
  ['MA-02', '系统', '异常研判任务超时', '失败', 30000, 0, 0],
  ['MA-05', '王建民', '生成月度运营简报', '成功', 4280, 4120, 6]
]
export const agentLogs = agentLogRows.map(([agent, user, question, result, ms, tokens, toolCalls], i) => ({
  id: `AL${String(i + 1).padStart(3, '0')}`,
  agent, user, question, result, ms, tokens, toolCalls,
  at: `2026-11-23 ${String(8 + (i % 12)).padStart(2, '0')}:${String((i * 11) % 60).padStart(2, '0')}`,
  error: result === '失败' ? (i === 9 ? '工具 T05 超时，已回滚，未写入派单数据' : '模型响应超时，已触发降级话术') : ''
}))

export const auditLogs = [
  { id: 'AU01', target: '结算单 JS-202611-002', action: '金额调整', user: '刘敏', before: '¥24,418', after: '¥24,032', reason: '质量抽测扣减 386 元', at: '2026-11-22 16:20' },
  { id: 'AU02', target: '地块 YD-2026-004', action: '边界修正', user: '王建民', before: '46.4 亩', after: '46.8 亩', reason: '测绘复核，边界微调', at: '2026-11-21 10:05' },
  { id: 'AU03', target: '作业单 ZY-20261123-002', action: '复核状态变更', user: '王建民', before: '待复核', after: '异常', reason: '确认超界作业，面积按规则扣除', at: '2026-11-23 09:30' },
  { id: 'AU04', target: 'Agent MA-01', action: '权限范围调整', user: '王建民', before: '河东片区', after: '全公司', reason: '管理员授权', at: '2026-11-20 14:20' }
]

export const alarms = [
  { id: 'AL01', level: '高', title: '检测到越权数据访问请求', detail: '账号 U003（河西片区）尝试查询北塬片区地块 LD010，接口层已拒绝', at: '2026-11-23 09:42', status: '未处理' },
  { id: 'AL02', level: '中', title: 'Agent 工具调用连续失败', detail: 'MA-01 调用 T05 创建调度建议连续失败 3 次，已自动熔断', at: '2026-11-23 08:58', status: '处理中' },
  { id: 'AL03', level: '中', title: '跨片区批量派单', detail: '王强发起跨片区派单 2 条，已进入管理员确认队列', at: '2026-11-22 17:10', status: '已处理' },
  { id: 'AL04', level: '低', title: '夜间非工作时段登录', detail: '账号 U004 于 23:48 登录，来源为公司内网', at: '2026-11-21 23:48', status: '已处理' }
]

export const sessions = [
  {
    id: 'S-20261123-01', agent: 'MA-01', user: '王强', title: '河东片区今日派单建议',
    at: '2026-11-23 08:20', turns: 4, status: '已确认',
    messages: [
      { role: 'user', text: '帮我看看河东片区今天还有哪些地没安排，怎么派比较合适？' },
      { role: 'assistant', text: '已查询河东片区待作业地块 2 块、可用农机 2 台。建议将【河东村5号地 44.2 亩】派给王强（NJ-001，距 1.8km，近30天效率 4.8 亩/小时）；【河西村2号地】跨片区不建议纳入本次。是否需要生成派单建议？' }
    ]
  },
  {
    id: 'S-20261123-02', agent: 'MA-02', user: '王建民', title: 'ZY-20261123-002 异常研判',
    at: '2026-11-23 09:25', turns: 3, status: '已复核',
    messages: [
      { role: 'user', text: '这单为什么只算了 38.4 亩？' },
      { role: 'assistant', text: '该作业单 GPS 轨迹面积 47.2 亩，有效面积 38.4 亩，差异 -18.6%。三处依据：09:12 越界 3.1 亩；10:05 异常停留 8 分钟；11:20 重复覆盖扣 2.4 亩。以上数据来自作业单轨迹与边界裁剪结果。' }
    ]
  },
  {
    id: 'S-20261123-03', agent: 'MA-03', user: '刘敏', title: '结算周期与付款条款',
    at: '2026-11-23 08:40', turns: 2, status: '已结束',
    messages: [
      { role: 'user', text: '合作农机的结算周期是多久？' },
      { role: 'assistant', text: '按《合作农机作业结算管理办法》V2.1 第 6 条：结算周期为自然月，次月 5 日前出具结算单，7 个工作日内完成付款。' }
    ]
  }
]
