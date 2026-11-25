<template>
  <div class="page" v-loading="loading">
    <PageHeader
      title="统计报表"
      code="MP-05"
      group="业务管理"
      desc="数据口径：面积 / 效率 / 异常取自作业单核算结果，金额为不含税结算金额（万元），按当前用户数据范围聚合。"
    >
      <template #actions>
        <div class="seg">
          <button v-for="t in timeOptions" :key="t.value" :class="{ active: timeDim === t.value }" @click="timeDim = t.value">{{ t.label }}</button>
        </div>
        <el-button type="primary" :icon="Download" @click="exportCsv">导出报表</el-button>
      </template>
    </PageHeader>

    <div class="grid cols-6">
      <StatCard label="累计作业面积" :value="num(kpi.totalArea)" unit="亩" icon="MapLocation" foot="核算后有效面积" />
      <StatCard label="平均效率" :value="kpi.avgEfficiency" unit="亩/小时" icon="TrendCharts" foot="农机加权均值" />
      <StatCard label="异常率" :value="kpi.abnormalRate" unit="%" icon="Warning" tone="red" foot="超界 / 重复 / 中断" />
      <StatCard label="复核率" :value="kpi.reviewRate" unit="%" icon="Tickets" tone="amber" foot="需人工复核占比" />
      <StatCard label="结算金额" :value="kpi.settleAmount" unit="万元" icon="Wallet" tone="violet" foot="不含税 · 已生成结算单" />
      <StatCard label="任务完成率" :value="kpi.taskCompletion" unit="%" icon="CircleCheck" tone="blue" foot="近 30 天任务口径" />
    </div>

    <div class="filter-bar mt-3">
      <el-select v-model="filters.district" style="width: 132px">
        <el-option v-for="d in districtOptions" :key="d" :label="d" :value="d" />
      </el-select>
      <el-date-picker v-model="filters.range" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始日期" end-placeholder="结束日期" style="width: 250px" />
      <el-select v-model="filters.dimension" style="width: 118px">
        <el-option label="片区" value="片区" />
        <el-option label="农机" value="农机" />
        <el-option label="机手" value="机手" />
      </el-select>
      <span class="filter-hint grow">统计区间 {{ timeLabel }} · 片区筛选作用于片区图与机手表</span>
      <el-button @click="resetFilters">重置</el-button>
    </div>

    <div class="grid cols-2">
      <PanelCard title="近 6 月作业面积与结算金额" sub="柱：作业面积（亩）· 折线：结算金额（万元）">
        <ChartPanel :option="comboOption" :height="240" />
      </PanelCard>
      <PanelCard title="片区完成率与平均效率" sub="双轴对比 · 完成率（%）与效率（亩/小时）">
        <ChartPanel :option="districtOption" :height="240" />
      </PanelCard>
    </div>

    <div class="grid cols-2 mt-3">
      <PanelCard title="农机效率排行" sub="近 30 天平均效率（亩/小时）· 全公司口径">
        <ChartPanel :option="machineOption" :height="252" />
      </PanelCard>
      <PanelCard title="异常类型分布" sub="按核算异常记录归类">
        <ChartPanel :option="pieOption" :height="252" />
      </PanelCard>
    </div>

    <PanelCard title="机手工作量排行" sub="支持点击表头排序 · 数据随片区筛选联动" class="mt-3" flush>
      <el-table :data="pagedOperators" size="small" style="width: 100%" :default-sort="{ prop: 'area', order: 'descending' }" @sort-change="onOpSort">
        <el-table-column prop="name" label="机手" min-width="100">
          <template #default="{ row }"><span class="cell-main">{{ row.name }}</span></template>
        </el-table-column>
        <el-table-column prop="district" label="片区" min-width="110" />
        <el-table-column prop="area" label="作业面积（亩）" min-width="130" sortable="custom" align="right" />
        <el-table-column prop="tasks" label="任务数" width="100" sortable="custom" align="right" />
        <el-table-column prop="rate" label="达标率" width="150" sortable="custom" align="right">
          <template #default="{ row }">
            <div class="rate-cell">
              <span class="bar-track"><span class="bar-fill" :style="{ width: row.rate + '%' }" /></span>
              <span class="num">{{ row.rate }}%</span>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div class="panel-pager">
        <el-pagination v-model:current-page="opPage" :page-size="5" :total="sortedOperators.length" layout="total, prev, pager, next" small />
      </div>
    </PanelCard>

    <div class="grid mt-3 report-bottom">
      <PanelCard title="近 7 天趋势" sub="有效作业面积（亩）与异常任务数">
        <ChartPanel :option="trend7Option" :height="300" />
      </PanelCard>
      <div ref="assistantRef" class="agent-col">
        <div class="quick-qs">
          <div class="qs-title"><el-icon :size="13"><MagicStick /></el-icon>向 MA-04 提问</div>
          <button v-for="q in sampleQuestions" :key="q" @click="askAgent(q)">{{ q }}</button>
        </div>
        <AgentSidePanel agent-id="MA-04" title="MA-04 数据分析 Agent" desc="统计口径与区间可追溯，不推算缺失数据" :height="600" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, MagicStick } from '@element-plus/icons-vue'
