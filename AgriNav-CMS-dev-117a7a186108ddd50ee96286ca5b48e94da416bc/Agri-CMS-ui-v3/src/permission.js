import router from '@/router'
import { ElMessage } from 'element-plus'
import { getToken } from '@/utils/auth'
import { setUnauthorizedHandler } from '@/utils/request'
import { useUserStore } from '@/stores/user'
import { usePermissionStore } from '@/stores/permission'
import { useAppStore } from '@/stores/app'

const whiteList = ['/login', '/bind-phone']
let initPromise = null
// 会话代数：每次清理会话后自增，用于丢弃“清理过程中仍在飞行”的旧初始化结果。
let initGeneration = 0

// 统一错误文案：request.js 的业务异常把后端 msg 放在 err.payload.msg，
// 401/网络异常只有 err.message，两种情况都要能取到。
export function authErrorMessage(err, fallback) {
  return err?.payload?.msg || err?.message || fallback
}

/**
 * 登录提交与刷新恢复共用的唯一初始化入口。
 * 顺序固定：getInfo 验证 Token → getRouters 注册动态路由 → 全部成功后才标记完整认证。
 * 任一步失败都会抛出，由调用方清理会话；不在这里吞异常、也不写成功状态。
 * @param {object} options force=true 时忽略已加载标志，强制重新验证（用于新 Token 登录）。
 */
export function initializeAuthState({ force = false } = {}) {
  const userStore = useUserStore()
  const permissionStore = usePermissionStore()
  const appStore = useAppStore()

  console.debug('[AUTH-TRACE] initializeAuthState: ENTER', 'force=', force, 'hasToken=', !!getToken(), 'userLoaded=', userStore.loaded, 'permLoaded=', permissionStore.loaded, 'loggedIn=', appStore.loggedIn, 'initPromiseActive=', !!initPromise)

  if (!getToken()) {
    console.debug('[AUTH-TRACE] initializeAuthState: ABORT no token')
    return Promise.reject(new Error('未获取到登录凭证'))
  }

  // Token 存在不等于认证成功：三段状态（用户信息、权限路由、认证标志）都就绪才短路。
  if (!force && userStore.loaded && permissionStore.loaded && appStore.loggedIn) {
    console.debug('[AUTH-TRACE] initializeAuthState: SHORT-CIRCUIT all loaded')
    return Promise.resolve()
  }

  if (!initPromise) {
    console.debug('[AUTH-TRACE] initializeAuthState: create new initPromise')
    const generation = initGeneration
    const verifyNotCleared = () => {
      if (generation !== initGeneration) throw new Error('登录状态已变更，请重新登录')
    }

    initPromise = (async () => {
      // 1) getInfo 验证 Token；失败时 userStore.getInfo 不会置 loaded=true。
      console.debug('[AUTH-TRACE] init step1: BEFORE getInfo', 'force=', force, 'userLoaded=', userStore.loaded)
      if (force || !userStore.loaded) {
        await userStore.getInfo()
        console.debug('[AUTH-TRACE] init step1: AFTER getInfo', 'userLoaded=', userStore.loaded)
        verifyNotCleared()
      } else {
        console.debug('[AUTH-TRACE] init step1: SKIP getInfo (already loaded)')
      }
      // 2) getRouters 成功并完成路由注册后，permissionStore 才会置 loaded=true。
      console.debug('[AUTH-TRACE] init step2: BEFORE generateRoutes', 'force=', force, 'permLoaded=', permissionStore.loaded)
      if (force || !permissionStore.loaded) {
        await permissionStore.generateRoutes()
        console.debug('[AUTH-TRACE] init step2: AFTER generateRoutes', 'permLoaded=', permissionStore.loaded)
        verifyNotCleared()
      } else {
        console.debug('[AUTH-TRACE] init step2: SKIP generateRoutes (already loaded)')
      }
      // 3) 以上都成功，才落地展示信息并标记完整认证成功。
      appStore.applyUserInfo(userStore.user, userStore.roles, userStore.departmentNames)
      appStore.markAuthenticated()
      console.debug('[AUTH-TRACE] init step3: DONE markAuthenticated', 'loggedIn=', appStore.loggedIn)
    })().finally(() => {
      initPromise = null
      console.debug('[AUTH-TRACE] init: FINALLY initPromise cleared')
    })
  } else {
    console.debug('[AUTH-TRACE] initializeAuthState: REUSE existing initPromise (skip new run)')
  }
  return initPromise
}

