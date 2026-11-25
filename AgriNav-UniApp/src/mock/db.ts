import type {
  AgentSession,
  FeedbackItem,
  Machinery,
  NotificationItem,
  Plot,
  Settlement,
  Task,
  UserProfile,
  WorkOrder
} from '@/types'

const DB_KEY = 'mock.db.v1'

export interface MockDb {
  version: number
  user: UserProfile
  plots: Plot[]
  tasks: Task[]
  workOrders: WorkOrder[]
  settlements: Settlement[]
  notifications: NotificationItem[]
  feedbacks: FeedbackItem[]
  machineries: Machinery[]
  agentSessions: AgentSession[]
  counters: {
    receipt: number
    workOrder: number
    feedback: number
    session: number
  }
  scenario: {
    /** 0~1，随机失败概率，用于演示网络异常分支 */
    failRate: number
    /** 强制失败的下一个操作，执行后清空 */
    forceFail: string | null
  }
}

function p2(n: number): string {
  return n < 10 ? `0${n}` : String(n)
}

export function fmtDateTime(d: Date): string {
  return `${d.getFullYear()}-${p2(d.getMonth() + 1)}-${p2(d.getDate())} ${p2(
    d.getHours()
  )}:${p2(d.getMinutes())}:${p2(d.getSeconds())}`
}

function daysAgo(days: number, hour = 8, minute = 0): string {
  const d = new Date()
  d.setDate(d.getDate() - days)
  d.setHours(hour, minute, 0, 0)
  return fmtDateTime(d)
}

function todayAt(hour: number, minute = 0): string {
  const d = new Date()
  d.setHours(hour, minute, 0, 0)
  return fmtDateTime(d)
}

function inHours(hours: number): string {
  return fmtDateTime(new Date(Date.now() + hours * 3600 * 1000))
}

const plots: Plot[] = [
  {
    plotId: 1001,
    plotName: '东风村3号地块',
    plotNo: 'DF-2026-003',
    address: '黑龙江省绥化市北林区东富镇东风村',
    area: 128.6,
    cropName: '玉米',
    center: { latitude: 46.6482, longitude: 127.0231 },
    boundary: [
      { latitude: 46.6492, longitude: 127.0208 },
      { latitude: 46.6494, longitude: 127.0252 },
      { latitude: 46.6476, longitude: 127.0259 },
      { latitude: 46.647, longitude: 127.0215 },
      { latitude: 46.6492, longitude: 127.0208 }
    ]
  },
  {
    plotId: 1002,
    plotName: '兴胜村7号地块',
    plotNo: 'XS-2026-007',
    address: '黑龙江省绥化市北林区兴胜村',
    area: 231.4,
    cropName: '大豆',
    center: { latitude: 46.6712, longitude: 127.0562 },
    boundary: [
      { latitude: 46.673, longitude: 127.052 },
      { latitude: 46.6735, longitude: 127.0598 },
      { latitude: 46.6702, longitude: 127.0605 },
      { latitude: 46.669, longitude: 127.0528 },
      { latitude: 46.673, longitude: 127.052 }
    ]
  },
  {
    plotId: 1003,
    plotName: '长胜村1号地块',
    plotNo: 'CS-2026-001',
    address: '黑龙江省绥化市北林区太平川镇长胜村',
    area: 86.2,
    cropName: '玉米',
    center: { latitude: 46.6188, longitude: 126.9872 },
    boundary: [
      { latitude: 46.6201, longitude: 126.984 },
      { latitude: 46.6204, longitude: 126.9901 },
      { latitude: 46.6175, longitude: 126.9906 },
      { latitude: 46.617, longitude: 126.9845 },
      { latitude: 46.6201, longitude: 126.984 }
    ]
  },
  {
    plotId: 1004,
    plotName: '永安村5号地块',
    plotNo: 'YA-2026-005',
    address: '黑龙江省绥化市北林区永安镇永安村',
    area: 305.8,
    cropName: '水稻',
    center: { latitude: 46.7005, longitude: 127.0912 },
    boundary: [
      { latitude: 46.703, longitude: 127.0865 },
      { latitude: 46.7034, longitude: 127.0958 },
      { latitude: 46.6982, longitude: 127.0966 },
      { latitude: 46.6978, longitude: 127.0872 },
      { latitude: 46.703, longitude: 127.0865 }
    ]
  },
  {
    plotId: 1005,
    plotName: '兴胜村2号地块',
    plotNo: 'XS-2026-002',
    address: '黑龙江省绥化市北林区兴胜村',
    area: 196.5,
    cropName: '大豆',
    center: { latitude: 46.6688, longitude: 127.0488 },
    boundary: [
      { latitude: 46.6705, longitude: 127.0455 },
      { latitude: 46.6709, longitude: 127.0521 },
      { latitude: 46.6672, longitude: 127.0526 },
      { latitude: 46.6668, longitude: 127.0461 },
      { latitude: 46.6705, longitude: 127.0455 }
    ]
  },
  {
    plotId: 1006,
    plotName: '长胜村9号地块',
    plotNo: 'CS-2026-009',
    address: '黑龙江省绥化市北林区太平川镇长胜村',
    area: 142.3,
    cropName: '玉米',
    center: { latitude: 46.6102, longitude: 126.9755 },
    boundary: [
      { latitude: 46.6118, longitude: 126.9724 },
      { latitude: 46.6121, longitude: 126.9786 },
      { latitude: 46.6087, longitude: 126.979 },
      { latitude: 46.6084, longitude: 126.9728 },
      { latitude: 46.6118, longitude: 126.9724 }
    ]
  }
]

