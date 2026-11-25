<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import SfIcon from '@/components/SfIcon.vue'
import TaskCard from '@/components/TaskCard.vue'
import AppTabBar from '@/components/AppTabBar.vue'
import type { Plot, Task } from '@/types'
import type { TaskFilter } from '@/api/task'
import { fetchTaskList } from '@/api/task'
import { fetchPlots } from '@/api/user'
import { isMockEnabled } from '@/config'
import { useTaskStore } from '@/stores/task'
import { formatArea } from '@/utils/format'
import { confirmAction, errorMessage, hideLoading, showLoading, toast } from '@/utils/ui'
import { go, goBack } from '@/utils/nav'

const store = useTaskStore()
const realMode = !isMockEnabled()

type ModuleKey = 'tasks' | 'done' | 'mine'
const modules: Array<{ key: ModuleKey; label: string }> = [
  { key: 'tasks', label: '作业任务' },
  { key: 'done', label: '已收土地' },
  { key: 'mine', label: '我的土地' }
]
const moduleKey = ref<ModuleKey>('tasks')

const tabs: Array<{ scope: string; label: string; filter: TaskFilter }> = [
  { scope: 'today', label: '今日', filter: 'all' },
  { scope: 'pending', label: '待接收', filter: 'pending' },
  { scope: 'todo', label: '待作业', filter: 'claimed' },
  { scope: 'doing', label: '进行中', filter: 'working' },
  { scope: 'done', label: '已完成', filter: 'done' },
  { scope: 'timeout', label: '已超时', filter: 'timeout' }
]
const scope = ref('today')

const plots = ref<Plot[]>([])
const plotLoading = ref(false)
const donePlotIds = ref<number[]>([])

const todayKey = computed(() => {
  const d = new Date()
  const p2 = (n: number) => (n < 10 ? `0${n}` : String(n))
  return `${d.getFullYear()}-${p2(d.getMonth() + 1)}-${p2(d.getDate())}`
})

const tasks = computed<Task[]>(() => {
  if (scope.value !== 'today') return store.list
  return store.list.filter(
    (t) =>
      String(t.deadline).startsWith(todayKey.value) ||
      String(t.publishTime).startsWith(todayKey.value) ||
      t.status === 'working'
  )
})

const scopeLabel = computed(() => tabs.find((t) => t.scope === scope.value)?.label || '')
const totalArea = computed(() => plots.value.reduce((n, p) => n + Number(p.area || 0), 0))

const visiblePlots = computed<Plot[]>(() => {
  if (moduleKey.value !== 'done') return plots.value
  return plots.value.filter((p) => donePlotIds.value.includes(p.plotId))
})

const emptyTitle = computed(() => {
  const map: Record<string, string> = {
    today: '今天暂时没有任务',
    pending: '没有待接收的任务',
    todo: '没有待作业任务',
    doing: '当前没有进行中的作业',
    done: '还没有已完成的作业',
    timeout: '没有超时任务'
  }
  return map[scope.value] || '暂无数据'
})

const emptySub = computed(() => {
  const map: Record<string, string> = {
    today: '有新的作业单会在这里出现',
    pending: '任务都接完了',
    todo: '接收任务后会出现在这里',
    doing: '开工后任务会出现在这里',
    done: '完成作业后会出现在这里',
    timeout: '暂无超时任务'
  }
  return map[scope.value] || ''
})

onShow(() => {
  void reload(true)
})

async function loadTasks() {
  const filter = tabs.find((t) => t.scope === scope.value)?.filter || 'all'
  await store.fetchList(filter)
  await store.fetchCounts()
  if (moduleKey.value === 'done' && !realMode) {
    try {
      const doneList = await fetchTaskList('done')
      donePlotIds.value = doneList.map((t) => t.plot.plotId)
    } catch (e) {
      donePlotIds.value = []
    }
  }
}

async function loadPlots() {
  plotLoading.value = true
  try {
    plots.value = await fetchPlots()
    if (moduleKey.value === 'done' && !realMode) {
      const doneList = await fetchTaskList('done')
      donePlotIds.value = doneList.map((t) => t.plot.plotId)
    }
  } catch (e) {
    plots.value = []
    toast(errorMessage(e, '地块数据加载失败'))
  } finally {
    plotLoading.value = false
  }
}

async function reload(_force = false) {
  if (moduleKey.value === 'tasks') {
    await loadTasks()
  } else {
    await loadPlots()
  }
}

