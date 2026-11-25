import type { Task, TaskStatus } from '@/types'
import { MockError, delay, getDb, maybeFail, nextReceipt, saveDb } from './db'

export type TaskFilter = 'all' | TaskStatus

const STATUS_WEIGHT: Record<TaskStatus, number> = {
  working: 0,
  claimed: 1,
  pending: 2,
  timeout: 3,
  done: 4
}

export async function fetchTaskList(filter: TaskFilter = 'all'): Promise<Task[]> {
  await delay(220, 480)
  maybeFail('taskList')
  const db = getDb()
  const list = filter === 'all' ? db.tasks : db.tasks.filter((t) => t.status === filter)
  return [...list].sort((a, b) => {
    const w = STATUS_WEIGHT[a.status] - STATUS_WEIGHT[b.status]
    if (w !== 0) return w
    return new Date(a.deadline).getTime() - new Date(b.deadline).getTime()
  })
}

export async function fetchTodayTasks(): Promise<Task[]> {
  await delay(180, 360)
  const db = getDb()
  return db.tasks.filter((t) => t.status === 'claimed' || t.status === 'working')
}

export async function fetchTaskDetail(id: string): Promise<Task> {
  await delay(160, 340)
  const task = getDb().tasks.find((t) => t.id === id)
  if (!task) throw new MockError('任务不存在或已下架', 404)
  return task
}

export interface ClaimResult {
  taskId: string
  receiptNo: string
  claimedAt: string
}

export async function claimTask(id: string): Promise<ClaimResult> {
  await delay(380, 720)
  maybeFail('claimTask')
  const db = getDb()
  const task = db.tasks.find((t) => t.id === id)
  if (!task) throw new MockError('任务不存在', 404)
  if (task.status !== 'pending') {
    throw new MockError('该任务已被接收，请选择其他任务', 409)
  }
  const now = new Date()
  task.status = 'claimed'
  task.claimedAt = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(
    now.getDate()
  ).padStart(2, '0')} ${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(
    2,
    '0'
  )}:${String(now.getSeconds()).padStart(2, '0')}`
  saveDb(db)
  return { taskId: task.id, receiptNo: nextReceipt(), claimedAt: task.claimedAt }
}

export interface TaskCounts {
  pending: number
  claimed: number
  working: number
  done: number
  timeout: number
}

export async function fetchTaskCounts(): Promise<TaskCounts> {
  await delay(120, 260)
  const tasks = getDb().tasks
  const counts: TaskCounts = { pending: 0, claimed: 0, working: 0, done: 0, timeout: 0 }
  tasks.forEach((t) => {
    counts[t.status] += 1
  })
  return counts
}
