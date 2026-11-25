// ============================================================
// 高德地图统一加载模块
// key 与安全密钥统一从环境变量读取（.env.* 中的 VUE_APP_AMAP_*）
// 各页面不再硬编码 key，统一调用 loadAMap(plugins)
// ============================================================
import AMapLoader from '@amap/amap-jsapi-loader'

// 高德 Web 端（JS API）Key
export const AMAP_KEY = import.meta.env.VITE_APP_AMAP_KEY || ''
// 配套安全密钥（2021-12-02 之后申请的 key 必须配置）
export const AMAP_SECURITY_CODE = import.meta.env.VITE_APP_AMAP_SECURITY_CODE || ''

/**
 * 加载高德 JS API 并返回 AMap 命名空间
 * @param {string[]} plugins 需要的插件，如 ['AMap.Polygon', 'AMap.Marker']
 * @returns {Promise<object>} AMap
 */
export function loadAMap(plugins = []) {
  // 安全密钥必须在 AMapLoader.load 之前配置
  window._AMapSecurityConfig = {
    securityJsCode: AMAP_SECURITY_CODE
  }
  return AMapLoader.load({
    key: AMAP_KEY,
    version: '2.0',
    plugins
  })
}

export default { loadAMap, AMAP_KEY, AMAP_SECURITY_CODE }
