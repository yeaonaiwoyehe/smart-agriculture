import router from '@/router'
import { ElMessage } from 'element-plus'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getToken } from '@/utils/auth'
import { isRelogin } from '@/utils/request'
import { useUserStore } from '@/stores/user'
import { usePermissionStore } from '@/stores/permission'
import { useSettingsStore } from '@/stores/settings'
import { encryptor, initRSAKey, enableEncrypt } from '@/utils/encrypt'

NProgress.configure({ showSpinner: false })

const whiteList = ['/login', '/bind-phone']

// 将密钥刷新逻辑移出路由守卫，创建单独的函数
const refreshPublicKey = async () => {
  const hasToken = getToken()
  const currentPath = window.location.pathname

  // 如果没有token或者在登录页面，则不刷新密钥
  if (!hasToken || currentPath === '/login' || whiteList.includes(currentPath)) {
    console.log('未登录或在登录页面，跳过密钥刷新')
    return
  }

  try {
    const { data: publicKey } = await encryptor.getPublicKey()
    console.log('公钥刷新成功:', publicKey)
    await initRSAKey(publicKey)
    enableEncrypt()
  } catch (error) {
    console.error('密钥刷新失败:', error)
  }
}

// 添加页面刷新事件监听器
window.addEventListener('load', refreshPublicKey)

// 注意：守卫只用两个参数（to, from）并以 return 给出导航结果，不使用 next。
// 旧写法是 async 却在内部用 .then() 调 next、没有 await，async 会提前 resolve，
// vue-router 在 next 执行前就判定 "Invalid navigation guard"，导致初始导航永久挂起。
router.beforeEach(async (to, from) => {
  NProgress.start()

  const hasToken = getToken()
  const userStore = useUserStore()
  const permissionStore = usePermissionStore()
  const settingsStore = useSettingsStore()

  // 判断是否是绑定手机号页面，并且携带了必要参数
  const isBindPhonePage = to.path === '/bind-phone' && to.query.userDetails

  if (hasToken) {
    to.meta.title && settingsStore.setTitle(to.meta.title)
    /* has token */
    if (to.path === '/login') {
      NProgress.done()
      return { path: '/' }
    }
    if (whiteList.indexOf(to.path) !== -1) {
      return true
    }
    if (userStore.roles.length !== 0) {
      return true
    }
    // 首次进入或刷新后角色为空：拉取用户信息并生成动态路由
    try {
      isRelogin.show = true
      await userStore.getInfo()
      isRelogin.show = false
      const accessRoutes = await permissionStore.generateRoutes()
      // 根据 roles 权限生成可访问的路由表，外链已在 generateRoutes 内由 internalRoutes 过滤
      accessRoutes.forEach(route => {
        router.addRoute(route)
      })
      // 确保动态路由添加完成后再进入目标地址
      return { ...to, replace: true }
    } catch (err) {
      try {
        await userStore.logout()
      } catch (logoutErr) {
        // 即使登出失败也继续跳转登录页
      }
      ElMessage.error(typeof err === 'string' ? err : (err && err.message) || String(err))
      NProgress.done()
      return { path: '/' }
    }
  }

  // 没有 token
  if (whiteList.indexOf(to.path) !== -1 || isBindPhonePage) {
    // 在免登录白名单，直接进入
    return true
  }
  NProgress.done()
  return `/login?redirect=${encodeURIComponent(to.fullPath)}` // 否则全部重定向到登录页
})

router.afterEach((to) => {
  NProgress.done()
  const settingsStore = useSettingsStore()
  if (settingsStore.dynamicTitle && to.meta && to.meta.title) {
    document.title = `${to.meta.title} - ${import.meta.env.VITE_APP_TITLE}`
  } else {
    document.title = import.meta.env.VITE_APP_TITLE
  }
})
