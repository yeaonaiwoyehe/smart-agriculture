<template>
  <div class="page" v-loading="loading">
    <PageHeader
      title="实时作业监控"
      code="MP-03"
      group="业务管理"
      desc="基于农机档案与真实上报轨迹，展示有效轨迹、原始轨迹及作业时长，支持轨迹回放与作业单追溯。"
    >
      <template #actions>
        <span class="live-badge"><span class="live-dot" />真实轨迹 · 5 秒刷新</span>
        <el-button :icon="Refresh" @click="refresh">刷新</el-button>
      </template>
    </PageHeader>

    <div class="grid cols-4 compact-stats">
      <StatCard label="轨迹农机" :value="stats.tracked" unit="台" icon="Van" :foot="`农机档案共 ${stats.total} 台`" />
      <StatCard label="有效轨迹" :value="stats.records" unit="条" icon="VideoPlay" foot="按有效轨迹记录统计" />
      <StatCard label="有效作业面积" :value="stats.effectiveArea" unit="亩" icon="DataLine" foot="按有效轨迹面积汇总" />
      <StatCard label="异常轨迹数" :value="stats.abnormalCount ?? '—'" :unit="stats.abnormalCount === null ? '' : '条'" icon="Warning" tone="red" foot="当前接口未提供异常判定" />
    </div>

    <div class="monitor-grid mt-3">
      <PanelCard title="农机轨迹分布" :sub="`${monitoredMachines.length} 台有轨迹 · ${lands.length} 个地块 · 仅展示真实上报点`">
        <template #extra><span class="tag green">{{ trackMode === 'original' ? '原始' : '有效' }}轨迹 · {{ activeTrack.length }} 点</span></template>
        <FarmMap
          :lands="lands"
          :machines="monitoredMachines"
          :active-track="activeTrack"
          :play-index="playIndex"
          :anomalies="anomalies"
          :height="560"
          show-labels
          :live="nowText"
          @select-machine="selectMachine"
        />
      </PanelCard>

      <div class="side-stack">
        <PanelCard title="轨迹农机" :sub="`点击切换监控对象 · ${monitoredMachines.length} 台`" tight>
          <div class="m-list">
            <div v-if="!monitoredMachines.length" class="hint" style="padding: 8px">暂无关联有效轨迹点位的农机</div>
            <div
              v-for="m in monitoredMachines"
              :key="m.id"
              class="m-row"
              :class="{ active: m.id === selectedId }"
              @click="selectMachine(m)"
            >
              <span class="m-dot" :class="m.workStatus === '作业中' ? 'green' : 'blue'" />
              <div style="min-width: 0">
                <div class="strong" style="font-size: 12px">
                  {{ m.code }} <span class="hint" style="font-size: 10.5px">{{ m.operatorName }}</span>
                </div>
                <div class="hint" style="font-size: 10.5px">{{ m.landName || '未关联地块' }} · {{ m.pointCount }} 个轨迹点</div>
              </div>
              <StatusTag :status="m.workStatus" />
            </div>
          </div>
        </PanelCard>

        <PanelCard title="作业详情" :sub="activeWorkorder ? `作业单 ${activeWorkorder.id}` : '当前农机暂无作业单'" tight>
          <div class="sel-head">
            <span class="strong" style="font-size: 12.5px">{{ selected?.code || '—' }}</span>
            <span class="hint" style="font-size: 11px">{{ currentLand }}</span>
            <span class="tag no-dot gray" style="margin-left: auto">{{ selected?.workStatus || '—' }}</span>
          </div>
          <div class="mini-grid">
            <div class="mini-metric"><div class="v num">{{ activeWorkorder?.effectiveArea ?? '—' }}</div><div class="l">有效面积（亩）</div></div>
            <div class="mini-metric"><div class="v num">{{ activeTrack.length }}</div><div class="l">轨迹点数</div></div>
            <div class="mini-metric"><div class="v num">{{ workHours }}</div><div class="l">作业时长（h）</div></div>
            <div class="mini-metric"><div class="v num" style="font-size: 12px">{{ reportTime }}</div><div class="l">最后上报</div></div>
          </div>
          <div class="progress-line">
            <span class="hint">轨迹面积 / 地块面积</span>
            <b class="num">{{ areaCompletion === null ? '—' : `${areaCompletion}%` }}</b>
          </div>
          <el-progress :percentage="areaCompletion || 0" :stroke-width="7" :show-text="false" />
          <div class="play-bar">
            <el-button
              size="small"
              :type="playing ? 'default' : 'primary'"
              :icon="playing ? VideoPause : VideoPlay"
              :disabled="!activeTrack.length"
              @click="togglePlay"
            >{{ playing ? '暂停' : '回放' }}</el-button>
            <el-button size="small" :loading="originalLoading" :disabled="!activeWorkorder" @click="loadOriginalTrajectory">原始轨迹</el-button>
            <el-button v-if="trackMode === 'original'" size="small" @click="showEffectiveTrajectory">有效轨迹</el-button>
            <div class="seg">
              <button v-for="s in [1, 2, 4]" :key="s" :class="{ active: playSpeed === s }" @click="setSpeed(s)">{{ s }}x</button>
            </div>
            <span class="hint" style="margin-left: auto; font-size: 10.5px">{{ pointTime }}</span>
          </div>
          <el-slider v-model="playIndex" :max="sliderMax" :disabled="!activeTrack.length" size="small" />
          <div class="hint" style="font-size: 10.5px">
            轨迹点 {{ activeTrack.length ? playIndex + 1 : 0 }} / {{ activeTrack.length }} · 点位时间 {{ pointTime }}
          </div>
        </PanelCard>

        <PanelCard title="超界与异常提醒" sub="当前有效轨迹接口未返回异常判定结果" tight>
          <div class="alert-title">超界作业提醒</div>
          <div v-for="a in boundaryAlerts" :key="a.key" class="alert-row" @click="focusAlert(a)">
            <span class="alert-dot red" />
            <div style="min-width: 0">
              <div class="strong" style="font-size: 11.5px">{{ a.landName }} · {{ machineLabel(a) }}</div>
              <div class="hint" style="font-size: 10.5px">{{ a.at?.slice(11) }} · 越界扣除 {{ a.area }} 亩</div>
            </div>
          </div>
          <div v-if="!boundaryAlerts.length" class="hint" style="font-size: 11px">当前接口未提供超界判定，不能据此认定无超界</div>
          <div class="alert-title mt-2">异常轨迹</div>
          <div v-for="w in abnormalOrders" :key="w.id" class="alert-row" @click="focusOrder(w)">
            <span class="alert-dot amber" />
            <div style="min-width: 0">
              <div class="strong" style="font-size: 11.5px">{{ w.landName }} · {{ machineLabel(w) }}</div>
              <div class="hint" style="font-size: 10.5px">{{ w.id }} · 差异 {{ w.diffRate }}%</div>
            </div>
          </div>
          <div v-if="!abnormalOrders.length" class="hint" style="font-size: 11px">当前接口未提供异常判定，不能据此认定无异常</div>
          <el-button text size="small" style="padding: 4px 0 0" @click="goWorkorders">查看全部作业单 →</el-button>
        </PanelCard>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh, VideoPause, VideoPlay } from '@element-plus/icons-vue'
