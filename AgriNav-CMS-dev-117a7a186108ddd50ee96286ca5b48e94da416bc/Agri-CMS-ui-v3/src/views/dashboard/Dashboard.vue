<template>
  <div class="page" v-loading="loading">
    <PageHeader title="农业土地与作物种植面积监测系统" code="MP-00" group="运营总览"
      desc="地块、农机与作业完成情况的真实数据视图。">
      <template #actions>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="MagicStick" @click="store.openAssistant('MA-04')">AI 运营分析</el-button>
      </template>
    </PageHeader>

    <div class="grid stat-grid">
      <StatCard label="地块总数" :value="landCount" unit="块" icon="MapLocation"
        :foot="landsFoot" />
      <StatCard label="地块总面积" :value="areaText" unit="亩" icon="DataLine" :foot="areaFoot" />
      <StatCard label="农机总数" :value="machineTotalText" unit="台" icon="Van" tone="blue"
        :foot="machineFoot" />
      <StatCard label="已上架农机" :value="listedText" unit="台" icon="Van" tone="blue"
        foot="上架状态，非实时在线" />
      <StatCard label="作业完成率" :value="completionText" unit="%" icon="CircleCheck" tone="amber"
        :foot="completionFoot" />
      <StatCard label="用户活跃率" :value="activeText" unit="%" icon="DataLine" tone="amber"
        :foot="activeFoot" />
      <StatCard label="Agent 交互审计记录数" :value="agentAuditText" unit="条" icon="Tickets"
        :foot="agentAuditFoot" />
    </div>

    <div v-if="fatalError" class="mt-3">
      <el-alert type="error" :closable="false" show-icon :title="fatalError">
        <el-button size="small" @click="load">重新加载</el-button>
      </el-alert>
    </div>

    <div v-if="moduleErrors.length" class="mt-3">
      <el-alert type="error" :closable="false" show-icon title="部分数据模块加载失败">
        <ul class="err-list">
          <li v-for="item in moduleErrors" :key="item.key">{{ item.label }}：{{ item.error.message }}</li>
        </ul>
        <el-button size="small" @click="load">重新加载</el-button>
      </el-alert>
    </div>

    <div class="quick-actions mt-3">
      <div class="card quick-card" @click="$router.push('/land')">
        <div class="quick-icon"><el-icon><MapLocation /></el-icon></div>
        <div><div class="quick-title">地块管理</div><div class="quick-desc">查看地块档案与测绘边界</div></div>
      </div>
      <div class="card quick-card" @click="$router.push('/machinery')">
        <div class="quick-icon blue"><el-icon><Van /></el-icon></div>
        <div><div class="quick-title">农机管理</div><div class="quick-desc">上架状态与作业能力查询</div></div>
      </div>
    </div>

    <div class="mt-3">
      <PanelCard title="地图化作业分布" :sub="mapSub" flush>
        <template #extra>
          <el-button text size="small" @click="$router.push('/monitor')">实时监控 →</el-button>
        </template>
        <div v-if="mapErrors.length" class="pad-alert">
          <el-alert type="warning" :closable="false" show-icon title="部分地图数据加载失败">
            <ul class="err-list">
              <li v-for="item in mapErrors" :key="item.key">{{ item.label }}：{{ item.error.message }}</li>
            </ul>
          </el-alert>
        </div>
        <FarmMap :lands="lands" :machines="machines" :heat="heatPoints" :height="360"
          @select-land="goLand" @select-machine="goMachine" />
        <div v-if="!mapErrors.length && !lands.length && !machines.length && !heatPoints.length" class="pad-empty">
          <el-empty description="暂无地块、农机或热力数据" />
        </div>
      </PanelCard>
    </div>

    <div class="grid grid-2 mt-3">
      <PanelCard title="数据范围说明">
        <ul class="note-list">
          <li>地块：接口仅返回当前账号创建的地块，不代表全系统地块总量。</li>
          <li>农机：管理员可见全量，其他账号仅可见本人发布；已上架不等于实时在线。</li>
          <li>地块总面积、作业完成率、用户活跃率来自大屏统计接口，需要具备大屏展示权限。</li>
        </ul>
      </PanelCard>
      <PanelCard title="更多统计能力待接入">
        <ul class="note-list">
          <li>今日 / 累计作业面积、任务数、作业效率</li>
          <li>7 日趋势、片区排行、农机效率 TOP5</li>
          <li>AI 运营洞察、业务动态、待办与风险、结算汇总</li>
        </ul>
        <div class="hint-line mt-2">上述指标后端暂无对应统计接口，本页不使用模拟或估算数据。</div>
      </PanelCard>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { MagicStick, Refresh } from '@element-plus/icons-vue'
import { useAppStore } from '@/stores/app'
import { getDashboard } from '@/services/api'
import PageHeader from '@/components/PageHeader.vue'
import PanelCard from '@/components/PanelCard.vue'
import StatCard from '@/components/StatCard.vue'
import FarmMap from '@/components/FarmMap.vue'

const store = useAppStore()
const router = useRouter()

const loading = ref(false)
const data = ref(null)
const fatalError = ref(null)

const lands = computed(() => data.value?.lands?.list || [])
const machines = computed(() => data.value?.machines?.list || [])
const heatPoints = computed(() => data.value?.heat?.points || [])

