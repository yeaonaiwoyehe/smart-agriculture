<template>
  <AmapMap
    v-if="useAmap"
    v-bind="{ ...$props, height, lands, machines, heat, tracks, activeTrack, playIndex, anomalies, selectedLand, showLabels, showLegend, live }"
    @select-land="$emit('select-land', $event)"
    @select-machine="$emit('select-machine', $event)"
  />
  <div v-else class="farm-map" :style="{ height: height + 'px' }">
    <div class="map-toolbar">
      <button class="map-btn" title="放大" @click="zoom(0.2)"><el-icon><Plus /></el-icon></button>
      <button class="map-btn" title="缩小" @click="zoom(-0.2)"><el-icon><Minus /></el-icon></button>
      <button class="map-btn" title="复位" @click="scale = 1"><el-icon><Aim /></el-icon></button>
    </div>
    <div v-if="live" class="map-live"><span class="live-dot" />实时数据 · {{ live }}</div>

    <svg viewBox="0 0 1000 620" preserveAspectRatio="xMidYMid slice">
      <defs>
        <linearGradient id="mapBg" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stop-color="#f7fbf8" />
          <stop offset="100%" stop-color="#eaf3ed" />
        </linearGradient>
        <linearGradient id="fieldActive" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stop-color="#a8e3c1" />
          <stop offset="100%" stop-color="#cdeedb" />
        </linearGradient>
        <filter id="softGlow" x="-50%" y="-50%" width="200%" height="200%">
          <feGaussianBlur stdDeviation="4" result="b" />
          <feMerge><feMergeNode in="b" /><feMergeNode in="SourceGraphic" /></feMerge>
        </filter>
      </defs>

      <rect width="1000" height="620" fill="url(#mapBg)" />
      <g :transform="`translate(500 310) scale(${scale}) translate(-500 -310)`">
        <!-- 网格 -->
        <g stroke="#dfe9e3" stroke-width="0.7" opacity="0.75">
          <line v-for="x in 10" :key="'vx' + x" :x1="x * 100" y1="0" :x2="x * 100" y2="620" />
          <line v-for="y in 6" :key="'hy' + y" x1="0" :y1="y * 100" x2="1000" :y2="y * 100" />
        </g>

        <!-- 河流 / 道路 -->
        <path d="M -20 470 C 180 430, 260 520, 430 505 S 760 540, 1030 470" fill="none" stroke="#d6e9f4" stroke-width="26" opacity="0.85" />
        <path d="M -20 470 C 180 430, 260 520, 430 505 S 760 540, 1030 470" fill="none" stroke="#bcdff0" stroke-width="1.4" stroke-dasharray="6 5" />
        <path d="M 40 300 C 240 270, 420 300, 560 250 S 840 180, 980 200" fill="none" stroke="#e6ded1" stroke-width="9" />
        <path d="M 40 300 C 240 270, 420 300, 560 250 S 840 180, 980 200" fill="none" stroke="#fff" stroke-width="1.2" stroke-dasharray="10 8" />
        <path d="M 500 0 L 520 200 S 560 420, 610 620" fill="none" stroke="#e6ded1" stroke-width="7" />
        <text x="720" y="520" font-size="11" fill="#9db4c4" letter-spacing="1">柳沟河</text>

        <!-- 村庄 -->
        <g fill="#93a69b" font-size="10.5" font-weight="600">
          <text x="90" y="46">河东村</text>
          <text x="520" y="36">河西村</text>
          <text x="850" y="76">南滩村</text>
          <text x="310" y="600">北塬村</text>
        </g>

        <!-- 热力 -->
        <g v-if="heat && heat.length">
          <circle
            v-for="(h, i) in heat.slice(0, 120)"
            :key="'h' + i"
            :cx="h[0]"
            :cy="h[1]"
            :r="7 + (h[2] || 0.3) * 10"
            fill="#1fae6b"
            :opacity="0.05 + (h[2] || 0.3) * 0.1"
          />
        </g>

        <!-- 地块 -->
        <g>
          <polygon
            v-for="l in lands"
            :key="l.id"
            :points="toPoints(l.points)"
            :fill="landFill(l)"
            :stroke="selectedLand === l.id ? '#0f5537' : '#9dc4ad'"
            :stroke-width="selectedLand === l.id ? 2.6 : 1.1"
            class="land-poly"
            @click="$emit('select-land', l)"
          >
            <title>{{ l.name }} · {{ l.area }} 亩 · {{ l.status }}</title>
          </polygon>
          <g v-for="l in lands" :key="'t' + l.id" pointer-events="none">
            <text
              v-if="showLabels"
              :x="l.center[0]"
              :y="l.center[1] - 3"
              text-anchor="middle"
              font-size="10.5"
              font-weight="600"
              fill="#28473a"
            >{{ shortName(l.name) }}</text>
            <text
              v-if="showLabels"
              :x="l.center[0]"
              :y="l.center[1] + 11"
              text-anchor="middle"
              font-size="9"
              fill="#5f7a6c"
            >{{ l.area }}亩</text>
          </g>
        </g>

        <!-- 轨迹 -->
        <g v-if="tracks && tracks.length">
          <polyline
            v-for="(t, i) in tracks"
            :key="'tk' + i"
            :points="toPoints(t)"
            fill="none"
            stroke="#9fb6a8"
            stroke-width="1.4"
            stroke-dasharray="4 4"
            opacity="0.55"
          />
        </g>
        <g v-if="activeTrack && activeTrack.length">
          <polyline :points="toPoints(activeTrack)" fill="none" stroke="#c9d8cf" stroke-width="3" opacity="0.8" />
          <polyline :points="progressPoints" fill="none" stroke="#0f5537" stroke-width="3" stroke-linecap="round" filter="url(#softGlow)" />
          <circle
            v-if="playIndex >= 0 && activeTrack[playIndex]"
            :cx="activeTrack[playIndex][0]"
            :cy="activeTrack[playIndex][1]"
            r="6"
            fill="#fff"
            stroke="#0f5537"
            stroke-width="3"
          />
        </g>

        <!-- 异常点位 -->
        <g v-for="(a, i) in anomalies" :key="'an' + i">
          <template v-if="activeTrack && activeTrack[a.point]">
            <circle :cx="activeTrack[a.point][0]" :cy="activeTrack[a.point][1]" r="12" fill="none" stroke="#e04b4b" stroke-width="2" opacity="0.75" class="pulse" />
            <circle :cx="activeTrack[a.point][0]" :cy="activeTrack[a.point][1]" r="3.5" fill="#e04b4b" />
            <text :x="activeTrack[a.point][0] + 10" :y="activeTrack[a.point][1] - 8" font-size="9.5" fill="#c53030">{{ a.type }}</text>
          </template>
        </g>

        <!-- 农机 -->
        <g
          v-for="m in machines"
          :key="m.id"
          :transform="`translate(${m.pos[0]} ${m.pos[1]})`"
          class="machine"
          @click="$emit('select-machine', m)"
        >
          <circle v-if="m.online" r="14" :fill="machineColor(m)" opacity="0.14" class="pulse" />
          <circle r="6.5" :fill="machineColor(m)" stroke="#fff" stroke-width="2" />
          <template v-if="showLabels">
            <rect x="9" y="-15" width="74" height="17" rx="4" fill="#fff" opacity="0.92" stroke="#dfe8e2" />
            <text x="13" y="-3" font-size="9.5" fill="#2d4538" font-weight="600">{{ m.code }}</text>
          </template>
        </g>
      </g>
    </svg>

    <div v-if="showLegend" class="map-legend">
      <span><i style="background: #b7e6cb" />作业中</span>
      <span><i style="background: #e9f1ec" />待作业</span>
      <span><i style="background: #dcecdc" />已完成</span>
      <span><i style="background: #fbe3c2" />待复核</span>
      <span><i style="background: #1fae6b;border-radius:50%" />在线农机</span>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import AmapMap from './AmapMap.vue'

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
defineEmits(['select-land', 'select-machine'])

