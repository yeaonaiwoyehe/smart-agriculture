<template>
  <div class="page" v-loading="loading">
    <PageHeader title="轨迹与面积核算" code="MP-03" group="业务管理" desc="轨迹裁剪 · 有效面积 · 异常可追溯：基于 GPS 轨迹、地块边界与核算规则生成作业单，异常点位可定位回放并提交人工复核。">
      <template #actions>
        <el-button :icon="Download" @click="exportCsv">导出核算清单</el-button>
        <el-button type="primary" :icon="MagicStick" @click="openAiReview">异常研判</el-button>
      </template>
    </PageHeader>

    <div class="grid cols-4">
      <StatCard label="作业单总数" :value="stats.total" unit="单" icon="Tickets" :foot="`覆盖 ${displayDistrictCount} 个片区`" />
      <StatCard label="未终审轨迹" :value="stats.pending" unit="单" icon="DataLine" tone="amber" foot="无终审结果的有效轨迹数" />
      <StatCard label="待队长初核" :value="stats.review" unit="单" icon="View" tone="blue" foot="含待初核与已通过初核的复核申请" />
      <StatCard label="已驳回复核" :value="stats.abnormal" unit="单" icon="Warning" tone="red" foot="含初核驳回与终审驳回" />
    </div>

    <div class="card compact-stats mt-3">
      <div class="cs-item"><span class="l">轨迹面积合计</span><b class="num">{{ stats.effective }} <small>亩</small></b></div>
      <div class="cs-item"><span class="l">作业时长合计</span><b class="num">{{ formatTotalDuration }} <small>h</small></b></div>
      <div class="cs-item"><span class="l">复核覆盖率</span><b class="num">{{ reviewCoverage }}</b></div>
    </div>

    <div class="filter-bar mt-3">
      <el-select v-model="statusFilter" size="small" style="width: 122px">
        <el-option v-for="s in statusOptions" :key="s" :label="s === '全部' ? '全部状态' : s" :value="s" />
      </el-select>
      <el-select v-model="districtFilter" size="small" style="width: 122px">
        <el-option label="全部片区" value="全部" />
        <el-option v-for="d in districtOptions" :key="d" :label="d" :value="d" />
      </el-select>
      <el-input v-model="keyword" size="small" clearable placeholder="作业单号 / 地块 / 机手" style="width: 208px"><template #prefix><el-icon><Search /></el-icon></template></el-input>
      <span class="filter-hint grow">共 {{ filtered.length }} 单 · 已驳回复核 {{ filtered.filter((w) => reviewFor(w.id)?.status === 2 || reviewFor(w.id)?.status === 4).length }} 单 · 面积来自后端有效轨迹</span>
    </div>

    <PanelCard title="作业单核算列表" sub="差异率 =（GPS 面积 − 有效面积）/ GPS 面积 · 点击单号查看轨迹回放">
      <template #extra><span class="tag no-dot gray">按开始时间倒序</span></template>
      <el-table :data="paged" size="small" row-key="id" style="width: 100%" :row-class-name="rowClassName">
        <el-table-column label="作业单号" width="138"><template #default="{ row }"><span class="table-link mono" style="font-size: 10.5px" @click="openDetail(row)">{{ row.id }}</span></template></el-table-column>
        <el-table-column label="地块" min-width="112"><template #default="{ row }"><div class="cell-main">{{ row.landName }}</div><div class="cell-sub">{{ row.district }} · {{ row.crop }}</div></template></el-table-column>
        <el-table-column label="机手" width="68"><template #default="{ row }"><span class="cell-main">{{ row.operatorName }}</span></template></el-table-column>
        <el-table-column label="类型" width="58"><template #default="{ row }">{{ row.type }}</template></el-table-column>
        <el-table-column label="开始时间" width="118"><template #default="{ row }"><span class="mono" style="font-size: 11px">{{ row.startAt }}</span></template></el-table-column>
        <el-table-column label="时长" width="70" align="right"><template #default="{ row }"><span class="num">{{ durationText(row) }}</span></template></el-table-column>
        <el-table-column label="GPS 面积" width="80" align="right"><template #default="{ row }"><span class="num">{{ fmtArea(row.gpsArea) }}</span></template></el-table-column>
        <el-table-column label="有效面积" width="82" align="right"><template #default="{ row }"><span class="num strong">{{ fmtArea(row.effectiveArea) }}</span></template></el-table-column>
        <el-table-column label="差异率" width="78" align="right"><template #default="{ row }"><span class="num strong">{{ fmtDiffRate(row.diffRate) }}</span></template></el-table-column>
        <el-table-column label="异常" width="60" align="center"><template #default="{ row }"><span v-if="(row.anomalies || []).length" class="abn-chip"><el-icon :size="11"><Warning /></el-icon>{{ row.anomalies.length }}</span><span v-else class="hint">—</span></template></el-table-column>
        <el-table-column label="状态" width="76"><template #default="{ row }"><StatusTag :status="webStatus(row)" /></template></el-table-column>
        <el-table-column label="操作" width="104" align="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" class="op-btn" @click="openDetail(row)">详情</el-button>
            <el-button link size="small" class="op-btn" :disabled="!['待核算', '待初核', '初核通过', '初核驳回'].includes(webStatus(row))" @click="openDetail(row, 'calc')">复核</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="page" :page-size="pageSize" :total="filtered.length" layout="total, prev, pager, next" background />
    </PanelCard>

    <el-drawer v-model="detailVisible" title="作业单详情" size="76%">
      <div v-if="detail" class="wo-detail">
        <div class="detail-head">
          <div style="min-width: 0">
            <div class="detail-title mono">{{ detail.id }}</div>
            <div class="detail-sub">{{ detail.landName }} · {{ detail.district }} · {{ detail.operatorName }} · {{ detail.type }} · {{ detail.startAt }}</div>
          </div>
          <div style="margin-left: auto; display: flex; gap: 6px; align-items: center">
            <span v-if="(detail.anomalies || []).length" class="tag red">{{ detail.anomalies.length }} 项异常</span>
            <StatusTag :status="webStatus(detail)" />
          </div>
        </div>

        <div class="top-metrics">
          <div class="mini-metric"><div class="v num">{{ fmtArea(detail.gpsArea) }} <small>亩</small></div><div class="l">轨迹面积</div></div>
          <div class="mini-metric"><div class="v num">{{ fmtArea(detail.effectiveArea) }} <small>亩</small></div><div class="l">有效作业面积</div></div>
          <div class="mini-metric"><div class="v num">{{ fmtDiffRate(detail.diffRate) }}</div><div class="l">面积差异率</div></div>
          <div class="mini-metric"><div class="v num">{{ durationText(detail) }}</div><div class="l">作业时长</div></div>
        </div>

        <div class="wo-body">
          <div class="wo-left">
            <FarmMap :lands="landShape" :active-track="displayTrack" :play-index="playIndex" :anomalies="displayAnomalies" :selected-land="detail.landId" :height="352" :show-legend="false" live="轨迹回放" />
            <div class="play-bar">
              <el-button :icon="playing ? VideoPause : VideoPlay" type="primary" circle size="small" @click="togglePlay" />
              <div class="seg">
                <button v-for="s in [1, 2, 4]" :key="s" :class="{ active: speed === s }" @click="speed = s">{{ s }}x</button>
              </div>
              <el-slider v-model="playIndex" :min="0" :max="maxIndex" :show-tooltip="false" size="small" class="grow" @input="stopPlay" />
              <div class="play-info"><span class="mono">{{ displayTrack.length ? playIndex + 1 : 0 }} / {{ displayTrack.length }} 点</span><span class="hint">{{ pointTime }}</span></div>
              <div class="filter-switch"><span class="hint">轨迹滤波</span><el-switch v-model="filterOn" size="small" @change="onFilterChange" /><span v-if="filterOn" class="tag amber no-dot">滤波后 {{ displayTrack.length }} 点</span></div>
            </div>
          </div>

          <div class="wo-right">
            <el-tabs v-model="tab">
              <el-tab-pane label="核算明细" name="calc">
                <div class="calc-list">
                  <div v-for="r in calcRows" :key="r.label" class="calc-row"><span>{{ r.label }}</span><b class="num" :class="r.tone">{{ r.value }}</b></div>
                </div>
                <div class="formula">
                  <div class="formula-title">面积关系说明</div>
                  <div class="mono">轨迹面积 {{ fmtArea(detail.gpsArea) }} 亩 · 地块面积 {{ detail.landArea !== null && detail.landArea !== undefined ? fmtArea(detail.landArea) + ' 亩' : '—' }} · 有效作业面积 {{ fmtArea(detail.effectiveArea) }} 亩{{ deductArea !== null ? ` · 差异 ${Math.abs(deductArea)} 亩` : '' }}</div>
                  <div class="hint">后端有效轨迹接口当前未返回 GPS 面积与差异率，以上数据为轨迹点集合面积与地块档案的对比参考。异常点位与操作日志也待后端补充。</div>
                </div>
              </el-tab-pane>

              <el-tab-pane :label="`异常点位(${(detail.anomalies || []).length})`" name="anomaly">
                <div v-if="!(detail.anomalies || []).length" class="empty">后端当前未返回异常判定结果，无法认定无异常</div>
                <div v-for="(a, i) in detail.anomalies" :key="i" class="abn-item">
                  <div class="abn-top"><span class="tag red no-dot">{{ a.type }}</span><span class="hint mono">轨迹点 #{{ a.point }}</span><el-button link type="primary" size="small" style="margin-left: auto" @click="locate(a)">定位</el-button></div>
                  <div class="abn-meta">{{ a.at }} · 影响：{{ a.area ? `${a.area} 亩` : `${a.minutes} 分钟` }}</div>
                  <div class="abn-desc">{{ a.desc }}</div>
                </div>
              </el-tab-pane>

              <el-tab-pane label="操作日志" name="logs">
                <el-timeline style="padding-left: 2px; margin-top: 4px">
                  <el-timeline-item v-for="(l, i) in detail.logs" :key="i" :timestamp="l.at">
                    <div class="strong" style="font-size: 12px">{{ l.action }}</div>
                    <div class="hint" style="font-size: 11px">{{ l.user }}</div>
                  </el-timeline-item>
                </el-timeline>
              </el-tab-pane>

              <el-tab-pane label="AI 研判" name="ai">
                <AgentSidePanel agent-id="MA-02" title="MA-02 异常研判 Agent" desc="仅解释异常 · 不修改复核状态" :height="420" />
              </el-tab-pane>
            </el-tabs>
          </div>
        </div>

        <div class="wo-actions">
          <span class="hint" style="margin-right: auto">复核结果将写入作业单并进入结算候选，操作全程留痕。</span>
          <el-button type="danger" plain :disabled="reviewLocked" @click="doReview('reject')">驳回</el-button>
          <el-button :disabled="reviewLocked" @click="openCorrect">人工修正</el-button>
          <el-button type="primary" :disabled="reviewLocked" @click="doReview('pass')">复核通过</el-button>
        </div>
      </div>
    </el-drawer>

    <el-dialog v-model="correctVisible" title="人工修正有效面积" width="480px">
      <div class="hint-line" style="margin-bottom: 12px"><el-icon :size="13"><Warning /></el-icon><span>后端当前未提供人工修正接口，本面板仅用于查看。面积异议需通过复核申请驳回后重新核算。</span></div>
      <el-form label-width="82px">
        <el-form-item label="作业单"><span class="mono">{{ detail?.id }}</span></el-form-item>
        <el-form-item label="修正面积"><el-input-number v-model="correct.area" :min="0" :precision="1" :step="0.1" style="width: 100%" /></el-form-item>
        <el-form-item label="修正原因" required><el-input v-model="correct.reason" type="textarea" :rows="3" maxlength="120" show-word-limit placeholder="请说明修正依据，如：补充边界测绘截图，超界扣除 2.3 亩" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="correctVisible = false">取消</el-button><el-button type="primary" @click="submitCorrect">提交修正</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, MagicStick, VideoPause, VideoPlay } from '@element-plus/icons-vue'
