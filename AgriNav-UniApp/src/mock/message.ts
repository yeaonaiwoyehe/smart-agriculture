import type { NotificationItem } from '@/types'
import { MockError, delay, getDb, saveDb } from './db'

export async function fetchNotifications(type?: NotificationItem['type']): Promise<NotificationItem[]> {
  await delay(180, 380)
  const db = getDb()
  const list = type ? db.notifications.filter((n) => n.type === type) : db.notifications
  return list.slice().sort((a, b) => (a.createTime < b.createTime ? 1 : -1))
}

export async function fetchUnreadCount(): Promise<number> {
  await delay(100, 220)
  return getDb().notifications.filter((n) => !n.read).length
}

export async function markRead(id: string | number): Promise<void> {
  await delay(100, 200)
  const db = getDb()
  const item = db.notifications.find((n) => String(n.id) === String(id))
  if (item) {
    item.read = true
    saveDb(db)
  }
}

export async function markAllRead(): Promise<void> {
  await delay(200, 360)
  const db = getDb()
  db.notifications.forEach((n) => {
    n.read = true
  })
  saveDb(db)
}

export async function fetchNotificationDetail(id: string | number): Promise<NotificationItem> {
  await delay(140, 280)
  const item = getDb().notifications.find((n) => String(n.id) === String(id))
  if (!item) throw new MockError('消息不存在', 404)
  return { ...item }
}
