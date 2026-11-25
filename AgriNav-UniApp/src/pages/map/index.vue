<script setup lang="ts">
import { computed, onUnmounted, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import SfIcon from '@/components/SfIcon.vue'
import PlotMap from '@/components/PlotMap.vue'
import type { Plot, WorkOrder } from '@/types'
import type { TrackPoint } from '@/mock/trajectory'
import { fetchCurrentWork, fetchWorkDetail } from '@/api/work'
import { fetchPlots } from '@/api/user'
import { trackLength } from '@/mock/trajectory'
import { formatArea, formatMoney } from '@/utils/format'
import { errorMessage, toast } from '@/utils/ui'
import { go, goBack } from '@/utils/nav'

const plots = ref<Plot[]>([])
const plot = ref<Plot | null>(null)
const order = ref<WorkOrder | null>(null)
const ratio = ref(100)
const playing = ref(false)
const loading = ref(true)
const error = ref('')
let playTimer: ReturnType<typeof setInterval> | null = null

const trajectory = computed<TrackPoint[]>(() => order.value?.trajectory || [])

const executed = computed<TrackPoint[]>(() => {
  if (!trajectory.value.length) return []
  const count = Math.max(1, Math.floor((trajectory.value.length * ratio.value) / 100))
  return trajectory.value.slice(0, count)
})

const lengthKm = computed(() => {
  const meters = trackLength(trajectory.value)
  return meters > 0 ? (meters / 1000).toFixed(2) : '0'
})

onLoad(async (query) => {
  const plotId = Number(query?.plotId || 0)
  const orderId = String(query?.orderId || '')
  try {
    plots.value = await fetchPlots()
    // 后端暂无作业会话/轨迹详情接口时，不影响地块地图展示
    try {
      order.value = orderId ? await fetchWorkDetail(orderId) : await fetchCurrentWork()
    } catch {
      order.value = null
    }
    const targetId = plotId || order.value?.plotId || plots.value[0]?.plotId
    plot.value = plots.value.find((p) => p.plotId === targetId) || null
  } catch (e) {
    error.value = errorMessage(e, '地块数据加载失败')
  } finally {
    loading.value = false
  }
})

onUnmounted(() => {
  if (playTimer) clearInterval(playTimer)
})

function onRatioChange(e: any) {
  ratio.value = Number(e.detail.value)
}

function togglePlay() {
  if (!trajectory.value.length) {
    toast('当前没有可回放轨迹')
    return
  }
  if (playing.value) {
    playing.value = false
    if (playTimer) clearInterval(playTimer)
    playTimer = null
    return
  }
  playing.value = true
  if (ratio.value >= 100) ratio.value = 0
  playTimer = setInterval(() => {
    ratio.value = Math.min(100, ratio.value + 2)
    if (ratio.value >= 100 && playTimer) {
      clearInterval(playTimer)
      playTimer = null
      playing.value = false
    }
  }, 160)
}

function openNavigation() {
  if (!plot.value) return
  uni.openLocation({
    latitude: plot.value.center.latitude,
    longitude: plot.value.center.longitude,
    name: plot.value.plotName,
    address: plot.value.address,
    fail: () => toast('当前环境不支持地图导航')
  })
}

function openWork() {
  if (!order.value) return
  go(`/pages/work/detail?id=${encodeURIComponent(order.value.trajectoryInfoId)}`)
}
</script>

<template>
  <view class="page-root">
    <view class="page-head">
      <view class="icon-btn" @tap="goBack('/pages/task/index')">
        <SfIcon name="back" :size="40" color="#10231b" />
      </view>
      <text class="page-title grow">地块档案</text>
      <view class="icon-btn" @tap="openNavigation">
        <SfIcon name="navigation" :size="38" color="#10231b" />
      </view>
    </view>

    <view v-if="loading" class="pad" style="padding-top: 32rpx">
      <view class="skeleton" style="height: 380rpx; border-radius: 44rpx" />
      <view class="skeleton" style="height: 200rpx; border-radius: 36rpx; margin-top: 24rpx" />
    </view>

    <view v-else-if="error" class="card empty" style="margin: 40rpx 32rpx; padding: 60rpx 40rpx">
      <view class="empty-ico"><SfIcon name="alert" :size="56" color="#c47a0a" /></view>
      <text class="empty-title">{{ error }}</text>
    </view>

    <template v-else-if="plot">
      <view class="pad" style="padding-top: 8rpx">
        <view class="card" style="padding: 30rpx">
          <view class="row-between">
            <view class="plot-name">
              {{ plot.plotName }}
              <text class="plot-code">{{ plot.plotNo }}</text>
            </view>
            <text class="status status--done">本人地块</text>
          </view>
          <view class="task-meta">
            <text>{{ formatArea(plot.area) }} 亩</text>
            <text>{{ plot.cropName }}</text>
            <text>{{ plot.address }}</text>
          </view>
          <view class="plot-map-wrap" style="margin-top: 22rpx">
            <PlotMap
              :plot="plot"
              :trajectory="trajectory"
              :executed="executed"
              :width-rpx="622"
              :height-rpx="304"
              :legend="!!trajectory.length"
            />
          </view>
          <view class="task-actions">
            <view class="btn btn--primary btn--block" @tap="openNavigation">
              <SfIcon name="navigation" :size="34" color="#ffffff" />
              <text>导航到地块</text>
            </view>
          </view>
        </view>

        <view class="card" style="padding: 28rpx 30rpx; margin-top: 20rpx">
          <view class="plot-stats" style="margin-top: 0; padding-top: 0; border-top: none">
            <view>
              <text class="plot-stat-num">{{ formatArea(plot.area) }}</text>
              <text>地块面积(亩)</text>
            </view>
            <view>
              <text class="plot-stat-num">{{ plot.cropName }}</text>
              <text>种植作物</text>
            </view>
            <view>
              <text class="plot-stat-num">{{ plot.boundary.length }}</text>
              <text>边界点数</text>
            </view>
          </view>
        </view>

        <view v-if="order" class="card" style="padding: 30rpx; margin-top: 20rpx">
          <view class="row-between">
            <text class="section-title" style="font-size: 30rpx">作业轨迹</text>
            <text class="tiny muted">{{ trajectory.length }} 点 · {{ lengthKm }} km</text>
          </view>
          <view class="plot-map-wrap" style="margin-top: 20rpx">
            <PlotMap
              :plot="plot"
              :trajectory="trajectory"
              :executed="executed"
              :width-rpx="622"
              :height-rpx="380"
            />
          </view>

          <view class="row-between" style="margin-top: 24rpx">
            <text class="strong">轨迹回放</text>
            <text class="tiny muted">{{ ratio }}%</text>
          </view>
          <slider
            :value="ratio"
            min="0"
            max="100"
            step="1"
            active-color="#c8f45a"
            background-color="#dbe3d3"
            block-color="#0e3b2e"
            block-size="22"
            style="margin: 12rpx 0"
            @changing="onRatioChange"
            @change="onRatioChange"
          />
          <view class="task-actions" style="margin-top: 8rpx">
            <view class="btn btn--ghost grow" @tap="togglePlay">
              <SfIcon :name="playing ? 'pause' : 'play'" :size="32" color="#10231b" />
              <text>{{ playing ? '暂停回放' : '开始回放' }}</text>
            </view>
            <view class="btn btn--primary grow" @tap="openWork">作业单详情</view>
          </view>
        </view>

        <view v-if="order" class="card list-card" style="margin-top: 20rpx">
          <view class="work-row" @tap="openWork">
            <view class="grow">
              <text class="work-title">{{ order.trajectoryInfoId }}</text>
              <text>{{ order.workTypeName }} · {{ order.totalPoints }} 轨迹点</text>
            </view>
            <view class="work-amount">
              <text class="work-money">{{ order.amount ? '¥' + formatMoney(order.amount) : '—' }}</text>
              <text>{{ order.status === 3 ? '已完成' : order.status === 1 ? '作业中' : '待核算' }}</text>
            </view>
          </view>
        </view>
      </view>

      <view class="tiny muted footnote">
        地块边界、作业轨迹与核算面积均由平台按 GPS 数据生成。
      </view>
    </template>
  </view>
</template>

<style scoped>
.empty-ico {
  display: flex;
  justify-content: center;
  margin-bottom: 20rpx;
}

.empty-title {
  display: block;
  font-size: 30rpx;
  font-weight: 700;
  color: var(--ink-2);
}

.plot-stat-num {
  display: block;
  font-size: 34rpx;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}

.work-title {
  display: block;
  font-size: 28rpx;
  font-weight: 700;
  color: var(--ink-2);
}

.work-money {
  display: block;
  font-size: 30rpx;
  font-weight: 800;
  color: var(--green-700);
}

.footnote {
  text-align: center;
  margin: 32rpx 48rpx 20rpx;
  line-height: 1.7;
}
</style>
