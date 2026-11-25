import type { AjaxResult, Settlement, SettlementItem } from '@/types'
import { isMockEnabled } from '@/config'
import request from './request'
import * as mockSettlement from '@/mock/settlement'
import { fetchPlots } from './user'

/** 兼容 AjaxResult / TableDataInfo / Page 三种分页返回结构 */
function asRows<T = any>(res: any): T[] {
  if (!res) return []
  if (Array.isArray(res)) return res
  if (Array.isArray(res.rows)) return res.rows
  if (Array.isArray(res.data)) return res.data
  if (Array.isArray(res.data?.records)) return res.data.records
  if (Array.isArray(res.records)) return res.records
  return []
}

/** 后端 status：0 已生成 / 1 已确认 / 2 已付款；前端仅 pending/paid 两态可对应 */
function mapStatus(status: unknown): Settlement['status'] {
  if (Number(status) === 2) return 'paid'
  return 'pending'
}

function mapSettlement(raw: any): Settlement {
  return {
    // 实测为 19 位雪花 ID 字符串，数值化会丢精度导致详情 404
    settlementId: String(raw.settlementId ?? raw.id ?? ''),
    settlementNo: raw.settlementNo || raw.settlementNumber || '--',
    periodStart: raw.periodStart || raw.startDate || '--',
    periodEnd: raw.periodEnd || raw.endDate || '--',
    totalArea: Number(raw.totalArea ?? 0),
    totalAmount: Number(raw.totalAmount ?? 0),
    // 后端结算单无已付金额字段，不虚构
    paidAmount: Number(raw.paidAmount ?? 0),
    status: mapStatus(raw.status),
    itemsCount: Number(raw.itemsCount ?? 0),
    createTime: raw.createTime || '--'
  }
}

function mapItem(
  raw: any,
  plotNames: Record<string, string> = {},
  workTypes: Record<string, string> = {}
): SettlementItem {
  const plotId = Number(raw.plotId ?? 0)
  const workTypeCode = String(raw.workType ?? '')
  return {
    // 实测明细主键同为雪花 ID 字符串
    itemId: String(raw.itemId ?? raw.id ?? ''),
    trajectoryInfoId: String(raw.trajectoryInfoId ?? ''),
    plotId,
    // 结算明细无地块名，按 plotId 回填（列表未命中时再查详情）
    plotName: raw.plotName || plotNames[String(plotId)] || '--',
    // 后端只有作业类型编码，名称取自 GET /fieldWork/list
    workTypeName: raw.workTypeName || workTypes[workTypeCode] || workTypeCode || '--',
    area: Number(raw.area ?? 0),
    unitPrice: Number(raw.unitPrice ?? 0),
    amount: Number(raw.amount ?? 0),
    deduction: raw.deduction === undefined ? undefined : Number(raw.deduction),
    deductionReason: raw.deductionReason || raw.remark
  }
}

/** 作业类型字典：GET /fieldWork/list（实测 data: [{ id: 4001, workName: "耕地" }]） */
async function fetchWorkTypeNames(): Promise<Record<string, string>> {
  try {
    const res = await request<AjaxResult>({ url: '/fieldWork/list', method: 'GET' })
    return asRows(res).reduce<Record<string, string>>((acc, w: any) => {
      if (w.id !== undefined && w.workName) acc[String(w.id)] = String(w.workName)
      return acc
    }, {})
  } catch {
    return {}
  }
}

/** 明细里的 plotId 可能不在「我的地块」列表中，逐个查详情回填地块名 */
async function resolvePlotName(
  plotId: number,
  cache: Record<string, string>
): Promise<string> {
  if (!plotId) return '--'
  const key = String(plotId)
  if (cache[key]) return cache[key]
  try {
    const res = await request<AjaxResult>({ url: `/landPlot/${plotId}`, method: 'GET' })
    const name = ((res.data || {}) as any).landName || '--'
    cache[key] = name
    return name
  } catch {
    return '--'
  }
}

