<script setup lang="ts">
import { computed, onUnmounted, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import SfIcon from '@/components/SfIcon.vue'
import PlotMap from '@/components/PlotMap.vue'
import AppTabBar from '@/components/AppTabBar.vue'
import AppDock from '@/components/AppDock.vue'
import type { FeedbackItem, Plot, Settlement, Task } from '@/types'
import type { SettlementSummary } from '@/mock/settlement'
import type { TaskCounts } from '@/mock/task'
import type { WorkStats } from '@/mock/work'
import { fetchTodayTasks, fetchTaskCounts } from '@/api/task'
import { fetchWorkStats } from '@/api/work'
import { fetchSettlementList, fetchSettlementSummary } from '@/api/settlement'
import { fetchFeedbackList } from '@/api/exception'
import { fetchPlots } from '@/api/user'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import { useWorkStore } from '@/stores/work'
import { formatArea, formatMoney, formatDuration, formatTime } from '@/utils/format'
import { confirmAction, errorMessage, hideLoading, showLoading, toast } from '@/utils/ui'
import { goAgent, go } from '@/utils/nav'

const app = useAppStore()
const user = useUserStore()
const work = useWorkStore()

const tasks = ref<Task[]>([])
const plots = ref<Plot[]>([])
const counts = ref<TaskCounts>({ pending: 0, claimed: 0, working: 0, done: 0, timeout: 0 })
const workStats = ref<WorkStats>({ todayArea: 0, todayOrders: 0, monthArea: 0 })
const summary = ref<SettlementSummary | null>(null)
const settlements = ref<Settlement[]>([])
const tickets = ref<FeedbackItem[]>([])
const nowTs = ref(Date.now())
const loading = ref(false)
const error = ref('')
let timer: ReturnType<typeof setInterval> | null = null

const isWorking = computed(() => work.current?.status === 1)
const currentTask = computed(() => tasks.value.find((t) => t.status === 'claimed') || tasks.value[0] || null)
const currentPlot = computed<Plot | null>(() => {
  const id = work.current?.plotId || currentTask.value?.plot.plotId
  return plots.value.find((p) => p.plotId === id) || null
})
const pendingTicket = computed(() => tickets.value.find((t) => t.status !== 'resolved') || null)
const lastPaid = computed(() => settlements.value.find((s) => s.status === 'paid') || null)
const elapsed = computed(() => {
  if (!work.current || work.current.status !== 1) return 0
  return Math.max(0, (nowTs.value - new Date(work.current.startTime.replace(' ', 'T')).getTime()) / 1000)
})

const todayLabel = computed(() => {
  const d = new Date(nowTs.value)
  const week = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][d.getDay()]
  return `${d.getMonth() + 1} 月 ${d.getDate()} 日 · ${week}`
})

const greetingText = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '凌晨好'
  if (h < 11) return '早上好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const totalTasks = computed(() => Object.values(counts.value).reduce((a, b) => a + b, 0))
const heroArea = computed(() => (isWorking.value ? work.current?.area || 0 : workStats.value.todayArea))

const quick = [
  { label: '今天有啥活', q: '今天有什么任务？' },
  { label: '导航到地块', q: '帮我导航到作业地块' },
  { label: '查询作业面积', q: '查询我的作业面积' },
  { label: '这单为什么少算', q: '这单为什么少算了？' },
  { label: '上报异常', q: '我要上报异常' }
]

