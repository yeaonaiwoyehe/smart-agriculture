const TOKEN_KEY = 'app.token'
const RSA_KEY = 'app.rsaPublicKey'
const USER_KEY = 'app.userInfo'
const READY_KEY = 'app.readyAt'

export function getToken(): string {
  return (uni.getStorageSync(TOKEN_KEY) as string) || ''
}

export function setToken(token: string): void {
  uni.setStorageSync(TOKEN_KEY, token)
}

export function getRsaPublicKey(): string {
  return (uni.getStorageSync(RSA_KEY) as string) || ''
}

export function setRsaPublicKey(key: string): void {
  uni.setStorageSync(RSA_KEY, key)
}

export function getCachedUser<T = any>(): T | null {
  const raw = uni.getStorageSync(USER_KEY)
  if (!raw) return null
  try {
    return typeof raw === 'string' ? (JSON.parse(raw) as T) : (raw as T)
  } catch (e) {
    return null
  }
}

export function setCachedUser(user: unknown): void {
  uni.setStorageSync(USER_KEY, JSON.stringify(user))
}

export function setReadyAt(ts: number): void {
  uni.setStorageSync(READY_KEY, ts)
}

export function getReadyAt(): number {
  return Number(uni.getStorageSync(READY_KEY) || 0)
}

export function clearAuth(): void {
  uni.removeStorageSync(TOKEN_KEY)
  uni.removeStorageSync(RSA_KEY)
  uni.removeStorageSync(USER_KEY)
  uni.removeStorageSync(READY_KEY)
}
