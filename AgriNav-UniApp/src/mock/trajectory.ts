import type { Plot } from '@/types'

export interface TrackPoint {
  latitude: number
  longitude: number
  timestamp: number
}

function pointInPolygon(lat: number, lng: number, polygon: Plot['boundary']): boolean {
  let inside = false
  for (let i = 0, j = polygon.length - 1; i < polygon.length; j = i++) {
    const yi = polygon[i].latitude
    const xi = polygon[i].longitude
    const yj = polygon[j].latitude
    const xj = polygon[j].longitude
    const intersect =
      yi > lat !== yj > lat && lng < ((xj - xi) * (lat - yi)) / (yj - yi || 1e-12) + xi
    if (intersect) inside = !inside
  }
  return inside
}

/**
 * 生成演示用蛇形作业轨迹（仅用于地图展示，不代表真实定位）。
 * 业务面积始终由 Mock/后端计算，不由轨迹反推。
 */
export function buildDemoTrajectory(plot: Plot, passes = 14, startTs = Date.now()): TrackPoint[] {
  const lats = plot.boundary.map((p) => p.latitude)
  const lngs = plot.boundary.map((p) => p.longitude)
  const minLat = Math.min(...lats)
  const maxLat = Math.max(...lats)
  const minLng = Math.min(...lngs)
  const maxLng = Math.max(...lngs)
  const result: TrackPoint[] = []
  const step = (maxLng - minLng) / 70
  let ts = startTs

  for (let i = 0; i < passes; i += 1) {
    const lat = minLat + ((i + 0.5) * (maxLat - minLat)) / passes
    const rowPoints: TrackPoint[] = []
    for (let lng = minLng; lng <= maxLng; lng += step) {
      if (pointInPolygon(lat, lng, plot.boundary)) {
        rowPoints.push({ latitude: lat, longitude: lng, timestamp: ts })
        ts += 15000
      }
    }
    if (i % 2 === 1) rowPoints.reverse()
    result.push(...rowPoints)
  }
  return result
}

/** 两点间距离（米） */
export function distanceMeters(a: TrackPoint, b: TrackPoint): number {
  const R = 6378137
  const dLat = ((b.latitude - a.latitude) * Math.PI) / 180
  const dLng = ((b.longitude - a.longitude) * Math.PI) / 180
  const lat1 = (a.latitude * Math.PI) / 180
  const lat2 = (b.latitude * Math.PI) / 180
  const h =
    Math.sin(dLat / 2) ** 2 + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) ** 2
  return 2 * R * Math.asin(Math.min(1, Math.sqrt(h)))
}

/** 轨迹总长度（米） */
export function trackLength(points: TrackPoint[]): number {
  let total = 0
  for (let i = 1; i < points.length; i += 1) {
    total += distanceMeters(points[i - 1], points[i])
  }
  return total
}

/** 按时间取轨迹中已完成的部分（用于地图上的实时位置） */
export function pointsBefore(points: TrackPoint[], ts: number): TrackPoint[] {
  if (!points.length) return []
  const idx = points.findIndex((p) => p.timestamp > ts)
  if (idx < 0) return points.slice()
  return points.slice(0, Math.max(1, idx))
}
