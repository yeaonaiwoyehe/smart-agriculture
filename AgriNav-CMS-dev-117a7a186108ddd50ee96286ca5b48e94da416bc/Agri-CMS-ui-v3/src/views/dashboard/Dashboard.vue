<template>
  <div class="page" v-loading="loading">
    <PageHeader
      title="农业土地与作物种植面积监测系统"
      code="MP-00"
      group="运营总览"
      desc="先进的农业生产智能化解决方案，助力现代农业发展，实时掌控农机作业动态与精准调度。"
    >
      <template #actions>
        <el-button-group>
          <el-button :type="range === 'today' ? 'primary' : 'default'" @click="range = 'today'">今日</el-button>
          <el-button :type="range === 'week' ? 'primary' : 'default'" @click="range = 'week'">近 7 天</el-button>
          <el-button :type="range === 'month' ? 'primary' : 'default'" @click="range = 'month'">本月</el-button>
        </el-button-group>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="MagicStick" @click="store.openAssistant('MA-04')">AI 运营分析</el-button>
      </template>
    </PageHeader>

    <div class="grid cols-6">
      <StatCard label="今日作业面积" :value="stats.todayArea" unit="亩" icon="MapLocation" :delta="6.4" :spark="[820, 940, 1180, 1090, 1320, 1402, 1286]" />
      <StatCard label="累计作业面积" :value="stats.totalArea" unit="亩" icon="DataLine" :delta="4.2" foot="较上月同期" :spark="[8200, 9600, 11800, 13200, 15600, 17200, 18462]" />
      <StatCard label="今日任务" :value="stats.todayTasks" unit="个" icon="Tickets" :delta="3.1" foot="已完成 31 · 待执行 11" :spark="[26, 31, 35, 33, 40, 45, 42]" />
      <StatCard label="在线农机" :value="stats.onlineMachines" unit="台" icon="Van" tone="blue" :delta="0" delta-tone="up" :foot="`在线率 ${stats.onlineRate}%`" :spark="[4, 5, 5, 6, 5, 6, 6]" />
      <StatCard label="作业完成率" :value="stats.completionRate" unit="%" icon="CircleCheck" tone="amber" :delta="-2.6" foot="低于目标 76%" :spark="[78, 76, 74, 79, 77, 75, 73.8]" />
      <StatCard label="待复核 / 异常" :value="`${stats.pendingReview} / ${stats.abnormalTasks}`" unit="单" icon="Warning" tone="red" :delta="12.5" delta-tone="down" foot="3 单已滞留超 18h" :spark="[3, 4, 5, 4, 6, 8, 10]" />
    </div>

    <!-- 新增：快捷入口卡片 -->
    <div class="quick-actions mt-3">
      <button class="quick-card" type="button" @click="$router.push('/monitor')">
        <span class="quick-icon green"><el-icon :size="22"><Location /></el-icon></span>
        <span class="quick-body">
          <span class="quick-title">智能轨迹管理</span>
          <span class="quick-sub">实时监控与轨迹回放</span>
        </span>
        <el-icon class="quick-arrow"><ArrowRight /></el-icon>
      </button>
      <button class="quick-card" type="button" @click="$router.push('/lands')">
        <span class="quick-icon blue"><el-icon :size="22"><Grid /></el-icon></span>
        <span class="quick-body">
          <span class="quick-title">精准地块管理</span>
          <span class="quick-sub">地块属性与作业规划</span>
        </span>
        <el-icon class="quick-arrow"><ArrowRight /></el-icon>
      </button>
    </div>

    <div class="grid cols-3 mt-3" style="grid-template-columns: minmax(0, 1.75fr) minmax(0, 1fr)">
      <PanelCard title="作业趋势与效率" sub="有效作业面积与任务量，数据来源：核算引擎">
        <template #extra>
          <div class="seg">
            <button :class="{ active: trendTab === 'area' }" @click="trendTab = 'area'">作业面积</button>
            <button :class="{ active: trendTab === 'task' }" @click="trendTab = 'task'">任务量</button>
          </div>
        </template>
        <ChartPanel :option="trendOption" :height="248" />
        <div class="trend-foot">
          <div><span class="hint">7 日均值</span><b class="num">{{ avgArea }} 亩</b></div>
          <div><span class="hint">峰值</span><b class="num">1,428 亩 · 11-22</b></div>
          <div><span class="hint">异常任务</span><b class="num" style="color: #c53030">3 个</b></div>
          <div><span class="hint">环比</span><b class="num" style="color: #1fae6b">+6.4%</b></div>
        </div>
      </PanelCard>

      <div class="ai-insight">
        <div class="ai-title-row">
          <span class="ai-dot" />
          <span class="ai-title">AI 运营分析</span>
          <span class="ai-chip" style="margin-left: auto; background: rgba(190, 242, 100, 0.14); border-color: rgba(190, 242, 100, 0.3); color: #d9ffb0">MA-04</span>
        </div>
        <div class="ai-summary">
          今日整体完成率 <b>73.8%</b>，较昨日下降 2.6 个百分点。北塬片区受降雨影响进度偏慢，另有 <b>3 台</b>农机处于闲置或待机状态，建议在 16:00 前完成一轮跨片区补位调度。
        </div>
        <div v-for="item in insights" :key="item.id" class="insight-item" @click="$router.push(item.route)">
          <span class="sev" :class="item.severity" />
          <div>
            <div class="txt" v-html="item.text" />
          </div>
          <span class="go">{{ item.action }} →</span>
        </div>
        <div class="ai-footnote">分析基于 2026-11-23 09:50 业务快照 · 面积/时长/金额均取自业务库，模型仅做归因与建议</div>
      </div>
    </div>

    <div class="grid mt-3" style="grid-template-columns: minmax(0, 1.55fr) minmax(0, 1fr) minmax(0, 1.05fr)">
      <PanelCard title="地图化作业分布" sub="12 个地块 · 6 台在线农机 · 作业热力">
        <template #extra>
          <span class="tag green">实时</span>
          <el-button text size="small" @click="$router.push('/monitor')">实时监控 →</el-button>
        </template>
        <FarmMap :lands="lands" :machines="machines" :heat="heatPoints" :height="336" live="09:50 更新" @select-land="goLand" @select-machine="goMachine" />
      </PanelCard>

      <PanelCard title="片区作业效率排行" sub="按完成率排序">
        <div v-for="(d, i) in districtRank" :key="d.name" class="rank-row">
          <span class="rank-no" :class="`top${i + 1}`">{{ i + 1 }}</span>
          <span class="rank-name">{{ d.name }}</span>
          <span class="rank-bar">
            <span class="bar-track"><span class="bar-fill" :class="{ amber: d.rate < 75 }" :style="{ width: d.rate + '%' }" /></span>
          </span>
          <span class="rank-val num">{{ d.rate }}%</span>
        </div>
        <div class="divider" />
        <div class="grid cols-2">
          <div class="mini-metric">
            <div class="v num">{{ stats.todayArea }}</div>
            <div class="l">今日作业面积（亩）</div>
          </div>
          <div class="mini-metric">
            <div class="v num" style="color: #c2740a">¥ {{ (stats.pendingAmount / 10000).toFixed(1) }}万</div>
            <div class="l">待结算金额</div>
          </div>
        </div>
      </PanelCard>

      <PanelCard title="农机作业动态" sub="实时上报事件流">
        <template #extra><span class="tag no-dot gray">近 2 小时</span></template>
        <div class="activity-list">
          <div v-for="a in activities" :key="a.id" class="activity-row">
            <div class="activity-time mono">{{ a.time }}</div>
            <div class="activity-line">
              <span class="activity-dot" :class="a.tone" />
            </div>
            <div class="activity-body">
              <div class="activity-type" :class="a.tone">{{ a.type }}</div>
              <div class="activity-text">{{ a.text }}</div>
            </div>
          </div>
        </div>
      </PanelCard>
    </div>

    <div class="grid mt-3" style="grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) minmax(0, 1fr)">
      <PanelCard title="农机效率 TOP5" sub="近 30 天平均效率（亩/小时）">
        <div v-for="m in machineRank" :key="m.name" class="m-eff-row">
          <div class="m-eff-icon"><el-icon :size="14"><Van /></el-icon></div>
          <div style="min-width: 0">
            <div class="strong" style="font-size: 12px">{{ m.name }}</div>
            <div class="hint" style="font-size: 10px">{{ m.model }}</div>
          </div>
          <div class="m-eff-bar"><span :style="{ width: (m.eff / 24) * 100 + '%' }" /></div>
          <div class="num strong" style="width: 58px; text-align: right">{{ m.eff }}</div>
        </div>
      </PanelCard>

      <PanelCard title="待办与风险" sub="需要人工处理的事项">
        <div class="todo-row" @click="$router.push('/workorders')">
          <span class="todo-badge red">7</span>
          <div><div class="strong">待复核作业单</div><div class="hint">3 单存在面积差异，需队长初核</div></div>
          <el-icon><ArrowRight /></el-icon>
        </div>
        <div class="todo-row" @click="$router.push('/dispatch')">
          <span class="todo-badge amber">4</span>
          <div><div class="strong">超时 / 临期任务</div><div class="hint">北塬村2号地任务 16:00 到期</div></div>
          <el-icon><ArrowRight /></el-icon>
        </div>
        <div class="todo-row" @click="$router.push('/settlement')">
          <span class="todo-badge violet">2</span>
          <div><div class="strong">结算单待核对</div><div class="hint">涉及扣减 386 元，需确认条款</div></div>
          <el-icon><ArrowRight /></el-icon>
        </div>
        <div class="hint-line mt-2">
          <el-icon :size="13"><Lock /></el-icon>
          <span>财务人员仅可访问结算与报表数据；所有写操作均需二次确认并留痕。</span>
        </div>
      </PanelCard>

      <PanelCard title="结算与面积概览" sub="2026-11 上半月">
        <ChartPanel :option="settleOption" :height="150" />
        <div class="grid cols-2 mt-2">
          <div class="mini-metric">
            <div class="v num">¥ 128,640</div>
            <div class="l">待结算金额</div>
          </div>
          <div class="mini-metric">
            <div class="v num">5,226</div>
            <div class="l">Agent 调用次数</div>
          </div>
        </div>
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
import StatCard from '@/components/StatCard.vue'
import PanelCard from '@/components/PanelCard.vue'
import ChartPanel from '@/components/ChartPanel.vue'
import FarmMap from '@/components/FarmMap.vue'

