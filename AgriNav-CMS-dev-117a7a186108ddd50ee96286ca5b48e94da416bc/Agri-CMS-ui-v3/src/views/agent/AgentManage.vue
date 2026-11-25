<template>
  <div class="page">
    <PageHeader
      title="Agent 管理"
      code="AI-CENTER"
      group="AI 智能体"
      desc="统一维护智能体的模型、提示词、工具与权限范围。MA-04 / MA-05 为扩展能力，不影响既有业务模块。"
    >
      <template #actions>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="createOpen = true">新建 Agent</el-button>
      </template>
    </PageHeader>

    <div class="grid cols-4">
      <StatCard label="Agent 总数" :value="list.length" unit="个" icon="Cpu" foot="覆盖 5 类业务场景" />
      <StatCard label="运行中" :value="runningCount" unit="个" icon="VideoPlay" tone="blue" foot="灰度 1 个" />
      <StatCard label="累计调用" :value="totalCalls.toLocaleString()" unit="次" icon="DataLine" :delta="12.6" foot="较上周" />
      <StatCard label="平均成功率" value="97.4" unit="%" icon="CircleCheck" tone="amber" foot="失败主要来自工具超时" />
    </div>

    <div class="grid cols-3 mt-3">
      <div v-for="a in list" :key="a.id" class="agent-card">
        <div class="agent-head">
          <div class="agent-avatar">{{ a.id.replace('MA-', 'M') }}</div>
          <div style="min-width: 0">
            <div class="agent-code">{{ a.id }} · {{ a.type }}</div>
            <div class="agent-name">{{ a.name }}</div>
          </div>
          <StatusTag :status="a.status" style="margin-left: auto" />
        </div>
        <div class="agent-desc">{{ a.desc }}</div>
        <div class="agent-tools">
          <span v-for="t in toolNames(a.tools)" :key="t" class="tag no-dot gray" style="font-size: 10px; height: 20px">{{ t }}</span>
        </div>
        <div class="agent-metrics">
          <div class="agent-metric"><div class="v num">{{ a.calls.toLocaleString() }}</div><div class="l">调用次数</div></div>
          <div class="agent-metric"><div class="v num">{{ a.successRate }}%</div><div class="l">成功率</div></div>
          <div class="agent-metric"><div class="v num">{{ a.latency }}s</div><div class="l">平均响应</div></div>
          <div class="agent-metric"><div class="v num">{{ (a.tokens / 1000).toFixed(0) }}k</div><div class="l">Token</div></div>
        </div>
        <div class="agent-foot">
          <el-button size="small" @click="openConfig(a)">配置</el-button>
          <el-button size="small" text @click="openRuns(a)">运行记录</el-button>
          <span style="flex: 1" />
          <el-switch
            :model-value="a.status === '运行中'"
            size="small"
            @change="(v) => toggle(a, v)"
          />
        </div>
      </div>
    </div>

    <el-drawer v-model="configOpen" :title="`配置 ${form.id} · ${form.name}`" size="580px">
      <el-tabs v-model="tab">
        <el-tab-pane label="基础配置" name="base">
          <el-form label-position="top">
            <el-form-item label="Agent 名称"><el-input v-model="form.name" /></el-form-item>
            <el-form-item label="类型">
              <el-select v-model="form.type" style="width: 100%">
                <el-option v-for="t in ['业务决策辅助', '风险研判', '知识问答', '数据分析', '多工具编排']" :key="t" :value="t" :label="t" />
              </el-select>
            </el-form-item>
            <el-form-item label="职责描述"><el-input v-model="form.desc" type="textarea" :rows="3" /></el-form-item>
            <el-form-item label="数据权限范围">
              <el-select v-model="form.scope" style="width: 100%">
                <el-option v-for="s in ['全公司（按用户角色收敛）', '本片区 / 全公司', '按知识域权限过滤', '结算范围']" :key="s" :value="s" :label="s" />
              </el-select>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="模型参数" name="model">
          <el-form label-position="top">
            <el-form-item label="模型">
              <el-select v-model="form.model" style="width: 100%">
                <el-option v-for="m in ['GLM-4.5', 'GLM-4.5-Air', 'Qwen2.5-72B', 'Qwen2.5-32B']" :key="m" :value="m" :label="m" />
              </el-select>
            </el-form-item>
            <el-form-item :label="`温度 temperature：${form.temperature}`">
              <el-slider v-model="form.temperature" :min="0" :max="1" :step="0.1" />
            </el-form-item>
            <div class="hint-line">
              <el-icon :size="13"><InfoFilled /></el-icon>
              <span>业务类 Agent 建议温度 ≤ 0.3，避免生成不可追溯的面积或金额结论。</span>
            </div>
            <el-form-item label="最大上下文轮次" style="margin-top: 14px">
              <el-input-number v-model="form.maxTurns" :min="1" :max="20" />
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="系统提示词" name="prompt">
          <el-input v-model="form.prompt" type="textarea" :rows="14" class="mono" />
          <div class="hint" style="font-size: 11px; margin-top: 6px">提示词变更将记录审计日志，并影响下一次会话。</div>
        </el-tab-pane>
        <el-tab-pane label="工具与权限" name="tools">
          <div class="hint" style="font-size: 11.5px; margin-bottom: 8px">勾选该 Agent 可调用的业务工具（工具本身还会做二次权限校验）：</div>
          <el-checkbox-group v-model="form.tools">
            <div v-for="t in tools" :key="t.id" class="tool-check">
              <el-checkbox :value="t.id">
                <span class="mono" style="font-size: 11px">{{ t.id }}</span>
                <span style="margin-left: 6px">{{ t.name }}</span>
                <span class="hint" style="font-size: 10.5px; margin-left: 6px">· {{ t.module }}</span>
              </el-checkbox>
            </div>
          </el-checkbox-group>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="configOpen = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存配置</el-button>
      </template>
    </el-drawer>

    <el-drawer v-model="runsOpen" :title="`${runAgent?.id} 运行记录`" size="640px">
      <el-table :data="runs" size="small">
        <el-table-column prop="at" label="时间" width="130" />
        <el-table-column prop="user" label="调用人" width="80" />
        <el-table-column prop="question" label="问题 / 任务" show-overflow-tooltip />
        <el-table-column prop="result" label="结果" width="70">
          <template #default="{ row }"><StatusTag :status="row.result" /></template>
        </el-table-column>
        <el-table-column prop="ms" label="耗时" width="72">
          <template #default="{ row }">{{ row.ms }}ms</template>
        </el-table-column>
        <el-table-column prop="toolCalls" label="工具" width="56" />
      </el-table>
    </el-drawer>

    <el-dialog v-model="createOpen" title="新建 Agent" width="480px">
      <el-form label-position="top">
        <el-form-item label="名称"><el-input v-model="newAgent.name" placeholder="如：地块巡查 Agent" /></el-form-item>
        <el-form-item label="职责描述"><el-input v-model="newAgent.desc" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="模型">
          <el-select v-model="newAgent.model" style="width: 100%">
            <el-option label="GLM-4.5-Air" value="GLM-4.5-Air" />
            <el-option label="Qwen2.5-32B" value="Qwen2.5-32B" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createOpen = false">取消</el-button>
        <el-button type="primary" @click="createAgent">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAgents, getTools, getAgentMonitor, saveAgent } from '@/services/api'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import StatusTag from '@/components/StatusTag.vue'