import { getWorkorder, listWorkorders, listReviewRequests, reviewWorkorder } from '@/services/api'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import PanelCard from '@/components/PanelCard.vue'
import StatusTag from '@/components/StatusTag.vue'
import FarmMap from '@/components/FarmMap.vue'
import AgentSidePanel from '@/components/AgentSidePanel.vue'

const loading = ref(false)
const allRows = ref([])
const reviewRequests = ref([])
const statusFilter = ref('全部')
const districtFilter = ref('全部')
const keyword = ref('')
const page = ref(1)
const pageSize = 8

async function load() {
  loading.value = true
  try {
    const [ws, rs] = await Promise.all([
      listWorkorders(),
      listReviewRequests({ pageNum: 1, pageSize: 1000 }).catch(() => [])
    ])
    allRows.value = ws
    reviewRequests.value = rs
  } finally {
    loading.value = false
  }
}
load()

const districtOptions = computed(() => {
  const set = new Set()
  for (const w of allRows.value) if (w.district) set.add(w.district)
  return [...set]
})
const displayDistrictCount = computed(() => districtOptions.value.length)

function reviewFor(trajectoryInfoId) {
  return reviewRequests.value.find((r) => r.trajectoryInfoId === trajectoryInfoId) || null
}
function webStatus(w) {
  const r = reviewFor(w.id)
  if (r) {
    // 复核存在 → 用复核状态映射 web 状态
    if (r.status === 3) return '终审通过'
    if (r.status === 4) return '终审驳回'
    if (r.status === 2) return '初核驳回'
    if (r.status === 1) return '初核通过'
    if (r.status === 0) return '待初核'
  }
  // 无复核 → 轨迹原始状态（后端通常为空）
  if (w.status) return w.status
  return '待核算'
}

