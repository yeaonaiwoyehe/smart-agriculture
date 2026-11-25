<script setup lang="ts">
import { getCurrentInstance, nextTick, onMounted, watch } from 'vue'
import type { Plot } from '@/types'

interface MapPoint {
  latitude: number
  longitude: number
  timestamp?: number
}

const props = withDefaults(
  defineProps<{
    plot?: Plot | null
    trajectory?: MapPoint[]
    executed?: MapPoint[]
    current?: MapPoint | null
    heightRpx?: number
    widthRpx?: number
    legend?: boolean
  }>(),
  { heightRpx: 420, widthRpx: 686, trajectory: () => [], executed: () => [], current: null, legend: true }
)

const instance = getCurrentInstance()
const canvasId = `plotmap${Math.random().toString(36).slice(2, 9)}`
const sys = uni.getSystemInfoSync()
const widthPx = Math.floor((sys.windowWidth * props.widthRpx) / 750)
const heightPx = Math.floor((sys.windowWidth * props.heightRpx) / 750)

function drawPlaceholder(ctx: any) {
  ctx.setFillStyle('#E9EFEB')
  ctx.fillRect(0, 0, widthPx, heightPx)
  ctx.setFillStyle('#8B978F')
  ctx.setFontSize(13)
  ctx.setTextAlign('center')
  ctx.fillText('暂无地块数据', widthPx / 2, heightPx / 2)
  ctx.draw()
}

function draw() {
  const ctx = uni.createCanvasContext(canvasId, instance?.proxy as any)
  const plot = props.plot
  if (!plot || !plot.boundary || plot.boundary.length < 3) {
    drawPlaceholder(ctx)
    return
  }

  const boundary = plot.boundary
  const all: MapPoint[] = [...boundary, ...(props.trajectory || []), ...(props.executed || [])]
  const lats = all.map((p) => p.latitude)
  const lngs = all.map((p) => p.longitude)
  const latMin = Math.min(...lats)
  const latMax = Math.max(...lats)
  const lngMin = Math.min(...lngs)
  const lngMax = Math.max(...lngs)
  const latMid = (latMin + latMax) / 2
  const lngMid = (lngMin + lngMax) / 2
  const kx = Math.cos((latMid * Math.PI) / 180)

  const spanX = Math.max((lngMax - lngMin) * kx, 1e-6)
  const spanY = Math.max(latMax - latMin, 1e-6)
  const padding = 20
  const scale = Math.min((widthPx - padding * 2) / spanX, (heightPx - padding * 2) / spanY)

  const project = (p: MapPoint) => ({
    x: widthPx / 2 + (p.longitude - lngMid) * kx * scale,
    y: heightPx / 2 - (p.latitude - latMid) * scale
  })

  // 背景
  ctx.setFillStyle('#F7FAF8')
  ctx.fillRect(0, 0, widthPx, heightPx)

  // 网格
  ctx.setStrokeStyle('#E7EEEA')
  ctx.setLineWidth(1)
  for (let i = 1; i < 4; i += 1) {
    const y = (heightPx / 4) * i
    ctx.beginPath()
    ctx.moveTo(0, y)
    ctx.lineTo(widthPx, y)
    ctx.stroke()
  }

  // 地块边界
  ctx.beginPath()
  boundary.forEach((p, idx) => {
    const pt = project(p)
    if (idx === 0) ctx.moveTo(pt.x, pt.y)
    else ctx.lineTo(pt.x, pt.y)
  })
  ctx.closePath()
  ctx.setFillStyle('#E4F1E9')
  ctx.fill()
  ctx.setStrokeStyle('#1F7A4D')
  ctx.setLineWidth(2)
  ctx.stroke()

  // 规划/演示轨迹
  if (props.trajectory && props.trajectory.length > 1) {
    ctx.beginPath()
    props.trajectory.forEach((p, idx) => {
      const pt = project(p)
      if (idx === 0) ctx.moveTo(pt.x, pt.y)
      else ctx.lineTo(pt.x, pt.y)
    })
    ctx.setStrokeStyle('#D9A441')
    ctx.setLineWidth(1.5)
    ctx.stroke()
  }

  // 已执行轨迹
  if (props.executed && props.executed.length > 1) {
    ctx.beginPath()
    props.executed.forEach((p, idx) => {
      const pt = project(p)
      if (idx === 0) ctx.moveTo(pt.x, pt.y)
      else ctx.lineTo(pt.x, pt.y)
    })
    ctx.setStrokeStyle('#155F3B')
    ctx.setLineWidth(3)
    ctx.stroke()
  }

  // 当前农机位置
  const last = props.current || (props.executed && props.executed[props.executed.length - 1])
  if (last) {
    const pt = project(last)
    ctx.beginPath()
    ctx.arc(pt.x, pt.y, 7, 0, Math.PI * 2)
    ctx.setFillStyle('#FFFFFF')
    ctx.fill()
    ctx.beginPath()
    ctx.arc(pt.x, pt.y, 5, 0, Math.PI * 2)
    ctx.setFillStyle('#C0392B')
    ctx.fill()
  }

  // 地块名
  ctx.setFillStyle('#5F6B63')
  ctx.setFontSize(12)
  ctx.setTextAlign('left')
  ctx.fillText(plot.plotName, 12, 20)

  ctx.draw()
}

watch(
  () => [props.plot, props.trajectory, props.executed, props.current],
  () => {
    nextTick(() => setTimeout(draw, 30))
  },
  { deep: true }
)

onMounted(() => {
  setTimeout(draw, 80)
})

defineExpose({ redraw: draw })
</script>

<template>
  <view class="plot-map">
    <canvas
      :canvas-id="canvasId"
      :id="canvasId"
      class="plot-map__canvas"
      :style="{ width: widthPx + 'px', height: heightPx + 'px' }"
    />
    <view v-if="legend" class="plot-map__legend">
      <view class="legend-item"><view class="legend-dot legend-dot--plot" />地块边界</view>
      <view class="legend-item"><view class="legend-dot legend-dot--track" />演示轨迹</view>
      <view class="legend-item"><view class="legend-dot legend-dot--done" />已作业</view>
      <text class="legend-note">本地绘制，非真实地图</text>
    </view>
  </view>
</template>

<style scoped lang="scss">
.plot-map {
  width: 100%;
}

.plot-map__canvas {
  display: block;
  width: 100%;
  border-radius: 12rpx;
  overflow: hidden;
}

.plot-map__legend {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 18rpx;
  margin-top: 14rpx;
}

.legend-item {
  display: flex;
  align-items: center;
  font-size: 22rpx;
  color: var(--muted, #6a7b70);
}

.legend-dot {
  width: 16rpx;
  height: 16rpx;
  border-radius: 4rpx;
  margin-right: 8rpx;
}

.legend-dot--plot {
  background: #e4f1e9;
  border: 1rpx solid #1c6a4f;
}

.legend-dot--track {
  background: #f0a63a;
}

.legend-dot--done {
  background: #14513d;
}

.legend-note {
  margin-left: auto;
  font-size: 20rpx;
  color: var(--muted-2, #8d9c91);
}
</style>
