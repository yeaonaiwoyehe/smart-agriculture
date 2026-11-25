import type { AjaxResult, NotificationItem } from '@/types'
import { isMockEnabled } from '@/config'
import request from './request'
import * as mockMessage from '@/mock/message'

function asRows<T = any>(res: any): T[] {
  if (!res) return []
  if (Array.isArray(res)) return res
  if (Array.isArray(res.rows)) return res.rows
  if (Array.isArray(res.data)) return res.data
  if (Array.isArray(res.data?.records)) return res.data.records
  return []
}

/** 后端 type 为 NotificationType 枚举名，映射到前端分类 */
function mapType(raw: any): NotificationItem['type'] {
  const t = String(raw.type ?? raw.notificationType ?? '')
  if (['task', 'work', 'settlement', 'exception', 'system'].includes(t)) {
    return t as NotificationItem['type']
  }
  switch (t) {
    case 'FARM_WORK':
      return 'task'
    case 'FARM_WORK_NOTIFICATION':
      return 'work'
    case 'FARM_MACHINERY_NOTIFICATION':
      return 'task'
    case 'FEEDBACK_REPLY':
      return 'exception'
    default:
      return 'system'
  }
}

function mapTitle(raw: any, type: NotificationItem['type']): string {
  if (raw.title) return raw.title
  const map: Record<NotificationItem['type'], string> = {
    task: '任务通知',
    work: '作业通知',
    settlement: '结算通知',
    exception: '反馈回复',
    system: '系统通知'
  }
  return map[type]
}

function mapNotice(raw: any): NotificationItem {
  const type = mapType(raw)
  const payload = raw.payload || {}
  return {
    id: String(raw.id ?? raw.notificationId ?? ''),
    title: mapTitle(raw, type),
    // 后端通知主体在 payload 中，无统一 title/content 字段
    content: raw.content || payload.content || payload.reply || payload.noticeContent || '',
    type,
    read: raw.read === true || Number(raw.isRead) === 1 || Number(raw.status) === 1,
    createTime: raw.createTime || raw.sendTime || '--',
    bizId: raw.bizId || raw.relationIdentity ? String(raw.bizId ?? raw.relationIdentity) : undefined
  }
}

/**
 * 消息列表：GET /system/notification
 * 注意：该 Controller 类级标注 @PreAuthorize("ss.hasRole('admin')")，普通机手会收到 403。
 */
export async function fetchNotifications(
  type?: NotificationItem['type']
): Promise<NotificationItem[]> {
  if (isMockEnabled()) return mockMessage.fetchNotifications(type)
  const res = await request<AjaxResult>({
    url: '/system/notification',
    method: 'GET',
    query: { pageNum: 1, pageSize: 50 }
  })
  const list = asRows(res).map(mapNotice)
  return type ? list.filter((n) => n.type === type) : list
}

/** 未读数：GET /system/notification/unread 返回的是未读消息列表，取其长度 */
export async function fetchUnreadCount(): Promise<number> {
  if (isMockEnabled()) return mockMessage.fetchUnreadCount()
  const res = await request<AjaxResult>({ url: '/system/notification/unread', method: 'GET' })
  const list = asRows(res)
  if (list.length) return list.length
  return Number((res.data as any)?.count ?? 0) || 0
}

export async function markRead(id: string | number): Promise<void> {
  if (isMockEnabled()) return mockMessage.markRead(id)
  if (id === '' || id === null || id === undefined) return
  await request<AjaxResult>({ url: `/system/notification/${id}`, method: 'PUT' })
}

export async function markAllRead(): Promise<void> {
  if (isMockEnabled()) return mockMessage.markAllRead()
  await request<AjaxResult>({ url: '/system/notification', method: 'PUT' })
}

export async function fetchNotificationDetail(
  id: string | number
): Promise<NotificationItem> {
  if (isMockEnabled()) return mockMessage.fetchNotificationDetail(id)
  const list = await fetchNotifications()
  const found = list.find((n) => String(n.id) === String(id))
  if (!found) throw new Error('消息不存在')
  return found
}