function fmtNumber(value, digits = 1) {
  if (value === null || value === undefined || value === '') return '—'
  const n = Number(value)
  if (!Number.isFinite(n)) return '—'
  return n.toLocaleString('zh-CN', { maximumFractionDigits: digits })
}

const landCount = computed(() => (data.value?.lands?.error ? '—' : String(lands.value.length)))
const landsFoot = computed(() => {
  if (data.value?.lands?.error) return '不可用'
  if (data.value?.lands?.truncated) return '已取前 1000 条，实际可能更多'
  return '数据范围：当前账号可见地块'
})

const areaText = computed(() => (data.value?.totalArea?.error ? '—' : fmtNumber(data.value?.totalArea?.value)))
const areaFoot = computed(() => {
  if (data.value?.totalArea?.error) return data.value.totalArea.error.message
  if (data.value?.totalArea?.value === null) return '暂无数据'
  return '全量地块面积统计'
})

const machineTotalText = computed(() => (data.value?.machines?.error ? '—' : fmtNumber(data.value?.machines?.total, 0)))
const listedText = computed(() => (data.value?.machines?.error ? '—' : fmtNumber(data.value?.machines?.listedCount, 0)))
const machineFoot = computed(() => {
  if (data.value?.machines?.error) return data.value.machines.error.message
  if (data.value?.machines?.truncated) return '已取前 1000 条，实际可能更多'
  return '管理员全量 / 其他账号本人'
})

const completionText = computed(() => {
  if (data.value?.completionRate?.error) return '—'
  return fmtNumber(data.value?.completionRate?.value)
})
const completionFoot = computed(() => {
  if (data.value?.completionRate?.error) return data.value.completionRate.error.message
  if (data.value?.completionRate?.value === null) return '暂无数据'
  return '全量农活完成率'
})

const activeText = computed(() => {
  if (data.value?.activeRate?.error) return '—'
  return fmtNumber(data.value?.activeRate?.value)
})
const activeFoot = computed(() => {
  if (data.value?.activeRate?.error) return data.value.activeRate.error.message
  if (data.value?.activeRate?.value === null) return '暂无数据'
  return '当日活跃用户占比'
})

const agentAuditText = computed(() => {
  if (data.value?.agentAudit?.error) return '—'
  return fmtNumber(data.value?.agentAudit?.total, 0)
})
const agentAuditFoot = computed(() => {
  if (data.value?.agentAudit?.error) return data.value.agentAudit.error.message
  if (data.value?.agentAudit?.total === null) return '暂无数据'
  return '审计记录数，不是工具调用次数'
})

const moduleErrors = computed(() => {
  if (!data.value) return []
  const items = [
    { key: 'lands', label: '地块', error: data.value.lands?.error },
    { key: 'machines', label: '农机', error: data.value.machines?.error },
    { key: 'heat', label: '热力', error: data.value.heat?.error },
    { key: 'area', label: '地块总面积', error: data.value.totalArea?.error },
    { key: 'completion', label: '作业完成率', error: data.value.completionRate?.error },
    { key: 'active', label: '用户活跃率', error: data.value.activeRate?.error },
    { key: 'agentAudit', label: 'Agent 审计', error: data.value.agentAudit?.error }
  ]
  return items.filter((i) => i.error)
})

const mapErrors = computed(() => moduleErrors.value.filter((i) => ['lands', 'machines', 'heat'].includes(i.key)))

const mapSub = computed(() => {
  if (data.value?.lands?.error && data.value?.machines?.error) return '地图数据暂不可用'
  return `${lands.value.length} 个可见地块 · ${data.value?.machines?.listedCount ?? 0} 台已上架农机 · ${heatPoints.value.length} 个热力点`
})

async function load() {
  loading.value = true
  fatalError.value = null
  try {
    data.value = await getDashboard()
  } catch (err) {
    fatalError.value = err?.message || '加载失败，请稍后重试。'
  } finally {
    loading.value = false
  }
}

function goLand(land) {
  router.push(land?.id ? `/land?focus=${land.id}` : '/land')
}
function goMachine(machine) {
  router.push(machine?.id ? `/machinery?focus=${machine.id}` : '/machinery')
}

onMounted(load)
</script>

<style scoped>
.stat-grid { grid-template-columns: repeat(auto-fit, minmax(168px, 1fr)); }
.quick-actions { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.quick-card { display: flex; align-items: center; gap: 12px; padding: 14px 16px; cursor: pointer; transition: border-color .15s; }
.quick-card:hover { border-color: var(--brand-500); }
.quick-icon { width: 36px; height: 36px; border-radius: 10px; display: grid; place-items: center; color: var(--brand-600); background: var(--brand-50); font-size: 18px; }
.quick-icon.blue { color: #2563eb; background: #eff6ff; }
.quick-title { font-size: 13px; font-weight: 600; }
.quick-desc { font-size: 11.5px; color: var(--text-2); margin-top: 2px; }
.grid-2 { grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); }
.err-list { margin: 6px 0; padding-left: 18px; font-size: 12px; line-height: 1.7; }
.note-list { margin: 0; padding-left: 18px; font-size: 12.5px; line-height: 1.9; color: var(--text-2); }
.pad-alert { padding: 10px 14px; }
.pad-empty { padding: 18px 14px; }
@media (max-width: 900px) {
  .quick-actions, .grid-2 { grid-template-columns: 1fr; }
}
</style>
