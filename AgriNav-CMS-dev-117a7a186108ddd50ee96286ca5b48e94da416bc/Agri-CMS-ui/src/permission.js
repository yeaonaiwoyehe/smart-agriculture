import router from './router'
import store from './store'
import { Message } from 'element-ui'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getToken } from '@/utils/auth'
import { isRelogin } from '@/utils/request'
import {encryptor, Encryptor} from '@/utils/encrypt'
import { initRSAKey, enableEncrypt } from '@/utils/encrypt'

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

  // 判断是否是绑定手机号页面，并且携带了必要参数
  const isBindPhonePage = to.path === '/bind-phone' && to.query.userDetails

  if (hasToken) {
    to.meta.title && store.dispatch('settings/setTitle', to.meta.title)
    /* has token*/
    if (to.path === '/login') {
      next({ path: '/' })
      NProgress.done()
    } else if (whiteList.indexOf(to.path) !== -1) {
      next()
    } else {
      if (store.getters.roles.length === 0) {
        isRelogin.show = true
        // 判断当前用户是否已拉取完user_info信息
        store.dispatch('GetInfo').then(() => {
          isRelogin.show = false
          store.dispatch('GenerateRoutes').then(accessRoutes => {
            // 根据roles权限生成可访问的路由表
            router.addRoutes(accessRoutes) // 动态添加可访问路由表
            next({ ...to, replace: true }) // hack方法 确保addRoutes已完成
          })
        }).catch(err => {
          store.dispatch('LogOut').then(() => {
            Message.error(err)
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

router.afterEach(() => {
  NProgress.done()
})