import { listMachines, listLands, listWorkorders, getOriginalTrajectory } from '@/services/api'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import PanelCard from '@/components/PanelCard.vue'
import StatusTag from '@/components/StatusTag.vue'
import FarmMap from '@/components/FarmMap.vue'

const router = useRouter()
const loading = ref(false)
const machines = ref([])
const lands = ref([])
const workorders = ref([])
const selectedId = ref('')
const selectedOrderId = ref('')
const playIndex = ref(0)
const playing = ref(false)
const playSpeed = ref(1)
const nowText = ref('等待数据')
const trackMode = ref('effective')
const originalTrajectory = ref({ track: [], trackTimes: [] })
const originalLoading = ref(false)
let tickTimer = null
let playTimer = null
let requestPending = false
let loadedOnce = false

const pad = (n) => String(n).padStart(2, '0')
const fmtTime = (d) => `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`

function timeValue(value) {
  if (!value) return 0
  const text = String(value)
  const time = new Date(text.includes('T') ? text : text.replace(/-/g, '/')).getTime()
  return Number.isFinite(time) ? time : 0
}
function displayTime(value) {
  if (!value) return '—'
  const text = String(value).replace('T', ' ')
  return text.length >= 19 ? text.slice(11, 19) : text
}
function ordersForMachine(machine) {
  if (!machine) return []
  return workorders.value.filter((w) =>
    (w.machineId && w.machineId === machine.id) ||
    (w.phoneNumber && w.phoneNumber === machine.phone)
  )
}
function latestOrderFor(machine) {
  return [...ordersForMachine(machine)].sort((a, b) => {
    const aTimes = a.trackTimes?.filter(Boolean) || []
    const bTimes = b.trackTimes?.filter(Boolean) || []
    return timeValue(bTimes[bTimes.length - 1] || b.endAt || b.startAt) -
      timeValue(aTimes[aTimes.length - 1] || a.endAt || a.startAt)
  })[0] || null
}