async function switchModule(key: ModuleKey) {
  if (moduleKey.value === key) return
  moduleKey.value = key
  if (key === 'tasks') {
    await loadTasks()
  } else {
    await loadPlots()
  }
}

function switchScope(value: string) {
  if (scope.value === value) return
  scope.value = value
  void loadTasks()
}

function openTask(t: Task) {
  go(`/pages/task/detail?id=${t.id}`)
}

function openPlot(p: Plot) {
  go(`/pages/map/index?plotId=${p.plotId}`)
}

function navigateTo(t: Task) {
  uni.openLocation({
    latitude: t.plot.center.latitude,
    longitude: t.plot.center.longitude,
    name: t.plot.plotName,
    address: t.plot.address,
    fail: () => toast('当前环境不支持地图导航')
  })
}

async function claim(t: Task) {
  const ok = await confirmAction({
    title: '确认接收任务',
    content: `地块：${t.plot.plotName}\n作业：${t.workTypeName}\n面积：${formatArea(t.area)} 亩\n\n接单后将生成认领回执。`,
    confirmText: '接收任务'
  })
  if (!ok) return
  showLoading('接单中')
  try {
    const res = await store.claim(t.id)
    hideLoading()
    toast(`接单成功 · 回执 ${res.receiptNo}`)
    await loadTasks()
    go(`/pages/task/detail?id=${t.id}`)
  } catch (e) {
    hideLoading()
    toast(errorMessage(e, '接单失败'))
  }
}
</script>

