import type { FeedbackItem } from '@/types'
import { MockError, delay, fmtDateTime, getDb, maybeFail, saveDb } from './db'

export interface ExceptionLabel {
  code: string
  name: string
}

const LABELS: ExceptionLabel[] = [
  { code: 'machine', name: '农机故障' },
  { code: 'plot', name: '地块问题' },
  { code: 'road', name: '道路受阻' },
  { code: 'gps', name: 'GPS/设备异常' },
  { code: 'weather', name: '天气环境' },
  { code: 'other', name: '其他问题' }
]

export async function fetchExceptionLabels(): Promise<ExceptionLabel[]> {
  await delay(120, 260)
  return LABELS
}

export interface SubmitFeedbackPayload {
  type: string
  content: string
  images?: string[]
  taskId?: string
}

export async function submitFeedback(payload: SubmitFeedbackPayload): Promise<FeedbackItem> {
  await delay(480, 860)
  maybeFail('submitFeedback')
  if (!payload.content || payload.content.trim().length < 5) {
    throw new MockError('请至少填写 5 个字的问题描述', 400)
  }
  const label = LABELS.find((l) => l.code === payload.type) || LABELS[LABELS.length - 1]
  const db = getDb()
  const id = `FB${Date.now()}`
  const item: FeedbackItem = {
    id,
    type: label.code,
    typeName: label.name,
    content: payload.content.trim(),
    images: payload.images || [],
    status: 'submitted',
    createTime: fmtDateTime(new Date()),
    receiptNo: id,
    reply: undefined
  }
  db.feedbacks.unshift(item)
  db.notifications.unshift({
    id: Date.now() + 1,
    title: '异常已上报',
    content: `您上报的「${label.name}」已提交，工单号 ${id}，工作人员会尽快处理。`,
    type: 'exception',
    read: false,
    createTime: fmtDateTime(new Date()),
    bizId: id
  })
  saveDb(db)
  return { ...item }
}

export async function fetchFeedbackList(): Promise<FeedbackItem[]> {
  await delay(200, 400)
  const db = getDb()
  return db.feedbacks.slice().sort((a, b) => (a.createTime < b.createTime ? 1 : -1))
}

export async function fetchFeedbackDetail(id: string): Promise<FeedbackItem> {
  await delay(150, 300)
  const item = getDb().feedbacks.find((f) => f.id === id)
  if (!item) throw new MockError('工单不存在', 404)
  return { ...item }
}
