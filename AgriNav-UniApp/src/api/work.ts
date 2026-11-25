import type { AjaxResult, WorkOrder, WorkStatus } from '@/types'
import { isMockEnabled } from '@/config'
import request from './request'
import * as mockWork from '@/mock/work'
import { NotSupportedError } from './task'

export type WorkStats = mockWork.WorkStats

/** 真实后端是否有开工/完工（作业会话）接口 */
export const WORK_SESSION_SUPPORTED_IN_REAL = false
const REAL_UNSUPPORTED = '后端暂未提供开工/完工接口（作业会话），当前仅演示数据可用'

function mapRealStatus(status: unknown): WorkStatus {
  if (status === 3 || status === '3' || status === 'finished') return 3
  if (status === 2 || status === '2') return 2
  if (status === 1 || status === '1' || status === 'working') return 1
  return 0
}

export async function fetchCurrentWork(): Promise<WorkOrder | null> {
  if (isMockEnabled()) return mockWork.fetchCurrentWork()
  return null
}

export async function fetchWorkDetail(trajectoryInfoId: string): Promise<WorkOrder> {
  if (isMockEnabled()) return mockWork.fetchWorkDetail(trajectoryInfoId)
  throw new NotSupportedError(REAL_UNSUPPORTED)
}

function asRows<T = any>(res: any): T[] {
  if (!res) return []
  if (Array.isArray(res.data)) return res.data
  if (Array.isArray(res.rows)) return res.rows
  if (Array.isArray(res.data?.records)) return res.data.records
  return []
}

function parseTime(value: unknown): number {
  if (!value) return 0
  return new Date(String(value).replace(' ', 'T')).getTime() || 0
}

/** 历史作业单：真实模式读取 POST /effectiveTrajectory（有效轨迹，需 trajectory:effective:list 权限） */
export async function fetchWorkHistory(): Promise<WorkOrder[]> {
  if (isMockEnabled()) return mockWork.fetchWorkHistory()
  const res = await request<AjaxResult>({
    url: '/effectiveTrajectory',
    method: 'POST',
    query: { pageNum: 1, pageSize: 50 },
    data: {}
  })
  const rows = asRows(res)
  return rows.map((r) => {
    const points = Array.isArray(r.points) ? r.points : []
    return {
      trajectoryInfoId: String(r.trajectoryInfoId ?? r.id ?? ''),
      taskId: '',
      plotId: Number(r.plotId ?? 0),
      plotName: r.plotName || '--',
      address: r.address || r.location || '--',
      workType: String(r.workType ?? ''),
      // 后端只返回作业类型编码，无名称字段
      workTypeName: r.workTypeName || '--',
      phoneNumber: r.phoneNumber || '',
      // 有效轨迹均为已结束的作业记录
      status: mapRealStatus(r.status ?? (r.endTime ? 3 : 1)),
      startTime: r.startTime || '--',
      endTime: r.endTime,
      area: Number(r.area ?? 0),
      width: Number(r.width ?? 0),
      totalPoints: points.length,
      pendingPoints: 0,
      trajectory: points.map((p: any, idx: number) => ({
        latitude: Number(p.latitude ?? p.y ?? 0),
        longitude: Number(p.longitude ?? p.x ?? 0),
        timestamp: parseTime(p.time) || Date.now() + idx
      })),
      unitPrice: Number(r.unitPrice ?? 0)
    }
  })
}

export async function startWork(taskId: string): Promise<WorkOrder> {
  if (isMockEnabled()) return mockWork.startWork(taskId)
  throw new NotSupportedError(REAL_UNSUPPORTED)
}

export async function finishWork(trajectoryInfoId: string): Promise<WorkOrder> {
  if (isMockEnabled()) return mockWork.finishWork(trajectoryInfoId)
  throw new NotSupportedError(REAL_UNSUPPORTED)
}

export async function confirmSettlement(trajectoryInfoId: string): Promise<WorkOrder> {
  if (isMockEnabled()) return mockWork.confirmSettlement(trajectoryInfoId)
  throw new NotSupportedError(REAL_UNSUPPORTED)
}

export async function syncPendingPoints(trajectoryInfoId: string): Promise<number> {
  if (isMockEnabled()) return mockWork.syncPendingPoints(trajectoryInfoId)
  return 0
}

export async function fetchWorkStats(): Promise<mockWork.WorkStats> {
  if (isMockEnabled()) return mockWork.fetchWorkStats()
  return { todayArea: 0, todayOrders: 0, monthArea: 0 }
}
