import { defineStore } from 'pinia'
import { login as loginApi, getInfo as getInfoApi, logout as logoutApi } from '@/api/login'
import { getToken, setToken } from '@/utils/auth'
import defaultAvatar from '@/assets/images/profile.jpg'

// 并发退出只发一次 /logout，但所有调用方都等待同一个 Promise。
let logoutPromise = null

// 只保存前端业务真正需要的用户字段，避免把后端返回对象（可能含 password 等敏感字段）
// 整体写入 Pinia。页面已使用的字段：userId / userName / nickName / avatar / deptId。
const SAFE_USER_KEYS = ['userId', 'userName', 'nickName', 'avatar', 'deptId']
function pickSafeUser(raw) {
  const src = raw && typeof raw === 'object' ? raw : {}
  const user = {}
  SAFE_USER_KEYS.forEach((key) => {
    if (src[key] !== undefined && src[key] !== null) user[key] = src[key]
  })
  return user
}

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken() || '',
    user: null,
    userId: null,
    name: '',
    avatar: '',
    roles: [],
    permissions: [],
    departmentNames: [],
    loaded: false
  }),
  actions: {
    // 真实登录：POST /login?phoneNumber=&captcha=，Token 在响应顶层 token。
    async login(phoneNumber, captcha) {
      console.debug('[AUTH-TRACE] userStore.login: ENTER')
      console.debug('[AUTH-TRACE] userStore.login: BEFORE loginApi')
      const res = await loginApi(phoneNumber, captcha)
      console.debug('[AUTH-TRACE] userStore.login: AFTER loginApi', 'hasRes=', !!res, 'hasToken=', !!res?.token)
      const token = res?.token
      if (!token) {
        console.debug('[AUTH-TRACE] userStore.login: NO TOKEN in response -> throw')
        const err = new Error('登录响应未返回 token')
        err.payload = res
        throw err
      }
      setToken(token)
      this.token = token
      console.debug('[AUTH-TRACE] userStore.login: AFTER setToken', 'tokenStored=', !!this.token)
      return res
    },

    // 真实用户信息：GET /getInfo，字段为 user / roles / permissions / departmentNames。
    async getInfo() {
      console.debug('[AUTH-TRACE] userStore.getInfo: ENTER')
      console.debug('[AUTH-TRACE] userStore.getInfo: BEFORE getInfoApi')
      const res = await getInfoApi()
      console.debug('[AUTH-TRACE] userStore.getInfo: AFTER getInfoApi', 'hasRes=', !!res, 'hasUser=', !!res?.user, 'rolesIsArray=', Array.isArray(res?.roles))
      const user = pickSafeUser(res?.user)
      this.user = user
      this.userId = user.userId ?? null
      this.name = user.userName || user.nickName || ''
      this.avatar = user.avatar || defaultAvatar
      this.roles = Array.isArray(res?.roles) ? res.roles : []
      this.permissions = Array.isArray(res?.permissions) ? res.permissions : []
      this.departmentNames = Array.isArray(res?.departmentNames) ? res.departmentNames : []
      this.loaded = true
      console.debug('[AUTH-TRACE] userStore.getInfo: DONE', 'loaded=', this.loaded, 'rolesLen=', this.roles.length)
      return res
    },

    // 退出：后端提供 POST /logout（Spring Security filter）；无论接口成功与否都清理本地态。
    logout() {
      if (logoutPromise) return logoutPromise
      logoutPromise = (async () => {
        try {
          await logoutApi()
        } catch (err) {
          console.warn('[logout] 后端退出失败，继续清理本地登录态', err?.message)
        } finally {
          this.clearLocal()
          logoutPromise = null
        }
      })()
      return logoutPromise
    },

    clearLocal() {
      setToken('')
      this.token = ''
      this.user = null
      this.userId = null
      this.name = ''
      this.avatar = ''
      this.roles = []
      this.permissions = []
      this.departmentNames = []
      this.loaded = false
    },

    // 纯前端清理：401 / 权限初始化失败时使用，不调用后端接口。
    fedLogout() {
      this.clearLocal()
      return Promise.resolve()
    }
  }
})