const stats = computed(() => {
  const list = allRows.value
  const gps = list.reduce((s, w) => s + (Number.isFinite(w.gpsArea) ? w.gpsArea : 0), 0)
  const eff = list.reduce((s, w) => s + (Number.isFinite(w.effectiveArea) ? w.effectiveArea : 0), 0)
  const diffs = list
    .map((w) => Number.isFinite(w.diffRate) ? w.diffRate : null)
    .filter((v) => v !== null)
  const reviewPending = reviewRequests.value.filter((r) => r.status === 0 || r.status === 1).length
  const reviewRejected = reviewRequests.value.filter((r) => r.status === 2 || r.status === 4).length
  return {
    total: list.length,
    pending: list.length - reviewRequests.value.filter((r) => r.status === 3 || r.status === 4).length,
    review: reviewPending,
    abnormal: reviewRejected,
    gps: +gps.toFixed(1),
    effective: +eff.toFixed(1),
    avgDiff: diffs.length ? +(diffs.reduce((s, v) => s + v, 0) / diffs.length).toFixed(1) : null
  }
})
const formatTotalDuration = computed(() => {
  const totalMinutes = allRows.value.reduce((s, w) => s + (Number.isFinite(w.duration) ? w.duration : 0), 0)
  return (totalMinutes / 60).toFixed(1)
})
const reviewCoverage = computed(() => {
  if (!allRows.value.length) return '0%'
  const reviewed = allRows.value.filter((w) => reviewFor(w.id) !== null).length
  return Math.round((reviewed / allRows.value.length) * 100) + '%'
})
const statusOptions = computed(() => ['全部', ...new Set(allRows.value.map((w) => webStatus(w)))])
const filtered = computed(() => {
  const k = keyword.value.trim()
  return allRows.value.filter((w) =>
    (statusFilter.value === '全部' || webStatus(w) === statusFilter.value) &&
    (districtFilter.value === '全部' || w.district === districtFilter.value) &&
    (!k || w.id.includes(k) || w.landName.includes(k) || w.operatorName.includes(k)))
})
const paged = computed(() => filtered.value.slice((page.value - 1) * pageSize, page.value * pageSize))
watch([statusFilter, districtFilter, keyword], () => { page.value = 1 })

