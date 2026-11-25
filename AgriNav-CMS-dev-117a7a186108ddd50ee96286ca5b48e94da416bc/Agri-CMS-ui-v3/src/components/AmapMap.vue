<template>
  <div class="amap-wrap" :style="{ height: height + 'px' }">
    <div ref="mapEl" class="amap-canvas" />
    <div v-if="live" class="amap-live"><span class="dot" />实时数据 · {{ live }}</div>
    <div class="amap-toolbar">
      <button title="放大" @click="zoom(1)">＋</button>
      <button title="缩小" @click="zoom(-1)">－</button>
      <button title="复位" @click="fitAll">复位</button>
      <button title="卫星图" @click="toggleSatellite">卫</button>
      <button title="路网图" @click="toggleRoadNet">路</button>
    </div>
    <div v-if="error" class="amap-error">高德地图加载失败：{{ error }}</div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { loadAMap } from '@/utils/amap'

const props = defineProps({
  lands: { type: Array, default: () => [] },
  machines: { type: Array, default: () => [] },
  heat: { type: Array, default: () => [] },
  tracks: { type: Array, default: () => [] },
  activeTrack: { type: Array, default: () => [] },
  playIndex: { type: Number, default: -1 },
  anomalies: { type: Array, default: () => [] },
  selectedLand: { type: String, default: '' },
  height: { type: Number, default: 420 },
  showLabels: { type: Boolean, default: true },
  showLegend: { type: Boolean, default: true },
  live: { type: String, default: '' }
})
const emit = defineEmits(['select-land', 'select-machine'])

const mapEl = ref(null)
const error = ref('')
let AMap = null
let map = null
const overlays = []
let markerMap = new Map()
let satelliteLayer = null
let roadNetLayer = null

const clearOverlays = () => {
  overlays.splice(0).forEach((o) => { try { map?.remove(o) } catch (e) {} })
  markerMap.clear()
}

const toLngLat = (p) => {
  if (!p) return null
  if (Array.isArray(p)) return p.length >= 2 ? [Number(p[0]), Number(p[1])] : null
  if (p.longitude !== undefined) return [Number(p.longitude), Number(p.latitude)]
  return null
}

function draw() {
  if (!map || !AMap) return
  clearOverlays()

  const allPoints = []
  addLands(allPoints)
  addTracks(allPoints)
  addMachines(allPoints)
  addHeat(allPoints)

  if (allPoints.length) map.setFitView(null, false, [40, 40, 40, 40])
}

function addLands(allPoints) {
  props.lands.forEach((l) => {
    const pts = (l.lnglat && l.lnglat.length ? l.lnglat : l.points).map(toLngLat).filter(Boolean)
    if (!pts.length) return
    allPoints.push(...pts.map((p) => new AMap.LngLat(p[0], p[1])))
    const polygon = new AMap.Polygon({
      path: pts,
      strokeColor: props.selectedLand === String(l.id) ? '#0f5537' : '#3b8f66',
      strokeWeight: props.selectedLand === String(l.id) ? 3 : 1.5,
      fillColor: props.selectedLand === String(l.id) ? '#a8e3c1' : '#b7e6cb',
      fillOpacity: 0.45,
      cursor: 'pointer'
    })
    polygon.on('click', () => emit('select-land', l))
    map.add(polygon); overlays.push(polygon)

    if (props.showLabels) {
      const c = toLngLat(l.centerLngLat || l.center) || pts[0]
      const marker = new AMap.Marker({
        position: c,
        content: `<div style="background:#fff;border:1px solid #dfe8e2;border-radius:5px;padding:2px 6px;font-size:11px;color:#28473a;white-space:nowrap">${l.name} · ${l.area}亩</div>`,
        offset: new AMap.Pixel(-30, -12)
      })
      map.add(marker); overlays.push(marker)
    }
  })
}

function addTracks(allPoints) {
  const addLine = (pts, color, weight, opacity) => {
    const path = pts.map(toLngLat).filter(Boolean)
    if (path.length < 2) return null
    allPoints.push(...path.map((p) => new AMap.LngLat(p[0], p[1])))
    const line = new AMap.Polyline({ path, strokeColor: color, strokeWeight: weight, strokeOpacity: opacity })
    map.add(line); overlays.push(line)
    return path
  }

  props.tracks.forEach((t) => addLine(t, '#9fb6a8', 3, 0.6))
  const path = addLine(props.activeTrack, '#0f5537', 5, 0.9)
  if (path && props.playIndex >= 0 && path[props.playIndex]) {
    const marker = new AMap.Marker({
      position: path[props.playIndex],
      content: '<div style="width:14px;height:14px;background:#fff;border:3px solid #0f5537;border-radius:50%"></div>'
    })
    map.add(marker); overlays.push(marker)
  }
  props.anomalies.forEach((a) => {
    const p = path?.[a.point]
    if (!p) return
    const marker = new AMap.Marker({
      position: p,
      content: `<div style="background:#e04b4b;color:#fff;font-size:10px;padding:2px 6px;border-radius:4px">${a.type || '异常'}</div>`
    })
    map.add(marker); overlays.push(marker)
  })
}