/** 结算单列表：GET /settlement/list（TableDataInfo：rows/total） */
export async function fetchSettlementList(status?: Settlement['status']): Promise<Settlement[]> {
  if (isMockEnabled()) return mockSettlement.fetchSettlementList(status)
  const res = await request<AjaxResult>({
    url: '/settlement/list',
    method: 'GET',
    query: { pageNum: 1, pageSize: 50 }
  })
  const list = asRows(res).map(mapSettlement)
  return status ? list.filter((s) => s.status === status) : list
}

/** 结算单详情：GET /settlement/{id} + GET /settlement/items/{id}（详情接口不含明细） */
export async function fetchSettlementDetail(id: string | number): Promise<Settlement> {
  if (isMockEnabled()) return mockSettlement.fetchSettlementDetail(Number(id))
  const res = await request<AjaxResult>({ url: `/settlement/${id}`, method: 'GET' })
  const data = (res.data || {}) as any

  const itemsRes = await request<AjaxResult>({ url: `/settlement/items/${id}`, method: 'GET' })
  const rawItems = asRows(itemsRes)

  let plotNames: Record<string, string> = {}
  let workTypes: Record<string, string> = {}
  if (rawItems.length) {
    const [plotResult, workTypeResult] = await Promise.allSettled([
      fetchPlots(),
      fetchWorkTypeNames()
    ])
    if (plotResult.status === 'fulfilled') {
      plotNames = plotResult.value.reduce<Record<string, string>>((acc, p) => {
        acc[String(p.plotId)] = p.plotName
        return acc
      }, {})
    }
    if (workTypeResult.status === 'fulfilled') {
      workTypes = workTypeResult.value
    }
  }

  const items: SettlementItem[] = []
  for (const raw of rawItems) {
    const mapped = mapItem(raw, plotNames, workTypes)
    if (mapped.plotName === '--') {
      mapped.plotName = await resolvePlotName(mapped.plotId, plotNames)
    }
    items.push(mapped)
  }
  return { ...mapSettlement(data), itemsCount: items.length, items }
}

export async function fetchSettlementItems(id: string | number): Promise<SettlementItem[]> {
  if (isMockEnabled()) return mockSettlement.fetchSettlementItems(Number(id))
  const [res, workTypes] = await Promise.all([
    request<AjaxResult>({ url: `/settlement/items/${id}`, method: 'GET' }),
    fetchWorkTypeNames()
  ])
  return asRows(res).map((item) => mapItem(item, {}, workTypes))
}

export async function fetchSettlementSummary(): Promise<mockSettlement.SettlementSummary> {
  if (isMockEnabled()) return mockSettlement.fetchSettlementSummary()
  const list = await fetchSettlementList()
  let totalArea = 0
  let totalAmount = 0
  let pendingAmount = 0
  let paidAmount = 0
  let reviewingAmount = 0
  list.forEach((s) => {
    totalArea += s.totalArea
    totalAmount += s.totalAmount
    if (s.status === 'pending') pendingAmount += s.totalAmount
    if (s.status === 'paid') paidAmount += s.totalAmount
    if (s.status === 'reviewing') reviewingAmount += s.totalAmount
  })
  return {
    totalArea: Math.round(totalArea * 100) / 100,
    totalAmount: Math.round(totalAmount * 100) / 100,
    pendingAmount: Math.round(pendingAmount * 100) / 100,
    paidAmount: Math.round(paidAmount * 100) / 100,
    reviewingAmount: Math.round(reviewingAmount * 100) / 100
  }
}

/**
 * 结算复核：后端 /reviewRequest 是「轨迹面积复核」业务（trajectoryInfoId/claimedArea 等），
 * 并非结算单复核，参数与语义均不匹配，因此真实模式不发起请求，避免伪造成功。
 */
export async function submitReview(
  payload: mockSettlement.ReviewPayload
): Promise<{ receiptNo: string; message: string }> {
  if (isMockEnabled()) return mockSettlement.submitReview(payload)
  throw new Error('后端暂未提供结算复核接口（现有 /reviewRequest 为轨迹面积复核业务，不适用）')
}
