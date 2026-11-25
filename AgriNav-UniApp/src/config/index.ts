export const APP_TITLE = import.meta.env.VITE_APP_TITLE || '农机作业助手'

/** 后端基础地址（末尾不带 /）；未配置 .env 时回落到现有开发后端 */
export const API_BASE_URL = (
  import.meta.env.VITE_API_BASE_URL || 'http://10.218.37.215:8088'
).replace(/\/$/, '')

const MOCK_FLAG_KEY = 'app.useMock'

/** 构建时的默认 Mock 开关 */
export const DEFAULT_MOCK_ENABLED = String(import.meta.env.VITE_USE_MOCK) !== 'false'

/**
 * 运行期是否使用演示数据。
 * 优先级：用户在「设置」中的选择 > 构建环境变量
 */
export function isMockEnabled(): boolean {
  const saved = uni.getStorageSync(MOCK_FLAG_KEY)
  if (saved === '' || saved === null || saved === undefined) {
    return DEFAULT_MOCK_ENABLED
  }
  return saved === true || saved === 'true'
}

export function setMockEnabled(enabled: boolean): void {
  uni.setStorageSync(MOCK_FLAG_KEY, enabled ? 'true' : 'false')
}

/** 演示环境标识，用于界面角标 */
export const DEMO_HINT = '演示数据'