const monitoredMachines = computed(() => machines.value.map((m) => {
  const latest = latestOrderFor(m)
  const lastPoint = latest?.track?.[latest.track.length - 1]
  if (!latest || !lastPoint) return null
  const times = latest.trackTimes?.filter(Boolean) || []
  return {
    ...m,
    // 在线状态无真实来源；置为未知，地图不展示在线脉冲。
    online: null,
    workStatus: '轨迹已回传',
    speed: null,
    todayArea: null,
    landId: latest.landId,
    landName: latest.landName,
    pointCount: latest.pointCount,
    lastActive: times[times.length - 1] || latest.endAt || latest.startAt || '',
    pos: lastPoint,
    lnglat: lastPoint
  }
}).filter(Boolean))
const selected = computed(() => monitoredMachines.value.find((m) => m.id === selectedId.value) || monitoredMachines.value[0] || null)
const activeWorkorder = computed(() => {
  const picked = selectedOrderId.value ? workorders.value.find((w) => w.id === selectedOrderId.value) : null
  return picked || latestOrderFor(selected.value)
})
const activeTrack = computed(() => trackMode.value === 'original'
  ? originalTrajectory.value.track
  : activeWorkorder.value?.track || [])
const activeTrackTimes = computed(() => trackMode.value === 'original'
  ? originalTrajectory.value.trackTimes
  : activeWorkorder.value?.trackTimes || [])
const anomalies = computed(() => activeWorkorder.value?.anomalies || [])
const sliderMax = computed(() => Math.max(0, activeTrack.value.length - 1))
const areaCompletion = computed(() => {
  const wo = activeWorkorder.value
  if (!wo || wo.landArea === null || wo.effectiveArea === null) return null
  const landArea = Number(wo.landArea)
  const effectiveArea = Number(wo.effectiveArea)
  if (!Number.isFinite(landArea) || landArea <= 0 || !Number.isFinite(effectiveArea)) return null
  return Math.min(100, Math.max(0, Math.round((effectiveArea / landArea) * 100)))
})
const currentLand = computed(() => activeWorkorder.value?.landName || selected.value?.landName || '暂无作业地块')
const reportTime = computed(() => displayTime(selected.value?.lastActive))
const workHours = computed(() => {
  const duration = activeWorkorder.value?.duration
  return duration === null || duration === undefined || !Number.isFinite(Number(duration))
    ? '—'
    : (Number(duration) / 60).toFixed(1)
})
const pointTime = computed(() => displayTime(activeTrackTimes.value[playIndex.value]))
const stats = computed(() => {
  const anomaliesCount = workorders.value.reduce((s, w) => s + (w.anomalies?.length || 0), 0)
  return {
    total: machines.value.length,
    tracked: monitoredMachines.value.length,
    records: workorders.value.length,
    effectiveArea: +workorders.value.reduce((s, w) => s + (Number(w.effectiveArea) || 0), 0).toFixed(1),
    abnormalCount: anomaliesCount || null
  }
})
const boundaryAlerts = computed(() => workorders.value.flatMap((w) => (w.anomalies || [])
  .filter((a) => a.type.includes('超出地块边界'))
  .map((a) => ({ key: `${w.id}-${a.point}`, landName: w.landName, machineId: w.machineId, phoneNumber: w.phoneNumber, workorderId: w.id, at: a.at, area: a.area, point: a.point }))))

