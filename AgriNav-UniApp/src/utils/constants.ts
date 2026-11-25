import type { NotificationItem, Settlement, TaskStatus } from '@/types'

export const TASK_STATUS_TEXT: Record<TaskStatus, string> = {
  pending: '待接收',
  claimed: '待作业',
  working: '进行中',
  done: '已完成',
  timeout: '已超时'
}

export const TASK_STATUS_CLASS: Record<TaskStatus, string> = {
  pending: 'tag--orange',
  claimed: 'tag--green',
  working: 'tag--green',
  done: 'tag--gray',
  timeout: 'tag--red'
}

export const WORK_STATUS_TEXT: Record<number, string> = {
  0: '待开工',
  1: '作业中',
  2: '待核算',
  3: '已完成'
}

export const SETTLEMENT_STATUS_TEXT: Record<Settlement['status'], string> = {
  pending: '待结算',
  paid: '已结算',
  reviewing: '复核中'
}

export const SETTLEMENT_STATUS_CLASS: Record<Settlement['status'], string> = {
  pending: 'tag--orange',
  paid: 'tag--green',
  reviewing: 'tag--red'
}

export const NOTICE_TYPE_TEXT: Record<NotificationItem['type'], string> = {
  task: '任务',
  work: '作业',
  settlement: '结算',
  exception: '异常',
  system: '系统'
}

export const FEEDBACK_STATUS_TEXT = {
  submitted: '已提交',
  processing: '处理中',
  resolved: '已解决'
} as const

export const WORK_TYPE_OPTIONS = [
  { code: 'FW01', name: '深松整地' },
  { code: 'FW02', name: '秸秆还田' },
  { code: 'FW03', name: '旋耕播种' },
  { code: 'FW04', name: '植保作业' },
  { code: 'FW05', name: '机械收割' }
]
