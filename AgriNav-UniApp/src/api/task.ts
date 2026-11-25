import type { Task, TaskStatus } from '@/types'
import { isMockEnabled } from '@/config'
import * as mockTask from '@/mock/task'

export type TaskCounts = mockTask.TaskCounts

export class NotSupportedError extends Error {
  constructor(message: string) {
    super(message)
    this.name = 'NotSupportedError'
  }
}

const REAL_UNSUPPORTED = '后端暂未提供任务中心接口（任务/认领），当前仅演示数据可用'

export type TaskFilter = 'all' | TaskStatus

export interface ClaimResult {
  taskId: string
  receiptNo: string
  claimedAt: string
}

export async function fetchTaskList(filter: TaskFilter = 'all'): Promise<Task[]> {
  if (isMockEnabled()) return mockTask.fetchTaskList(filter)
  throw new NotSupportedError(REAL_UNSUPPORTED)
}

export async function fetchTodayTasks(): Promise<Task[]> {
  if (isMockEnabled()) return mockTask.fetchTodayTasks()
  throw new NotSupportedError(REAL_UNSUPPORTED)
}

export async function fetchTaskDetail(id: string): Promise<Task> {
  if (isMockEnabled()) return mockTask.fetchTaskDetail(id)
  throw new NotSupportedError(REAL_UNSUPPORTED)
}

export async function claimTask(id: string): Promise<ClaimResult> {
  if (isMockEnabled()) return mockTask.claimTask(id)
  throw new NotSupportedError(REAL_UNSUPPORTED)
}

export async function fetchTaskCounts(): Promise<mockTask.TaskCounts> {
  if (isMockEnabled()) return mockTask.fetchTaskCounts()
  throw new NotSupportedError(REAL_UNSUPPORTED)
}

export const TASK_DOMAIN_SUPPORTED_IN_MOCK = true
export const TASK_DOMAIN_SUPPORTED_IN_REAL = false