import { getReport } from '@/services/api'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import PanelCard from '@/components/PanelCard.vue'
import ChartPanel from '@/components/ChartPanel.vue'
import AgentSidePanel from '@/components/AgentSidePanel.vue'

const loading = ref(false)
const report = ref({ kpi: {}, trend7: {}, trend6m: {}, district: [], machineEff: [], operator: [], anomalyRate: [] })
const kpi = computed(() => report.value.kpi || {})
const timeDim = ref('week')
const timeOptions = [
  { value: 'week', label: '近 7 天' },
  { value: 'month', label: '本月' },
  { value: 'quarter', label: '本季度' }
]
const timeLabel = computed(() => timeOptions.find((t) => t.value === timeDim.value)?.label || '近 7 天')
const filters = reactive({ district: '全公司', range: ['2026-11-17', '2026-11-23'], dimension: '片区' })
const opPage = ref(1)
const assistantRef = ref(null)
const opSort = reactive({ prop: 'area', order: 'descending' })
const sampleQuestions = ['本月哪个片区作业效率最高？', '近 7 天异常率变化趋势如何？', '各机手作业面积排行前 3 是谁？']

const num = (n = 0) => Number(n).toLocaleString('zh-CN', { maximumFractionDigits: 1 })
const districtOptions = computed(() => ['全公司', ...(report.value.district || []).map((d) => d.name)])
const visibleDistricts = computed(() => {
  const list = report.value.district || []
  return filters.district === '全公司' ? list : list.filter((d) => d.name === filters.district)
})
const visibleOperators = computed(() => {
  const list = report.value.operator || []
  return filters.district === '全公司' ? list : list.filter((o) => o.district === filters.district)
})
const sortedOperators = computed(() => {
  const arr = [...visibleOperators.value]
  if (!opSort.order) return arr
  const dir = opSort.order === 'ascending' ? 1 : -1
  return arr.sort((a, b) => (a[opSort.prop] - b[opSort.prop]) * dir)
})
const pagedOperators = computed(() => sortedOperators.value.slice((opPage.value - 1) * 5, opPage.value * 5))

async function load() {
  loading.value = true
  try {
    report.value = await getReport(timeDim.value)
  } finally {
    loading.value = false
  }
}
onMounted(load)
watch(timeDim, load)

function resetFilters() {
  filters.district = '全公司'
  filters.range = ['2026-11-17', '2026-11-23']
  filters.dimension = '片区'
  opPage.value = 1
}
function onOpSort({ prop, order }) {
  opSort.prop = prop || 'area'
  opSort.order = order
  opPage.value = 1
}
function askAgent(q) {
  assistantRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  ElMessage.info(`请在右侧「MA-04 数据分析 Agent」中输入：${q}`)
}

