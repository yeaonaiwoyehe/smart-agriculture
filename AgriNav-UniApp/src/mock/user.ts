import type { Machinery, Plot, UserProfile } from '@/types'
import { MockError, delay, getDb } from './db'

export async function fetchProfile(): Promise<UserProfile> {
  await delay(150, 300)
  return getDb().user
}

export async function fetchPlots(): Promise<Plot[]> {
  await delay(180, 380)
  return getDb().plots
}

export async function fetchMachineryList(): Promise<Machinery[]> {
  await delay(200, 420)
  return getDb().machineries
}

export async function fetchMachineryDetail(id: number): Promise<Machinery> {
  await delay(150, 300)
  const item = getDb().machineries.find((m) => m.id === id)
  if (!item) throw new MockError('农机不存在', 404)
  return item
}

export interface OverviewStats {
  todayArea: number
  monthArea: number
  monthIncome: number
  totalArea: number
  totalIncome: number
  doneTasks: number
}

export async function fetchOverviewStats(): Promise<OverviewStats> {
  await delay(160, 340)
  const db = getDb()
  const now = new Date()
  const monthPrefix = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
  const monthSettlements = db.settlements.filter((s) => s.periodStart.startsWith(monthPrefix))
  const monthArea = monthSettlements.reduce((sum, s) => sum + s.totalArea, 0)
  const monthIncome = monthSettlements.reduce((sum, s) => sum + s.totalAmount, 0)
  return {
    todayArea: 0,
    monthArea: Number(monthArea.toFixed(2)),
    monthIncome: Number(monthIncome.toFixed(2)),
    totalArea: db.user.totalArea || 0,
    totalIncome: db.user.totalIncome || 0,
    doneTasks: db.tasks.filter((t) => t.status === 'done').length
  }
}
