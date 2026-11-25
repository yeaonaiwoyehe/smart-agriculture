/* 作业单、轨迹、结算、报表 数据 */
import { lands, machines, operators, tasks } from './core'

const rnd = (n) => Math.round((Math.sin(n * 12.9898) * 43758.5453 % 1) * 1000) / 1000

export function makeTrack(land, rows = 16) {
  const xs = land.points.map((p) => p[0])
  const ys = land.points.map((p) => p[1])
  const minX = Math.min(...xs) + 12
  const maxX = Math.max(...xs) - 12
  const minY = Math.min(...ys) + 10
  const maxY = Math.max(...ys) - 10
  const step = (maxY - minY) / rows
  const pts = []
  for (let r = 0; r < rows; r++) {
    const y = minY + r * step
    const left = minX + (r % 2 ? 0 : 6)
    const right = maxX - (r % 2 ? 6 : 0)
    pts.push([Math.round(left), Math.round(y)])
    pts.push([Math.round((left + right) / 2), Math.round(y + step * 0.32)])
    pts.push([Math.round(right), Math.round(y)])
  }
  return pts
}

const WO_SEED = [
  ['LD003', 'OP03', 'NJ-003', '已完成', '2026-11-22 08:20', '2026-11-22 13:40', 38.1, 38.4, 0.8, []],
  ['LD004', 'OP03', 'NJ-003', '异常', '2026-11-23 07:50', '2026-11-23 12:10', 47.2, 38.4, -18.6, [
    { type: '超出地块边界', at: '2026-11-23 09:12', point: 26, area: 3.1, desc: '轨迹向东越界约 14 米，持续 11 分钟，按规则不计入有效面积' },
    { type: '异常停留', at: '2026-11-23 10:05', point: 44, minutes: 8, desc: '地块东南角停留 8 分钟，判断为转场/故障，未计入作业时长' },
    { type: '重复轨迹', at: '2026-11-23 11:20', point: 61, area: 2.4, desc: '同一区域重复覆盖 2 次，去重后扣除 2.4 亩' }
  ]],
  ['LD001', 'OP01', 'NJ-001', '待复核', '2026-11-23 08:05', '2026-11-23 12:35', 43.8, 42.6, -2.7, [
    { type: '超出地块边界', at: '2026-11-23 08:56', point: 18, area: 1.2, desc: '北侧边界外扩 6 米后回切，扣除 1.2 亩' }
  ]],
  ['LD006', 'OP01', 'NJ-001', '作业中', '2026-11-23 13:10', '', 21.4, 21.8, 1.8, []],
  ['LD007', 'OP05', 'NJ-005', '已结算', '2026-11-21 07:40', '2026-11-21 15:20', 66.2, 65.8, -0.6, []],
  ['LD009', 'OP06', 'NJ-006', '已复核', '2026-11-22 09:00', '2026-11-22 11:30', 27.5, 27.4, -0.4, []],
  ['LD010', 'OP05', 'NJ-005', '待核算', '2026-11-23 13:05', '2026-11-23 15:35', 71.9, 71.6, -0.4, []],
  ['LD011', 'OP07', 'NJ-007', '异常', '2026-11-23 08:15', '2026-11-23 11:05', 50.9, 49.7, -2.3, [
    { type: '超出地块边界', at: '2026-11-23 09:38', point: 32, area: 2.3, desc: '西侧边界外作业 9 分钟，扣除 2.3 亩' }
  ]],
  ['LD002', 'OP02', 'NJ-002', '待核算', '2026-11-23 09:00', '2026-11-23 13:20', 44.6, 44.2, -0.9, []],
  ['LD005', 'OP07', 'NJ-007', '已复核', '2026-11-20 08:00', '2026-11-20 12:40', 31.1, 31.5, 1.3, []],
  ['LD012', 'OP08', 'NJ-008', '待核算', '2026-11-19 08:30', '2026-11-19 12:00', 36.4, 36.9, 1.4, []]
]

