// 仅处理显示数据，不修改 WKT、数据库面积或原始坐标。
export function parsePolygonWkt(wkt) {
  const match = String(wkt || '').trim().match(/^(?:SRID\s*=\s*4326\s*;\s*)?POLYGON(?:\s+Z)?\s*\(\s*\(([\s\S]*)\)\s*\)$/i)
  if (!match) throw new Error('地块边界格式无效，暂时无法显示地图。')
  return match[1].split(/\)\s*,\s*\(/).map(ring => {
    const points = ring.split(',').map(position => {
      const values = position.trim().split(/\s+/).map(Number)
      const [lon, lat] = values
      if ((values.length !== 2 && values.length !== 3) || !values.every(Number.isFinite) ||
          lon < -180 || lon > 180 || lat < -90 || lat > 90) throw new Error('边界含有无效经纬度。')
      return [lon, lat]
    })
    if (points.length < 4 || points[0][0] !== points[points.length - 1][0] || points[0][1] !== points[points.length - 1][1]) {
      throw new Error('地块边界未闭合，暂时无法显示地图。')
    }
    return points
  })
}

export function outerPerimeterMetres(ring) {
  let distance = 0
  const radians = value => value * Math.PI / 180
  for (let i = 1; i < ring.length; i++) {
    const [lon1, lat1] = ring[i - 1].map(radians)
    const [lon2, lat2] = ring[i].map(radians)
    const a = Math.sin((lat2 - lat1) / 2) ** 2 + Math.cos(lat1) * Math.cos(lat2) * Math.sin((lon2 - lon1) / 2) ** 2
    distance += 2 * 6371000 * Math.asin(Math.sqrt(Math.max(0, Math.min(1, a))))
  }
  return distance
}

// 高德官方 convertFrom 接口：https://lbs.amap.com/api/javascript-api-v2/guide/transform/convertfrom
// 每次批量转换，不把整块地用同一个平移量代替，也不在失败时回退为原始坐标。
export async function convertWgs84Paths(AMap, paths, options = {}) {
  const { timeoutMs = 12000, batchSize = 40 } = options
  if (!Number.isInteger(batchSize) || batchSize < 1 || timeoutMs <= 0) throw new Error('坐标转换参数无效。')
  const keys = new Map()
  paths.forEach(path => path.forEach(point => {
    const [lon, lat] = point
    if (!Number.isFinite(lon) || !Number.isFinite(lat) || lon < -180 || lon > 180 || lat < -90 || lat > 90) {
      throw new Error('边界含有无效经纬度。')
    }
    keys.set(JSON.stringify(point), [lon, lat])
  }))
  const entries = [...keys.entries()]
  const converted = new Map()
  for (let offset = 0; offset < entries.length; offset += batchSize) {
    const batch = entries.slice(offset, offset + batchSize)
    const locations = await new Promise((resolve, reject) => {
      const timer = setTimeout(() => reject(new Error('坐标转换超时，请重试。')), timeoutMs)
      try {
        AMap.convertFrom(batch.map(([, point]) => point.slice()), 'gps', (status, result) => {
          clearTimeout(timer)
          if (status !== 'complete' || !result || !Array.isArray(result.locations) || result.locations.length !== batch.length) {
            reject(new Error('坐标转换失败，未绘制边界。请重试。'))
          } else resolve(result.locations)
        })
      } catch (error) { clearTimeout(timer); reject(new Error('坐标转换失败，请重试。')) }
    })
    locations.forEach((point, index) => {
      const lon = point.getLng ? point.getLng() : point[0]
      const lat = point.getLat ? point.getLat() : point[1]
      if (!Number.isFinite(lon) || !Number.isFinite(lat) || lon < -180 || lon > 180 || lat < -90 || lat > 90) {
        throw new Error('坐标转换返回无效位置，未绘制边界。')
      }
      converted.set(batch[index][0], [lon, lat])
    })
  }
  return paths.map(path => path.map(point => converted.get(JSON.stringify(point)).slice()))
}