<template>
  <view class="page-root has-tabbar">
    <view class="page-head">
      <view class="icon-btn" @tap="goBack('/pages/home/index')">
        <SfIcon name="back" :size="40" color="#10231b" />
      </view>
      <text class="page-title grow">我的任务</text>
      <view class="icon-btn" @tap="reload(true)">
        <SfIcon name="refresh" :size="38" color="#10231b" />
      </view>
    </view>

    <view v-if="realMode && moduleKey === 'tasks'" class="real-notice">
      任务中心尚未接入真实后端接口，当前展示演示数据；切换演示模式后可体验接单、开工与核算全流程。
    </view>

    <view class="module-switch">
      <view
        v-for="m in modules"
        :key="m.key"
        class="module-btn"
        :class="{ 'module-btn--active': moduleKey === m.key }"
        @tap="switchModule(m.key)"
      >
        {{ m.label }}
      </view>
    </view>

    <template v-if="moduleKey === 'tasks'">
      <scroll-view class="tabs-scroll" scroll-x :show-scrollbar="false">
        <view class="tabs">
          <view
            v-for="t in tabs"
            :key="t.scope"
            class="tab"
            :class="{ 'tab--active': scope === t.scope }"
            @tap="switchScope(t.scope)"
          >
            {{ t.label }}
          </view>
        </view>
      </scroll-view>

      <view class="pad row-between" style="margin-top: 12rpx">
        <text class="small muted">{{ tasks.length }} 个任务 · 仅显示本人数据</text>
        <text class="tiny muted">{{ scopeLabel }}</text>
      </view>

      <view class="task-list" style="margin-top: 16rpx">
        <template v-if="store.loading && !tasks.length">
          <view v-for="i in 3" :key="i" class="card" style="padding: 32rpx">
            <view class="skeleton" style="height: 36rpx; width: 55%" />
            <view class="skeleton" style="height: 24rpx; width: 80%; margin-top: 18rpx" />
            <view class="skeleton" style="height: 76rpx; width: 100%; margin-top: 24rpx" />
          </view>
        </template>

        <TaskCard
          v-for="t in tasks"
          :key="t.id"
          :task="t"
          @view="openTask"
          @navigate="navigateTo"
          @claim="claim"
        />

        <view v-if="!store.loading && !tasks.length" class="card empty">
          <view class="empty-ico">
            <SfIcon name="list" :size="60" color="#1c6a4f" />
          </view>
          <text class="empty-title">{{ emptyTitle }}</text>
          <text class="tiny empty-sub">{{ emptySub }}</text>
        </view>
      </view>

      <view class="tiny muted footnote">
        接单需二次确认并生成认领回执；任务、地块与结算数据均由后端按账号鉴权返回。
      </view>
    </template>

    <template v-else>
      <view class="pad row-between" style="margin-top: 20rpx">
        <text class="small muted">
          {{ visiblePlots.length }} 块地 · {{ moduleKey === 'done' ? '已完成作业' : '本人归属地块' }}
        </text>
        <text class="tiny muted">累计面积 {{ formatArea(totalArea) }} 亩</text>
      </view>

      <view class="task-list" style="margin-top: 16rpx">
        <template v-if="plotLoading && !visiblePlots.length">
          <view v-for="i in 3" :key="i" class="card" style="padding: 32rpx">
            <view class="skeleton" style="height: 36rpx; width: 60%" />
            <view class="skeleton" style="height: 24rpx; width: 82%; margin-top: 18rpx" />
            <view class="skeleton" style="height: 92rpx; width: 100%; margin-top: 24rpx" />
          </view>
        </template>

        <view
          v-for="p in visiblePlots"
          :key="p.plotId"
          class="card plot-card"
          @tap="openPlot(p)"
        >
          <view class="row-between">
            <view class="plot-name">
              {{ p.plotName }}
              <text class="plot-code">{{ p.plotNo }}</text>
            </view>
            <text class="status" :class="moduleKey === 'done' ? 'status--done' : 'status--todo'">
              {{ moduleKey === 'done' ? '已完成' : p.cropName }}
            </text>
          </view>
          <view class="task-meta">
            <text>{{ formatArea(p.area) }} 亩</text>
            <text>{{ p.cropName }}</text>
            <text>{{ p.address }}</text>
          </view>
          <view class="plot-stats">
            <view>
              <text class="plot-stat-num">{{ formatArea(p.area) }}</text>
              <text>地块面积(亩)</text>
            </view>
            <view>
              <text class="plot-stat-num">{{ p.cropName }}</text>
              <text>种植作物</text>
            </view>
            <view>
              <text class="plot-stat-num">{{ p.boundary.length }}</text>
              <text>边界点数</text>
            </view>
          </view>
          <view class="plot-foot">
            <text>{{ p.address }}</text>
            <view class="plot-link">
              <text>查看地块</text>
              <SfIcon name="chevron" :size="26" color="#1c6a4f" />
            </view>
          </view>
        </view>

        <view v-if="!plotLoading && !visiblePlots.length" class="card empty">
          <view class="empty-ico">
            <SfIcon name="map" :size="60" color="#1c6a4f" />
          </view>
          <text class="empty-title">
            {{ moduleKey === 'done' ? '还没有已完成作业的土地' : '还没有属于您的土地' }}
          </text>
          <text class="tiny empty-sub">
            {{ realMode ? '真实模式下已完成作业统计接口尚未接入' : '完成作业后地块会出现在这里' }}
          </text>
        </view>
      </view>

      <view class="tiny muted footnote">
        土地档案由本人任务自动关联，可查看地块边界、历史作业与累计核算面积。
      </view>
    </template>

    <AppTabBar active="tasks" />
  </view>
</template>

<style scoped>
.tabs-scroll {
  width: 100%;
  white-space: nowrap;
}

.tabs-scroll .tabs {
  display: inline-flex;
  position: static;
  background: transparent;
  padding: 24rpx 36rpx 12rpx;
}

.tabs-scroll .tab {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-right: 12rpx;
  flex: none;
}

.real-notice {
  margin: 20rpx 36rpx 0;
  padding: 20rpx 24rpx;
  border-radius: 28rpx;
  background: var(--amber-soft);
  border: 2rpx solid #f7d59b;
  color: #7a4b05;
  font-size: 24rpx;
  line-height: 1.6;
}

.skeleton {
  border-radius: 16rpx;
  background: linear-gradient(90deg, #eef2e8 25%, #f7f9f1 37%, #eef2e8 63%);
  background-size: 400% 100%;
  animation: shimmer 1.4s ease infinite;
}

@keyframes shimmer {
  0% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0 50%;
  }
}

.empty {
  padding: 56rpx 40rpx;
}

.empty-ico {
  margin-bottom: 20rpx;
  display: flex;
  justify-content: center;
}

.empty-title {
  display: block;
  font-size: 30rpx;
  font-weight: 700;
  color: var(--ink-2);
}

.empty-sub {
  display: block;
  margin-top: 8rpx;
}

.footnote {
  text-align: center;
  margin: 44rpx 48rpx 12rpx;
  line-height: 1.7;
}

.plot-stat-num {
  display: block;
  font-size: 34rpx;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}
</style>
