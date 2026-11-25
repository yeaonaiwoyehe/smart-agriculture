import type { Settlement, SettlementItem } from '@/types'
import { MockError, delay, fmtDateTime, getDb, maybeFail, nextReceipt, saveDb } from './db'

export interface SettlementSummary {
  totalArea: number
  totalAmount: number
  pendingAmount: number
  paidAmount: number
  reviewingAmount: number
}

function round2(n: number): number {
  return Math.round(n * 100) / 100
}

export async function fetchSettlementList(status?: Settlement['status']): Promise<Settlement[]> {
  await delay(220, 460)
  maybeFail('settlementList')
  const db = getDb()
  const list = status ? db.settlements.filter((s) => s.status === status) : db.settlements
  return list
    .slice()
    .sort((a, b) => Number(b.settlementId) - Number(a.settlementId))
    .map((s) => ({ ...s, items: undefined }))
}

export async function fetchSettlementDetail(id: number): Promise<Settlement> {
  await delay(180, 360)
  const settlement = getDb().settlements.find((s) => s.settlementId === id)
  if (!settlement) throw new MockError('结算单不存在', 404)
  return { ...settlement }
}

export async function fetchSettlementItems(id: number): Promise<SettlementItem[]> {
  const detail = await fetchSettlementDetail(id)
  return detail.items || []
}

export async function fetchSettlementSummary(): Promise<SettlementSummary> {
  await delay(150, 320)
  const db = getDb()
  let totalArea = 0
  let totalAmount = 0
  let pendingAmount = 0
  let paidAmount = 0
  let reviewingAmount = 0
  db.settlements.forEach((s) => {
    totalArea += s.totalArea
    totalAmount += s.totalAmount
    if (s.status === 'pending') pendingAmount += s.totalAmount
    if (s.status === 'paid') paidAmount += s.totalAmount
    if (s.status === 'reviewing') reviewingAmount += s.totalAmount
  })
  return {
    totalArea: round2(totalArea),
    totalAmount: round2(totalAmount),
    pendingAmount: round2(pendingAmount),
    paidAmount: round2(paidAmount),
    reviewingAmount: round2(reviewingAmount)
  }
}

export interface ReviewPayload {
  settlementId: string | number
  itemId: string | number
  reason: string
  images?: string[]
}

export async function submitReview(payload: ReviewPayload): Promise<{ receiptNo: string; message: string }> {
  await delay(420, 780)
  maybeFail('submitReview')
  if (!payload.reason || payload.reason.trim().length < 5) {
    throw new MockError('请至少填写 5 个字的复核原因', 400)
  }
  const db = getDb()
  const settlement = db.settlements.find((s) => s.settlementId === payload.settlementId)
  if (!settlement) throw new MockError('结算单不存在', 404)
  const items = settlement.items || []
  // 将扣减原因标记为复核中
  const item = items.find((i) => i.itemId === payload.itemId)
  if (item) {
    item.deductionReason = `复核申请中：${payload.reason}`
  }
  settlement.status = 'reviewing'
  db.notifications.unshift({
    id: Date.now(),
    title: '复核申请已提交',
    content: `结算单 ${settlement.settlementNo} 的复核申请已提交，工作人员将在 1 个工作日内处理。`,
    type: 'settlement',
    read: false,
    createTime: fmtDateTime(new Date()),
    bizId: String(settlement.settlementId)
  })
  saveDb(db)
  return { receiptNo: nextReceipt(), message: '复核申请已提交' }
}