const store = useAppStore()
const router = useRouter()
const loading = ref(false)
const range = ref('today')
const trendTab = ref('area')
const stats = ref({})
const insights = ref([])
const activities = ref([])
const lands = ref([])
const machines = ref([])
const heatPoints = ref([])
const districtRank = ref([])
const machineRank = ref([])
const trend = ref({ days: [], area: [], tasks: [], abnormal: [] })

async function load() {
  loading.value = true
  try {
    const d = await getDashboard()
    stats.value = d.stats
    insights.value = d.insights
    activities.value = d.activities
    lands.value = d.lands
    machines.value = d.machines.filter((m) => m.online)
    heatPoints.value = d.heatPoints
    districtRank.value = d.districtRank
    machineRank.value = d.machineRank
    trend.value = d.trend
  } finally {
    loading.value = false
  }
}
onMounted(load)

const avgArea = computed(() => {
  const arr = trend.value.area || []
  return arr.length ? Math.round(arr.reduce((a, b) => a + b, 0) / arr.length).toLocaleString() : 0
})

const trendOption = computed(() => {
  const isArea = trendTab.value === 'area'
  return {
    grid: { left: 44, right: 36, top: 30, bottom: 26, containLabel: true },
    legend: { show: true, right: 0, top: 0, itemWidth: 9, itemHeight: 9, textStyle: { fontSize: 10.5 } },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: trend.value.days, axisTick: { show: false } },
    yAxis: [
      { type: 'value', name: '亩', nameTextStyle: { fontSize: 10, color: '#8b9992' }, splitLine: { lineStyle: { color: '#eef3f0' } } },
      { type: 'value', name: '个', nameTextStyle: { fontSize: 10, color: '#8b9992' }, splitLine: { show: false } }
    ],
    series: isArea
      ? [
          { name: '作业面积', type: 'bar', data: trend.value.area, barWidth: 18, itemStyle: { borderRadius: [5, 5, 0, 0], color: '#146b45' } },
          { name: '异常任务', type: 'line', yAxisIndex: 1, data: trend.value.abnormal, smooth: true, symbolSize: 5, lineStyle: { width: 2, color: '#e08a12' }, itemStyle: { color: '#e08a12' } }
        ]
      : [
          { name: '任务量', type: 'bar', data: trend.value.tasks, barWidth: 18, itemStyle: { borderRadius: [5, 5, 0, 0], color: '#1fae6b' } },
          { name: '异常任务', type: 'line', yAxisIndex: 1, data: trend.value.abnormal, smooth: true, symbolSize: 5, lineStyle: { width: 2, color: '#e04b4b' }, itemStyle: { color: '#e04b4b' } }
        ]
  }
})