function addMachines(allPoints) {
  props.machines.forEach((m, i) => {
    const p = toLngLat(m.lnglat) || toLngLat(m.pos)
    if (!p) return
    const ll = new AMap.LngLat(p[0], p[1])
    allPoints.push(ll)
    const color = !m.online ? '#a8b4ae' : m.workStatus === '作业中' ? '#0f7a4e' : '#3b7cf0'
    // 沿用老版本的农机图标 trunk.png（放在 public/images 下）
    const icon = new AMap.Icon({
      image: '/images/trunk.png',
      size: new AMap.Size(20, 20),
      imageSize: new AMap.Size(20, 20)
    })
    const marker = new AMap.Marker({
      position: ll,
      icon,
      offset: new AMap.Pixel(-10, -10),
      title: `${m.code || m.id} · ${m.workStatus || ''}`
    })
    marker.on('click', () => emit('select-machine', m))
    map.add(marker); overlays.push(marker); markerMap.set(String(m.id), marker)
  })
}

function addHeat(allPoints) {
  if (!props.heat?.length || !AMap.HeatMap) return
  try {
    const heatmap = new AMap.HeatMap(map, { radius: 25, opacity: [0, 0.6] })
    const data = props.heat.map((h) => {
      const p = toLngLat(h)
      return p ? { lng: p[0], lat: p[1], count: Number(h[2] ?? 1) } : null
    }).filter(Boolean)
    if (data.length) {
      heatmap.setDataSet({ data, max: 4 })
      overlays.push(heatmap)
    }
  } catch (e) { /* 热力插件未加载时忽略 */ }
}

const zoom = (d) => { if (map) d > 0 ? map.zoomIn() : map.zoomOut() }
const fitAll = () => { if (map) map.setFitView(null, false, [40, 40, 40, 40]) }

const toggleSatellite = () => {
  if (!map || !AMap.TileLayer?.Satellite) return
  if (satelliteLayer) {
    map.remove(satelliteLayer); satelliteLayer = null
  } else {
    satelliteLayer = new AMap.TileLayer.Satellite()
    map.add(satelliteLayer)
  }
}
const toggleRoadNet = () => {
  if (!map || !AMap.TileLayer?.RoadNet) return
  if (roadNetLayer) {
    map.remove(roadNetLayer); roadNetLayer = null
  } else {
    roadNetLayer = new AMap.TileLayer.RoadNet()
    map.add(roadNetLayer)
  }
}

onMounted(async () => {
  try {
    AMap = await loadAMap(['AMap.Polygon', 'AMap.Polyline', 'AMap.Marker', 'AMap.HeatMap', 'AMap.ToolBar', 'AMap.Scale', 'AMap.TileLayer.Satellite', 'AMap.TileLayer.RoadNet'])
    map = new AMap.Map(mapEl.value, { zoom: 12, viewMode: '2D' })
    map.addControl(new AMap.ToolBar({ position: 'RB' }))
    map.addControl(new AMap.Scale())
    draw()
  } catch (e) {
    error.value = e.message
  }
})

watch(() => [props.lands, props.machines, props.tracks, props.activeTrack, props.heat, props.playIndex], draw, { deep: false })
onBeforeUnmount(() => { clearOverlays(); try { map?.destroy() } catch (e) {} })
</script>

<style scoped>
.amap-wrap { position: relative; border-radius: 12px; overflow: hidden; border: 1px solid var(--border); background: #eef4f0; }
.amap-canvas { width: 100%; height: 100%; }
.amap-live { position: absolute; right: 12px; top: 12px; z-index: 3; display: flex; align-items: center; gap: 6px; padding: 5px 9px; border-radius: 7px; background: rgba(255,255,255,.92); border: 1px solid var(--border); font-size: 10.5px; color: var(--text-2); }
.dot { width: 6px; height: 6px; border-radius: 50%; background: #1fae6b; }
.amap-toolbar { position: absolute; left: 12px; top: 12px; z-index: 3; display: flex; flex-direction: column; gap: 4px; }
.amap-toolbar button { width: 28px; height: 28px; border-radius: 7px; border: 1px solid var(--border); background: rgba(255,255,255,.94); color: var(--text-2); cursor: pointer; font-size: 12px; }
.amap-error { position: absolute; inset: 0; display: grid; place-items: center; background: rgba(255,255,255,.9); color: #c53030; font-size: 13px; }
</style>