const comboOption = computed(() => {
  const t = report.value.trend6m || {}
  return {
    grid: { left: 44, right: 44, top: 34, bottom: 24, containLabel: true },
    legend: { show: true, right: 0, top: 0, itemWidth: 9, itemHeight: 9, textStyle: { fontSize: 10.5 } },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: t.months || [], axisTick: { show: false } },
    yAxis: [
      { type: 'value', name: '亩', nameTextStyle: { fontSize: 10, color: '#8b9992' }, splitLine: { lineStyle: { color: '#eef3f0' } } },
      { type: 'value', name: '万元', nameTextStyle: { fontSize: 10, color: '#8b9992' }, splitLine: { show: false } }
    ],
    series: [
      { name: '作业面积', type: 'bar', barWidth: 20, data: t.area || [], itemStyle: { borderRadius: [5, 5, 0, 0], color: '#146b45' } },
      { name: '结算金额', type: 'line', yAxisIndex: 1, smooth: true, symbolSize: 6, data: t.amount || [], lineStyle: { width: 2, color: '#84cc16' }, itemStyle: { color: '#84cc16' } }
    ]
  }
})
const districtOption = computed(() => {
  const list = visibleDistricts.value
  return {
    grid: { left: 44, right: 44, top: 34, bottom: 24, containLabel: true },
    legend: { show: true, right: 0, top: 0, itemWidth: 9, itemHeight: 9, textStyle: { fontSize: 10.5 } },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: list.map((d) => d.name), axisTick: { show: false }, axisLabel: { fontSize: 10 } },
    yAxis: [
      { type: 'value', name: '%', max: 100, nameTextStyle: { fontSize: 10, color: '#8b9992' }, splitLine: { lineStyle: { color: '#eef3f0' } } },
      { type: 'value', name: '亩/小时', nameTextStyle: { fontSize: 10, color: '#8b9992' }, splitLine: { show: false } }
    ],
    series: [
      { name: '完成率', type: 'bar', barWidth: 26, data: list.map((d) => d.rate), itemStyle: { borderRadius: [5, 5, 0, 0], color: '#1fae6b' } },
      { name: '平均效率', type: 'line', yAxisIndex: 1, smooth: true, symbolSize: 6, data: list.map((d) => d.eff), lineStyle: { width: 2, color: '#e08a12' }, itemStyle: { color: '#e08a12' } }
    ]
  }
})
const machineOption = computed(() => {
  const list = [...(report.value.machineEff || [])].sort((a, b) => a.eff - b.eff)
  return {
    grid: { left: 64, right: 34, top: 16, bottom: 24, containLabel: true },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: { type: 'value', name: '亩/小时', nameTextStyle: { fontSize: 10, color: '#8b9992' }, splitLine: { lineStyle: { color: '#eef3f0' } } },
    yAxis: { type: 'category', data: list.map((m) => m.name), axisTick: { show: false }, axisLabel: { fontSize: 10.5 } },
    series: [{ name: '平均效率', type: 'bar', barWidth: 13, data: list.map((m) => m.eff), itemStyle: { borderRadius: [0, 5, 5, 0], color: '#146b45' }, label: { show: true, position: 'right', fontSize: 10, color: '#56645d' } }]
  }
})
const pieOption = computed(() => ({
  tooltip: { trigger: 'item' },
  legend: { bottom: 0, itemWidth: 8, itemHeight: 8, textStyle: { fontSize: 10.5 } },
  series: [
    {
      name: '异常类型',
      type: 'pie',
      radius: ['48%', '70%'],
      center: ['50%', '44%'],
      itemStyle: { borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      data: report.value.anomalyRate || []
    }
  ],
  graphic: [
    { type: 'text', left: 'center', top: '34%', style: { text: '异常类型', fill: '#8b9992', fontSize: 11, textAlign: 'center' } },
    { type: 'text', left: 'center', top: '41%', style: { text: '4 类', fill: '#14201a', fontSize: 20, fontWeight: 700, textAlign: 'center' } }
  ]
}))
const trend7Option = computed(() => {
  const t = report.value.trend7 || {}
  return {
    grid: { left: 44, right: 40, top: 32, bottom: 24, containLabel: true },
    legend: { show: true, right: 0, top: 0, itemWidth: 9, itemHeight: 9, textStyle: { fontSize: 10.5 } },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: t.days || [], axisTick: { show: false } },
    yAxis: [
      { type: 'value', name: '亩', nameTextStyle: { fontSize: 10, color: '#8b9992' }, splitLine: { lineStyle: { color: '#eef3f0' } } },
      { type: 'value', name: '个', nameTextStyle: { fontSize: 10, color: '#8b9992' }, splitLine: { show: false } }
    ],
    series: [
      { name: '作业面积', type: 'line', smooth: true, symbolSize: 6, data: t.area || [], lineStyle: { width: 2.5, color: '#146b45' }, itemStyle: { color: '#146b45' }, areaStyle: { color: 'rgba(31,174,107,.12)' } },
      { name: '异常任务', type: 'bar', yAxisIndex: 1, barWidth: 12, data: t.abnormal || [], itemStyle: { borderRadius: [4, 4, 0, 0], color: '#e08a12' } }
    ]
  }
})

