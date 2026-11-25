/* 组织、地块、农机、农机手、作业任务 数据 */

const DISTRICTS = ['河东片区', '河西片区', '南滩片区', '北塬片区']
const VILLAGES = { 河东片区: '河东村', 河西片区: '河西村', 南滩片区: '南滩村', 北塬片区: '北塬村' }

let seed = 7
const rnd = () => ((seed = (seed * 9301 + 49297) % 233280) / 233280)
const pick = (arr) => arr[Math.floor(rnd() * arr.length)]

function fieldPoints(col, row) {
  const cw = 205
  const ch = 150
  const x = 70 + col * 222
  const y = 55 + row * 168
  const j = () => (rnd() - 0.5) * 18
  const pts = [
    [x + 10 + j(), y + 8 + j()],
    [x + cw - 12 + j(), y + 4 + j()],
    [x + cw + 2 + j(), y + ch * 0.5 + j()],
    [x + cw - 18 + j(), y + ch - 6 + j()],
    [x + 16 + j(), y + ch + 2 + j()],
    [x - 2 + j(), y + ch * 0.42 + j()]
  ]
  return pts.map(([px, py]) => [Math.round(px), Math.round(py)])
}

const LAND_SEED = [
  ['河东村3号地', '河东片区', '小麦', 42.6, '作业中', 45, 3.6, '一类', '河东村集体', '壤土'],
  ['河东村5号地', '河东片区', '玉米', 44.2, '待作业', 42, 3.3, '一类', '河东村集体', '壤土'],
  ['河东村7号地', '河东片区', '小麦', 38.4, '已完成', 45, 3.6, '二类', '李守田', '砂壤土'],
  ['河西村7号地', '河西片区', '玉米', 46.8, '待复核', 48, 3.8, '一类', '河西村合作社', '壤土'],
  ['河西村2号地', '河西片区', '大豆', 31.5, '待作业', 52, 4.2, '二类', '王春生', '黏壤土'],
  ['河西村9号地', '河西片区', '小麦', 52.3, '作业中', 45, 3.6, '一类', '河西村集体', '壤土'],
  ['南滩村1号地', '南滩片区', '水稻', 65.8, '已完成', 68, 4.5, '一类', '南滩村合作社', '水稻土'],
  ['南滩村4号地', '南滩片区', '水稻', 58.2, '待作业', 68, 4.5, '二类', '南滩村集体', '水稻土'],
  ['南滩村6号地', '南滩片区', '油菜', 27.4, '已完成', 55, 3.0, '三类', '陈玉兰', '砂壤土'],
  ['北塬村2号地', '北塬片区', '玉米', 71.6, '待复核', 48, 3.8, '一类', '北塬村集体', '褐土'],
  ['北塬村8号地', '北塬片区', '小麦', 49.7, '作业中', 45, 3.6, '二类', '孙德胜', '褐土'],
  ['柳沟村5号地', '北塬片区', '大豆', 36.9, '待作业', 52, 4.2, '三类', '柳沟村集体', '砂壤土']
]

export const lands = LAND_SEED.map((s, i) => {
  const [name, district, crop, area, status, price, width, grade, owner, soil] = s
  const col = i % 4
  const row = Math.floor(i / 4)
  return {
    id: `LD${String(i + 1).padStart(3, '0')}`,
    code: `YD-2026-${String(i + 1).padStart(3, '0')}`,
    name,
    district,
    village: VILLAGES[district],
    crop,
    area,
    status,
    price,
    width,
    grade,
    owner,
    soil,
    slope: `${(rnd() * 4 + 0.6).toFixed(1)}°`,
    points: fieldPoints(col, row),
    center: [0, 0],
    version: `V${(i % 3) + 1}.${i % 2}`,
    updatedAt: `2026-11-${String(18 + (i % 5)).padStart(2, '0')} ${String(9 + (i % 8)).padStart(2, '0')}:20`,
    lastWork: `2026-11-${String(15 + (i % 8)).padStart(2, '0')}`,
    workCount: 3 + (i % 6),
    quality: pick(['秸秆还田达标', '耕深≥25cm', '割茬≤8cm', '无漏耕漏播']),
    history: [
      { version: `V${(i % 3) + 1}.0`, at: '2026-10-12 10:20', user: '王建民', note: '边界微调，面积由原值修正 +0.4 亩' },
      { version: `V${(i % 3) + 1}.1`, at: '2026-09-03 15:42', user: '测绘组', note: '导入一地一码矢量数据，初始赋码' }
    ]
  }
})
lands.forEach((l) => {
  l.center = [
    Math.round(l.points.reduce((s, p) => s + p[0], 0) / l.points.length),
    Math.round(l.points.reduce((s, p) => s + p[1], 0) / l.points.length)
  ]
})

export const operators = [
  ['OP01', '王强', '13705371101', '河东片区', 'NJ-001', 4.8, 186.4, 'A'],
  ['OP02', '李建国', '13705371102', '河东片区', 'NJ-002', 4.6, 152.8, 'A'],
  ['OP03', '张海涛', '13705371103', '河西片区', 'NJ-003', 4.7, 201.5, 'A'],
  ['OP04', '赵永福', '13705371104', '河西片区', 'NJ-004', 4.3, 118.2, 'B'],
  ['OP05', '刘志远', '13705371105', '南滩片区', 'NJ-005', 4.9, 214.7, 'A'],
  ['OP06', '陈立新', '13705371106', '南滩片区', 'NJ-006', 4.5, 96.3, 'B'],
  ['OP07', '孙国庆', '13705371107', '北塬片区', 'NJ-007', 4.4, 133.9, 'A'],
  ['OP08', '周文斌', '13705371108', '北塬片区', 'NJ-008', 4.1, 87.6, 'B']
].map(([id, name, phone, district, machineId, rating, workload, cert]) => ({
  id,
  name,
  phone,
  district,
  machineId,
  rating,
  workload,
  cert,
  joinDate: `2025-0${(Number(id.slice(2)) % 8) + 1}-12`,
  status: '在职',
  todayHours: (rnd() * 6 + 2).toFixed(1)
}))