const settleOption = {
  grid: { left: 0, right: 0, top: 10, bottom: 0 },
  xAxis: { type: 'category', data: ['待生成', '待核对', '待付款', '已付款'], axisLine: { show: false }, axisTick: { show: false }, axisLabel: { fontSize: 10 } },
  yAxis: { type: 'value', show: false },
  series: [{ type: 'bar', data: [12800, 46320, 38600, 30920], barWidth: 22, itemStyle: { borderRadius: [5, 5, 0, 0], color: (p) => ['#9db4a7', '#e08a12', '#1fae6b', '#146b45'][p.dataIndex] } }]
}

function goLand(l) {
  router.push('/lands')
}
function goMachine() {
  router.push('/machines')
}
</script>

<style scoped>
/* 新增：快捷入口卡片 */
.quick-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}
.quick-card {
  display: flex;
  align-items: center;
  gap: 13px;
  width: 100%;
  min-width: 0;
  padding: 13px 16px;
  text-align: left;
  cursor: pointer;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  box-shadow: 0 1px 3px rgba(16, 46, 33, 0.04);
  font-family: inherit;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}
.quick-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 22px rgba(16, 46, 33, 0.10);
  border-color: var(--brand-300);
}
.quick-card:active {
  transform: translateY(0);
}
.quick-icon {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  flex: none;
}
.quick-icon.green {
  background: var(--brand-50);
  color: var(--brand-600);
}
.quick-icon.blue {
  background: #eaf2fe;
  color: #2f6fd0;
}
.quick-body {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}
.quick-title {
  font-size: 13.5px;
  font-weight: 650;
  color: var(--text-1);
}
.quick-sub {
  font-size: 11.5px;
  color: var(--text-3);
}
.quick-arrow {
  margin-left: auto;
  color: var(--text-3);
  transition: transform 0.2s ease, color 0.2s ease;
}
.quick-card:hover .quick-arrow {
  color: var(--brand-600);
  transform: translateX(3px);
}
@media (max-width: 900px) {
  .quick-actions {
    grid-template-columns: minmax(0, 1fr);
  }
}
@media (max-width: 1200px) {
  .quick-actions .quick-title {
    font-size: 12.5px;
  }
  .quick-actions .quick-sub {
    font-size: 10.5px;
  }
}