const seededTasks: Task[] = [
  {
    id: 'TK20261124001',
    taskNo: 'TK20261124001',
    plot: plots[0],
    distanceKm: 2.4,
    workTypeId: 'FW01',
    workTypeName: '深松整地',
    area: 128.6,
    unitPrice: 45,
    status: 'pending',
    deadline: todayAt(18, 0),
    publishTime: daysAgo(1, 9, 20),
    requirement: '深松深度不低于 30cm，作业后地表平整，无漏耕',
    publisher: '北林区农机合作社',
    contactPhone: '0455-8888123'
  },
  {
    id: 'TK20261124002',
    taskNo: 'TK20261124002',
    plot: plots[1],
    distanceKm: 6.8,
    workTypeId: 'FW02',
    workTypeName: '秸秆还田',
    area: 231.4,
    unitPrice: 38,
    status: 'pending',
    deadline: inHours(26),
    publishTime: daysAgo(1, 15, 5),
    requirement: '秸秆粉碎长度小于 10cm，抛撒均匀',
    publisher: '兴胜村集体经济组织',
    contactPhone: '0455-8888567'
  },
  {
    id: 'TK20261123003',
    taskNo: 'TK20261123003',
    plot: plots[2],
    distanceKm: 11.2,
    workTypeId: 'FW03',
    workTypeName: '旋耕播种',
    area: 86.2,
    unitPrice: 52,
    status: 'claimed',
    deadline: todayAt(20, 0),
    publishTime: daysAgo(2, 8, 40),
    requirement: '播种深度 4~5cm，行距 60cm，播后镇压',
    publisher: '长胜村种植大户 王建国',
    contactPhone: '13804551234',
    claimedAt: daysAgo(0, 7, 30)
  },
  {
    id: 'TK20261122004',
    taskNo: 'TK20261122004',
    plot: plots[3],
    distanceKm: 15.6,
    workTypeId: 'FW05',
    workTypeName: '机械收割',
    area: 305.8,
    unitPrice: 68,
    status: 'claimed',
    deadline: inHours(50),
    publishTime: daysAgo(2, 10, 0),
    requirement: '水稻收割损失率低于 3%，留茬高度 10~15cm',
    publisher: '永安镇农业服务中心',
    contactPhone: '0455-8888901',
    claimedAt: daysAgo(1, 16, 10)
  },
  {
    id: 'TK20261120005',
    taskNo: 'TK20261120005',
    plot: plots[4],
    distanceKm: 5.1,
    workTypeId: 'FW02',
    workTypeName: '秸秆还田',
    area: 196.5,
    unitPrice: 38,
    status: 'done',
    deadline: daysAgo(2, 18, 0),
    publishTime: daysAgo(4, 9, 0),
    requirement: '秸秆粉碎还田，粉碎长度小于 10cm',
    publisher: '兴胜村集体经济组织',
    contactPhone: '0455-8888567',
    claimedAt: daysAgo(3, 7, 40),
    finishedAt: daysAgo(2, 16, 25)
  },
  {
    id: 'TK20261119006',
    taskNo: 'TK20261119006',
    plot: plots[5],
    distanceKm: 13.4,
    workTypeId: 'FW01',
    workTypeName: '深松整地',
    area: 142.3,
    unitPrice: 45,
    status: 'timeout',
    deadline: daysAgo(1, 18, 0),
    publishTime: daysAgo(5, 9, 0),
    requirement: '深松深度不低于 30cm',
    publisher: '北林区农机合作社',
    contactPhone: '0455-8888123'
  }
]

