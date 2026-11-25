<template>
  <div class="page">
    <PageHeader
      title="Agent 运行监控"
      code="AI-CENTER"
      group="AI 智能体"
      desc="监控 Agent 调用量、成功率、响应时延、Token 消耗与工具失败情况，支持会话回溯与错误定位。"
    >
      <template #actions>
        <div class="seg">
          <button v-for="r in ['今日', '近7天', '近30天']" :key="r" :class="{ active: range === r }" @click="range = r">{{ r }}</button>
        </div>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </template>
    </PageHeader>

    <div class="grid cols-6">
      <StatCard label="调用总次数" :value="totals.calls.toLocaleString()" unit="次" icon="DataLine" :delta="12.4" foot="较上周期" />
      <StatCard label="调用成功率" :value="totals.successRate" unit="%" icon="CircleCheck" :delta="0.8" foot="目标 ≥ 97%" />
      <StatCard label="平均响应" :value="totals.avgLatency" unit="s" icon="Timer" tone="blue" :delta="-0.3" foot="P95 4.8s" />
      <StatCard label="Token 消耗" :value="(totals.tokens / 1000000).toFixed(2)" unit="M" icon="Coin" tone="violet" :delta="8.1" foot="本月预算 62%" />
      <StatCard label="工具调用" :value="totals.toolCalls.toLocaleString()" unit="次" icon="Tools" tone="amber" :delta="15.2" foot="平均 3.6 次/会话" />
      <StatCard label="失败调用" :value="totals.failures" unit="次" icon="Warning" tone="red" delta="18" delta-tone="down" foot="已自动熔断 2 次" />
    </div>

    <div class="grid cols-2 mt-3">
      <PanelCard title="调用量与失败趋势" sub="近 7 天 · 单位：次">
        <ChartPanel :option="trendOption" :height="238" />
      </PanelCard>
      <PanelCard title="Agent 调用占比" sub="按累计调用次数">
        <ChartPanel :option="donutOption" :height="238" />
      </PanelCard>
    </div>

    <div class="grid cols-2 mt-3">
      <PanelCard title="响应时间分布" sub="P50 / P95 · 单位：秒">
        <ChartPanel :option="latencyOption" :height="232" />
      </PanelCard>
      <PanelCard title="失败原因分布" sub="近 7 天错误日志聚合">
        <ChartPanel :option="errorOption" :height="232" />
      </PanelCard>
    </div>

    <PanelCard title="Agent 运行状态" :sub="`${agents.length} 个 Agent · 2 个业务域`" class="mt-3" flush>
      <el-table :data="agents" size="small">
        <el-table-column label="Agent" min-width="190">
          <template #default="{ row }">
            <div style="display: flex; align-items: center; gap: 8px">
              <div class="agent-avatar" style="width: 28px; height: 28px; font-size: 10px">{{ row.id.replace('MA-', 'M') }}</div>
              <div>
                <div class="cell-main" style="font-size: 12px">{{ row.name }}</div>
                <div class="cell-sub mono">{{ row.id }} · {{ row.model }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="86">
          <template #default="{ row }"><StatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column prop="calls" label="调用" width="86" align="right">
          <template #default="{ row }"><span class="num">{{ row.calls.toLocaleString() }}</span></template>
        </el-table-column>
        <el-table-column prop="successRate" label="成功率" width="86" align="right">
          <template #default="{ row }"><span class="num">{{ row.successRate }}%</span></template>
        </el-table-column>
        <el-table-column prop="latency" label="平均响应" width="86" align="right">
          <template #default="{ row }"><span class="num">{{ row.latency }}s</span></template>
        </el-table-column>
        <el-table-column label="Token" width="96" align="right">
          <template #default="{ row }"><span class="num">{{ (row.tokens / 1000).toFixed(0) }}k</span></template>
        </el-table-column>
        <el-table-column label="健康度" width="120">
          <template #default="{ row }">
            <div class="bar-track"><div class="bar-fill" :class="{ amber: row.successRate < 97 }" :style="{ width: row.successRate + '%' }" /></div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }"><el-button size="small" text type="primary" @click="openAgent(row)">详情</el-button></template>
        </el-table-column>
      </el-table>
    </PanelCard>

    <div class="grid mt-3" style="grid-template-columns: minmax(0, 1.4fr) minmax(0, 1fr)">
      <PanelCard title="失败调用记录" sub="可定位具体会话与错误原因" flush>
        <el-table :data="failedLogs" size="small">
          <el-table-column prop="at" label="时间" width="120" />
          <el-table-column prop="agent" label="Agent" width="72" />
          <el-table-column prop="user" label="用户" width="72" />
          <el-table-column prop="question" label="任务" show-overflow-tooltip min-width="180" />
          <el-table-column prop="ms" label="耗时" width="76">
            <template #default="{ row }">{{ row.ms }}ms</template>
          </el-table-column>
          <el-table-column label="错误" min-width="200">
            <template #default="{ row }"><span class="err-text">{{ row.error }}</span></template>
          </el-table-column>
        </el-table>
      </PanelCard>

      <PanelCard title="最近会话" sub="点击查看完整对话与执行链">
        <div v-for="s in sessions" :key="s.id" class="session-row" @click="openSession(s)">
          <div class="session-avatar">{{ s.agent.replace('MA-', 'M') }}</div>
          <div style="min-width: 0">
            <div class="session-title">{{ s.title }}</div>
            <div class="session-meta">{{ s.user }} · {{ s.turns }} 轮 · {{ s.at.slice(5, 16) }}</div>
          </div>
          <StatusTag :status="s.status" style="margin-left: auto" />
        </div>
        <div class="hint-line mt-2">
          <el-icon :size="12"><InfoFilled /></el-icon>
          <span>会话状态保存在 Redis（TTL 24h），长期运行记录落库并纳入审计。</span>
        </div>
      </PanelCard>
    </div>

    <el-drawer v-model="agentOpen" :title="`${current?.id} · ${current?.name}`" size="520px">
      <div v-if="current" class="agent-detail">
        <div class="detail-grid">
          <div class="detail-field"><div class="detail-label">状态</div><StatusTag :status="current.status" /></div>
          <div class="detail-field"><div class="detail-label">模型</div><div class="detail-value mono">{{ current.model }}</div></div>
          <div class="detail-field"><div class="detail-label">成功率</div><div class="detail-value">{{ current.successRate }}%</div></div>
          <div class="detail-field"><div class="detail-label">平均响应</div><div class="detail-value">{{ current.latency }}s</div></div>
          <div class="detail-field"><div class="detail-label">数据范围</div><div class="detail-value">{{ current.scope }}</div></div>
          <div class="detail-field"><div class="detail-label">累计 Token</div><div class="detail-value">{{ (current.tokens / 1000).toFixed(0) }}k</div></div>
        </div>
        <div class="divider" />
        <div class="detail-label" style="margin-bottom: 6px">最近调用</div>
        <el-table :data="agentLogs" size="small">
          <el-table-column prop="at" label="时间" width="116" />
          <el-table-column prop="user" label="用户" width="66" />
          <el-table-column prop="question" label="任务" show-overflow-tooltip />
          <el-table-column prop="result" label="结果" width="64">
            <template #default="{ row }"><StatusTag :status="row.result" /></template>
          </el-table-column>
        </el-table>
      </div>
    </el-drawer>

    <el-dialog v-model="sessionOpen" :title="sessionDetail?.title" width="620px">
      <div v-if="sessionDetail">
        <div class="hint" style="font-size: 11px; margin-bottom: 10px">
          {{ sessionDetail.agent }} · {{ sessionDetail.user }} · {{ sessionDetail.at }} · 状态 {{ sessionDetail.status }}
        </div>
        <div v-for="(m, i) in sessionDetail.messages" :key="i" class="msg" :class="m.role" style="margin-bottom: 10px">
          <div class="msg-avatar">{{ m.role === 'user' ? '我' : 'AI' }}</div>
          <div class="msg-body"><div class="bubble">{{ m.text }}</div></div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { getAgentMonitor } from '@/services/api'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import PanelCard from '@/components/PanelCard.vue'