// 有真实经纬度（后端 WKT 解析结果）且开关打开时，自动切换到老版本的高德地图；
// 否则（mock 的像素坐标 / 离线演示）继续使用内置 SVG 示意图。
const useAmap = computed(() => {
  const enabled = String(import.meta.env.VITE_APP_USE_AMAP ?? 'false') === 'true'
  if (!enabled) return false
  const isLngLat = (p) => Array.isArray(p) && p.length >= 2 &&
    Number(p[0]) >= -180 && Number(p[0]) <= 180 &&
    Number(p[1]) >= -90 && Number(p[1]) <= 90
  return [...props.lands, ...props.machines].some((i) =>
    isLngLat(i?.lnglat?.[0]) || isLngLat(i?.centerLngLat) || isLngLat(i?.lnglat)
  )
})

const scale = ref(1)
const zoom = (d) => {
  scale.value = Math.min(2.2, Math.max(0.8, +(scale.value + d).toFixed(2)))
}
const toPoints = (pts) => (pts || []).map((p) => p.join(',')).join(' ')
const progressPoints = computed(() => {
  const arr = props.activeTrack
  if (!arr.length) return ''
  const end = props.playIndex < 0 ? arr.length : props.playIndex + 1
  return toPoints(arr.slice(0, Math.max(1, end)))
})
const shortName = (n) => n.replace('村', '·').replace('号地', '号')
const landFill = (l) => {
  if (props.selectedLand === l.id) return 'url(#fieldActive)'
  return {
    作业中: '#b7e6cb',
    待作业: '#e9f1ec',
    已完成: '#dcecdc',
    待复核: '#fbe3c2'
  }[l.status] || '#eef4f0'
}
const machineColor = (m) => (!m.online ? '#a8b4ae' : m.workStatus === '作业中' ? '#0f7a4e' : m.workStatus === '待机' ? '#3b7cf0' : '#e08a12')
</script>