onShow(() => {
  void load()
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    const jobs: Array<Promise<any>> = [
      work.refresh(),
      // 后端暂无任务接口，真实模式下保持空数据，不阻塞首页其他模块
      fetchTodayTasks()
        .then((v) => (tasks.value = v))
        .catch(() => (tasks.value = [])),
      fetchTaskCounts()
        .then((v) => (counts.value = v))
        .catch(() => (counts.value = { pending: 0, claimed: 0, working: 0, done: 0, timeout: 0 })),
      fetchWorkStats().then((v) => (workStats.value = v)),
      fetchSettlementSummary().then((v) => (summary.value = v)),
      fetchSettlementList().then((v) => (settlements.value = v)),
      fetchFeedbackList().then((v) => (tickets.value = v)),
      fetchPlots().then((v) => (plots.value = v))
    ]
    await Promise.all(jobs)
    if (!user.profile) await user.loadProfile()
    await app.refreshUnread()
    startTick()
  } catch (e) {
    error.value = errorMessage(e, '数据加载失败')
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

async function handleStart() {
  const task = currentTask.value
  if (!task) {
    go('/pages/task/index')
    return
  }
  if (isWorking.value) {
    go('/pages/work/index')
    return
  }
  const ok = await confirmAction({
    title: '确认开始作业',
    content: `地块：${task.plot.plotName}\n作业：${task.workTypeName}\n面积：${task.area} 亩\n单价：${task.unitPrice} 元/亩`,
    confirmText: '开始作业'
  })
  if (!ok) return
  showLoading('开工中')
  try {
    await app.requestGps()
    const order = await work.start(task.id)
    hideLoading()
    toast('开工成功')
    uni.showModal({
      title: '开工成功',
      content: `作业单号：${order.trajectoryInfoId}\n回执编号：${order.receiptNo || '--'}`,
      showCancel: false,
      success: () => go('/pages/work/index')
    })
    void load()
  } catch (e) {
    hideLoading()
    toast(errorMessage(e, '开工失败'))
  }
}

async function handleFinish() {
  if (!work.current || work.current.status !== 1) return
  const ok = await confirmAction({
    title: '确认结束作业',
    content: `地块：${work.current.plotName}\n已作业：${formatDuration(elapsed.value)}\n\n结束后进入面积核算。`,
    confirmText: '结束作业',
    danger: true
  })
  if (!ok) return
  showLoading('提交中')
  try {
    const finished = await work.finish(work.current.trajectoryInfoId)
    uni.showLoading({ title: '面积核算中', mask: true })
    await new Promise((r) => setTimeout(r, 1500))
    const settled = await work.settle(finished.trajectoryInfoId)
    hideLoading()
    uni.showModal({
      title: '作业核算完成',
      content: `面积：${formatArea(settled.area)} 亩\n金额：${formatMoney(settled.amount)} 元\n\n已写入结算单。`,
      showCancel: false
    })
    void load()
  } catch (e) {
    hideLoading()
    toast(errorMessage(e, '结束作业失败'))
  }
}

function openTask(id: string) {
  uni.navigateTo({ url: `/pages/task/detail?id=${id}` })
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

function openSync() {
  toast(app.online ? '作业数据已同步' : '离线作业中，联网后自动上传')
}
</script>

<template>
  <view class="page-root has-dock">
    <view class="head head--home">
      <view class="head-top">
        <view class="head-greet">
          <text class="head-date">{{ todayLabel }}</text>
          <text class="head-title">{{ greetingText }}，{{ user.displayName }}</text>
          <view class="head-machine">
            <SfIcon name="truck" :size="26" color="rgba(255,255,255,.55)" />
            <text class="head-machine-text">{{ user.profile?.deptName || '未分配作业队' }}</text>
            <text class="dot-sep">·</text>
            <text>{{ user.phone }}</text>
          </view>
        </view>
        <view class="avatar" @tap="go('/pages/mine/index')">
          {{ (user.profile?.nickName || '农').slice(0, 1) }}
        </view>
      </view>

      <view class="head-tools">
        <view class="head-ai" @tap="goAgent()">
          <SfIcon name="spark" :size="28" color="#082a21" />
          <text>AI 助手</text>
        </view>
        <view class="status-pill" :class="{ 'status-pill--off': !app.online }" @tap="openSync">
          <view class="dot" />
          <text>{{ app.online ? '数据已同步' : '离线作业' }}</text>
        </view>
      </view>
    </view>

    <view class="home-hero">
      <view class="hero-card">
        <view class="row-between">
          <view class="hero-status">
            <view
              class="sync-dot"
              :class="{ 'sync-dot--pending': !isWorking && currentTask, 'sync-dot--off': !app.online }"
            />
            <text>{{ isWorking ? '作业中' : currentTask ? '待作业' : '今日空闲' }}</text>
          </view>
          <text class="tiny muted">{{ isWorking ? 'GPS 轨迹采集中' : '今日作业' }}</text>
        </view>

        <view v-if="isWorking && work.current" class="hero-task">
          {{ work.current.plotName }}
          <text class="hero-task-code">{{ work.current.trajectoryInfoId }}</text>
        </view>
        <view v-else-if="currentTask" class="hero-task">
          {{ currentTask.plot.plotName }}
          <text class="hero-task-code">{{ currentTask.plot.plotNo }}</text>
        </view>
        <view v-else class="hero-task" style="font-size: 40rpx">今天暂时没有任务</view>

        <view class="row-between" style="margin-top: 18rpx">
          <view class="hero-meta" style="margin-top: 0">
            <template v-if="isWorking && work.current">
              <text>{{ work.current.workTypeName }}</text>
              <text>轨迹 <text class="hero-meta-strong">{{ work.current.totalPoints }}</text> 点</text>
            </template>
            <template v-else-if="currentTask">
              <text>{{ currentTask.workTypeName }}</text>
              <text>{{ currentTask.area }} 亩</text>
              <text>距 {{ currentTask.distanceKm }} km</text>
              <text>截止 <text class="hero-meta-strong">{{ formatTime(currentTask.deadline) }}</text></text>
            </template>
            <template v-else>
              <text>有新的派单会在这里出现</text>
            </template>
          </view>
          <view v-if="isWorking" class="timer-pill">
            <SfIcon name="clock" :size="30" color="#c8f45a" />
            <text>{{ formatDuration(elapsed) }}</text>
          </view>
        </view>

        <view class="stats">
          <view class="stat">
            <view class="stat-num">
              {{ formatArea(heroArea) }}<text class="stat-num-unit">亩</text>
            </view>
            <view class="stat-label">{{ isWorking ? '已核算' : '今日作业' }}</view>
          </view>
          <view class="stat">
            <view class="stat-num">
              {{ counts.done }}<text class="stat-num-unit">/{{ totalTasks }}</text>
            </view>
            <view class="stat-label">完成 / 今日任务</view>
          </view>
          <view class="stat">
            <view class="stat-num" :style="{ color: counts.pending > 0 ? '#b8730a' : '' }">
              {{ counts.pending }}<text class="stat-num-unit">个</text>
            </view>
            <view class="stat-label">待接收</view>
          </view>
        </view>
      </view>
    </view>

    <view v-if="error" class="pad" style="margin-top: 20rpx">
      <view class="card" style="padding: 24rpx; color: var(--red); font-size: 24rpx">
        {{ error }}
      </view>
    </view>

    <view class="section-head" style="margin: 40rpx 36rpx 16rpx">
      <text class="section-title">有问题，直接说</text>
      <view class="section-more" @tap="goAgent()">
        <SfIcon name="spark" :size="26" color="#14513d" />
        <text>进入助手</text>
        <SfIcon name="chevron" :size="26" color="#14513d" />
      </view>
    </view>
    <scroll-view class="quick-scroll" scroll-x>
      <view class="quick-chip quick-chip--voice" @tap="goAgent('打开语音助手')">
        <SfIcon name="mic" :size="34" color="#c8f45a" />
        <text>语音说</text>
      </view>
      <view v-for="q in quick" :key="q.label" class="quick-chip" @tap="goAgent(q.q)">
        {{ q.label }}
      </view>
    </scroll-view>

    <view class="section">
      <view class="section-head">
        <text class="section-title">当前作业</text>
        <view class="section-more" @tap="go(isWorking ? '/pages/work/index' : '/pages/task/index')">
          <text>{{ isWorking ? '作业控制台' : '全部任务' }}</text>
          <SfIcon name="chevron" :size="26" color="#14513d" />
        </view>
      </view>

      <view v-if="isWorking && work.current" class="card task-hero">
        <view class="row-between">
          <view>
            <view class="plot-name">
              {{ work.current.plotName }}
              <text class="plot-code">{{ work.current.plotId }}</text>
            </view>
            <view class="task-meta">
              <text>{{ work.current.workTypeName }}</text>
              <text>开始 {{ formatTime(work.current.startTime) }}</text>
              <text class="row gap-4" style="color: #16794a">
                <view class="sync-dot" style="width: 14rpx; height: 14rpx" />
                定位正常
              </text>
            </view>
          </view>
          <text class="status status--doing">作业中</text>
        </view>
        <view class="task-hero-map">
          <PlotMap
            :plot="currentPlot"
            :trajectory="work.current.trajectory"
            :executed="work.current.trajectory"
            :width-rpx="614"
            :height-rpx="232"
            :legend="false"
          />
        </view>
        <view class="task-actions">
          <view class="btn btn--primary grow" @tap="go('/pages/work/index')">
            <SfIcon name="satellite" :size="34" color="#ffffff" />
            <text>进入控制台</text>
          </view>
          <view class="btn btn--ghost" @tap="navigateToPlot">
            <SfIcon name="navigation" :size="34" color="#10231b" />
            <text>导航</text>
          </view>
        </view>
      </view>

      <view v-else-if="currentTask" class="card task-hero">
        <view class="row-between">
          <view class="grow">
            <view class="plot-name">
              {{ currentTask.plot.plotName }}
              <text class="plot-code">{{ currentTask.plot.plotNo }}</text>
            </view>
            <view class="task-meta">
              <text>{{ currentTask.workTypeName }}</text>
              <text>{{ currentTask.area }} 亩</text>
              <text>{{ currentTask.unitPrice }} 元/亩</text>
            </view>
          </view>
          <text class="status" :class="currentTask.status === 'claimed' ? 'status--todo' : 'status--pending'">
            {{ currentTask.status === 'claimed' ? '已接收' : '待接收' }}
          </text>
        </view>
        <view class="task-hero-map">
          <PlotMap
            :plot="currentTask.plot"
            :width-rpx="614"
            :height-rpx="232"
            :legend="false"
          />
        </view>
        <view class="task-actions">
          <view class="btn btn--primary grow" @tap="openTask(currentTask.id)">查看任务详情</view>
          <view class="btn btn--ghost" @tap="navigateToPlot">
            <SfIcon name="navigation" :size="34" color="#10231b" />
            <text>导航</text>
          </view>
        </view>
      </view>

      <view v-else class="card empty" style="padding: 60rpx 40rpx">
        <view class="empty-ico">
          <SfIcon name="leaf" :size="60" color="#1c6a4f" />
        </view>
        <text>今天暂时没有派给您的任务</text>
        <text class="tiny" style="display: block; margin-top: 8rpx">
          有新的作业单，助手会第一时间提醒您。
        </text>
      </view>
    </view>

    <view v-if="pendingTicket" class="section">
      <view class="alert-banner" @tap="go('/pages/exception/index')">
        <view class="icon-wrap">
          <SfIcon name="alert" :size="38" color="#c47a0a" />
        </view>
        <view class="grow">
          <text class="alert-banner-title">{{ pendingTicket.typeName }}</text>
          <text class="tiny alert-banner-sub">
            工单 {{ pendingTicket.receiptNo }} · 点击查看处理进度
          </text>
        </view>
        <SfIcon name="chevron" :size="34" color="#7a4b05" />
      </view>
    </view>

    <view class="section">
      <view class="settle-strip" @tap="go('/pages/settlement/index')">
        <view class="grow">
          <text class="settle-label">本月待结算</text>
          <view class="settle-amount">
            ¥{{ formatMoney(summary?.pendingAmount || 0) }}
          </view>
          <text class="settle-sub">
            {{ formatArea(summary?.totalArea || 0) }} 亩 · {{ settlements.filter((s) => s.status === 'pending').length }} 个结算单
          </text>
        </view>
        <view style="text-align: right">
          <text class="settle-last">上月已到账</text>
          <text class="settle-last-amount">¥{{ formatMoney(lastPaid?.totalAmount || 0) }}</text>
          <view class="settle-more">
            <text>查看明细</text>
            <SfIcon name="chevron" :size="22" color="#c8f45a" />
          </view>
        </view>
      </view>
    </view>

    <view class="section">
      <view class="func-grid">
        <view class="func-tile" @tap="go('/pages/task/index')">
          <view class="ico"><SfIcon name="list" :size="42" color="#14513d" /></view>
          <text>我的任务</text>
        </view>
        <view class="func-tile func-tile--lime" @tap="go('/pages/settlement/index')">
          <view class="ico"><SfIcon name="wallet" :size="42" color="#0e3b2e" /></view>
          <text>结算中心</text>
        </view>
        <view class="func-tile" @tap="go('/pages/exception/index')">
          <view class="ico" style="position: relative">
            <SfIcon name="alert" :size="42" color="#14513d" />
            <text v-if="tickets.filter((t) => t.status !== 'resolved').length" class="func-badge">
              {{ tickets.filter((t) => t.status !== 'resolved').length }}
            </text>
          </view>
          <text>异常上报</text>
        </view>
        <view class="func-tile" @tap="go('/pages/mine/help')">
          <view class="ico"><SfIcon name="help" :size="42" color="#14513d" /></view>
          <text>使用帮助</text>
        </view>
      </view>
    </view>

    <view class="section" style="margin-bottom: 10rpx">
      <view class="card sync-card" @tap="openSync">
        <view class="sync-dot" :class="{ 'sync-dot--off': !app.online }" />
        <view class="grow">
          <text class="sync-title">{{ app.online ? '作业数据已同步' : '离线作业中' }}</text>
          <text class="tiny muted sync-sub">
            {{ app.online ? '定位与作业数据由平台统一托管' : '轨迹缓存于本机，联网后自动上传' }}
          </text>
        </view>
        <SfIcon :name="app.online ? 'sync' : 'cloud-off'" :size="40" color="#1c6a4f" />
      </view>
    </view>

    <AppDock
      :running="isWorking"
      :can-start="!!currentTask"
      @start="handleStart"
      @end="handleFinish"
    />
    <AppTabBar active="home" :running="isWorking" />
  </view>
</template>

<style scoped>
.head--home {
  padding-bottom: 56rpx;
}

.head-top {
  align-items: flex-start;
}

.head-greet {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.head-greet .head-title {
  font-size: 46rpx;
  line-height: 1.3;
  margin-top: 6rpx;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.head-greet .head-date {
  letter-spacing: 0.06em;
}

.head-machine {
  display: flex;
  align-items: center;
  gap: 8rpx;
  margin-top: 14rpx;
  color: rgba(255, 255, 255, 0.58);
  font-size: 23rpx;
  white-space: nowrap;
  overflow: hidden;
}

.head-machine-text {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dot-sep {
  opacity: 0.4;
}

.head-top .avatar {
  margin-top: 4rpx;
  flex: none;
}

.head-tools {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 16rpx;
  margin-top: 30rpx;
  flex-wrap: nowrap;
}

.head-tools .head-ai {
  flex: 0 0 auto;
  height: 60rpx;
  padding: 0 24rpx 0 16rpx;
  font-size: 24rpx;
  letter-spacing: 0.02em;
  white-space: nowrap;
  box-shadow: 0 8rpx 20rpx rgba(200, 244, 90, 0.18);
}

.head-tools .status-pill {
  flex: 0 0 auto;
  height: 54rpx;
  padding: 0 22rpx;
  font-size: 23rpx;
  white-space: nowrap;
}

.head-ai text,
.status-pill text {
  flex: 0 0 auto;
  white-space: nowrap;
}

.empty-ico {
  margin: 0 auto 20rpx;
  display: flex;
  justify-content: center;
}

.alert-banner-title {
  display: block;
  font-size: 28rpx;
  font-weight: 700;
}

.alert-banner-sub {
  display: block;
  margin-top: 4rpx;
  font-weight: 500;
  opacity: 0.8;
}

.settle-label {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.6);
  letter-spacing: 0.04em;
}

.settle-sub {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.55);
  margin-top: 6rpx;
  display: block;
}

.settle-last {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.55);
}

.settle-last-amount {
  display: block;
  font-size: 34rpx;
  font-weight: 800;
  margin-top: 4rpx;
}

.settle-more {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 6rpx;
  margin-top: 12rpx;
  font-size: 22rpx;
  color: var(--lime);
}

.sync-title {
  display: block;
  font-size: 28rpx;
  font-weight: 700;
}

.sync-sub {
  display: block;
  margin-top: 2rpx;
}
</style>
