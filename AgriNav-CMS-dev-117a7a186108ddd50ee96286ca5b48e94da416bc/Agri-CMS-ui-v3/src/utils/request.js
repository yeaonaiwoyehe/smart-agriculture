/**
 * 真实后端请求封装（对应老版本 Agri-CMS-ui/src/utils/request.js）
 * 用原生 fetch 实现，无需新增 axios 依赖。
 * 保持若依约定：Bearer token、source: Web 请求头、{ code, msg, data } 响应结构。
 */

const BASE = (import.meta.env.VITE_APP_BASE_API || '/dev-api').trim().replace(/\/$/, '')

const TOKEN_KEY = 'Admin-Token'

// Token 存 sessionStorage：关闭浏览器窗口/标签页后自动失效，
// 重新打开站点必定回到登录页（不再依赖 localStorage 的“7 天免登录”残留）。
// 注意：sessionStorage 按标签页隔离，同一浏览器新开标签页不会共享登录态。
const readSessionToken = () => {
  try {
    return sessionStorage.getItem(TOKEN_KEY) || ''
  } catch (e) {
    // 隐私模式等场景下 sessionStorage 可能不可用，降级为内存态
    return ''
  }
}

// 一次性清理旧版本遗留在 localStorage 的旧 Token：
// 否则旧会话仍会让用户跳过登录页，与“每次打开都进登录页”的预期不符。
let legacyTokenPurged = false
function purgeLegacyLocalToken() {
  if (legacyTokenPurged) return
  legacyTokenPurged = true
  try {
    if (localStorage.getItem(TOKEN_KEY)) localStorage.removeItem(TOKEN_KEY)
  } catch (e) {
    // 忽略：清理失败不影响主流程
  }
}

export const getToken = () => {
  purgeLegacyLocalToken()
  return readSessionToken()
}
export const setToken = (token) => {
  purgeLegacyLocalToken()
  try {
    if (token) sessionStorage.setItem(TOKEN_KEY, token)
    else sessionStorage.removeItem(TOKEN_KEY)
  } catch (e) {
    // 存储不可用时不做持久化，认证链路仍依赖内存态与后端校验
  }
}

// 401 统一出口：只清一次 Token，并通知应用层清理状态/跳转登录。
// 由 permission.js 在应用入口注册，避免在请求层直接依赖 Pinia store。
let unauthorizedHandler = null
let handlingUnauthorized = false
export const setUnauthorizedHandler = (handler) => {
  unauthorizedHandler = typeof handler === 'function' ? handler : null
}
function triggerUnauthorized(url) {
  setToken('')
  // 并发 401 只触发一次业务清理，避免重复退出/重复跳转。
  if (handlingUnauthorized) return
  handlingUnauthorized = true
  setTimeout(() => { handlingUnauthorized = false }, 0)
  if (unauthorizedHandler) unauthorizedHandler(url)
}

function buildUrl(url, params) {
  let full = url.startsWith('http') ? url : BASE + url
  if (params && Object.keys(params).length) {
    const query = Object.entries(params)
      .filter(([, v]) => v !== undefined && v !== null && v !== '')
      .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`)
      .join('&')
    if (query) full += (full.includes('?') ? '&' : '?') + query
  }
  return full
}

async function request({ url, method = 'GET', params, data, headers = {}, responseType }) {
  const opts = { method, headers: { ...headers } }

  const token = getToken()
  if (token && opts.headers.isToken !== false) opts.headers.Authorization = 'Bearer ' + token
  delete opts.headers.isToken
  opts.headers.source = 'Web'

  if (data !== undefined && data !== null) {
    if (data instanceof FormData) {
      opts.body = data
    } else {
      opts.headers['Content-Type'] = 'application/json;charset=utf-8'
      opts.body = JSON.stringify(data)
    }
  }

  let res
  try {
    res = await fetch(buildUrl(url, params), opts)
  } catch (e) {
    const err = new Error('后端接口连接异常')
    err.raw = e
    throw err
  }

  if (!res.ok) {
    const err = new Error(`系统接口${res.status}异常`)
    err.status = res.status
    if (res.status === 401) triggerUnauthorized(url)
    throw err
  }

  if (responseType === 'blob') return res.blob()

  const payload = await res.json().catch(() => ({}))
  const code = payload.code ?? 200
  if (code === 200) return payload
  if (code === 401) {
    // 登录/验证码接口本身的 401 不应触发“会话过期”跳转。
    if (!['/login', '/captcha', '/register'].includes(url)) triggerUnauthorized(url)
    const err = new Error(payload.msg || '登录状态已过期，请重新登录')
    err.code = 401
    throw err
  }
  const err = new Error(payload.msg || '接口返回异常')
  err.code = code
  err.payload = payload
  throw err
}

export const get = (url, params, config = {}) => request({ url, method: 'GET', params, ...config })
export const post = (url, data, params, config = {}) => request({ url, method: 'POST', data, params, ...config })
export const put = (url, data, params, config = {}) => request({ url, method: 'PUT', data, params, ...config })
export const patch = (url, data, params, config = {}) => request({ url, method: 'PATCH', data, params, ...config })
export const del = (url, params, config = {}) => request({ url, method: 'DELETE', params, ...config })
/** 下载类接口：返回 blob，用于 /common/download、/settlement/export/{id} 等 */
export const download = (url, params, config = {}) => request({ url, method: 'GET', params, responseType: 'blob', ...config })
export default request