function machineFor(workorder) {
  return machines.value.find((m) => workorder?.machineId && m.id === workorder.machineId)
    || machines.value.find((m) => workorder?.phoneNumber && m.phone === workorder.phoneNumber)
    || null
}
function machineLabel(workorder) {
  return machineFor(workorder)?.code || '未关联农机'
}
const abnormalOrders = computed(() => workorders.value.filter((w) => w.status === '异常' || (w.anomalies?.length || 0) > 1))

async function load(showTip = false, silent = false) {
  if (requestPending) return
  requestPending = true
  if (!silent) loading.value = true
  try {
    const [ms, ls, ws] = await Promise.all([listMachines(), listLands(), listWorkorders()])
    const rows = ws.map((w) => {
      const land = ls.find((item) => item.id === w.landId)
      return land
        ? { ...w, landName: land.name || w.landName, district: land.district || w.district, landArea: land.area, landPoints: land.points || [] }
        : w
    })
    machines.value = ms
    lands.value = ls
    workorders.value = rows
    if (selectedOrderId.value && !rows.some((w) => w.id === selectedOrderId.value)) selectedOrderId.value = ''
    if (!monitoredMachines.value.some((m) => m.id === selectedId.value)) {
      selectedId.value = monitoredMachines.value[0]?.id || ''
      selectedOrderId.value = ''
    }
    if (!loadedOnce) {
      playIndex.value = sliderMax.value
      loadedOnce = true
    } else {
      playIndex.value = Math.min(playIndex.value, sliderMax.value)
    }
    nowText.value = `${fmtTime(new Date())} 更新`
    if (showTip) ElMessage.success('实时监控数据已刷新')
  } catch (error) {
    nowText.value = '刷新失败'
    if (!silent || showTip) {
      ElMessage.error('实时监控数据加载失败：' + (error?.payload?.msg || error?.msg || error?.message || '未知错误'))
    }
  } finally {
    requestPending = false
    if (!silent) loading.value = false
  }
}

function refresh() {
  load(true)
}

async function loadOriginalTrajectory() {
  const order = activeWorkorder.value
  if (!order?.phoneNumber || !order.startAt || !order.endAt) {
    ElMessage.warning('当前轨迹缺少手机号、开始时间或结束时间，无法查询原始轨迹')
    return
  }
  if (originalLoading.value) return
  originalLoading.value = true
  stopPlay()
  try {
    const result = await getOriginalTrajectory({
      phone: order.phoneNumber,
      startTime: order.startAt,
      endTime: order.endAt
    })
    if (activeWorkorder.value?.id !== order.id) return
    originalTrajectory.value = result
    trackMode.value = 'original'
    playIndex.value = 0
    if (result.track.length) ElMessage.success(`已加载 ${result.track.length} 个原始轨迹点`)
    else ElMessage.info('该时段没有查询到原始轨迹点')
  } catch (error) {
    ElMessage.error('原始轨迹加载失败：' + (error?.payload?.msg || error?.msg || error?.message || '未知错误'))
  } finally {
    originalLoading.value = false
  }
}

function showEffectiveTrajectory() {
  stopPlay()
  trackMode.value = 'effective'
  playIndex.value = sliderMax.value
}

function selectMachine(m) {
  if (!m) return
  selectedId.value = m.id
  selectedOrderId.value = ''
}

function stopPlay() {
  playing.value = false
  if (playTimer) {
    clearInterval(playTimer)
    playTimer = null
  }
}

function startPlay() {
  if (!activeTrack.value.length) {
    ElMessage.warning('当前农机暂无轨迹数据')
    return
  }
  if (playIndex.value >= sliderMax.value) playIndex.value = 0
  playing.value = true
  playTimer = setInterval(() => {
    if (playIndex.value >= sliderMax.value) {
      stopPlay()
      return
    }
    playIndex.value += 1
  }, 560 / playSpeed.value)
}