.trend-foot {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  padding-top: 10px;
  border-top: 1px solid var(--border);
  margin-top: 4px;
}
.trend-foot > div {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.trend-foot b {
  font-size: 13px;
}
.activity-list {
  display: flex;
  flex-direction: column;
}
.activity-row {
  display: flex;
  gap: 8px;
  min-height: 42px;
}
.activity-time {
  width: 36px;
  flex: none;
  font-size: 10.5px;
  color: var(--text-3);
  padding-top: 2px;
}
.activity-line {
  width: 12px;
  flex: none;
  position: relative;
  display: flex;
  justify-content: center;
}
.activity-line::after {
  content: "";
  position: absolute;
  top: 12px;
  bottom: 0;
  width: 1px;
  background: var(--border);
}
.activity-row:last-child .activity-line::after {
  display: none;
}
.activity-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  margin-top: 5px;
  position: relative;
  z-index: 1;
  background: var(--text-3);
}
.activity-dot.green {
  background: #1fae6b;
}
.activity-dot.red {
  background: #e04b4b;
}
.activity-dot.blue {
  background: #3b7cf0;
}
.activity-dot.violet {
  background: #8b5cf6;
}
.activity-dot.gray {
  background: #a8b4ae;
}
.activity-body {
  padding-bottom: 10px;
  min-width: 0;
}
.activity-type {
  font-size: 10px;
  font-weight: 700;
  color: var(--text-3);
}
.activity-type.green {
  color: #1b8655;
}
.activity-type.red {
  color: #c53030;
}
.activity-type.blue {
  color: #2160d4;
}
.activity-type.violet {
  color: #6d3bd1;
}
.activity-text {
  font-size: 11.5px;
  color: var(--text-1);
  line-height: 1.5;
}
.m-eff-row {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 7px 0;
}
.m-eff-icon {
  width: 26px;
  height: 26px;
  border-radius: 7px;
  background: var(--brand-50);
  color: var(--brand-600);
  display: grid;
  place-items: center;
  flex: none;
}
.m-eff-bar {
  flex: 1;
  height: 5px;
  border-radius: 4px;
  background: var(--surface-3);
  overflow: hidden;
}
.m-eff-bar span {
  display: block;
  height: 100%;
  border-radius: 4px;
  background: linear-gradient(90deg, #146b45, #8be0b4);
}
.todo-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 8px;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 4px;
}
.todo-row:hover {
  background: var(--surface-2);
}
.todo-row .el-icon {
  margin-left: auto;
  color: var(--text-3);
}
.todo-row .strong {
  font-size: 12px;
}
.todo-row .hint {
  font-size: 10.5px;
}
.todo-badge {
  width: 26px;
  height: 26px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  font-size: 11px;
  font-weight: 700;
  flex: none;
}
.todo-badge.red {
  background: var(--red-100);
  color: var(--red-600);
}
.todo-badge.amber {
  background: var(--amber-100);
  color: var(--amber-600);
}
.todo-badge.violet {
  background: var(--violet-100);
  color: var(--violet-600);
}
</style>
