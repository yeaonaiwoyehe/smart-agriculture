import type { WorkOrder } from '@/types'
import { MockError, delay, fmtDateTime, getDb, maybeFail, nextReceipt, nextWorkOrderNo, saveDb } from './db'
import { buildDemoTrajectory } from './trajectory'

function round2(n: number): number {
  return Math.round(n * 100) / 100
}

export async function fetchCurrentWork(): Promise<WorkOrder | null> {
  await delay(160, 320)
  const db = getDb()
  const active = db.workOrders.find((o) => o.status === 1 || o.status === 2)
  return active ? { ...active } : null
}

export async function fetchWorkHistory(): Promise<WorkOrder[]> {
  await delay(200, 420)
  const db = getDb()
  return db.workOrders
    .filter((o) => o.status === 3 || o.status === 2)
    .slice()
    .sort((a, b) => new Date(b.startTime).getTime() - new Date(a.startTime).getTime())
}

export async function fetchWorkDetail(trajectoryInfoId: string): Promise<WorkOrder> {
  await delay(160, 340)
  const order = getDb().workOrders.find((o) => o.trajectoryInfoId === trajectoryInfoId)
  if (!order) throw new MockError('作业单不存在', 404)
  return { ...order }
}

export async function startWork(taskId: string): Promise<WorkOrder> {
  await delay(420, 760)
  maybeFail('startWork')
  const db = getDb()
  const task = db.tasks.find((t) => t.id === taskId)
  if (!task) throw new MockError('任务不存在', 404)
  if (task.status === 'working') throw new MockError('该任务已在作业中', 409)
  if (task.status !== 'claimed') throw new MockError('任务状态不允许开工，请先接收任务', 409)
  if (db.workOrders.some((o) => o.status === 1)) {
    throw new MockError('存在进行中的作业，请先结束当前作业', 409)
  }

  const startTime = fmtDateTime(new Date())
  const phoneNumber = db.user.phoneNumber
  const order: WorkOrder = {
    trajectoryInfoId: nextWorkOrderNo(phoneNumber),
    taskId: task.id,
    plotId: task.plot.plotId,
    plotName: task.plot.plotName,
    address: task.plot.address,
    workType: task.workTypeId,
    workTypeName: task.workTypeName,
    phoneNumber,
    status: 1,
    startTime,
    width: 3.2,
    totalPoints: 0,
    pendingPoints: 0,
    trajectory: [],
    receiptNo: nextReceipt(),
    unitPrice: task.unitPrice
  }
  order.trajectory = buildDemoTrajectory(task.plot, 14, new Date(startTime.replace(' ', 'T')).getTime())
  order.totalPoints = order.trajectory.length

  task.status = 'working'
  db.workOrders.unshift(order)
  saveDb(db)
  return { ...order }
}

export async function finishWork(trajectoryInfoId: string): Promise<WorkOrder> {
  await delay(520, 900)
  maybeFail('finishWork')
  const db = getDb()
  const order = db.workOrders.find((o) => o.trajectoryInfoId === trajectoryInfoId)
  if (!order) throw new MockError('作业单不存在', 404)
  if (order.status !== 1) throw new MockError('作业单状态不允许完工', 409)

  const endTime = fmtDateTime(new Date())
  const task = db.tasks.find((t) => t.id === order.taskId)
  const plotArea = task ? task.plot.area : order.area || 0
  const ratio = 0.955 + Math.random() * 0.04
  const area = round2(plotArea * ratio)
  const unitPrice = order.unitPrice || 45

  order.status = 2
  order.endTime = endTime
  order.area = area
  order.amount = round2(area * unitPrice)
  order.pendingPoints = 0
  saveDb(db)
  return { ...order }
}

/** 核算完成：作业单转为已完成，并写入结算单 */
export async function confirmSettlement(trajectoryInfoId: string): Promise<WorkOrder> {
  await delay(600, 1000)
  const db = getDb()
  const order = db.workOrders.find((o) => o.trajectoryInfoId === trajectoryInfoId)
  if (!order) throw new MockError('作业单不存在', 404)
  if (order.status !== 2) return { ...order }

  order.status = 3
  if (!order.area) order.area = 0
  if (!order.amount) order.amount = round2(order.area * (order.unitPrice || 45))

  const task = db.tasks.find((t) => t.id === order.taskId)
  if (task) {
    task.status = 'done'
    task.finishedAt = order.endTime
  }

  const target = db.settlements.find((s) => s.status === 'pending')
  if (target && order.area > 0) {
    target.items = target.items || []
    target.items.push({
      itemId: target.items.length + 100,
      trajectoryInfoId: order.trajectoryInfoId,
      plotId: order.plotId,
      plotName: order.plotName,
      workTypeName: order.workTypeName,
      area: order.area,
      unitPrice: order.unitPrice || 45,
      amount: order.amount
    })
    target.itemsCount = target.items.length
    target.totalArea = round2(target.items.reduce((sum, i) => sum + i.area, 0))
    target.totalAmount = round2(target.items.reduce((sum, i) => sum + i.amount, 0))
  }

  db.notifications.unshift({
    id: Date.now(),
    title: '作业已完成',
    content: `${order.plotName} ${order.workTypeName}作业已完成，核算面积 ${order.area} 亩，金额 ${order.amount} 元。`,
    type: 'work',
    read: false,
    createTime: fmtDateTime(new Date()),
    bizId: order.trajectoryInfoId
  })

  saveDb(db)
  return { ...order }
}

export async function syncPendingPoints(trajectoryInfoId: string): Promise<number> {
  await delay(400, 700)
  const db = getDb()
  const order = db.workOrders.find((o) => o.trajectoryInfoId === trajectoryInfoId)
  if (!order) throw new MockError('作业单不存在', 404)
  const count = order.pendingPoints
  order.pendingPoints = 0
  saveDb(db)
  return count
}

export interface WorkStats {
  todayArea: number
  todayOrders: number
  monthArea: number
}

export async function fetchWorkStats(): Promise<WorkStats> {
  await delay(150, 300)
  const db = getDb()
  const today = new Date()
  const todayKey = fmtDateTime(today).slice(0, 10)
  const monthKey = fmtDateTime(today).slice(0, 7)
  let todayArea = 0
  let todayOrders = 0
  let monthArea = 0
  db.workOrders.forEach((o) => {
    if (o.status === 3 && o.area) {
      if (o.startTime.startsWith(todayKey)) {
        todayArea += o.area
        todayOrders += 1
      }
      if (o.startTime.startsWith(monthKey)) monthArea += o.area
    }
  })
  return { todayArea: round2(todayArea), todayOrders, monthArea: round2(monthArea) }
}
