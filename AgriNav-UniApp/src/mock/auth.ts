import type { LoginResult, UserProfile } from '@/types'
import { MockError, delay, getDb, maybeFail, saveDb, updateUser } from './db'

const CAPTCHA_KEY = 'mock.captcha'
const PHONE_RE = /^1[3-9]\d{9}$/

interface StoredCaptcha {
  phoneNumber: string
  code: string
  expireAt: number
}

/** 演示环境固定验证码 */
export const MOCK_CAPTCHA_CODE = '123456'

export async function sendCaptcha(phoneNumber: string): Promise<{ mockCode: string; expireIn: number }> {
  await delay(180, 420)
  if (!PHONE_RE.test(phoneNumber)) {
    throw new MockError('请输入正确的手机号', 400)
  }
  maybeFail('sendCaptcha')
  const stored: StoredCaptcha = {
    phoneNumber,
    code: MOCK_CAPTCHA_CODE,
    expireAt: Date.now() + 3 * 60 * 1000
  }
  uni.setStorageSync(CAPTCHA_KEY, JSON.stringify(stored))
  return { mockCode: MOCK_CAPTCHA_CODE, expireIn: 180 }
}

export async function login(phoneNumber: string, captcha?: string): Promise<LoginResult> {
  await delay(320, 620)
  if (!PHONE_RE.test(phoneNumber)) {
    throw new MockError('手机号格式不正确', 400)
  }
  maybeFail('login')

  const raw = uni.getStorageSync(CAPTCHA_KEY)
  if (captcha) {
    let stored: StoredCaptcha | null = null
    try {
      stored = raw ? (JSON.parse(raw as string) as StoredCaptcha) : null
    } catch (e) {
      stored = null
    }
    if (stored) {
      if (stored.phoneNumber !== phoneNumber) {
        throw new MockError('验证码与手机号不匹配', 400)
      }
      if (Date.now() > stored.expireAt) {
        throw new MockError('验证码已过期，请重新获取', 400)
      }
      if (captcha !== stored.code) {
        throw new MockError('验证码错误', 400)
      }
    } else if (captcha !== MOCK_CAPTCHA_CODE) {
      throw new MockError('验证码错误', 400)
    }
  }

  const db = getDb()
  db.user.phoneNumber = phoneNumber
  db.user.userName = phoneNumber
  if (!db.user.nickName || db.user.phoneNumber !== phoneNumber) {
    db.user.nickName = `机手${phoneNumber.slice(-4)}`
  }
  saveDb(db)
  uni.removeStorageSync(CAPTCHA_KEY)

  return {
    token: `mock-token-${phoneNumber}-${Date.now()}`,
    rsa_public_key: 'MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmockPublicKeyForDemoOnly'
  }
}

export async function getInfo(): Promise<UserProfile> {
  await delay(150, 320)
  maybeFail('getInfo')
  return getDb().user
}

export async function updateProfile(patch: Partial<UserProfile>): Promise<UserProfile> {
  await delay(200, 400)
  return updateUser(patch)
}

export async function logout(): Promise<void> {
  await delay(120, 220)
  uni.removeStorageSync(CAPTCHA_KEY)
}
