<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import SfIcon from '@/components/SfIcon.vue'
import PlotMap from '@/components/PlotMap.vue'
import type { Plot, WorkOrder } from '@/types'
import { fetchWorkDetail } from '@/api/work'
import { fetchPlots } from '@/api/user'
import { WORK_STATUS_TEXT } from '@/utils/constants'
import { formatArea, formatDuration, formatMoney } from '@/utils/format'
import { errorMessage, toast } from '@/utils/ui'
import { go, goBack } from '@/utils/nav'
import { useWorkStore } from '@/stores/work'

const store = useWorkStore()
const order = ref<WorkOrder | null>(null)
const plot = ref<Plot | null>(null)
const loading = ref(true)
const error = ref('')

const duration = computed(() => {
  if (!order.value) return 0
  const start = new Date(order.value.startTime.replace(' ', 'T')).getTime()
  const end = order.value.endTime
    ? new Date(order.value.endTime.replace(' ', 'T')).getTime()
    : Date.now()
  return Math.max(0, (end - start) / 1000)
})

const statusClass = computed(() => {
  if (!order.value) return 'status--done'
  if (order.value.status === 1) return 'status--doing'
  if (order.value.status === 2) return 'status--review'
  if (order.value.status === 0) return 'status--todo'
  return 'status--done'
})

onLoad(async (query) => {
  const id = String(query?.id || '')
  if (!id) {
    error.value = '缺少作业单号'
    loading.value = false
    return
  }
  try {
    const [detail, plots] = await Promise.all([fetchWorkDetail(id), fetchPlots().catch(() => [])])
    order.value = detail
    plot.value = plots.find((p) => p.plotId === detail.plotId) || null
  } catch (e) {
    error.value = errorMessage(e, '作业单加载失败')
  } finally {
    loading.value = false
  }
})

function openMap() {
  if (!order.value) return
  go(
    `/pages/map/index?plotId=${order.value.plotId}&orderId=${encodeURIComponent(
      order.value.trajectoryInfoId
    )}`
  )
}

async function handleSync() {
  if (!order.value) return
  try {
    const count = await store.sync(order.value.trajectoryInfoId)
    toast(count > 0 ? `已同步 ${count} 个轨迹点` : '没有待同步数据')
  } catch (e) {
    toast(errorMessage(e, '同步失败'))
  }
}
</script>

<template>
  <view class="page-root">
    <view class="page-head">
      <view class="icon-btn" @tap="goBack('/pages/work/index')">
        <SfIcon name="back" :size="40" color="#10231b" />
      </view>
      <text class="page-title grow">作业单详情</text>
      <view class="icon-btn" @tap="openMap">
        <SfIcon name="map" :size="38" color="#10231b" />
      </view>
    </view>

    <view v-if="loading" class="pad" style="padding-top: 32rpx">
      <view class="skeleton" style="height: 420rpx; border-radius: 44rpx" />
      <view class="skeleton" style="height: 460rpx; border-radius: 40rpx; margin-top: 24rpx" />
    </view>

    <view v-else-if="error" class="card empty" style="margin: 40rpx 32rpx; padding: 60rpx 40rpx">
      <view class="empty-ico"><SfIcon name="alert" :size="56" color="#c47a0a" /></view>
      <text class="empty-title">{{ error }}</text>
      <view style="margin-top: 24rpx">
        <view class="btn btn--ghost btn--sm" @tap="goBack('/pages/work/index')">返回</view>
      </view>
    </view>

    <template v-else-if="order">
      <view style="padding: 32rpx 32rpx 0">
        <view class="card" style="padding: 30rpx">
          <view class="row-between">
            <view class="grow">
              <view class="plot-name" style="font-size: 42rpx">
                {{ order.plotName }}
                <text class="plot-code">{{ order.workTypeName }}</text>
              </view>
              <view class="task-meta">
                <text>{{ order.address }}</text>
              </view>
            </view>
            <text class="status" :class="statusClass">{{ WORK_STATUS_TEXT[order.status] }}</text>
          </view>

          <view class="stats" style="margin-top: 28rpx">
            <view class="stat">
              <view class="stat-num">
                {{ order.area ? formatArea(order.area) : '—' }}<text class="stat-num-unit">亩</text>
              </view>
              <view class="stat-label">核算面积</view>
            </view>
            <view class="stat">
              <view class="stat-num">
                {{ order.amount ? formatMoney(order.amount) : '—' }}<text class="stat-num-unit">元</text>
              </view>
              <view class="stat-label">作业金额</view>
            </view>
            <view class="stat">
              <view class="stat-num" style="font-size: 40rpx; padding-top: 10rpx">
                {{ formatDuration(duration) }}
              </view>
              <view class="stat-label">作业时长</view>
            </view>
          </view>

          <view class="task-hero-map" style="height: 440rpx">
            <PlotMap
              :plot="plot"
              :trajectory="order.trajectory"
              :executed="order.trajectory"
              :width-rpx="622"
              :height-rpx="440"
            />
          </view>
          <view class="task-actions">
            <view class="btn btn--ghost btn--block" @tap="openMap">全屏查看 / 轨迹回放</view>
          </view>
        </view>
      </view>

      <view class="section">
        <view class="section-head"><text class="section-title">作业单信息</text></view>
        <view class="card" style="padding: 8rpx 30rpx">
          <view class="info-row"><text>作业单号</text><text>{{ order.trajectoryInfoId }}</text></view>
          <view class="info-row"><text>地块</text><text>{{ order.plotName }}</text></view>
          <view class="info-row"><text>地址</text><text>{{ order.address }}</text></view>
          <view class="info-row"><text>作业类型</text><text>{{ order.workTypeName }}</text></view>
          <view class="info-row"><text>作业手机号</text><text>{{ order.phoneNumber }}</text></view>
          <view class="info-row"><text>开始时间</text><text>{{ order.startTime }}</text></view>
          <view class="info-row"><text>结束时间</text><text>{{ order.endTime || '--' }}</text></view>
          <view class="info-row"><text>作业幅宽</text><text>{{ order.width }} m</text></view>
          <view class="info-row"><text>轨迹点数</text><text>{{ order.totalPoints }}</text></view>
          <view class="info-row"><text>待同步点数</text><text>{{ order.pendingPoints }}</text></view>
          <view class="info-row"><text>回执编号</text><text>{{ order.receiptNo || '--' }}</text></view>
        </view>
      </view>

      <view class="task-actions" style="padding: 0 32rpx; margin-top: 32rpx">
        <view
          class="btn btn--soft grow"
          :class="{ 'dock-btn--disabled': order.pendingPoints === 0 }"
          @tap="handleSync"
        >
          同步轨迹
        </view>
        <view v-if="order.status === 1" class="btn btn--primary grow" @tap="go('/pages/work/index')">
          前往作业控制台
        </view>
        <view v-else-if="order.status === 2" class="btn btn--soft grow dock-btn--disabled">核算中</view>
      </view>

      <view class="tiny muted footnote">
        作业轨迹与核算结果由平台统一生成，助手不会自行计算。
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

.footnote {
  text-align: center;
  margin: 36rpx 48rpx 20rpx;
  line-height: 1.7;
}
</style>
