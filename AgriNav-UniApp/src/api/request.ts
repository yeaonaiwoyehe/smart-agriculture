import { API_BASE_URL } from '@/config'
import { getToken, clearAuth } from '@/utils/storage'

export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE'

export interface RequestOptions {
  url: string
  method?: HttpMethod
  /** JSON body */
  data?: Record<string, any>
  /** 表单参数（application/x-www-form-urlencoded） */
  form?: Record<string, any>
  /** 追加到 query string 的参数 */
  query?: Record<string, any>
  header?: Record<string, string>
  timeout?: number
  /** 是否需要登录态，默认 true */
  auth?: boolean
}

let redirecting = false

function toQueryString(params?: Record<string, any>): string {
  if (!params) return ''
  return Object.keys(params)
    .filter((k) => params[k] !== undefined && params[k] !== null && params[k] !== '')
    .map((k) => `${encodeURIComponent(k)}=${encodeURIComponent(String(params[k]))}`)
    .join('&')
}

function redirectToLogin(): void {
  if (redirecting) return
  redirecting = true
  clearAuth()
  uni.reLaunch({
    url: '/pages/login/index',
    complete: () => {
      setTimeout(() => {
        redirecting = false
      }, 1200)
    }
  })
}

/**
 * 统一请求封装。
 * 说明：登录类接口使用 query / 表单参数（后端 @RequestParam），不接受 JSON。
 */
export default function request<T = any>(opt: RequestOptions): Promise<T> {
  const query = toQueryString(opt.query)
  const url = `${API_BASE_URL}${opt.url}${query ? (opt.url.includes('?') ? '&' : '?') + query : ''}`

  return new Promise<T>((resolve, reject) => {
    const header: Record<string, string> = {
      source: 'App',
      ...(opt.header || {})
    }
    if (opt.form) {
      header['Content-Type'] = 'application/x-www-form-urlencoded'
    } else if (opt.data) {
      header['Content-Type'] = 'application/json;charset=UTF-8'
    }
    const token = getToken()
    if (token && opt.auth !== false) {
      header.Authorization = token
    }

    uni.request({
      url,
      method: opt.method || 'GET',
      data: opt.form ?? opt.data,
      header,
      timeout: opt.timeout || 15000,
      success: (res: any) => {
        const body = res.data || {}
        const status = res.statusCode

        if (status === 401 || body.code === 401) {
          redirectToLogin()
          reject(new Error('登录已失效，请重新登录'))
          return
        }
        if (status === 403 || body.code === 403) {
          reject(new Error(body.msg || '无权限访问'))
          return
        }
        if (status >= 400) {
          reject(new Error(body.msg || `请求失败（HTTP ${status}）`))
          return
        }
        if (typeof body.code === 'number' && body.code !== 200) {
          reject(new Error(body.msg || '请求失败'))
          return
        }
        if (import.meta.env.DEV) {
          console.log('[api]', opt.method || 'GET', opt.url, body.code)
        }
        resolve(body as T)
      },
      fail: (err: any) => {
        reject(new Error(err?.errMsg || '网络异常，请检查网络'))
      }
    })
  })
}

export function getApiBaseUrl(): string {
  return API_BASE_URL
}
