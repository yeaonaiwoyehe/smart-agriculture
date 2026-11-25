import type { AjaxResult, Machinery, Plot, UserProfile } from '@/types'
import { isMockEnabled } from '@/config'
import request from './request'
import * as mockUser from '@/mock/user'

export interface OverviewStats {
  todayArea: number
  monthArea: number
  monthIncome: number
  totalArea: number
  totalIncome: number
  doneTasks: number
}

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

/**
 * 解析后端 postgis 字段（Jackson 已序列化为 WKT 字符串）。
 * 兼容：WKT 字符串、GeoJSON、已解析对象、数组。
 */
function parsePoint(value: any): { latitude: number; longitude: number } {
  const empty = { latitude: 0, longitude: 0 }
  if (!value) return empty
  if (typeof value === 'string') {
    const wkt = value.replace(/^SRID=\d+;/i, '')
    const m = wkt.match(/POINT\s*\(\s*(-?\d+(?:\.\d+)?)\s+(-?\d+(?:\.\d+)?)/i)
    if (m) return { longitude: Number(m[1]), latitude: Number(m[2]) }
    return empty
  }
  if (typeof value === 'object') {
    if (value.latitude !== undefined && value.longitude !== undefined) {
      return { latitude: Number(value.latitude), longitude: Number(value.longitude) }
    }
    // postgis Point 对象：x 为经度、y 为纬度
    if (value.x !== undefined && value.y !== undefined) {
      return { latitude: Number(value.y), longitude: Number(value.x) }
    }
  }
  return empty
}

function parsePolygon(value: any): Array<{ latitude: number; longitude: number }> {
  if (!value) return []
  if (Array.isArray(value)) {
    return value
      .map((p) => parsePoint(p))
      .filter((p) => p.latitude !== 0 || p.longitude !== 0)
  }
  if (typeof value === 'string') {
    const wkt = value.replace(/^SRID=\d+;/i, '')
    const ring = wkt.match(/\(\s*\(([^)]*)\)/)
    if (!ring) return []
    return ring[1]
      .split(',')
      .map((pair) => pair.trim().split(/\s+/))
      .filter((xy) => xy.length >= 2)
      .map(([lon, lat]) => ({ latitude: Number(lat), longitude: Number(lon) }))
      .filter((p) => Number.isFinite(p.latitude) && Number.isFinite(p.longitude))
  }
  if (typeof value === 'object') {
    // postgis Polygon 对象或 GeoJSON
    const coords = value.coordinates?.[0]
    if (Array.isArray(coords)) {
      return coords.map(([lon, lat]: [number, number]) => ({
        latitude: Number(lat),
        longitude: Number(lon)
      }))
    }
    if (Array.isArray(value.points)) return parsePolygon(value.points)
  }
  return []
}

/**
 * 个人资料：GET /system/user/profile
 * 实测返回：data 内为 userId/userName/nickName/phonenumber/avatar，
 * departmentName 为「数组」（如 ["若依科技","深圳总公司","研发部门"]），角色在顶层 roleGroup 字符串。
 */
export async function fetchProfile(): Promise<UserProfile> {
  if (isMockEnabled()) return mockUser.fetchProfile()
  const res = await request<AjaxResult>({ url: '/system/user/profile', method: 'GET' })
  const data = (res.data || {}) as any
  const roleGroup =
    typeof res.roleGroup === 'string'
      ? res.roleGroup.split(',').map((r: string) => r.trim()).filter(Boolean)
      : []
  const dept = Array.isArray(res.departmentName)
    ? res.departmentName.filter(Boolean).join(' / ')
    : res.departmentName || data.dept?.deptName || ''
  return {
    userId: data.userId,
    userName: data.userName,
    nickName: data.nickName || data.userName || '机手',
    phoneNumber: data.phonenumber || data.phoneNumber || '',
    avatar: data.avatar,
    deptName: dept,
    roles: Array.isArray(data.roles) ? data.roles : roleGroup,
    totalArea: 0,
    totalIncome: 0
  }
}

/**
 * 我的农机：POST /agriculturalMachinery/list
 * 后端为「农机找活」信息表，非管理员需显式传 myAgriculturalMachinery=true 才只返回本人发布的数据。
 */
export async function fetchMachineryList(): Promise<Machinery[]> {
  if (isMockEnabled()) return mockUser.fetchMachineryList()
  const res = await request<AjaxResult>({
    url: '/agriculturalMachinery/list',
    method: 'POST',
    query: { pageNum: 1, pageSize: 50 },
    data: { myAgriculturalMachinery: true }
  })
  const rows = asRows(res)
  return rows.map((m) => ({
    // 实测农机主键为字符串（如 M001），不能转 Number
    id: String(m.id ?? ''),
    machineName: m.name || '--',
    // 后端农机找活表无车牌/编号、品牌、功率、幅宽字段
    machineNo: '--',
    machineType: m.typeName || '--',
    brand: '--',
    power: '--',
    workWidth: 0,
    // status：0 在架可用，1 已过期下架，2 已删除（列表已排除）
    status: Number(m.status) === 0 ? 'idle' : 'maintenance'
  }))
}

/** 我的地块：GET /landPlot?current=1&size=50（登录即可，仅返回当前用户创建的地块） */
export async function fetchPlots(): Promise<Plot[]> {
  if (isMockEnabled()) return mockUser.fetchPlots()
  const res = await request<AjaxResult>({
    url: '/landPlot',
    method: 'GET',
    query: { current: 1, size: 50 }
  })
  const rows = asRows(res)
  return rows.map((p) => ({
    plotId: Number(p.id ?? 0),
    plotName: p.landName || '未命名地块',
    // 后端 land_plot 无地块编号与作物字段
    plotNo: '--',
    address: p.address || '--',
    area: Number(p.landArea ?? 0),
    cropName: '--',
    center: parsePoint(p.centerPoint),
    boundary: parsePolygon(p.boundary)
  }))
}

/** 作业概览统计（真实模式由结算数据聚合，后端暂缺当日作业统计接口） */
export async function fetchOverviewStats(): Promise<OverviewStats> {
  if (isMockEnabled()) return mockUser.fetchOverviewStats()
  const res = await request<AjaxResult>({
    url: '/settlement/list',
    method: 'GET',
    query: { pageNum: 1, pageSize: 100 }
  })
  const rows = asRows(res)
  const now = new Date()
  const monthPrefix = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
  let totalArea = 0
  let totalIncome = 0
  let monthArea = 0
  let monthIncome = 0
  rows.forEach((s) => {
    const area = Number(s.totalArea || 0)
    const amount = Number(s.totalAmount || 0)
    totalArea += area
    totalIncome += amount
    if (String(s.periodStart || '').startsWith(monthPrefix)) {
      monthArea += area
      monthIncome += amount
    }
  })
  return {
    todayArea: 0,
    monthArea: Math.round(monthArea * 100) / 100,
    monthIncome: Math.round(monthIncome * 100) / 100,
    totalArea: Math.round(totalArea * 100) / 100,
    totalIncome: Math.round(totalIncome * 100) / 100,
    doneTasks: 0
  }
}