function togglePlay() {
  if (playing.value) stopPlay()
  else startPlay()
}

function setSpeed(s) {
  playSpeed.value = s
  if (playing.value) {
    stopPlay()
    startPlay()
  }
}

function focusAlert(a) {
  selectedOrderId.value = ''
  const machine = machineFor(a)
  if (machine) selectedId.value = machine.id
  selectedOrderId.value = a.workorderId
  nextTick(() => {
    playIndex.value = Math.min(a.point ?? 0, sliderMax.value)
  })
  ElMessage.success(`已定位 ${machineLabel(a)} · ${a.landName} 的超界点位`)
}

function focusOrder(w) {
  selectedOrderId.value = ''
  const machine = machineFor(w)
  if (machine) selectedId.value = machine.id
  selectedOrderId.value = w.id
  ElMessage.info(`已打开 ${w.id} 的轨迹回放（${machineLabel(w)}）`)
}

function goWorkorders() {
  ElMessage.info('正在打开作业单详情')
  router.push('/workorders')
}

function startRealtime() {
  tickTimer = setInterval(() => load(false, true), 5000)
}

watch(() => activeWorkorder.value?.id, () => {
  stopPlay()
  trackMode.value = 'effective'
  originalTrajectory.value = { track: [], trackTimes: [] }
  playIndex.value = sliderMax.value
})

onMounted(() => {
  load()
  startRealtime()
})
onBeforeUnmount(() => {
  if (tickTimer) clearInterval(tickTimer)
  stopPlay()
})
</script>

<style scoped>
.live-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 26px;
  padding: 0 10px;
  border-radius: 7px;
  background: var(--brand-100);
  color: var(--brand-700);
  font-size: 11.5px;
  font-weight: 600;
}
.live-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #1fae6b;
  animation: live-pulse 1.8s infinite;
}
@keyframes live-pulse {
  0%,
  100% {
    box-shadow: 0 0 0 3px rgba(31, 174, 107, 0.18);
  }
  50% {
    box-shadow: 0 0 0 6px rgba(31, 174, 107, 0.05);
  }
}
.compact-stats :deep(.stat-card) {
  padding: 12px 14px;
}
.compact-stats :deep(.stat-value) {
  font-size: 23px;
  margin-top: 6px;
}
.compact-stats :deep(.stat-foot) {
  margin-top: 4px;
}
.monitor-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 14px;
  align-items: start;
}
.side-stack {
  display: flex;
  flex-direction: column;
  gap: 14px;
  max-height: 626px;
  overflow-y: auto;
  padding-right: 2px;
}
.m-list {
  max-height: 190px;
  overflow-y: auto;
}
.m-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 8px;
  border-radius: 8px;
  cursor: pointer;
  border: 1px solid transparent;
}
.m-row:hover {
  background: var(--surface-2);
}
.m-row.active {
  background: var(--brand-50);
  border-color: #cdeadd;
}
.m-row .tag {
  margin-left: auto;
  flex: none;
}
.m-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex: none;
  background: var(--brand-500);
}
.m-dot.blue {
  background: var(--blue-500);
}
.sel-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.mini-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}
.mini-grid .mini-metric {
  padding: 8px 10px;
}
.mini-grid .mini-metric .v {
  font-size: 16px;
}
.progress-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 10px 0 4px;
  font-size: 11.5px;
}
.play-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
}
.alert-title {
  font-size: 11px;
  font-weight: 650;
  color: var(--text-2);
  padding: 2px 0 4px;
}
.alert-row {
  display: flex;
  align-items: flex-start;
  gap: 7px;
  padding: 6px 7px;
  border-radius: 7px;
  cursor: pointer;
}
.alert-row:hover {
  background: var(--surface-2);
}
.alert-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  margin-top: 5px;
  flex: none;
  background: var(--red-500);
}
.alert-dot.amber {
  background: var(--amber-500);
}
</style>