const historicalWorkOrder: WorkOrder = {
  trajectoryInfoId: '13800001234;20261122100211',
  taskId: 'TK20261120005',
  plotId: 1005,
  plotName: '兴胜村2号地块',
  address: '黑龙江省绥化市北林区兴胜村',
  workType: 'FW02',
  workTypeName: '秸秆还田',
  phoneNumber: '13800001234',
  status: 3,
  startTime: daysAgo(2, 9, 12),
  endTime: daysAgo(2, 16, 25),
  area: 194.82,
  width: 3.2,
  totalPoints: 1860,
  pendingPoints: 0,
  trajectory: [],
  receiptNo: 'R20261122001',
  unitPrice: 38,
  amount: 7403.16
}

const seededSettlements: Settlement[] = [
  {
    settlementId: 202611001,
    settlementNo: 'ST20261115001',
    periodStart: '2026-11-01',
    periodEnd: '2026-11-15',
    totalArea: 316.24,
    totalAmount: 12840.5,
    paidAmount: 0,
    status: 'pending',
    itemsCount: 2,
    createTime: daysAgo(8, 10, 0),
    items: [
      {
        itemId: 1,
        trajectoryInfoId: '13800001234;20261112093102',
        plotId: 1002,
        plotName: '兴胜村7号地块',
        workTypeName: '秸秆还田',
        area: 228.6,
        unitPrice: 38,
        amount: 8686.8
      },
      {
        itemId: 2,
        trajectoryInfoId: '13800001234;20261113141120',
        plotId: 1003,
        plotName: '长胜村1号地块',
        workTypeName: '旋耕播种',
        area: 87.64,
        unitPrice: 47.4,
        amount: 4153.7
      }
    ]
  },
  {
    settlementId: 202610002,
    settlementNo: 'ST20261031002',
    periodStart: '2026-10-01',
    periodEnd: '2026-10-31',
    totalArea: 412.86,
    totalAmount: 17240.32,
    paidAmount: 17240.32,
    status: 'paid',
    itemsCount: 3,
    createTime: daysAgo(24, 9, 30),
    items: [
      {
        itemId: 3,
        trajectoryInfoId: '13800001234;20261012081111',
        plotId: 1001,
        plotName: '东风村3号地块',
        workTypeName: '深松整地',
        area: 126.4,
        unitPrice: 45,
        amount: 5688
      },
      {
        itemId: 4,
        trajectoryInfoId: '13800001234;20261018100222',
        plotId: 1004,
        plotName: '永安村5号地块',
        workTypeName: '机械收割',
        area: 289.9,
        unitPrice: 39.4,
        amount: 11380.06
      },
      {
        itemId: 5,
        trajectoryInfoId: '13800001234;20261025201111',
        plotId: 1005,
        plotName: '兴胜村2号地块',
        workTypeName: '秸秆还田',
        area: 0,
        unitPrice: 38,
        amount: 172.26,
        deduction: 171.0,
        deductionReason: '轨迹覆盖不足 30%，超出部分不予计费'
      }
    ]
  },
  {
    settlementId: 202610001,
    settlementNo: 'ST20261015003',
    periodStart: '2026-10-01',
    periodEnd: '2026-10-15',
    totalArea: 188.42,
    totalAmount: 7536.8,
    paidAmount: 0,
    status: 'reviewing',
    itemsCount: 1,
    createTime: daysAgo(38, 10, 0),
    items: [
      {
        itemId: 6,
        trajectoryInfoId: '13800001234;20261008083333',
        plotId: 1002,
        plotName: '兴胜村7号地块',
        workTypeName: '秸秆还田',
        area: 188.42,
        unitPrice: 40,
        amount: 7536.8,
        deduction: 900.0,
        deductionReason: '复核申请中：地块东南角积水未作业'
      }
    ]
  }
]