<style scoped>
.farm-map {
  position: relative;
  border-radius: 12px;
  overflow: hidden;
  background: #eef4f0;
  border: 1px solid var(--border);
}
.farm-map svg {
  width: 100%;
  height: 100%;
  display: block;
}
.land-poly {
  cursor: pointer;
  transition: fill 0.2s;
}
.land-poly:hover {
  filter: brightness(0.98);
  stroke-width: 2;
}
.machine {
  cursor: pointer;
}
.pulse {
  animation: map-pulse 2.4s ease-out infinite;
  transform-origin: center;
}
@keyframes map-pulse {
  0%,
  100% {
    opacity: 0.16;
    transform: scale(0.9);
  }
  50% {
    opacity: 0.28;
    transform: scale(1.1);
  }
}
.map-toolbar {
  position: absolute;
  left: 12px;
  top: 12px;
  z-index: 3;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.map-btn {
  width: 28px;
  height: 28px;
  border-radius: 7px;
  border: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.94);
  color: var(--text-2);
  display: grid;
  place-items: center;
  cursor: pointer;
  font-size: 12px;
}
.map-btn:hover {
  color: var(--brand-700);
  border-color: var(--brand-300);
}
.map-live {
  position: absolute;
  right: 12px;
  top: 12px;
  z-index: 3;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px 9px;
  border-radius: 7px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid var(--border);
  font-size: 10.5px;
  color: var(--text-2);
}
.live-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #1fae6b;
  box-shadow: 0 0 0 3px rgba(31, 174, 107, 0.18);
  animation: map-pulse 2s infinite;
}
.map-legend {
  position: absolute;
  left: 12px;
  bottom: 10px;
  z-index: 3;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding: 6px 9px;
  border-radius: 7px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid var(--border);
  font-size: 10px;
  color: var(--text-2);
}
.map-legend span {
  display: flex;
  align-items: center;
  gap: 4px;
}
.map-legend i {
  width: 9px;
  height: 9px;
  border-radius: 2px;
  display: inline-block;
}
</style>