export const workorders = WO_SEED.map((s, i) => {
  const [landId, operatorId, machineId, status, startAt, endAt, gpsArea, effectiveArea, diffRate, anomalies] = s
  const land = lands.find((l) => l.id === landId)
  const track = makeTrack(land, 14 + (i % 5))
  return {
    id: `ZY-2026112${(i % 8) + 1}-${String(i + 1).padStart(3, '0')}`,
    taskId: tasks.find((t) => t.landId === landId)?.id || '-',
    landId,
    landName: land.name,
    district: land.district,
    crop: land.crop,
    machineId,
    operatorId,
    operatorName: operators.find((o) => o.id === operatorId)?.name,
    type: tasks.find((t) => t.landId === landId)?.type || '旋耕',
    width: land.width,
    price: land.price,
    status,
    startAt,
    endAt,
    duration: endAt ? Math.round((new Date(endAt) - new Date(startAt)) / 60000) : 0,
    pointCount: track.length,
    gpsArea,
    effectiveArea,
    landArea: land.area,
    diffRate,
    anomalies,
    track,
    landPoints: land.points,
    reviewedBy: ['已复核', '已结算'].includes(status) ? '王建民' : '',
    reviewedAt: ['已复核', '已结算'].includes(status) ? `2026-11-${22 - (i % 3)} 16:20` : '',
    reviewNote: ['已复核', '已结算'].includes(status) ? '经抽测核对，核算面积在允许误差范围内' : '',
    logs: [
      { at: `${startAt}`, action: '轨迹回传完成', user: '系统' },
      { at: `2026-11-23 13:00`, action: '异步核算完成，生成作业单', user: '核算引擎' },
      ...(status !== '作业中' && status !== '待核算' ? [{ at: `2026-11-23 15:10`, action: `${status}`, user: '王建民' }] : [])
    ]
  }
})

export const priceRules = [
  { id: 'PR01', name: '旋耕 · 一类地', type: '旋耕', grade: '一类', price: 45, unit: '元/亩', effective: '2026-01-01', status: '启用' },
  { id: 'PR02', name: '旋耕 · 二类地', type: '旋耕', grade: '二类', price: 42, unit: '元/亩', effective: '2026-01-01', status: '启用' },
  { id: 'PR03', name: '旋耕 · 三类地', type: '旋耕', grade: '三类', price: 38, unit: '元/亩', effective: '2026-01-01', status: '启用' },
  { id: 'PR04', name: '收割 · 一类地', type: '收割', grade: '一类', price: 68, unit: '元/亩', effective: '2026-01-01', status: '启用' },
  { id: 'PR05', name: '收割 · 二类地', type: '收割', grade: '二类', price: 62, unit: '元/亩', effective: '2026-01-01', status: '启用' },
  { id: 'PR06', name: '播种 · 一类地', type: '播种', grade: '一类', price: 52, unit: '元/亩', effective: '2026-03-01', status: '启用' },
  { id: 'PR07', name: '植保 · 统一价', type: '植保', grade: '全部', price: 22, unit: '元/亩', effective: '2026-04-01', status: '启用' },
  { id: 'PR08', name: '深松 · 一类地', type: '深松', grade: '一类', price: 58, unit: '元/亩', effective: '2026-02-01', status: '停用' }
]

export const deductRules = [
  { id: 'DR01', name: '超界作业扣除', desc: '轨迹超出地块边界部分不计入有效作业面积', value: '按实际越界面积扣除', source: '《作业面积核算规则》V2.1 第 4.2 条' },
  { id: 'DR02', name: '重复轨迹去重', desc: '同一地块重复覆盖区域仅计一次', value: '按去重后面积计', source: '《作业面积核算规则》V2.1 第 4.3 条' },
  { id: 'DR03', name: '异常停留剔除', desc: '单点停留超过 5 分钟不计入作业时长', value: '按停留时长剔除', source: '《作业面积核算规则》V2.1 第 4.5 条' },
  { id: 'DR04', name: '质量不达标扣减', desc: '耕深或割茬不达标，按 5%—10% 扣减', value: '5%—10%', source: '《合作农机作业结算管理办法》V2.1 第 8 条' }
]