const seededNotifications: NotificationItem[] = [
  {
    id: 1,
    title: '新任务待接收',
    content: '东风村3号地块深松整地任务已发布，作业面积 128.6 亩，请及时查看。',
    type: 'task',
    read: false,
    createTime: daysAgo(1, 9, 20),
    bizId: 'TK20261124001'
  },
  {
    id: 2,
    title: '作业单已生成',
    content: '作业单 13800001234;20261122100211 已生成，作业面积 194.82 亩，可在作业记录中查看。',
    type: 'work',
    read: false,
    createTime: daysAgo(2, 16, 30),
    bizId: '13800001234;20261122100211'
  },
  {
    id: 3,
    title: '结算单待确认',
    content: '11 月上半月结算单 ST20261115001 已生成，合计 12840.50 元，如有异议可提交复核。',
    type: 'settlement',
    read: false,
    createTime: daysAgo(8, 10, 5),
    bizId: '202611001'
  },
  {
    id: 4,
    title: '异常工单已受理',
    content: '您上报的「GPS 信号弱」问题已受理，现场处理预计今日完成。',
    type: 'exception',
    read: true,
    createTime: daysAgo(3, 14, 0),
    bizId: 'FB20261121001'
  },
  {
    id: 5,
    title: '系统维护通知',
    content: '平台将于本周日 02:00-04:00 进行版本升级，期间轨迹上传可能短暂延迟。',
    type: 'system',
    read: true,
    createTime: daysAgo(5, 18, 0)
  },
  {
    id: 6,
    title: '任务即将到期',
    content: '长胜村1号地块任务将于今日 20:00 到期，请尽快完成作业。',
    type: 'task',
    read: false,
    createTime: daysAgo(0, 8, 0),
    bizId: 'TK20261123003'
  }
]

const seededFeedbacks: FeedbackItem[] = [
  {
    id: 'FB20261121001',
    type: 'gps',
    typeName: 'GPS 信号弱',
    content: '永安村5号地块东侧林带附近 GPS 信号长时间漂移，轨迹出现断点。',
    images: [],
    status: 'processing',
    createTime: daysAgo(3, 13, 40),
    receiptNo: 'FB20261121001',
    reply: '已安排现场核查，建议作业时避开林带 20 米范围。'
  },
  {
    id: 'FB20261118002',
    type: 'machine',
    typeName: '农机故障',
    content: '深松机左侧铲柄开裂，作业暂停半天，已联系维修。',
    images: [],
    status: 'resolved',
    createTime: daysAgo(6, 9, 10),
    receiptNo: 'FB20261118002',
    reply: '配件已更换，故障记录已同步至农机档案。'
  }
]