import StatusTag from '@/components/StatusTag.vue'
import ChartPanel from '@/components/ChartPanel.vue'

const range = ref('近7天')
const agents = ref([])
const logs = ref([])
const sessions = ref([])
const totals = ref({ calls: 0, successRate: 0, avgLatency: 0, tokens: 0, toolCalls: 0, failures: 0 })
const agentOpen = ref(false)
const sessionOpen = ref(false)
const current = ref(null)
const sessionDetail = ref(null)

const failedLogs = computed(() => logs.value.filter((l) => l.result === '失败'))
const agentLogs = computed(() => logs.value.filter((l) => l.agent === current.value?.id))
const trendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { right: 0, top: 0, itemWidth: 9, itemHeight: 9, textStyle: { fontSize: 10.5 } },
  xAxis: { type: 'category', data: ['11-17', '11-18', '11-19', '11-20', '11-21', '11-22', '11-23'] },
  yAxis: [{ type: 'value', splitLine: { lineStyle: { color: '#eef3f0' } } }, { type: 'value', splitLine: { show: false } }],
  series: [
    { name: '调用次数', type: 'bar', data: [620, 702, 756, 810, 884, 912, 542], barWidth: 16, itemStyle: { borderRadius: [4, 4, 0, 0], color: '#146b45' } },
    { name: '失败次数', type: 'line', yAxisIndex: 1, data: [6, 8, 5, 9, 4, 7, 3], smooth: true, symbolSize: 5, lineStyle: { color: '#e04b4b', width: 2 }, itemStyle: { color: '#e04b4b' } }
  ]
}))
const donutOption = computed(() => ({
  legend: { bottom: 0, itemWidth: 9, itemHeight: 9, textStyle: { fontSize: 10 } },
  series: [
    {
      type: 'pie',
      radius: ['48%', '70%'],
      center: ['50%', '44%'],
      avoidLabelOverlap: true,
      label: { show: false },
      itemStyle: { borderColor: '#fff', borderWidth: 2 },
      data: agents.value.map((a) => ({ name: a.id, value: a.calls }))
    }
  ]
}))
const latencyOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { right: 0, top: 0, itemWidth: 9, itemHeight: 9, textStyle: { fontSize: 10.5 } },
  xAxis: { type: 'category', data: agents.value.map((a) => a.id) },
  yAxis: { type: 'value', splitLine: { lineStyle: { color: '#eef3f0' } } },
  series: [
    { name: 'P50', type: 'bar', data: [2.1, 2.6, 1.5, 2.4, 3.8], barWidth: 12, itemStyle: { borderRadius: [3, 3, 0, 0], color: '#1fae6b' } },
    { name: 'P95', type: 'bar', data: [3.6, 4.8, 2.9, 4.2, 6.4], barWidth: 12, itemStyle: { borderRadius: [3, 3, 0, 0], color: '#e08a12' } }
  ]
}))
const errorOption = computed(() => ({
  tooltip: { trigger: 'item' },
  legend: { bottom: 0, itemWidth: 9, itemHeight: 9, textStyle: { fontSize: 10 } },
  series: [
    {
      type: 'pie',
      radius: ['42%', '68%'],
      center: ['50%', '43%'],
      label: { show: true, fontSize: 10, formatter: '{b}\n{d}%' },
      data: [
        { name: '工具超时', value: 26, itemStyle: { color: '#e04b4b' } },
        { name: '模型超时', value: 14, itemStyle: { color: '#e08a12' } },
        { name: '权限拒绝', value: 11, itemStyle: { color: '#3b7cf0' } },
        { name: '参数错误', value: 7, itemStyle: { color: '#8b5cf6' } }
      ]
    }
  ]
}))

async function load() {
  const d = await getAgentMonitor()
  agents.value = d.agents
  logs.value = d.logs
  sessions.value = d.sessions
  totals.value = d.totals
}
onMounted(load)
function openAgent(a) {
  current.value = a
  agentOpen.value = true
}
function openSession(s) {
  sessionDetail.value = s
  sessionOpen.value = true
}
</script>

<style scoped>
.err-text {
  color: #c53030;
  font-size: 11px;
}
.session-row {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 8px;
  border-radius: 8px;
  cursor: pointer;
}
.session-row:hover {
  background: var(--surface-2);
}
.session-avatar {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: var(--ai-gradient);
  color: #fff;
  display: grid;
  place-items: center;
  font-size: 10px;
  font-weight: 700;
  flex: none;
}
.session-title {
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.session-meta {
  font-size: 10px;
  color: var(--text-3);
  margin-top: 1px;
}
.agent-detail {
  padding: 2px;
}
</style>