const stmtBase = [
  ['JS-202611-001', 'OP01', '河东片区', 318.6, 14082, 0, '已付款'],
  ['JS-202611-002', 'OP03', '河西片区', 402.4, 24418, 386, '待付款'],
  ['JS-202611-003', 'OP05', '南滩片区', 366.9, 21102, 0, '待核对'],
  ['JS-202611-004', 'OP02', '河东片区', 254.2, 10676, 128, '待核对'],
  ['JS-202611-005', 'OP07', '北塬片区', 289.7, 12986, 0, '待生成'],
  ['JS-202611-006', 'OP06', '南滩片区', 142.5, 3135, 0, '已付款']
]
export const statements = stmtBase.map(([id, operatorId, district, area, amount, deduction, status], i) => {
  const op = operators.find((o) => o.id === operatorId)
  const items = lands.slice(i % 3, (i % 3) + 3).map((l, k) => {
    const a = +(l.area * (0.72 + k * 0.12)).toFixed(1)
    const price = l.price
    const ded = k === 1 ? +(a * price * 0.05).toFixed(2) : 0
    return {
      landName: l.name,
      type: k === 2 ? '收割' : '旋耕',
      area: a,
      price,
      amount: +(a * price - ded).toFixed(2),
      deduction: ded,
      reason: ded ? '质量抽测未达标，按 5% 扣减（DR04）' : ''
    }
  })
  return {
    id,
    operatorId,
    operatorName: op.name,
    district,
    period: '2026-11 上半月',
    cycle: '半月',
    area,
    amount,
    deduction,
    status,
    createdAt: `2026-11-${String(18 + (i % 4)).padStart(2, '0')} 10:0${i}`,
    payAt: status === '已付款' ? '2026-11-21' : '',
    items
  }
})

const days = ['11-17', '11-18', '11-19', '11-20', '11-21', '11-22', '11-23']
export const reportData = {
  trend7: {
    days,
    area: [842, 968, 1204, 1092, 1350, 1428, 1286],
    tasks: [28, 33, 41, 37, 46, 49, 42],
    abnormal: [2, 1, 4, 2, 3, 5, 3]
  },
  trend6m: {
    months: ['6月', '7月', '8月', '9月', '10月', '11月'],
    area: [8420, 11260, 13840, 16220, 15380, 9840],
    amount: [42.6, 56.8, 69.2, 81.4, 77.6, 48.2]
  },
  district: [
    { name: '河东片区', area: 3128.4, rate: 86.2, eff: 5.2, abnormal: 3, review: 2 },
    { name: '河西片区', area: 2746.8, rate: 78.5, eff: 4.8, abnormal: 5, review: 4 },
    { name: '南滩片区', area: 2412.6, rate: 91.4, eff: 5.6, abnormal: 2, review: 1 },
    { name: '北塬片区', area: 1984.2, rate: 68.9, eff: 4.1, abnormal: 6, review: 3 }
  ],
  machineEff: [
    { name: 'NJ-005', model: '沃得锐龙 4LZ-6.0', eff: 10.2, area: 214.7, hours: 21.0 },
    { name: 'NJ-003', model: '雷沃谷神 GM100', eff: 9.6, area: 201.5, hours: 21.0 },
    { name: 'NJ-006', model: '大疆 T60', eff: 22.5, area: 96.3, hours: 4.3 },
    { name: 'NJ-001', model: '东方红 LX954', eff: 4.8, area: 186.4, hours: 38.8 },
    { name: 'NJ-007', model: '东方红 MF904', eff: 4.5, area: 133.9, hours: 29.8 },
    { name: 'NJ-002', model: '约翰迪尔 6J-2104', eff: 4.2, area: 152.8, hours: 36.4 }
  ],
  operator: [
    { name: '刘志远', district: '南滩片区', area: 214.7, tasks: 18, rate: 96 },
    { name: '张海涛', district: '河西片区', area: 201.5, tasks: 17, rate: 94 },
    { name: '王强', district: '河东片区', area: 186.4, tasks: 16, rate: 92 },
    { name: '李建国', district: '河东片区', area: 152.8, tasks: 14, rate: 88 },
    { name: '孙国庆', district: '北塬片区', area: 133.9, tasks: 12, rate: 85 },
    { name: '陈立新', district: '南滩片区', area: 96.3, tasks: 9, rate: 82 }
  ],
  anomalyRate: [
    { name: '超界作业', value: 46 },
    { name: '重复轨迹', value: 21 },
    { name: '异常停留', value: 18 },
    { name: '轨迹中断', value: 15 }
  ],
  kpi: {
    totalArea: 10272.0,
    avgEfficiency: 5.1,
    abnormalRate: 3.2,
    reviewRate: 6.8,
    settleAmount: 105.4,
    taskCompletion: 86.4
  }
}
