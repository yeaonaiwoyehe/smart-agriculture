function pad(n: number): string {
  return n < 10 ? `0${n}` : String(n)
}

/** 兼容 'YYYY-MM-DD HH:mm:ss'（iOS 需要 T 分隔） */
function toDate(input?: string | number | Date | null): Date | null {
  if (input === null || input === undefined || input === '') return null
  if (input instanceof Date) return input
  if (typeof input === 'string') {
    return new Date(input.includes('T') ? input : input.replace(' ', 'T'))
  }
  return new Date(input)
}

export function formatDate(input?: string | number | Date | null): string {
  const d = toDate(input)
  if (!d || Number.isNaN(d.getTime())) return '--'
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(
    d.getHours()
  )}:${pad(d.getMinutes())}`
}

export function formatDateOnly(input?: string | number | Date | null): string {
  const d = toDate(input)
  if (!d || Number.isNaN(d.getTime())) return '--'
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

export function formatTime(input?: string | number | Date | null): string {
  const d = toDate(input)
  if (!d || Number.isNaN(d.getTime())) return '--'
  return `${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** 秒 → 03:25:09 */
export function formatDuration(seconds: number): string {
  const s = Math.max(0, Math.floor(seconds || 0))
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  const sec = s % 60
  return `${pad(h)}:${pad(m)}:${pad(sec)}`
}

/** 秒 → 3小时25分 */
export function formatDurationShort(seconds: number): string {
  const s = Math.max(0, Math.floor(seconds || 0))
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  if (h > 0) return `${h}小时${m}分`
  return `${m}分钟`
}

export function formatArea(value?: number | null): string {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return '--'
  return Number(value).toFixed(2)
}

export function formatMoney(value?: number | null): string {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return '--'
  return Number(value).toFixed(2)
}

export function relativeTime(input?: string | number | null): string {
  const d = toDate(input)
  if (!d || Number.isNaN(d.getTime())) return '--'
  const diff = Date.now() - d.getTime()
  if (diff < 60 * 1000) return '刚刚'
  if (diff < 60 * 60 * 1000) return `${Math.floor(diff / 60000)} 分钟前`
  if (diff < 24 * 60 * 60 * 1000) return `${Math.floor(diff / 3600000)} 小时前`
  if (diff < 7 * 24 * 60 * 60 * 1000) return `${Math.floor(diff / 86400000)} 天前`
  return formatDateOnly(d)
}

export function maskPhone(phone?: string): string {
  if (!phone || phone.length < 7) return phone || '--'
  return `${phone.slice(0, 3)}****${phone.slice(-4)}`
}

export function nowStamp(): number {
  return Date.now()
}
