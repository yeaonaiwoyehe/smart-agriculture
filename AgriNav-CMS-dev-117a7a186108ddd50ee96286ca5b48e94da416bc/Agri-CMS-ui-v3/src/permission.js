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

router.beforeEach(async (to, from, next) => {
  NProgress.start()

  const hasToken = getToken()
  const userStore = useUserStore()
  const permissionStore = usePermissionStore()
  const settingsStore = useSettingsStore()

  // 判断是否是绑定手机号页面，并且携带了必要参数
  const isBindPhonePage = to.path === '/bind-phone' && to.query.userDetails

  if (hasToken) {
    to.meta.title && settingsStore.setTitle(to.meta.title)
    /* has token*/
    if (to.path === '/login') {
      next({ path: '/' })
      NProgress.done()
    } else if (whiteList.indexOf(to.path) !== -1) {
      next()
    } else {
      if (userStore.roles.length === 0) {
        isRelogin.show = true
        // 判断当前用户是否已拉取完user_info信息
        userStore.getInfo().then(() => {
          isRelogin.show = false
          permissionStore.generateRoutes().then(accessRoutes => {
            // 根据roles权限生成可访问的路由表
            accessRoutes.forEach(route => {
              router.addRoute(route)
            })
            next({ ...to, replace: true }) // hack方法 确保路由已添加完成
          })
        }).catch(err => {
          userStore.logout().then(() => {
            ElMessage.error(err)
            next({ path: '/' })
          })
        })
      } else {
        next()
      }
    }
  } else {
    // 没有token
    if (whiteList.indexOf(to.path) !== -1 || isBindPhonePage) {
      // 在免登录白名单，直接进入
      next()
    } else {
      next(`/login?redirect=${encodeURIComponent(to.fullPath)}`) // 否则全部重定向到登录页
      NProgress.done()
    }
  }
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