const list = ref([])
const tools = ref([])
const logs = ref([])
const configOpen = ref(false)
const runsOpen = ref(false)
const createOpen = ref(false)
const saving = ref(false)
const tab = ref('base')
const form = reactive({})
const runAgent = ref(null)
const newAgent = reactive({ name: '', desc: '', model: 'GLM-4.5-Air' })

const runningCount = computed(() => list.value.filter((a) => a.status === '运行中').length)
const totalCalls = computed(() => list.value.reduce((s, a) => s + a.calls, 0))
const runs = computed(() => logs.value.filter((l) => l.agent === runAgent.value?.id))
const toolNames = (ids) => ids.map((id) => tools.value.find((t) => t.id === id)?.name || id)

async function load() {
  const [a, t, m] = await Promise.all([getAgents(), getTools(), getAgentMonitor()])
  list.value = a
  tools.value = t
  logs.value = m.logs
}
onMounted(load)

function openConfig(a) {
  Object.assign(form, JSON.parse(JSON.stringify(a)), { maxTurns: a.maxTurns || 6 })
  tab.value = 'base'
  configOpen.value = true
}
function openRuns(a) {
  runAgent.value = a
  runsOpen.value = true
}
async function save() {
  saving.value = true
  try {
    const res = await saveAgent({ ...form })
    const idx = list.value.findIndex((a) => a.id === form.id)
    if (idx >= 0) list.value[idx] = { ...list.value[idx], ...form }
    configOpen.value = false
    ElMessage.success(res.message)
  } finally {
    saving.value = false
  }
}
async function toggle(a, v) {
  const next = v ? '运行中' : '停用'
  try {
    await ElMessageBox.confirm(
      `${v ? '启用' : '停用'} ${a.id} ${a.name}？停用后该 Agent 将无法被业务页面调用。`,
      '变更 Agent 状态',
      { type: 'warning', confirmButtonText: '确认变更', cancelButtonText: '取消' }
    )
    a.status = next
    ElMessage.success(`已${v ? '启用' : '停用'} ${a.name}`)
  } catch {
    a.status = v ? '停用' : '运行中'
  }
}
function createAgent() {
  if (!newAgent.name.trim()) return ElMessage.warning('请填写 Agent 名称')
  list.value.push({
    id: `MA-0${list.value.length + 1}`,
    name: newAgent.name,
    type: '自定义',
    status: '停用',
    model: newAgent.model,
    temperature: 0.3,
    scope: '本片区 / 全公司',
    desc: newAgent.desc || '新建自定义 Agent',
    tools: [],
    prompt: '',
    calls: 0,
    successRate: 0,
    latency: 0,
    tokens: 0,
    updatedAt: '2026-11-23 10:00'
  })
  createOpen.value = false
  newAgent.name = ''
  newAgent.desc = ''
  ElMessage.success('Agent 已创建，默认停用，请完成配置后启用')
}
</script>

<style scoped>
.agent-tools {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 8px;
  min-height: 20px;
}
.agent-foot {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 10px;
  padding-top: 9px;
  border-top: 1px dashed var(--border);
}
.tool-check {
  padding: 6px 0;
  border-bottom: 1px dashed #f0f4f1;
}
</style>
