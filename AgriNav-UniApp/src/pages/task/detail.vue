<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import SfIcon from '@/components/SfIcon.vue'
import PlotMap from '@/components/PlotMap.vue'
import type { Task } from '@/types'
import { fetchTaskDetail } from '@/api/task'
import { useTaskStore } from '@/stores/task'
import { useWorkStore } from '@/stores/work'
import { TASK_STATUS_TEXT } from '@/utils/constants'
import { formatArea, formatMoney, formatTime, relativeTime } from '@/utils/format'
import { confirmAction, errorMessage, hideLoading, showLoading, toast } from '@/utils/ui'
import { go, goAgent, goBack } from '@/utils/nav'

const task = ref<Task | null>(null)
const loading = ref(true)
const error = ref('')
const taskStore = useTaskStore()
const workStore = useWorkStore()

const amount = computed(() => (task.value ? task.value.area * task.value.unitPrice : 0))

const statusClass = computed(() => {
  switch (task.value?.status) {
    case 'pending':
      return 'status--pending'
    case 'claimed':
      return 'status--todo'
    case 'working':
      return 'status--doing'
    case 'done':
      return 'status--done'
    default:
      return 'status--timeout'
  }
})

onLoad(async (query) => {
  const id = String(query?.id || '')
  if (!id) {
    error.value = '缺少任务编号'
    loading.value = false
    return
  }
  await load(id)
})

async function load(id: string) {
  loading.value = true
  error.value = ''
  try {
    task.value = await fetchTaskDetail(id)
  } catch (e) {
    error.value = errorMessage(e, '任务加载失败')
    task.value = null
  } finally {
    loading.value = false
  }
}

async function handleClaim() {
  if (!task.value) return
  const ok = await confirmAction({
    title: '确认接收任务',
    content: `地块：${task.value.plot.plotName}\n作业：${task.value.workTypeName}\n面积：${formatArea(task.value.area)} 亩\n单价：${task.value.unitPrice} 元/亩\n\n接收后请按截止时间完成作业。`,
    confirmText: '接收任务'
  })
  if (!ok) return
  showLoading('提交中')
  try {
    const res = await taskStore.claim(task.value.id)
    hideLoading()
    uni.showModal({
      title: '接收成功',
      content: `回执编号：${res.receiptNo}\n接收时间：${res.claimedAt}\n\n任务已进入「待作业」。`,
      showCancel: false,
      confirmText: '好的'
    })
    await load(task.value.id)
  } catch (e) {
    hideLoading()
    toast(errorMessage(e, '接收失败'))
  }
}

async function handleStart() {
  if (!task.value) return
  const ok = await confirmAction({
    title: '确认开始作业',
    content: `地块：${task.value.plot.plotName}\n作业：${task.value.workTypeName}\n\n开工后开始记录时长与轨迹。`,
    confirmText: '开始作业'
  })
  if (!ok) return
  showLoading('开工中')
  try {
    const order = await workStore.start(task.value.id)
    hideLoading()
    uni.showModal({
      title: '开工成功',
      content: `作业单号：${order.trajectoryInfoId}\n回执编号：${order.receiptNo || '--'}`,
      showCancel: false,
      success: () => go('/pages/work/index')
    })
  } catch (e) {
    hideLoading()
    toast(errorMessage(e, '开工失败'))
  }
}

function navigateToPlot() {
  if (!task.value) return
  uni.openLocation({
    latitude: task.value.plot.center.latitude,
    longitude: task.value.plot.center.longitude,
    name: task.value.plot.plotName,
    address: task.value.plot.address,
    fail: () => toast('当前环境不支持地图导航，可在地块地图中查看位置')
  })
}

function openMap() {
  if (!task.value) return
  go(`/pages/map/index?plotId=${task.value.plot.plotId}`)
}

function callPublisher() {
  if (!task.value) return
  uni.makePhoneCall({
    phoneNumber: task.value.contactPhone,
    fail: () => toast('当前环境不支持拨号')
  })
}

function askAgent() {
  goAgent(task.value ? `${task.value.plot.plotName}在哪里？` : '帮我看看任务详情')
}
</script>

