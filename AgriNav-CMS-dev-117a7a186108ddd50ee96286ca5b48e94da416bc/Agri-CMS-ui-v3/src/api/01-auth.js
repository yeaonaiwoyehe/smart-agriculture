/**
 * 01-认证与登录（14 个接口）
 * 依据 AgriNav接口清单.md 自动生成，方法与路径逐条对应。
 */
import { get, post, put, del, patch } from '@/utils/request'

export const getCaptcha = (...args) => get('/captcha', ...args)
export const getGetInfo = (...args) => get('/getInfo', ...args)
export const getGetRouters = (...args) => get('/getRouters', ...args)
export const postLogin = (...args) => post('/login', ...args)
export const getLoginOAuth = (...args) => get('/loginOAuth', ...args)
export const postLoginQR = (...args) => post('/loginQR', ...args)
export const getLoginWechatMobile = (...args) => get('/loginWechatMobile', ...args)
export const getLoginWithoutSecret = (...args) => get('/loginWithoutSecret', ...args)
export const removeRender = (...args) => del('/render', ...args)
export const getRender = (...args) => get('/render', ...args)
export const patchRender = (...args) => patch('/render', ...args)
export const postRender = (...args) => post('/render', ...args)
export const putRender = (...args) => put('/render', ...args)
export const removeSignOutByUsername = (username, ...rest) => del(`/signOut/${encodeURIComponent(username)}`, ...rest)

export const authApi = {
  getCaptcha,
  getGetInfo,
  getGetRouters,
  postLogin,
  getLoginOAuth,
  postLoginQR,
  getLoginWechatMobile,
  getLoginWithoutSecret,
  removeRender,
  getRender,
  patchRender,
  postRender,
  putRender,
  removeSignOutByUsername,
}
export default authApi
