export * from './core'
export * from './ops'
export * from './platform'
import { workorders } from './ops'
import { machines, lands, tasks } from './core'

export const activities = [
  { id: 1, time: '09:42', type: '异常', text: '北塬村2号地作业单触发超界预警，已推送队长复核', tone: 'red' },
  { id: 2, time: '09:18', type: '作业', text: 'NJ-005 完成南滩村1号地收割 65.8 亩，用时 7.7 小时', tone: 'green' },
  { id: 3, time: '08:56', type: '调度', text: '王强确认 MA-01 调度建议，向河东村5号地派单', tone: 'blue' },
  { id: 4, time: '08:30', type: '结算', text: '财务生成 2026-11 上半月结算单 6 份，待核对', tone: 'violet' },
  { id: 5, time: '08:12', type: '系统', text: '一地一码数据同步完成，共 12 个地块版本更新', tone: 'gray' },
  { id: 6, time: '07:55', type: '作业', text: 'NJ-001 在河东村3号地开始旋耕作业', tone: 'green' }
]

export const aiInsights = [
  {
    id: 'AI01', severity: 'red', title: '作业进度异常',
    text: '北塬片区完成率 <b>68.9%</b>，低于公司均值 17.5 个百分点；北塬村2号地、8号地任务临近时限仍未完成。',
    action: '查看任务', route: '/dispatch'
  },
  {
    id: 'AI02', severity: 'amber', title: '农机闲置提示',
    text: 'NJ-002（约翰迪尔 6J-2104）已待机 <b>2.6 小时</b>，距待作业地块 2.1km，建议纳入河东片区下一轮派单。',
    action: '查看农机', route: '/machines'
  },
  {
    id: 'AI03', severity: 'amber', title: '待复核积压',
    text: '当前有 <b>7 份</b>作业单待复核，其中 3 份存在面积差异，最早一份已滞留 18 小时。',
    action: '去复核', route: '/workorders'
  },
  {
    id: 'AI04', severity: 'green', title: '效率提升',
    text: '南滩片区近 7 天平均效率 <b>5.6 亩/小时</b>，环比提升 8.2%，主因是收割机调度半径缩短。',
    action: '查看报表', route: '/reports'
  }
]

export const heatPoints = workorders
  .filter((w) => w.status !== '作业中')
  .flatMap((w) => w.track.filter((_, i) => i % 5 === 0).map((p, i) => [p[0], p[1], 0.35 + ((i % 4) * 0.15)]))

export const dispatchSuggestions = [
  { landId: 'LD002', landName: '河东村5号地', area: 44.2, machineId: 'NJ-002', machineName: '约翰迪尔 6J-2104', operatorName: '李建国', distance: 1.8, efficiency: 4.2, deadline: '2026-11-23 17:30', score: 92, reason: '距离最近 1.8km，当前待机，近 30 天同类作业效率 4.2 亩/小时，可在时限内完成' },
  { landId: 'LD005', landName: '河西村2号地', area: 31.5, machineId: 'NJ-002', machineName: '约翰迪尔 6J-2104', operatorName: '李建国', distance: 6.4, efficiency: 4.2, deadline: '2026-11-24 12:00', score: 78, reason: '跨片区调度，距离 6.4km，需管理员确认；可避开河东片区高峰' },
  { landId: 'LD008', landName: '南滩村4号地', area: 58.2, machineId: 'NJ-005', machineName: '沃得锐龙 4LZ-6.0', operatorName: '刘志远', distance: 3.2, efficiency: 10.2, deadline: '2026-11-25 18:00', score: 88, reason: '机型匹配水稻收割，效率 10.2 亩/小时，预计 5.7 小时完成' }
]

export const quickSearchIndex = [
  ...lands.map((l) => ({ type: '地块', id: l.id, name: l.name, meta: `${l.district} · ${l.area} 亩`, route: '/lands' })),
  ...machines.map((m) => ({ type: '农机', id: m.id, name: `${m.code} ${m.model}`, meta: `${m.district} · ${m.workStatus}`, route: '/machines' })),
  ...tasks.map((t) => ({ type: '任务', id: t.id, name: t.landName, meta: `${t.type} · ${t.status}`, route: '/dispatch' })),
  ...workorders.map((w) => ({ type: '作业单', id: w.id, name: w.landName, meta: `${w.status} · ${w.effectiveArea} 亩`, route: '/workorders' }))
]

export const settlementSummary = {
  pending: 128640,
  settled: 263800,
  deduction: 514,
  statements: 6
}