const seededMachineries: Machinery[] = [
  {
    id: 1,
    machineName: '约翰迪尔 6J-2104',
    machineNo: '黑A-AG-2026',
    machineType: '轮式拖拉机',
    brand: '约翰迪尔',
    power: '210 马力',
    workWidth: 3.2,
    status: 'idle'
  },
  {
    id: 2,
    machineName: '雷沃谷神 GM100',
    machineNo: '黑A-HV-0917',
    machineType: '联合收割机',
    brand: '雷沃重工',
    power: '100 马力',
    workWidth: 2.6,
    status: 'maintenance'
  }
]

function createSeed(): MockDb {
  return {
    version: 1,
    user: {
      userId: 10001,
      userName: '13800001234',
      nickName: '张师傅',
      phoneNumber: '13800001234',
      deptName: '北林区农机服务队',
      roles: ['operator'],
      permissions: ['app:task', 'app:work', 'app:settlement'],
      workYears: 8,
      totalArea: 12860.4,
      totalIncome: 486320.5
    },
    plots,
    tasks: seededTasks,
    workOrders: [historicalWorkOrder],
    settlements: seededSettlements,
    notifications: seededNotifications,
    feedbacks: seededFeedbacks,
    machineries: seededMachineries,
    agentSessions: [],
    counters: {
      receipt: 20,
      workOrder: 3,
      feedback: 2,
      session: 0
    },
    scenario: {
      failRate: 0,
      forceFail: null
    }
  }
}

let memoryDb: MockDb | null = null

export function getDb(): MockDb {
  if (memoryDb) return memoryDb
  const raw = uni.getStorageSync(DB_KEY)
  if (!raw) {
    memoryDb = createSeed()
    saveDb(memoryDb)
    return memoryDb
  }
  try {
    const parsed = typeof raw === 'string' ? (JSON.parse(raw) as MockDb) : (raw as MockDb)
    if (!parsed || parsed.version !== 1 || !parsed.tasks) {
      memoryDb = createSeed()
      saveDb(memoryDb)
      return memoryDb
    }
    memoryDb = parsed
    return memoryDb
  } catch (e) {
    memoryDb = createSeed()
    saveDb(memoryDb)
    return memoryDb
  }
}

export function saveDb(db: MockDb): void {
  memoryDb = db
  uni.setStorageSync(DB_KEY, JSON.stringify(db))
}

export function resetDb(): void {
  memoryDb = createSeed()
  saveDb(memoryDb)
}

export function nextReceipt(): string {
  const db = getDb()
  db.counters.receipt += 1
  saveDb(db)
  const d = new Date()
  return `R${d.getFullYear()}${p2(d.getMonth() + 1)}${p2(d.getDate())}${String(
    db.counters.receipt
  ).padStart(3, '0')}`
}

export function nextWorkOrderNo(phone: string): string {
  const db = getDb()
  db.counters.workOrder += 1
  saveDb(db)
  return `${phone};${Date.now()}`
}

/** 模拟网络延迟 */
export function delay(min = 220, max = 560): Promise<void> {
  const ms = Math.floor(min + Math.random() * (max - min))
  return new Promise((resolve) => setTimeout(resolve, ms))
}

export class MockError extends Error {
  code: number
  constructor(message: string, code = 500) {
    super(message)
    this.name = 'MockError'
    this.code = code
  }
}

/** 按 scenario 抛错；成功时什么也不做 */
export function maybeFail(action: string): void {
  const db = getDb()
  if (db.scenario.forceFail === action) {
    db.scenario.forceFail = null
    saveDb(db)
    throw new MockError('演示：已触发一次接口失败，请重试')
  }
  if (db.scenario.failRate > 0 && Math.random() < db.scenario.failRate) {
    throw new MockError('演示：网络不稳定，请稍后重试')
  }
}

export function updateUser(patch: Partial<UserProfile>): UserProfile {
  const db = getDb()
  db.user = { ...db.user, ...patch }
  saveDb(db)
  return db.user
}