function downloadCsv(rows, filename) {
  const csv = '\uFEFF' + rows.map((r) => r.map((c) => `"${String(c).replace(/"/g, '""')}"`).join(',')).join('\r\n')
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8;' }))
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}
function exportCsv() {
  const dim = filters.dimension
  let rows
  if (dim === '农机') {
    rows = [['农机', '型号', '平均效率(亩/小时)', '作业面积(亩)', '作业时长(小时)'], ...(report.value.machineEff || []).map((m) => [m.name, m.model, m.eff, m.area, m.hours])]
  } else if (dim === '机手') {
    rows = [['机手', '片区', '作业面积(亩)', '任务数', '达标率(%)'], ...visibleOperators.value.map((o) => [o.name, o.district, o.area, o.tasks, o.rate])]
  } else {
    rows = [['片区', '作业面积(亩)', '完成率(%)', '平均效率(亩/小时)', '异常数', '待复核数'], ...visibleDistricts.value.map((d) => [d.name, d.area, d.rate, d.eff, d.abnormal, d.review])]
  }
  downloadCsv(rows, `统计报表_${dim}_${timeLabel.value}_2026-11-23.csv`)
  ElMessage.success(`已按「${dim}」维度导出 ${rows.length - 1} 行数据`)
}
</script>

<style scoped>
.rate-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
.rate-cell .bar-track {
  flex: 1;
  min-width: 40px;
}
.panel-pager {
  padding: 4px 14px 10px;
}
.report-bottom {
  grid-template-columns: minmax(0, 1fr) 360px;
  align-items: start;
}
.agent-col {
  scroll-margin-top: 14px;
}
.quick-qs {
  margin-bottom: 10px;
  padding: 11px 12px;
  border-radius: var(--radius-md);
  background: var(--surface);
  border: 1px solid var(--border);
}
.qs-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 650;
  color: var(--brand-700);
  margin-bottom: 7px;
}
.quick-qs button {
  display: block;
  width: 100%;
  text-align: left;
  margin-bottom: 6px;
  border: 1px solid var(--border);
  background: var(--surface-2);
  border-radius: 7px;
  padding: 6px 9px;
  font-size: 11.5px;
  color: var(--text-2);
  cursor: pointer;
}
.quick-qs button:last-child {
  margin-bottom: 0;
}
.quick-qs button:hover {
  border-color: var(--brand-300);
  background: var(--brand-50);
  color: var(--brand-700);
}
@media (max-width: 1280px) {
  .report-bottom {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
