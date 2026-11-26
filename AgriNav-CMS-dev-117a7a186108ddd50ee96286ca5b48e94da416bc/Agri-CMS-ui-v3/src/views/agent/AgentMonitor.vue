<template>
  <div class="page">
    <PageHeader
      title="Agent 运行监控"
      code="AI-CENTER"
      group="AI 智能体"
      desc="基于智能体指令审计记录的真实只读监控；卡片统计范围仅限当前页，总数以筛选条件下的后端分页 total 为准。"
    >
      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
      </template>
    </PageHeader>

    <div class="grid stat-grid">
      <StatCard label="审计记录总数" :value="totalText" unit="条" icon="DataLine"
        foot="筛选条件下的后端分页 total" />
      <StatCard label="当前页失败" :value="failedCount" unit="条" icon="Warning" tone="red"
        :foot="pageFoot" />
      <StatCard label="当前页待确认" :value="confirmCount" unit="条" icon="Tickets" tone="amber"
        :foot="pageFoot" />
      <StatCard label="当前页待补充参数" :value="paramsCount" unit="条" icon="Tickets" tone="amber"
        :foot="pageFoot" />
      <StatCard label="当前页已回复" :value="replyCount" unit="条" icon="CircleCheck"
        :foot="pageFoot" />
      <StatCard label="当前页平均耗时" :value="avgCostText" unit="ms" icon="Timer" tone="blue"
        foot="仅统计有耗时字段的记录" />
    </div>

    <div class="filter-bar mt-3">
      <el-input v-model="filters.userName" placeholder="用户账号" clearable style="width: 150px"
        :prefix-icon="Search" @keyup.enter="apply" />
      <el-input v-model="filters.intent" placeholder="意图" clearable style="width: 130px"
        @keyup.enter="apply" />
      <el-select v-model="filters.status" style="width: 140px" @change="apply">
        <el-option label="全部状态" value="" />
        <el-option label="已回复" value="REPLY" />
        <el-option label="待确认" value="NEED_CONFIRM" />
        <el-option label="待补充参数" value="NEED_PARAMS" />
        <el-option label="失败" value="FAILED" />
      </el-select>
      <el-select v-model="filters.side" style="width: 120px" @change="apply">
        <el-option label="全部端" value="" />
        <el-option label="农机端" value="ag" />
        <el-option label="管理端" value="ma" />
      </el-select>
      <el-date-picker v-model="filters.dateRange" type="datetimerange" value-format="YYYY-MM-DD HH:mm:ss"
        start-placeholder="开始时间" end-placeholder="结束时间" style="width: 330px" />
      <el-button type="primary" @click="apply">查询</el-button>
      <el-button @click="reset">重置</el-button>
    </div>

    <div v-if="error" class="state-block">
      <el-alert :type="error.kind === 'forbidden' ? 'warning' : 'error'" :closable="false" show-icon
        :title="error.message" />
      <el-button v-if="error.kind !== 'forbidden'" size="small" @click="load">重新加载</el-button>
    </div>

    <PanelCard v-else title="审计记录" sub="按创建时间倒序 · 只读" flush>
      <div v-loading="loading">
        <el-table v-if="list.length" :data="list" size="small" style="width: 100%">
          <el-table-column label="时间" min-width="140">
            <template #default="{ row }"><span class="mono">{{ row.createTime || '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="user" label="用户" min-width="100">
            <template #default="{ row }">{{ row.user || '—' }}</template>
          </el-table-column>
          <el-table-column label="端" width="86" align="center">
            <template #default="{ row }">{{ sideText(row.side) }}</template>
          </el-table-column>
          <el-table-column prop="intent" label="意图" min-width="110">
            <template #default="{ row }">{{ row.intent || '—' }}</template>
          </el-table-column>
          <el-table-column prop="question" label="用户问题" min-width="200" show-overflow-tooltip>
            <template #default="{ row }">{{ row.question || '—' }}</template>
          </el-table-column>
          <el-table-column label="工具" min-width="120">
            <template #default="{ row }">{{ row.toolName || '—' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="110" align="center">
            <template #default="{ row }"><StatusTag :status="row.statusLabel" /></template>
          </el-table-column>
          <el-table-column label="耗时" width="90" align="right">
            <template #default="{ row }"><span class="mono">{{ row.costMs ?? '—' }}{{ row.costMs === null ? '' : ' ms' }}</span></template>
          </el-table-column>
          <el-table-column label="回执号" min-width="120">
            <template #default="{ row }"><span class="mono">{{ row.receiptNo || '—' }}</span></template>
          </el-table-column>
          <el-table-column label="操作" width="80" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else-if="!loading" description="暂无审计记录" />
      </div>
      <div v-if="total !== null" class="panel-pager">
        <el-pagination v-model:current-page="page" v-model:page-size="size"
          :page-sizes="[10, 20, 50]" :total="total"
          layout="total, sizes, prev, pager, next" small
          @current-change="load" @size-change="onSize" />
      </div>
    </PanelCard>

    <PanelCard class="mt-3" title="聚合统计与趋势" sub="接口待接入">
      <div class="hint-line">
        调用量趋势、Agent 占比、响应时延分布、失败原因聚合暂无后端统计接口；本页不使用 Mock 或前端推算数据。
      </div>
    </PanelCard>

    <el-drawer v-model="detailOpen" size="520px" title="审计记录详情">
      <div v-loading="detailLoading">
        <div v-if="detailError" class="state-block">
          <el-alert :type="detailError.kind === 'forbidden' ? 'warning' : 'error'" :closable="false" show-icon
            :title="detailError.message" />
          <el-button v-if="detailError.kind !== 'forbidden'" size="small" @click="retryDetail">重新加载</el-button>
        </div>
        <template v-else-if="detail">
          <div class="detail-row"><span class="detail-label">时间</span><span class="mono">{{ detail.createTime || '—' }}</span></div>
          <div class="detail-row"><span class="detail-label">用户</span><span>{{ detail.user || '—' }}</span></div>
          <div class="detail-row"><span class="detail-label">端类型</span><span>{{ sideText(detail.side) }}</span></div>
          <div class="detail-row"><span class="detail-label">意图</span><span>{{ detail.intent || '—' }}</span></div>
          <div class="detail-row"><span class="detail-label">状态</span><StatusTag :status="detail.statusLabel" /></div>
          <div class="detail-row"><span class="detail-label">耗时</span><span class="mono">{{ detail.costMs ?? '—' }}{{ detail.costMs === null ? '' : ' ms' }}</span></div>
          <div class="detail-row"><span class="detail-label">回执号</span><span class="mono">{{ detail.receiptNo || '—' }}</span></div>
          <div class="detail-row"><span class="detail-label">工具</span><span>{{ detail.toolName || '—' }}</span></div>
          <div class="detail-block"><span class="detail-label">用户问题</span><div class="detail-text">{{ detail.question || '—' }}</div></div>
          <div class="detail-block"><span class="detail-label">工具参数</span><pre class="detail-pre">{{ detail.toolArguments || '—' }}</pre></div>
          <div class="detail-block"><span class="detail-label">工具结果</span><pre class="detail-pre">{{ detail.toolResult || '—' }}</pre></div>
          <div class="detail-block"><span class="detail-label">回复</span><div class="detail-text">{{ detail.reply || '—' }}</div></div>
          <div class="detail-block"><span class="detail-label">错误信息</span><div class="detail-text err-text">{{ detail.error || '—' }}</div></div>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { listAgentAuditLogs, getAgentAuditLogDetail } from '@/services/api'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import PanelCard from '@/components/PanelCard.vue'
