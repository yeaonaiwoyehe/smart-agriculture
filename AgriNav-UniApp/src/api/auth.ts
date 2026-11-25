import type { AjaxResult, LoginResult, UserProfile } from '@/types'
import { isMockEnabled } from '@/config'
import request from './request'
import * as mockAuth from '@/mock/auth'

export interface CaptchaResult {
  mockCode?: string
  expireIn: number
}

/** 发送短信验证码：GET /captcha?phoneNumber= */
export async function sendCaptcha(phoneNumber: string): Promise<CaptchaResult> {
  if (isMockEnabled()) {
    const res = await mockAuth.sendCaptcha(phoneNumber)
    return { mockCode: res.mockCode, expireIn: res.expireIn }
  }
  const res = await request<AjaxResult>({
    url: '/captcha',
    method: 'GET',
    query: { phoneNumber },
    auth: false
  })
  const mockCode = (res.mockCode || (res.data as any)?.mockCode) as string | undefined
  return { mockCode, expireIn: 180 }
}

/**
 * 手机号登录（登录即注册）。
 * 后端使用 @RequestParam，必须用 query / 表单，不能 JSON。
 */
export async function login(phoneNumber: string, captcha?: string): Promise<LoginResult> {
  if (isMockEnabled()) {
    return mockAuth.login(phoneNumber, captcha)
  }
  const res = await request<AjaxResult & LoginResult>({
    url: '/login',
    method: 'POST',
    query: captcha ? { phoneNumber, captcha } : { phoneNumber },
    auth: false
  })
  if (!res.token) {
    throw new Error(res.msg || '登录失败：后端未返回 token')
  }
  return { token: res.token, rsa_public_key: res.rsa_public_key }
}

/** 用户信息：GET /getInfo */
export async function getInfo(): Promise<UserProfile> {
  if (isMockEnabled()) {
    return mockAuth.getInfo()
  }
  const res = await request<AjaxResult>({ url: '/getInfo', method: 'GET' })
  const user = (res.user || res.data?.user || res.data) as any
  if (!user) throw new Error(res.msg || '获取用户信息失败')
  return {
    userId: user.userId,
    userName: user.userName,
    nickName: user.nickName || user.userName || '机手',
    phoneNumber: user.phonenumber || user.phoneNumber || '',
    avatar: user.avatar,
    deptName: user.dept?.deptName || user.deptName,
    roles: res.roles || user.roles || [],
    permissions: res.permissions || user.permissions || []
  }
}

export async function updateProfile(patch: Partial<UserProfile>): Promise<UserProfile> {
  if (isMockEnabled()) {
    return mockAuth.updateProfile(patch)
  }
  await request<AjaxResult>({
    url: '/system/user/profile',
    method: 'PUT',
    data: {
      nickName: patch.nickName,
      phonenumber: patch.phoneNumber,
      email: (patch as any).email
    }
  })
  return getInfo()
}

export async function logout(): Promise<void> {
  if (isMockEnabled()) {
    await mockAuth.logout()
  }
}

/** 换绑手机号：PUT /system/user/profile/phoneNumber */
export async function changePhone(phoneNumber: string, captcha: string): Promise<void> {
  if (isMockEnabled()) {
    const res = await mockAuth.updateProfile({ phoneNumber, userName: phoneNumber })
    if (!res) throw new Error('换绑失败')
    return
  }
  await request<AjaxResult>({
    url: '/system/user/profile/phoneNumber',
    method: 'PUT',
    query: { phoneNumber, captcha }
  })
}

/** 修改密码：PUT /system/user/profile/updatePwd */
export async function changePassword(oldPassword: string, newPassword: string): Promise<void> {
  if (isMockEnabled()) {
    await mockAuth.logout()
    return
  }
  await request<AjaxResult>({
    url: '/system/user/profile/updatePwd',
    method: 'PUT',
    query: { oldPassword, newPassword }
  })
}