<template>
  <view class="page-root">
    <view class="page-head">
      <view class="icon-btn" @tap="goBack('/pages/task/index')">
        <SfIcon name="back" :size="40" color="#10231b" />
      </view>
      <text class="page-title grow">任务详情</text>
      <view class="icon-btn" @tap="askAgent">
        <SfIcon name="spark" :size="38" color="#10231b" />
      </view>
    </view>

    <view v-if="loading" class="pad" style="padding-top: 32rpx">
      <view class="skeleton" style="height: 480rpx; border-radius: 44rpx" />
      <view class="skeleton" style="height: 240rpx; border-radius: 36rpx; margin-top: 28rpx" />
    </view>

    <template v-else-if="task">
      <view style="padding: 32rpx 32rpx 0">
        <view class="card" style="padding: 30rpx">
          <view class="row-between">
            <view>
              <view class="plot-name" style="font-size: 44rpx">
                {{ task.plot.plotName }}
                <text class="plot-code">{{ task.plot.plotNo }}</text>
              </view>
              <view class="task-meta">
                <text>{{ task.workTypeName }}</text>
                <text>{{ formatArea(task.area) }} 亩</text>
                <text>距 {{ task.distanceKm }} km</text>
              </view>
            </view>
            <text class="status" :class="statusClass">{{ TASK_STATUS_TEXT[task.status] }}</text>
          </view>

          <view class="task-hero-map" style="height: 380rpx; margin-top: 24rpx">
            <PlotMap
              :plot="task.plot"
              :width-rpx="622"
              :height-rpx="380"
              :legend="false"
            />
          </view>

          <view class="task-actions">
            <view class="btn btn--ghost" @tap="navigateToPlot">
              <SfIcon name="navigation" :size="34" color="#10231b" />
              <text>导航</text>
            </view>
            <view v-if="task.status === 'pending'" class="btn btn--lime grow" @tap="handleClaim">
              接收任务
            </view>
            <view v-else-if="task.status === 'claimed'" class="btn btn--primary grow" @tap="handleStart">
              <SfIcon name="play" :size="34" color="#ffffff" />
              <text>开始作业</text>
            </view>
            <view v-else-if="task.status === 'working'" class="btn btn--primary grow" @tap="go('/pages/work/index')">
              进入作业控制台
            </view>
            <view v-else-if="task.status === 'done'" class="btn btn--soft grow" @tap="go('/pages/settlement/index')">
              查看核算与结算
            </view>
            <view v-else class="btn btn--soft grow dock-btn--disabled">任务已超时</view>
          </view>
        </view>
      </view>

      <view class="section">
        <view class="section-head"><text class="section-title">地块与作业信息</text></view>
        <view class="card" style="padding: 8rpx 30rpx">
          <view class="info-row">
            <text>地块编号</text>
            <text>{{ task.plot.plotNo }}</text>
          </view>
          <view class="info-row">
            <text>所属村屯</text>
            <text>{{ task.plot.address }}</text>
          </view>
          <view class="info-row">
            <text>地块面积</text>
            <text>{{ formatArea(task.plot.area) }} 亩</text>
          </view>
          <view class="info-row">
            <text>种植作物</text>
            <text>{{ task.plot.cropName }}</text>
          </view>
          <view class="info-row">
            <text>作业类型</text>
            <text>{{ task.workTypeName }}</text>
          </view>
          <view class="info-row">
            <text>作业单价</text>
            <text>{{ task.unitPrice.toFixed(2) }} 元/亩</text>
          </view>
          <view class="info-row">
            <text>预计金额</text>
            <text>¥{{ formatMoney(amount) }}</text>
          </view>
          <view class="info-row">
            <text>作业截止</text>
            <text>{{ formatTime(task.deadline) }}</text>
          </view>
          <view class="info-row">
            <text>任务编号</text>
            <text>{{ task.taskNo }}</text>
          </view>
          <view class="info-row">
            <text>派单时间</text>
            <text>{{ relativeTime(task.publishTime) }}</text>
          </view>
          <view v-if="task.claimedAt" class="info-row">
            <text>接收时间</text>
            <text>{{ task.claimedAt }}</text>
          </view>
          <view class="info-row">
            <text>发布方</text>
            <text>{{ task.publisher }}</text>
          </view>
          <view class="info-row">
            <text>联系电话</text>
            <text style="color: var(--green-600)" @tap="callPublisher">{{ task.contactPhone }}</text>
          </view>
        </view>
        <view class="card" style="padding: 26rpx 30rpx; margin-top: 20rpx">
          <text class="tiny muted" style="font-weight: 700; letter-spacing: 0.04em">作业要求</text>
          <view style="font-size: 29rpx; margin-top: 10rpx; line-height: 1.6">{{ task.requirement }}</view>
        </view>
      </view>

      <view class="task-actions" style="padding: 0 32rpx; margin-top: 32rpx">
        <view class="btn btn--soft grow" @tap="openMap">查看地块地图</view>
      </view>

      <view class="tiny muted footnote">
        任务与地块数据按账号隔离；开始与结束作业均需二次确认，并写入操作审计。
      </view>
    </template>

    <view v-else class="card empty" style="margin: 40rpx 32rpx; padding: 60rpx 40rpx">
      <view class="empty-ico">
        <SfIcon name="alert" :size="56" color="#c47a0a" />
      </view>
      <text class="empty-title">没有找到该任务</text>
      <text class="tiny" style="display: block; margin-top: 8rpx">{{ error || '它可能不属于当前账号' }}</text>
      <view style="margin-top: 24rpx">
        <view class="btn btn--ghost btn--sm" @tap="go('/pages/task/index')">返回任务列表</view>
      </view>
    </view>
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
  margin: 40rpx 48rpx 12rpx;
  line-height: 1.7;
}
</style>