async function clearSession(message, doRedirect = true) {
  console.debug('[AUTH-TRACE] clearSession: ENTER', 'doRedirect=', doRedirect, 'hasMessage=', !!message, 'path=', router.currentRoute.value.path)
  const userStore = useUserStore()
  const permissionStore = usePermissionStore()
  const appStore = useAppStore()

  // Token 只在 request.js 的 setToken('') 一处清理；这里清理 Pinia 与动态路由。
  // 代数自增使仍在飞行的 initializeAuthState 结果作废，避免清理后又被标记认证。
  initGeneration += 1
  await userStore.fedLogout()
  permissionStore.resetRoutes()
  appStore.resetAuth()

  if (message && !router.currentRoute.value.path.startsWith('/login')) {
    ElMessage.warning(message)
  }
  if (doRedirect && !router.currentRoute.value.path.startsWith('/login')) {
    const fullPath = router.currentRoute.value.fullPath
    router.replace({ path: '/login', query: fullPath && fullPath !== '/' ? { redirect: fullPath } : {} })
  }
}

// request.js 收到 401 时只回调一次，这里统一清理状态并跳转登录。
setUnauthorizedHandler(() => {
  console.debug('[AUTH-TRACE] clearSession SOURCE: 401 unauthorized handler')
  clearSession('登录状态已过期，请重新登录')
})

router.beforeEach(async (to, _from, next) => {
  const token = getToken()
  console.debug('[AUTH-TRACE] beforeEach: ENTER', 'to=', to.path, 'hasToken=', !!token)

  if (to.path === '/login') {
    console.debug('[AUTH-TRACE] beforeEach branch: /login', 'hasToken=', !!token)
    if (!token) {
      console.debug('[AUTH-TRACE] beforeEach: /login no token -> next()')
      next()
      return
    }
    try {
      console.debug('[AUTH-TRACE] beforeEach(/login): BEFORE initializeAuthState')
      await initializeAuthState()
      console.debug('[AUTH-TRACE] beforeEach(/login): AFTER initializeAuthState')
      // P11：优先回到用户原本要访问的目标路由，避免一律回到首页。
      const target = typeof to.query.redirect === 'string' && to.query.redirect.startsWith('/') && !to.query.redirect.startsWith('//')
        ? to.query.redirect
        : '/dashboard'
      console.debug('[AUTH-TRACE] beforeEach(/login): redirect', 'hasRedirectParam=', typeof to.query.redirect === 'string', 'targetIsDashboard=', target === '/dashboard')
      next({ path: target, replace: true })
    } catch (err) {
      console.debug('[AUTH-TRACE] beforeEach(/login): CATCH -> clearSession')
      console.error('[permission] 已登录态恢复失败', err)
      await clearSession(authErrorMessage(err, '登录状态已失效，请重新登录'), false)
      next()
    }
    return
  }

  if (whiteList.includes(to.path)) {
    console.debug('[AUTH-TRACE] beforeEach branch: whitelist -> next()', 'to=', to.path)
    next()
    return
  }

  if (!token) {
    console.debug('[AUTH-TRACE] beforeEach branch: no token -> /login', 'to=', to.path)
    next({ path: '/login', query: { redirect: to.fullPath }, replace: true })
    return
  }

  console.debug('[AUTH-TRACE] beforeEach: guarded route with token -> initializeAuthState', 'to=', to.path)
  try {
    console.debug('[AUTH-TRACE] beforeEach(guarded): BEFORE initializeAuthState')
    await initializeAuthState()
    console.debug('[AUTH-TRACE] beforeEach(guarded): AFTER initializeAuthState')
    const appStore = useAppStore()
    const key = to.meta?.menuKey
    // permissionStore.loaded=false（getRouters 失败）时 canAccess 返回 false，
    // 非白名单业务页一律不放行，防止在无权限数据下进入业务页面。
    console.debug('[AUTH-TRACE] beforeEach(guarded): canAccess check', 'hasMenuKey=', !!key)
    if (key && !appStore.canAccess(key)) {
      console.debug('[AUTH-TRACE] beforeEach(guarded): canAccess FALSE -> /dashboard')
      next({ path: '/dashboard', replace: true })
      return
    }
    console.debug('[AUTH-TRACE] beforeEach(guarded): PASS -> next()')
    next()
  } catch (err) {
    console.debug('[AUTH-TRACE] beforeEach(guarded): CATCH -> clearSession')
    console.error('[permission] 登录态初始化失败', err)
    await clearSession(authErrorMessage(err, '登录状态失效，请重新登录'), false)
    next({ path: '/login', query: { redirect: to.fullPath }, replace: true })
  }
})