import StatusTag from '@/components/StatusTag.vue'

const loading = ref(false)
const error = ref(null)
const list = ref([])
const total = ref(null)
const page = ref(1)
const size = ref(10)

const filters = reactive({ userName: '', intent: '', status: '', side: '', dateRange: [] })

const detailOpen = ref(false)
const detailLoading = ref(false)
const detailError = ref(null)
const detail = ref(null)
const detailId = ref('')

function toError(err) {
  if (err?.status === 403 || err?.code === 403) {
    return { kind: 'forbidden', message: '当前账号没有访问该模块的权限。' }
  }
  if (err?.raw || /连接异常/.test(err?.message || '')) {
    return { kind: 'network', message: '网络或服务异常，请稍后重试。' }
  }
  return { kind: 'error', message: err?.payload?.msg || err?.message || '加载失败，请稍后重试。' }
}

function sideText(side) {
  return { ag: '农机端', ma: '管理端' }[side] || side || '—'
}

function buildQuery() {
  const query = { pageNum: page.value, pageSize: size.value }
  if (filters.userName.trim()) query.userName = filters.userName.trim()
  if (filters.intent.trim()) query.intent = filters.intent.trim()
  if (filters.status) query.status = filters.status
  if (filters.side) query.side = filters.side
  if (Array.isArray(filters.dateRange) && filters.dateRange.length === 2) {
    query.beginTime = filters.dateRange[0]
    query.endTime = filters.dateRange[1]
  }
  return query
}

