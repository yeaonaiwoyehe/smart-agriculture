import type { AjaxResult, FeedbackItem } from '@/types'
import { isMockEnabled } from '@/config'
import request from './request'
import * as mockException from '@/mock/exception'

function asRows<T = any>(res: any): T[] {
  if (!res) return []
  if (Array.isArray(res)) return res
  if (Array.isArray(res.rows)) return res.rows
  if (Array.isArray(res.data)) return res.data
  if (Array.isArray(res.data?.records)) return res.data.records
  return []
}

/** 后端反馈标签为 SysDictData：dictCode / dictLabel / dictValue */
function mapLabel(raw: any): mockException.ExceptionLabel {
  return {
    code: String(raw.dictCode ?? raw.code ?? raw.value ?? raw.id ?? ''),
    name: raw.dictLabel ?? raw.name ?? raw.label ?? '其他问题'
  }
}

function labelOf(raw: any): { code: string; name: string } {
  const first = Array.isArray(raw.labels) ? raw.labels[0] : null
  if (first && typeof first === 'object') {
    return {
      code: String(first.dictCode ?? ''),
      name: first.dictLabel ?? '其他问题'
    }
  }
  return {
    code: String(raw.type ?? ''),
    name: raw.typeName ?? '其他问题'
  }
}

function mapFeedback(raw: any): FeedbackItem {
  const label = labelOf(raw)
  const checked = raw.isChecked === 1 || raw.isChecked === true || raw.status === 'resolved'
  return {
    id: String(raw.id ?? raw.feedbackId ?? ''),
    type: label.code,
    typeName: label.name,
    content: raw.content || '',
    // 后端 Feedback 无图片字段
    images: raw.images || [],
    status: checked ? 'resolved' : raw.reply ? 'processing' : 'submitted',
    createTime: raw.commitTime || raw.createTime || '--',
    receiptNo: String(raw.id ?? raw.feedbackId ?? '--'),
    reply: raw.reply
  }
}

/** 反馈标签：GET /system/feedback/labels（返回 SysDictData 列表） */
export async function fetchExceptionLabels(): Promise<mockException.ExceptionLabel[]> {
  if (isMockEnabled()) return mockException.fetchExceptionLabels()
  const res = await request<AjaxResult>({ url: '/system/feedback/labels', method: 'GET' })
  return asRows(res).map(mapLabel).filter((l) => l.code !== '')
}

/**
 * 提交反馈：POST /system/feedback
 * 后端 @RequestBody Feedback，labels 为 List<SysDictData>，因此必须传 [{ dictCode }]；
 * 后端 Feedback 没有图片字段，图片无法随反馈保存。
 */
export async function submitFeedback(
  payload: mockException.SubmitFeedbackPayload
): Promise<FeedbackItem> {
  if (isMockEnabled()) return mockException.submitFeedback(payload)
  const dictCode = Number(payload.type)
  if (!Number.isFinite(dictCode)) {
    throw new Error('反馈类型无效：请使用后端返回的反馈标签')
  }
  const res = await request<AjaxResult>({
    url: '/system/feedback',
    method: 'POST',
    data: {
      content: payload.content,
      labels: [{ dictCode }]
    }
  })
  const data = (res.data || {}) as any
  const id = typeof data === 'object' ? data.id : ''
  return {
    id: String(id || '--'),
    type: payload.type,
    typeName: '',
    content: payload.content,
    images: [],
    status: 'submitted',
    createTime: new Date().toLocaleString('zh-CN'),
    receiptNo: String(id || '--')
  }
}

/** 我的反馈：GET /system/feedback/myFeedback（{total, data}） */
export async function fetchFeedbackList(): Promise<FeedbackItem[]> {
  if (isMockEnabled()) return mockException.fetchFeedbackList()
  const res = await request<AjaxResult>({
    url: '/system/feedback/myFeedback',
    method: 'GET',
    query: { pageNum: 1, pageSize: 50 }
  })
  return asRows(res).map(mapFeedback)
}

export async function fetchFeedbackDetail(id: string): Promise<FeedbackItem> {
  if (isMockEnabled()) return mockException.fetchFeedbackDetail(id)
  const list = await fetchFeedbackList()
  const found = list.find((f) => f.id === id)
  if (!found) throw new Error('工单不存在')
  return found
}
