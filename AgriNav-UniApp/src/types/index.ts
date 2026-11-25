/** 后端统一响应结构（AjaxResult） */
export interface AjaxResult<T = any> {
  code: number
  msg: string
  data?: T | T[]
  rows?: T[]
  total?: number
  [key: string]: any
}

export interface LoginResult {
  token: string
  rsa_public_key?: string
  [key: string]: any
}

export interface UserProfile {
  userId?: number
  userName?: string
  nickName: string
  phoneNumber: string
  avatar?: string
  deptName?: string
  roles?: string[]
  permissions?: string[]
  workYears?: number
  totalArea?: number
  totalIncome?: number
}

/** 地块（对齐 land_plot） */
export interface Plot {
  plotId: number
  plotName: string
  plotNo: string
  address: string
  area: number
  cropName: string
  boundary: Array<{ latitude: number; longitude: number }>
  center: { latitude: number; longitude: number }
}

export type TaskStatus = 'pending' | 'claimed' | 'working' | 'done' | 'timeout'

/** 任务（后端暂无对应表，字段为拟接入契约） */
export interface Task {
  id: string
  taskNo: string
  plot: Plot
  distanceKm: number
  workTypeId: string
  workTypeName: string
  area: number
  unitPrice: number
  status: TaskStatus
  deadline: string
  publishTime: string
  requirement: string
  publisher: string
  contactPhone: string
  claimedAt?: string
  finishedAt?: string
}

/** 作业单状态：0 待开工 / 1 作业中 / 2 待核算 / 3 已完成 */
export type WorkStatus = 0 | 1 | 2 | 3

/** 作业单（对齐 trajectory_info） */
export interface WorkOrder {
  trajectoryInfoId: string
  taskId: string
  plotId: number
  plotName: string
  address: string
  workType: string
  workTypeName: string
  phoneNumber: string
  status: WorkStatus
  startTime: string
  endTime?: string
  area?: number
  width: number
  totalPoints: number
  pendingPoints: number
  trajectory: Array<{ latitude: number; longitude: number; timestamp: number }>
  receiptNo?: string
  unitPrice?: number
  amount?: number
}

export interface SettlementItem {
  /** 后端为雪花 ID 字符串，数值化会丢精度 */
  itemId: string | number
  trajectoryInfoId: string
  plotId: number
  plotName: string
  workTypeName: string
  area: number
  unitPrice: number
  amount: number
  deduction?: number
  deductionReason?: string
}

export interface Settlement {
  /** 后端为雪花 ID 字符串，数值化会丢精度 */
  settlementId: string | number
  settlementNo: string
  periodStart: string
  periodEnd: string
  totalArea: number
  totalAmount: number
  paidAmount: number
  status: 'pending' | 'paid' | 'reviewing'
  itemsCount: number
  createTime: string
  items?: SettlementItem[]
}

export interface NotificationItem {
  /** 后端为 UUID 字符串，演示数据为数字 */
  id: string | number
  title: string
  content: string
  type: 'task' | 'work' | 'settlement' | 'exception' | 'system'
  read: boolean
  createTime: string
  bizId?: string
}

export interface FeedbackItem {
  id: string
  type: string
  typeName: string
  content: string
  images: string[]
  status: 'submitted' | 'processing' | 'resolved'
  createTime: string
  receiptNo: string
  reply?: string
}

export interface Machinery {
  /** 后端农机主键为字符串（如 M001），演示数据为数字 */
  id: string | number
  machineName: string
  machineNo: string
  machineType: string
  brand: string
  power: string
  workWidth: number
  status: 'idle' | 'working' | 'maintenance'
}

export type AgentRole = 'agent' | 'user'

export interface AgentCard {
  type: 'task' | 'work' | 'settlement' | 'exception' | 'receipt' | 'tip'
  title: string
  fields?: Array<{ label: string; value: string }>
  link?: string
}

export interface AgentMessage {
  id: string
  role: AgentRole
  text: string
  time: number
  toolCall?: {
    name: string
    label: string
    params: Record<string, any>
    status: 'running' | 'success' | 'failed'
    error?: string
  }
  card?: AgentCard
  pendingConfirm?: {
    action: string
    label: string
    summary: string
  }
}

export interface AgentSession {
  id: string
  title: string
  createTime: number
  messages: AgentMessage[]
}