async function load() {
  loading.value = true
  error.value = null
  try {
    const res = await listAgentAuditLogs(buildQuery())
    list.value = res.list
    total.value = res.total
  } catch (err) {
    list.value = []
    total.value = null
    error.value = toError(err)
  } finally {
    loading.value = false
  }
}

function apply() {
  page.value = 1
  load()
}
function reset() {
  Object.assign(filters, { userName: '', intent: '', status: '', side: '', dateRange: [] })
  page.value = 1
  load()
}
function onSize() {
  page.value = 1
  load()
}

/* 以下统计仅针对当前加载页，不代表全量口径 */
const countByStatus = (status) => list.value.filter((row) => row.status === status).length
const failedCount = computed(() => countByStatus('FAILED'))
const confirmCount = computed(() => countByStatus('NEED_CONFIRM'))
const paramsCount = computed(() => countByStatus('NEED_PARAMS'))
const replyCount = computed(() => countByStatus('REPLY'))
const totalText = computed(() => (total.value === null ? '—' : String(total.value)))
const pageFoot = computed(() => (list.value.length ? `当前页 ${list.value.length} 条` : '当前页暂无记录'))
const avgCostText = computed(() => {
  const values = list.value
    .map((row) => row.costMs)
    .filter((value) => typeof value === 'number' && Number.isFinite(value))
  if (!values.length) return '—'
  return String(Math.round(values.reduce((sum, value) => sum + value, 0) / values.length))
})

async function openDetail(row) {
  detailId.value = row.id
  detailOpen.value = true
  detail.value = null
  detailError.value = null
  detailLoading.value = true
  try {
    detail.value = await getAgentAuditLogDetail(row.id)
  } catch (err) {
    detailError.value = toError(err)
  } finally {
    detailLoading.value = false
  }
}
function retryDetail() {
  if (detailId.value) openDetail({ id: detailId.value })
}

onMounted(load)
</script>

<style scoped>
.stat-grid { grid-template-columns: repeat(auto-fit, minmax(168px, 1fr)); }
.filter-bar { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; margin-bottom: 10px; }
.state-block { display: flex; flex-direction: column; gap: 8px; align-items: flex-start; margin: 8px 0; }
.panel-pager { padding: 6px 14px 10px; display: flex; justify-content: flex-end; }
.detail-row { display: flex; gap: 10px; padding: 8px 0; border-bottom: 1px dashed var(--border); font-size: 12.5px; }
.detail-block { padding: 8px 0; border-bottom: 1px dashed var(--border); }
.detail-label { flex: none; width: 84px; color: var(--text-2); font-size: 12px; }
.detail-text { margin-top: 4px; font-size: 12.5px; line-height: 1.7; word-break: break-word; }
.detail-pre { margin: 4px 0 0; padding: 8px 10px; background: var(--surface-2); border-radius: 8px; font-size: 11.5px; line-height: 1.6; white-space: pre-wrap; word-break: break-all; }
.err-text { color: #c53030; font-size: 11.5px; }
.mono { font-variant-numeric: tabular-nums; }
</style>