export const machines = [
  ['NJ-001', '东方红 LX954', '拖拉机', '河东片区', 'OP01', true, '作业中', 186, 4.8],
  ['NJ-002', '约翰迪尔 6J-2104', '拖拉机', '河东片区', 'OP02', true, '待机', 152, 4.2],
  ['NJ-003', '雷沃谷神 GM100', '收割机', '河西片区', 'OP03', true, '作业中', 201, 9.6],
  ['NJ-004', '久保田 PRO688Q', '收割机', '河西片区', 'OP04', false, '离线', 118, 7.8],
  ['NJ-005', '沃得锐龙 4LZ-6.0', '收割机', '南滩片区', 'OP05', true, '作业中', 214, 10.2],
  ['NJ-006', '大疆 T60', '植保机', '南滩片区', 'OP06', true, '待机', 96, 22.5],
  ['NJ-007', '东方红 MF904', '拖拉机', '北塬片区', 'OP07', true, '待机', 133, 4.5],
  ['NJ-008', '雷沃 M1004', '拖拉机', '北塬片区', 'OP08', false, '维修', 87, 4.0]
].map(([id, model, type, district, operatorId, online, workStatus, totalArea, efficiency], i) => {
  const land = lands[(i * 2 + 1) % lands.length]
  return {
    id,
    code: id,
    model,
    type,
    district,
    operatorId,
    operatorName: operators.find((o) => o.id === operatorId)?.name,
    online,
    workStatus,
    totalArea,
    efficiency,
    todayArea: +(rnd() * 32 + 6).toFixed(1),
    speed: workStatus === '作业中' ? +(rnd() * 4 + 3).toFixed(1) : 0,
    fuel: Math.round(rnd() * 45 + 45),
    pos: [land.center[0] + Math.round((rnd() - 0.5) * 40), land.center[1] + Math.round((rnd() - 0.5) * 30)],
    landId: land.id,
    landName: land.name,
    lastActive: `2026-11-23 ${String(8 + (i % 8)).padStart(2, '0')}:${String((i * 7) % 60).padStart(2, '0')}`,
    purchase: `202${3 + (i % 3)}-0${(i % 9) + 1}-15`,
    maintainAt: `2026-12-${String(10 + i).padStart(2, '0')}`
  }
})

export const tasks = [
  ['LD001', '旋耕', 'NJ-001', 'OP01', '2026-11-23 18:00', '作业中', 62],
  ['LD002', '旋耕', 'NJ-002', 'OP02', '2026-11-23 17:30', '待执行', 0],
  ['LD003', '收割', 'NJ-003', 'OP03', '2026-11-22 18:00', '已完成', 100],
  ['LD004', '收割', 'NJ-003', 'OP03', '2026-11-23 16:00', '超时', 74],
  ['LD005', '播种', 'NJ-007', 'OP07', '2026-11-24 12:00', '待接单', 0],
  ['LD006', '旋耕', 'NJ-001', 'OP01', '2026-11-23 19:00', '作业中', 41],
  ['LD007', '收割', 'NJ-005', 'OP05', '2026-11-22 18:00', '已完成', 100],
  ['LD008', '深松', 'NJ-002', 'OP02', '2026-11-25 18:00', '待派单', 0],
  ['LD009', '植保', 'NJ-006', 'OP06', '2026-11-23 17:00', '已完成', 100],
  ['LD010', '收割', 'NJ-005', 'OP05', '2026-11-24 18:00', '待执行', 0],
  ['LD011', '旋耕', 'NJ-007', 'OP07', '2026-11-23 18:30', '作业中', 28],
  ['LD012', '播种', 'NJ-008', 'OP08', '2026-11-26 12:00', '待派单', 0]
].map(([landId, type, machineId, operatorId, deadline, status, progress], i) => {
  const land = lands.find((l) => l.id === landId)
  return {
    id: `RW-20261123-${String(i + 1).padStart(3, '0')}`,
    landId,
    landName: land.name,
    district: land.district,
    type,
    machineId,
    operatorId,
    operatorName: operators.find((o) => o.id === operatorId)?.name,
    deadline,
    status,
    progress,
    area: land.area,
    priority: i % 5 === 0 ? '高' : i % 3 === 0 ? '中' : '普通',
    createdAt: `2026-11-2${(i % 3) + 1} 09:${String(10 + i * 3).padStart(2, '0')}`,
    note: i % 4 === 0 ? '需在降雨前完成，注意土壤含水率' : ''
  }
})

export const org = {
  company: '田界智航农业服务有限公司',
  districts: DISTRICTS,
  villages: VILLAGES,
  crops: ['小麦', '玉米', '大豆', '水稻', '油菜'],
  workTypes: ['旋耕', '收割', '播种', '植保', '深松'],
  stats: {
    todayArea: 1286.4,
    totalArea: 18462.8,
    todayTasks: 42,
    doneTasks: 31,
    pendingTasks: 11,
    onlineMachines: 6,
    totalMachines: 8,
    activeLands: 5,
    completionRate: 73.8,
    abnormalTasks: 3,
    pendingReview: 7,
    pendingAmount: 128640,
    settledAmount: 263800
  }
}

export { DISTRICTS, VILLAGES }
