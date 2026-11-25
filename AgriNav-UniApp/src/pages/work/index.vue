<script setup lang="ts">
import { computed, onUnmounted, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import SfIcon from '@/components/SfIcon.vue'
import PlotMap from '@/components/PlotMap.vue'
import { WORK_SESSION_SUPPORTED_IN_REAL } from '@/api/work'
import type { Plot, WorkOrder } from '@/types'
import type { TrackPoint } from '@/mock/trajectory'
import { pointsBefore } from '@/mock/trajectory'
import { fetchPlots } from '@/api/user'
import { useAppStore } from '@/stores/app'
import { useWorkStore } from '@/stores/work'
import { WORK_STATUS_TEXT } from '@/utils/constants'
import { formatArea, formatDuration, formatMoney, formatTime } from '@/utils/format'
import { confirmAction, errorMessage, hideLoading, showLoading, toast } from '@/utils/ui'
import { go, goAgent, goBack } from '@/utils/nav'

const app = useAppStore()
const store = useWorkStore()

const plots = ref<Plot[]>([])
const nowTs = ref(Date.now())
const showTrack = ref(true)
const loading = ref(false)
const ending = ref(false)
const result = ref<WorkOrder | null>(null)
let timer: ReturnType<typeof setInterval> | null = null

const order = computed(() => store.current)
const paused = computed(() => !!order.value && order.value.status === 2)

const currentPlot = computed<Plot | null>(() => {
  if (!order.value) return null
  return plots.value.find((p) => p.plotId === order.value!.plotId) || null
})

const elapsedSeconds = computed(() => {
  if (!order.value) return 0
  if (order.value.status === 1) {
    return Math.max(0, (nowTs.value - parseTime(order.value.startTime)) / 1000)
  }
  return Math.max(0, (parseTime(order.value.endTime || '') - parseTime(order.value.startTime)) / 1000)
})

const executedPoints = computed<TrackPoint[]>(() => {
  if (!order.value) return []
  if (order.value.status !== 1) return order.value.trajectory || []
  return pointsBefore(order.value.trajectory || [], nowTs.value)
})

onShow(() => {
  void load()
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

function parseTime(value: string): number {
  if (!value) return Date.now()
  return new Date(value.replace(' ', 'T')).getTime()
}

async function load() {
  loading.value = true
  try {
    if (!plots.value.length) plots.value = await fetchPlots()
    await store.refresh()
    startTick()
  } catch (e) {
    toast(errorMessage(e, '作业数据加载失败'))
  } finally {
    loading.value = false
  }
}

function startTick() {
  if (timer) clearInterval(timer)
  nowTs.value = Date.now()
  timer = setInterval(() => {
    nowTs.value = Date.now()
  }, 1000)
}

function openMap() {
  if (!order.value) return
  go(
    `/pages/map/index?plotId=${order.value.plotId}&orderId=${encodeURIComponent(
      order.value.trajectoryInfoId
    )}`
  )
}

function openDetail() {
  if (!order.value) return
  go(`/pages/work/detail?id=${encodeURIComponent(order.value.trajectoryInfoId)}`)
}

function navigateToPlot() {
  const plot = currentPlot.value
  if (!plot) return
  uni.openLocation({
    latitude: plot.center.latitude,
    longitude: plot.center.longitude,
    name: plot.plotName,
    address: plot.address,
    fail: () => toast('当前环境不支持地图导航')
  })
}

async function syncTrajectory() {
  if (!order.value) return
  showLoading('同步中')
  try {
    const count = await store.sync(order.value.trajectoryInfoId)
    hideLoading()
    toast(count ? `已同步 ${count} 个轨迹点` : '当前没有待同步轨迹点')
  } catch (e) {
    hideLoading()
    toast(errorMessage(e, '同步失败'))
  }
}

async function endWork() {
  if (!order.value || order.value.status !== 1) return
  const ok = await confirmAction({
    title: '确认结束本次作业？',
    content: `地块：${order.value.plotName}\n已作业：${formatDuration(elapsedSeconds.value)}\n轨迹点：${order.value.totalPoints} 个\n\n结束后由平台核算面积。`,
    confirmText: '确认结束',
    danger: true
  })
  if (!ok) return
  ending.value = true
  showLoading('提交中')
  try {
    const finished = await store.finish(order.value.trajectoryInfoId)
    uni.showLoading({ title: '面积核算中', mask: true })
    await new Promise((r) => setTimeout(r, 1500))
    const settled = await store.settle(finished.trajectoryInfoId)
    hideLoading()
    result.value = settled
    await load()
  } catch (e) {
    hideLoading()
    toast(errorMessage(e, '结束作业失败'))
  } finally {
    ending.value = false
  }
}

function closeResult() {
  result.value = null
}

function goSettlement() {
  result.value = null
  go('/pages/settlement/index')
}
</script>

<template>
  <view class="page-root">
    <view class="page-head">
      <view class="icon-btn" @tap="goBack('/pages/home/index')">
        <SfIcon name="back" :size="40" color="#10231b" />
      </view>
      <text class="page-title grow">作业控制中心</text>
      <text class="status" :class="app.online ? 'status--ok' : 'status--review'">
        <view
          class="sync-dot"
          style="width: 12rpx; height: 12rpx; box-shadow: none"
          :style="{ background: app.online ? '#16794a' : '#b8730a' }"
        />
        {{ app.online ? '在线' : '离线' }}
      </text>
    </view>

    <view v-if="!WORK_SESSION_SUPPORTED_IN_REAL && !app.mockEnabled" class="real-notice">
      真实后端暂未提供开工 / 完工接口，当前仅可查看历史作业单。切换演示数据后可体验完整作业流程。
    </view>

    <view v-if="loading && !order" class="pad" style="padding-top: 32rpx">
      <view class="skeleton" style="height: 360rpx; border-radius: 48rpx" />
      <view class="skeleton" style="height: 480rpx; border-radius: 40rpx; margin-top: 28rpx" />
    </view>

    <view v-else-if="!order" class="empty" style="padding-top: 120rpx">
      <view class="empty-ico">
        <SfIcon name="satellite" :size="60" color="#1c6a4f" />
      </view>
      <text class="empty-title">当前没有进行中的作业</text>
      <text class="tiny" style="display: block; margin-top: 8rpx">
        接单后点「开始作业」，系统会自动采集 GPS 轨迹。
      </text>
      <view class="row gap-10" style="justify-content: center; margin-top: 32rpx">
        <view class="btn btn--ghost btn--sm" @tap="go('/pages/task/index')">去我的任务</view>
        <view class="btn btn--primary btn--sm" @tap="goAgent('当前作业怎么开始？')">问作业助手</view>
      </view>
    </view>

    <template v-else>
      <view class="work-hero" style="margin-top: 28rpx">
        <view class="row-between">
          <view class="work-flag" :class="{ 'work-flag--warn': paused, 'work-flag--off': !app.online }">
            <view
              class="sync-dot"
              style="width: 12rpx; height: 12rpx; box-shadow: none; background: currentColor"
            />
            <text>{{ paused ? '待核算' : app.online ? 'GPS 采集中' : '离线采集' }}</text>
          </view>
          <text class="tiny" style="color: rgba(255, 255, 255, 0.55)">
            {{ order.trajectoryInfoId }}
          </text>
        </view>

        <view style="margin-top: 24rpx; font-size: 40rpx; font-weight: 800">
          {{ order.plotName }}
          <text class="tiny" style="color: rgba(255, 255, 255, 0.5); font-weight: 500; margin-left: 12rpx">
            {{ order.workTypeName }}
          </text>
        </view>

        <view class="work-timer">
          {{ formatDuration(elapsedSeconds) }}
          <text>本次作业</text>
        </view>

        <view class="work-metrics">
          <view class="work-metric">
            <text style="display: block">{{ order.area ? formatArea(order.area) : '—' }}</text>
            <text>核算面积 / 亩</text>
          </view>
          <view class="work-metric">
            <text style="display: block">{{ order.totalPoints }}</text>
            <text>轨迹定位点</text>
          </view>
          <view class="work-metric">
            <text style="display: block; font-size: 30rpx; padding-top: 6rpx">
              {{ order.pendingPoints ? '待同步' : '已同步' }}
            </text>
            <text>数据上传</text>
          </view>
        </view>
      </view>

      <view style="padding: 28rpx 32rpx 0">
        <view class="map-box" style="height: 572rpx">
          <PlotMap
            :plot="currentPlot"
            :trajectory="showTrack ? order.trajectory : []"
            :executed="showTrack ? executedPoints : []"
            :width-rpx="686"
            :height-rpx="572"
          />
        </view>

        <view class="tool-row" style="padding: 0; margin-top: 20rpx">
          <view class="tool-chip" :class="{ 'tool-chip--on': showTrack }" @tap="showTrack = !showTrack">
            <SfIcon name="map" :size="32" color="#2c3f35" />
            <text>轨迹</text>
          </view>
          <view class="tool-chip" @tap="openMap">
            <SfIcon name="expand" :size="32" color="#2c3f35" />
            <text>放大地图</text>
          </view>
          <view class="tool-chip" @tap="syncTrajectory">
            <SfIcon name="sync" :size="32" color="#2c3f35" />
            <text>同步数据</text>
          </view>
        </view>
        <view class="tiny muted" style="text-align: center; margin-top: 12rpx">
          当前为机具定位模拟；接入真实 GPS 后按同样接口写入轨迹
        </view>
      </view>

      <view class="section">
        <view class="section-head"><text class="section-title">采集与上传</text></view>
        <view class="card" style="padding: 8rpx 30rpx">
          <view class="info-row">
            <text>定位状态</text>
            <text>{{ app.gpsLabel }}</text>
          </view>
          <view class="info-row">
            <text>轨迹采集</text>
            <text>{{ paused ? '已暂停' : '持续采集中' }} · {{ order.totalPoints }} 点</text>
          </view>
          <view class="info-row">
            <text>数据上传</text>
            <text :style="{ color: order.pendingPoints ? '#b8730a' : '#16794a' }">
              {{ order.pendingPoints ? `${order.pendingPoints} 条待上传` : '已同步' }}
            </text>
          </view>
          <view class="info-row">
            <text>数据来源</text>
            <text>{{ app.mockEnabled ? '演示轨迹' : '车载 GPS 定位' }}</text>
          </view>
          <view class="info-row">
            <text>作业数据归属</text>
            <text>仅本人账号可见</text>
          </view>
        </view>
      </view>

      <view class="section">
        <view class="section-head"><text class="section-title">作业单信息</text></view>
        <view class="card" style="padding: 8rpx 30rpx">
          <view class="info-row">
            <text>作业单号</text>
            <text>{{ order.trajectoryInfoId }}</text>
          </view>
          <view class="info-row">
            <text>作业地块</text>
            <text>{{ order.plotName }}</text>
          </view>
          <view class="info-row">
            <text>作业类型</text>
            <text>{{ order.workTypeName }}</text>
          </view>
          <view class="info-row">
            <text>作业单价</text>
            <text>{{ (order.unitPrice || 0).toFixed(2) }} 元/亩</text>
          </view>
          <view class="info-row">
            <text>开始时间</text>
            <text>{{ order.startTime }}</text>
          </view>
          <view class="info-row">
            <text>当前状态</text>
            <text>{{ WORK_STATUS_TEXT[order.status] }}</text>
          </view>
          <view v-if="order.amount" class="info-row">
            <text>核算金额</text>
            <text>¥{{ formatMoney(order.amount) }}</text>
          </view>
        </view>
      </view>

      <view class="pad" style="margin-top: 32rpx; margin-bottom: 16rpx">
        <view class="row gap-6" style="margin-bottom: 20rpx">
          <view class="btn btn--ghost grow" @tap="openDetail">作业单详情</view>
          <view class="btn btn--soft grow" @tap="navigateToPlot">
            <SfIcon name="navigation" :size="34" color="#0e3b2e" />
            <text>导航</text>
          </view>
        </view>
        <view
          class="btn btn--danger btn--block"
          :class="{ 'dock-btn--disabled': order.status !== 1 || ending }"
          @tap="endWork"
        >
          <SfIcon name="stop" :size="36" color="#a92f24" />
          <text>结束本次作业</text>
        </view>
        <view class="tiny muted" style="text-align: center; margin-top: 16rpx">
          结束后由平台核算面积，需二次确认，也可点底部红色按钮
        </view>
      </view>
    </template>

    <template v-if="result">
      <view class="backdrop" style="z-index: 96" @tap="closeResult" />
      <view class="sheet" style="z-index: 97">
        <view class="sheet-grip" />
        <view class="row gap-10">
          <view class="result-badge">
            <SfIcon name="check" :size="48" color="#0e3b2e" />
          </view>
          <view class="grow">
            <view class="sheet-title" style="font-size: 38rpx">本次作业已完成</view>
            <view class="sheet-sub">{{ result.plotName }} · {{ result.trajectoryInfoId }}</view>
          </view>
        </view>

        <view style="margin-top: 28rpx">
          <view class="tiny muted">平台核算面积</view>
          <view class="amount-big" style="font-size: 80rpx">
            {{ formatArea(result.area) }}
            <text>亩</text>
          </view>
        </view>

        <view class="confirm-lines">
          <view class="confirm-line">
            <text>作业时长</text>
            <text>{{ formatDuration(elapsedSeconds) }}</text>
          </view>
          <view class="confirm-line">
            <text>轨迹点</text>
            <text>{{ result.trajectory.length }} 个</text>
          </view>
          <view class="confirm-line">
            <text>核算金额</text>
            <text>¥{{ formatMoney(result.amount) }}</text>
          </view>
          <view class="confirm-line">
            <text>回执编号</text>
            <text>{{ result.receiptNo || '--' }}</text>
          </view>
          <view class="confirm-line">
            <text>最近结束</text>
            <text>{{ formatTime(result.endTime) }}</text>
          </view>
        </view>

        <view class="sheet-actions">
          <view class="btn btn--ghost" @tap="goSettlement">结算明细</view>
          <view class="btn btn--primary" @tap="closeResult">完成</view>
        </view>
      </view>
    </template>
  </view>
</template>

<style scoped>
.real-notice {
  margin: 20rpx 32rpx 0;
  padding: 20rpx 24rpx;
  border-radius: 28rpx;
  background: var(--amber-soft);
  border: 2rpx solid #f7d59b;
  color: #7a4b05;
  font-size: 24rpx;
  line-height: 1.6;
}

.empty-ico {
  display: flex;
  justify-content: center;
  margin-bottom: 20rpx;
}

.empty-title {
  display: block;
  font-size: 32rpx;
  font-weight: 800;
  color: var(--ink-2);
}

.result-badge {
  width: 96rpx;
  height: 96rpx;
  border-radius: 30rpx;
  background: var(--lime-soft);
  color: var(--green-800);
  display: grid;
  place-items: center;
  flex: none;
}

.work-timer text {
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.6);
  font-weight: 600;
  margin-left: 12rpx;
  letter-spacing: 0;
}

.work-metric text:first-child {
  font-size: 36rpx;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}

.work-metric text:last-child {
  font-size: 21rpx;
  color: rgba(255, 255, 255, 0.62);
}

.amount-big text {
  font-size: 28rpx;
  font-weight: 700;
  color: var(--muted);
  margin-left: 6rpx;
}
</style>