const rowClassName = ({ row }) => (row.anomalies && row.anomalies.length ? 'row-abnormal' : '')
function durationText(w) {
  if (w.duration === null || w.duration === undefined) return '—'
  if (!Number.isFinite(Number(w.duration))) return '—'
  const d = Number(w.duration)
  if (d <= 0) return '—'
  return `${Math.floor(d / 60)}h${String(Math.round(d) % 60).padStart(2, '0')}m`
}

const fmtNum = (v, digits = 1) => (Number.isFinite(Number(v)) ? Number(v).toFixed(digits) : '—')
const fmtArea = (v) => (Number.isFinite(Number(v)) ? `${Number(v).toFixed(1)}` : '—')
const fmtDiffRate = (v) => {
  if (!Number.isFinite(Number(v))) return '—'
  const n = Number(v)
  return `${n > 0 ? '+' : ''}${n.toFixed(1)}%`
}

function exportCsv() {
  const head = ['作业单号', '地块', '片区', '机手', '作业类型', '开始时间', '时长(分钟)', 'GPS面积(亩)', '有效面积(亩)', '差异率(%)', '异常数', '状态']
  const rows = filtered.value.map((w) => [
    w.id, w.landName, w.district, w.operatorName, w.type, w.startAt,
    Number.isFinite(w.duration) ? Math.round(w.duration) : '',
    fmtArea(w.gpsArea), fmtArea(w.effectiveArea), fmtDiffRate(w.diffRate),
    (w.anomalies || []).length, webStatus(w)
  ])
  const csv = '\ufeff' + [head, ...rows].map((r) => r.map((v) => `"${String(v).replace(/"/g, '""')}"`).join(',')).join('\r\n')
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }))
  const a = document.createElement('a')
  a.href = url
  a.download = `作业单核算清单_${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
  ElMessage.success(`已导出 ${rows.length} 条核算记录`)
}

/* ---------------- 详情与轨迹回放 ---------------- */
const detailVisible = ref(false)
const detail = ref(null)
const tab = ref('calc')
const filterOn = ref(false)
const playIndex = ref(0)
const playing = ref(false)
const speed = ref(1)
let timer = null

const displayTrack = computed(() => {
  const track = detail.value?.track || []
  return filterOn.value ? track.filter((_, i) => i % 2 === 0) : track
})
const displayTrackTimes = computed(() => {
  const times = detail.value?.trackTimes || []
  return filterOn.value ? times.filter((_, i) => i % 2 === 0) : times
})
const displayAnomalies = computed(() => {
  const list = detail.value?.anomalies || []
  return filterOn.value ? list.map((a) => ({ ...a, point: Math.floor(a.point / 2) })) : list
})
const maxIndex = computed(() => Math.max(displayTrack.value.length - 1, 0))
const cutArea = computed(() => +((detail.value?.anomalies || []).reduce((s, a) => s + (Number.isFinite(Number(a.area)) ? Number(a.area) : 0), 0)).toFixed(1))
const deductArea = computed(() => {
  const d = detail.value
  if (!d) return null
  if (!Number.isFinite(Number(d.gpsArea)) || !Number.isFinite(Number(d.effectiveArea))) return null
  return +(Number(d.gpsArea) - Number(d.effectiveArea)).toFixed(1)
})
const reviewLocked = computed(() => {
  const r = detail.value ? reviewFor(detail.value.id) : null
  if (!r) return false
  return r.status === 3 || r.status === 4 // 终审通过/驳回为终态
})
const calcRows = computed(() => {
  const d = detail.value
  if (!d) return []
  const width = d.width ? `${d.width} m` : '—'
  const pointCount = d.pointCount || d.track?.length || '—'
  const landArea = Number.isFinite(Number(d.landArea)) ? `${Number(d.landArea).toFixed(1)} 亩` : '—'
  const boundaryCut = cutArea.value > 0 ? `− ${cutArea.value} 亩` : '—'
  return [
    { label: '作业幅宽', value: width, tone: '' },
    { label: '轨迹点数', value: `${pointCount} 点`, tone: '' },
    { label: '地块面积', value: landArea, tone: '' },
    { label: '边界裁剪', value: boundaryCut, tone: cutArea.value > 0 ? 'neg' : '' },
    { label: 'GPS 面积', value: fmtArea(d.gpsArea), tone: '' },
    { label: '有效面积', value: fmtArea(d.effectiveArea), tone: '' }
  ]
})
const landShape = computed(() => {
  const d = detail.value
  if (!d) return []
  const pts = d.landPoints || []
  const center = pts.length
    ? [Math.round(pts.reduce((s, p) => s + p[0], 0) / pts.length), Math.round(pts.reduce((s, p) => s + p[1], 0) / pts.length)]
    : [500, 310]
  return [{ id: d.landId, name: d.landName, points: pts, center, area: Number.isFinite(Number(d.landArea)) ? Number(d.landArea) : 0, status: '' }]
})
const pointTime = computed(() => {
  const t = displayTrackTimes.value[playIndex.value]
  if (t) {
    const text = String(t).includes('T') ? String(t).replace('T', ' ').slice(5, 16) : String(t).slice(5, 16)
    return text || '—'
  }
  // 没有真实时间 → 用开始时间作为兜底，不做插值
  return detail.value?.startAt ? String(detail.value.startAt).slice(5, 16) : '—'
})

async function openDetail(row, defaultTab = 'calc') {
  const data = await getWorkorder(row.id, allRows.value)
  if (!data) return ElMessage.warning('作业单不存在')
  detail.value = data
  tab.value = defaultTab
  filterOn.value = false
  playIndex.value = 0
  stopPlay()
  detailVisible.value = true
}
watch(detailVisible, (v) => { if (!v) stopPlay() })

function togglePlay() {
  if (playing.value) return stopPlay()
  if (playIndex.value >= maxIndex.value) playIndex.value = 0
  playing.value = true
  timer = setInterval(() => {
    if (playIndex.value >= maxIndex.value) return stopPlay()
    playIndex.value += 1
  }, 520 / speed.value)
}
function stopPlay() {
  playing.value = false
  if (timer) { clearInterval(timer); timer = null }
}
watch(speed, () => { if (playing.value) { stopPlay(); togglePlay() } })
function onFilterChange() { stopPlay(); playIndex.value = 0 }
function locate(a) {
  stopPlay()
  const idx = filterOn.value ? Math.floor(a.point / 2) : a.point
  playIndex.value = Math.min(idx, maxIndex.value)
}
async function openAiReview() {
  // anomalies 由后端异常判定返回，当前为空；改从已驳回复核的轨迹里找
  const rejected = filtered.value.filter((w) => {
    const r = reviewFor(w.id)
    return r && (r.status === 2 || r.status === 4)
  })
  const first = rejected[0] || filtered.value[0]
  if (!first) return ElMessage.info('当前筛选范围内没有可研判的作业单')
  await openDetail(first, 'ai')
  ElMessage.info('已打开 MA-02 异常研判助手，可结合轨迹点位与地块边界进行分析')
}
onBeforeUnmount(stopPlay)

/* ---------------- 复核与人工修正 ---------------- */
async function doReview(action) {
  const d = detail.value
  if (!d) return
  let note = '复核通过，面积在允许误差范围内'
  if (action === 'reject') {
    try {
      const { value } = await ElMessageBox.prompt('请填写驳回原因，将退回片区重新处理', '驳回作业单', {
        confirmButtonText: '确认驳回', cancelButtonText: '取消', inputType: 'textarea',
        inputPlaceholder: '如：超界扣除依据不足，请补充边界测绘截图',
        inputValidator: (v) => (v && v.trim() ? true : '驳回原因不能为空'), type: 'warning'
      })
      note = value.trim()
    } catch { return }
  } else {
    try {
      await ElMessageBox.confirm(`确认队长初核通过「${d.id}」？当前轨迹面积 ${fmtArea(d.gpsArea)} 亩 / 有效面积 ${fmtArea(d.effectiveArea)} 亩。通过后进入管理员终审。`, '复核确认', { type: 'warning', confirmButtonText: '确认通过', cancelButtonText: '取消' })
    } catch { return }
  }
  try {
    const res = await reviewWorkorder(d.id, action, note)
    ElMessage.success(res.message)
  } catch (error) {
    const msg = error?.message || error?.payload?.msg || '复核操作失败'
    if (/复核单不存在|状态.*不匹配/.test(msg)) {
      ElMessage.error('复核单状态不匹配，可能已被其他处理人操作；请刷新列表后重试')
    } else {
      ElMessage.error('复核操作失败：' + msg)
    }
    return
  }
  await load()
  // 重新从 allRows 拿 detail（复用加载后的最新数据）
  const target = allRows.value.find((w) => w.id === d.id)
  if (target) detail.value = target
}

const correctVisible = ref(false)
const correct = reactive({ area: 0, reason: '' })
function openCorrect() {
  correct.area = Number.isFinite(Number(detail.value?.effectiveArea)) ? Number(detail.value.effectiveArea) : 0
  correct.reason = ''
  correctVisible.value = true
}
function submitCorrect() {
  // 后端无"人工修正面积"接口，禁止前端模拟提交成功
  ElMessage.warning('后端暂无"人工修正面积"接口。若面积确有异议，请通过「驳回复核」退回片区重新核算后再提交。')
  correctVisible.value = false
}
</script>

<style scoped>
.compact-stats { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; padding: 13px 16px; }
.cs-item { display: flex; flex-direction: column; gap: 1px; }
.cs-item + .cs-item { border-left: 1px solid var(--border); padding-left: 16px; }
.cs-item .l { font-size: 10.5px; color: var(--text-3); }
.cs-item b { font-size: 19px; font-weight: 700; letter-spacing: -0.3px; }
.cs-item b small { font-size: 10.5px; font-weight: 500; color: var(--text-3); }
.pos { color: var(--brand-600); }
.neg { color: var(--red-600); }
.op-btn { padding: 0 3px; }
.abn-chip { display: inline-flex; align-items: center; gap: 2px; height: 20px; padding: 0 6px; border-radius: 5px; background: var(--red-100); color: var(--red-600); font-size: 11px; font-weight: 700; }
:deep(.row-abnormal > td.el-table__cell) { background: #fff7f6; }
:deep(.row-abnormal:hover > td.el-table__cell) { background: #fdeeed !important; }
:deep(.row-abnormal td:first-child) { box-shadow: inset 3px 0 0 var(--red-500); }
.top-metrics { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px; margin-bottom: 14px; }
.mini-metric .v small { font-size: 10.5px; font-weight: 500; color: var(--text-3); }
.wo-body { display: grid; grid-template-columns: minmax(0, 1.6fr) minmax(0, 1fr); gap: 14px; align-items: start; }
.wo-left, .wo-right { min-width: 0; }
.play-bar { display: flex; align-items: center; flex-wrap: wrap; gap: 9px; padding: 9px 11px; margin-top: 10px; border: 1px solid var(--border); border-radius: var(--radius-md); background: var(--surface-2); }
.play-bar .grow { flex: 1; min-width: 120px; margin: 0 4px; }
.play-info { display: flex; flex-direction: column; gap: 1px; font-size: 10.5px; line-height: 1.35; text-align: right; white-space: nowrap; }
.filter-switch { display: flex; align-items: center; gap: 6px; padding-left: 9px; border-left: 1px solid var(--border-strong); }
.wo-right :deep(.el-tabs__content) { overflow: visible; }
.calc-list { border: 1px solid var(--border); border-radius: 10px; overflow: hidden; }
.calc-row { display: flex; align-items: center; justify-content: space-between; padding: 7px 11px; font-size: 12px; }
.calc-row + .calc-row { border-top: 1px dashed var(--border); }
.calc-row span { color: var(--text-2); }
.formula { margin-top: 10px; padding: 10px 11px; border-radius: 9px; background: var(--brand-50); border: 1px solid #d8efe3; font-size: 11px; line-height: 1.7; color: var(--brand-800); }
.formula-title { font-size: 11px; font-weight: 700; margin-bottom: 3px; }
.formula .hint { font-size: 10.5px; margin-top: 3px; line-height: 1.6; }
.abn-item { border: 1px solid var(--border); border-left: 3px solid var(--red-500); border-radius: 9px; padding: 9px 11px; margin-bottom: 8px; background: #fffafa; }
.abn-top { display: flex; align-items: center; gap: 7px; }
.abn-meta { font-size: 10.5px; color: var(--text-3); margin-top: 5px; }
.abn-desc { font-size: 11.5px; color: var(--text-2); line-height: 1.6; margin-top: 3px; }
.wo-actions { display: flex; align-items: center; gap: 9px; padding-top: 14px; margin-top: 16px; border-top: 1px solid var(--border); }
</style>
