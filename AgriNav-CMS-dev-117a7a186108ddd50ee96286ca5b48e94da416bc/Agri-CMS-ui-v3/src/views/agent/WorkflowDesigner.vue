<template>
  <div class="page">
    <PageHeader
      title="Agent 工作流编排"
      code="AI-CENTER"
      group="AI 智能体"
      desc="用可视化流程图描述 Agent 的执行逻辑：意图识别、工具调用、条件分支、人工审批与异常处理，全程可追踪。"
    >
      <template #actions>
        <el-select v-model="currentId" style="width: 230px" @change="loadFlow">
          <el-option v-for="f in flows" :key="f.id" :value="f.id" :label="`${f.id} · ${f.name}`" />
        </el-select>
        <el-button :icon="RefreshLeft" @click="reset">复位</el-button>
        <el-button :icon="VideoPlay" :loading="running" @click="runFlow">运行演示</el-button>
        <el-button type="primary" @click="save">保存并发布</el-button>
      </template>
    </PageHeader>

    <div class="flow-meta">
      <span class="tag green">{{ currentFlow?.agent }}</span>
      <span class="hint">{{ currentFlow?.desc }}</span>
      <span class="flow-stat"><b class="num">{{ nodes.length }}</b> 节点</span>
      <span class="flow-stat"><b class="num">{{ edges.length }}</b> 连线</span>
      <span class="flow-stat"><b class="num">{{ currentFlow?.runs }}</b> 次运行 · 成功率 {{ currentFlow?.successRate }}%</span>
    </div>

    <div class="flow-shell mt-2">
      <aside class="flow-palette">
        <div class="palette-title">节点组件</div>
        <div v-for="p in palette" :key="p.type" class="palette-item" @click="addNode(p.type)">
          <span class="node-dot" :style="{ background: p.color }" />
          <span>{{ p.label }}</span>
          <el-icon style="margin-left: auto; color: var(--text-3)" :size="12"><Plus /></el-icon>
        </div>
        <div class="palette-title" style="margin-top: 14px">使用提示</div>
        <div class="hint" style="font-size: 10.5px; line-height: 1.7">
          点击左侧添加节点，拖动节点调整位置；点击节点右侧圆点再点另一个节点左侧圆点即可连线。
        </div>
        <div class="palette-title" style="margin-top: 14px">审批约束</div>
        <div class="hint-line" style="font-size: 10px">
          <el-icon :size="12"><Lock /></el-icon>
          <span>派单/复核/结算类写操作必须经过人工审批节点。</span>
        </div>
      </aside>

      <div ref="canvasRef" class="flow-canvas" @click.self="selectedId = ''">
        <div class="flow-inner" :style="{ width: canvasW + 'px', height: canvasH + 'px' }">
          <svg :width="canvasW" :height="canvasH" class="flow-edges">
            <defs>
              <marker id="arrow" markerWidth="9" markerHeight="9" refX="7" refY="4.5" orient="auto">
                <path d="M0,0 L9,4.5 L0,9 Z" fill="#9db4a7" />
              </marker>
            </defs>
            <path
              v-for="(e, i) in edgePaths"
              :key="i"
              :d="e.d"
              fill="none"
              :stroke="e.active ? '#1fae6b' : '#b9c9bf'"
              :stroke-width="e.active ? 2.2 : 1.4"
              marker-end="url(#arrow)"
            />
          </svg>

          <div
            v-for="n in nodes"
            :key="n.id"
            class="flow-node"
            :class="{ selected: selectedId === n.id, running: runningId === n.id, done: doneIds.includes(n.id) }"
            :style="{ left: n.x + 'px', top: n.y + 'px' }"
            @mousedown.stop="startDrag($event, n)"
            @click.stop="selectedId = n.id"
          >
            <div class="flow-node-kind" :style="{ color: typeColor(n.type) }">{{ typeLabel(n.type) }}</div>
            <div class="flow-node-name">{{ n.name }}</div>
            <div v-if="n.tool || n.agent" class="flow-node-ref mono">{{ n.tool || n.agent }}</div>
            <span v-if="n.type !== 'start'" class="flow-port in" @click.stop="finishConnect(n.id)" />
            <span v-if="n.type !== 'output' && n.type !== 'exception'" class="flow-port out" @click.stop="startConnect(n.id)" />
          </div>
        </div>
      </div>

      <aside class="flow-config">
        <div class="palette-title">节点配置</div>
        <template v-if="selectedNode">
          <el-form label-position="top" size="small">
            <el-form-item label="节点名称"><el-input v-model="selectedNode.name" /></el-form-item>
            <el-form-item label="节点类型">
              <el-select v-model="selectedNode.type" style="width: 100%">
                <el-option v-for="p in palette" :key="p.type" :value="p.type" :label="p.label" />
              </el-select>
            </el-form-item>
            <el-form-item v-if="selectedNode.type === 'tool'" label="调用工具">
              <el-select v-model="selectedNode.tool" style="width: 100%">
                <el-option v-for="t in tools" :key="t.id" :value="t.id" :label="`${t.id} ${t.name}`" />
              </el-select>
            </el-form-item>
            <el-form-item v-if="selectedNode.type === 'agent'" label="执行 Agent">
              <el-select v-model="selectedNode.agent" style="width: 100%">
                <el-option v-for="a in agents" :key="a.id" :value="a.id" :label="`${a.id} ${a.name}`" />
              </el-select>
            </el-form-item>
            <el-form-item v-if="selectedNode.type === 'condition'" label="判断条件">
              <el-input v-model="selectedNode.condition" type="textarea" :rows="2" placeholder="如：预计完成时间 <= 截止时间" />
            </el-form-item>
            <el-form-item v-if="selectedNode.type === 'approval'" label="审批角色">
              <el-select v-model="selectedNode.role" style="width: 100%">
                <el-option label="片区生产队长" value="leader" />
                <el-option label="公司管理员" value="admin" />
                <el-option label="财务人员" value="finance" />
              </el-select>
            </el-form-item>
            <el-form-item v-if="selectedNode.type === 'output'" label="输出模板">
              <el-input v-model="selectedNode.output" type="textarea" :rows="3" placeholder="结论 + 数据依据 + 业务跳转" />
            </el-form-item>
          </el-form>
          <el-button size="small" type="danger" plain style="width: 100%" @click="removeNode">删除节点</el-button>
          <div class="hint" style="font-size: 10px; margin-top: 6px">节点 ID：<span class="mono">{{ selectedNode.id }}</span></div>
        </template>
        <div v-else class="empty" style="padding: 40px 6px">选择画布中的节点进行配置</div>
      </aside>
    </div>

    <PanelCard title="运行日志" sub="演示执行按节点顺序高亮，真实执行由 LangGraph 驱动" class="mt-3">
      <div v-if="runLogs.length === 0" class="empty" style="padding: 18px">点击右上角「运行演示」查看执行过程</div>
      <div v-else class="run-log">
        <div v-for="(l, i) in runLogs" :key="i" class="run-row">
          <span class="run-time mono">{{ l.time }}</span>
          <span class="tag no-dot" :class="l.tone" style="height: 19px; font-size: 10px">{{ l.kind }}</span>
          <span class="run-text">{{ l.text }}</span>
          <span class="run-status" :class="l.status === '成功' ? 'ok' : 'pending'">{{ l.status }}</span>
        </div>
      </div>
    </PanelCard>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { Plus, RefreshLeft, VideoPlay } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getWorkflows, getTools, getAgents, saveWorkflow } from '@/services/api'
import PageHeader from '@/components/PageHeader.vue'
import PanelCard from '@/components/PanelCard.vue'

const flows = ref([])
const currentId = ref('WF-01')
const nodes = ref([])
const edges = ref([])
const tools = ref([])
const agents = ref([])
const selectedId = ref('')
const runningId = ref('')
const doneIds = ref([])
const running = ref(false)
const runLogs = ref([])
const canvasRef = ref(null)
const canvasW = 1420
const canvasH = 620
let timers = []
let drag = null

const palette = [
  { type: 'start', label: '开始节点', color: '#146b45' },
  { type: 'intent', label: '意图识别', color: '#2160d4' },
  { type: 'condition', label: '条件判断', color: '#c2740a' },
  { type: 'agent', label: 'Agent 节点', color: '#6d3bd1' },
  { type: 'tool', label: '工具调用', color: '#1b8655' },
  { type: 'approval', label: '人工审批', color: '#e08a12' },
  { type: 'output', label: '结果输出', color: '#1fae6b' },
  { type: 'exception', label: '异常处理', color: '#c53030' }
]
const typeLabel = (t) => palette.find((p) => p.type === t)?.label || t
const typeColor = (t) => palette.find((p) => p.type === t)?.color || '#8b9992'
const currentFlow = computed(() => flows.value.find((f) => f.id === currentId.value))
const selectedNode = computed(() => nodes.value.find((n) => n.id === selectedId.value))

const edgePaths = computed(() =>
  edges.value
    .map(([from, to]) => {
      const a = nodes.value.find((n) => n.id === from)
      const b = nodes.value.find((n) => n.id === to)
      if (!a || !b) return null
      const x1 = a.x + 172
      const y1 = a.y + 34
      const x2 = b.x
      const y2 = b.y + 34
      const mx = (x1 + x2) / 2
      return {
        d: `M ${x1} ${y1} C ${mx} ${y1}, ${mx} ${y2}, ${x2} ${y2}`,
        active: doneIds.value.includes(from) && (doneIds.value.includes(to) || runningId.value === to)
      }
    })
    .filter(Boolean)
)

async function load() {
  const [f, t, a] = await Promise.all([getWorkflows(), getTools(), getAgents()])
  flows.value = f
  tools.value = t
  agents.value = a
  loadFlow()
}
function loadFlow() {
  const f = currentFlow.value
  nodes.value = JSON.parse(JSON.stringify(f?.nodes || []))
  edges.value = JSON.parse(JSON.stringify(f?.edges || []))
  selectedId.value = ''
  resetRun()
}
onMounted(load)

function startDrag(e, n) {
  drag = { id: n.id, sx: e.clientX, sy: e.clientY, ox: n.x, oy: n.y }
  selectedId.value = n.id
  window.addEventListener('mousemove', onDrag)
  window.addEventListener('mouseup', endDrag)
}
function onDrag(e) {
  if (!drag) return
  const n = nodes.value.find((x) => x.id === drag.id)
  if (!n) return
  n.x = Math.max(4, Math.min(canvasW - 190, drag.ox + e.clientX - drag.sx))
  n.y = Math.max(4, Math.min(canvasH - 80, drag.oy + e.clientY - drag.sy))
}
function endDrag() {
  drag = null
  window.removeEventListener('mousemove', onDrag)
  window.removeEventListener('mouseup', endDrag)
}

const connectingFrom = ref('')
function startConnect(id) {
  connectingFrom.value = id
  ElMessage.info('已选择起点，请点击目标节点左侧圆点完成连线')
}
function finishConnect(toId) {
  if (!connectingFrom.value || connectingFrom.value === toId) return
  if (edges.value.some(([a, b]) => a === connectingFrom.value && b === toId)) {
    connectingFrom.value = ''
    return ElMessage.warning('连线已存在')
  }
  edges.value.push([connectingFrom.value, toId])
  connectingFrom.value = ''
  ElMessage.success('连线已创建')
}

let seq = 100
function addNode(type) {
  const p = palette.find((x) => x.type === type)
  seq += 1
  nodes.value.push({
    id: `n${seq}`,
    type,
    name: p.label,
    x: 60 + (nodes.value.length % 4) * 30,
    y: 420,
    ...(type === 'condition' ? { condition: '是否满足业务约束' } : {}),
    ...(type === 'approval' ? { role: 'leader' } : {})
  })
  ElMessage.success(`已添加「${p.label}」，请拖动到合适位置`)
}
function removeNode() {
  const id = selectedId.value
  edges.value = edges.value.filter(([a, b]) => a !== id && b !== id)
  nodes.value = nodes.value.filter((n) => n.id !== id)
  selectedId.value = ''
}
function reset() {
  loadFlow()
  ElMessage.info('已恢复为最近保存的流程')
}
async function save() {
  try {
    await ElMessageBox.confirm('保存后将立即发布到线上工作流，是否继续？', '发布工作流', { type: 'warning', confirmButtonText: '保存并发布' })
    const res = await saveWorkflow({ ...currentFlow.value, nodes: nodes.value, edges: edges.value })
    const idx = flows.value.findIndex((f) => f.id === currentId.value)
    if (idx >= 0) flows.value[idx] = { ...flows.value[idx], nodes: nodes.value, edges: edges.value }
    ElMessage.success(res.message)
  } catch {}
}

function clearTimers() {
  timers.forEach((t) => clearTimeout(t))
  timers = []
}
function resetRun() {
  clearTimers()
  running.value = false
  runningId.value = ''
  doneIds.value = []
  runLogs.value = []
}
function buildPath() {
  const start = nodes.value.find((n) => n.type === 'start')
  if (!start) return []
  const path = [start.id]
  let cur = start.id
  const guard = new Set([cur])
  for (let i = 0; i < 20; i++) {
    const next = edges.value.find(([a]) => a === cur)
    if (!next || guard.has(next[1])) break
    cur = next[1]
    guard.add(cur)
    path.push(cur)
  }
  return path
}
function runFlow() {
  resetRun()
  const path = buildPath()
  if (!path.length) return ElMessage.warning('流程缺少开始节点')
  running.value = true
  let t = 0
  path.forEach((id, i) => {
    const n = nodes.value.find((x) => x.id === id)
    timers.push(
      setTimeout(() => {
        if (i > 0) doneIds.value.push(path[i - 1])
        runningId.value = id
        runLogs.value.unshift({
          time: new Date().toLocaleTimeString('zh-CN', { hour12: false }),
          kind: typeLabel(n.type),
          tone: n.type === 'approval' ? 'amber' : n.type === 'condition' ? 'blue' : 'green',
          text: n.name,
          status: i === path.length - 1 ? '成功' : '完成'
        })
        if (i === path.length - 1) {
          doneIds.value.push(id)
          runningId.value = ''
          running.value = false
          ElMessage.success('演示执行完成：结果已输出，未写入正式业务数据')
        }
      }, (t += 620))
    )
  })
}
onBeforeUnmount(() => {
  clearTimers()
  window.removeEventListener('mousemove', onDrag)
  window.removeEventListener('mouseup', endDrag)
})
</script>

<style scoped>
.flow-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.flow-stat {
  font-size: 11px;
  color: var(--text-3);
}
.flow-stat b {
  color: var(--text-1);
  font-size: 12px;
}
.flow-inner {
  position: relative;
}
.flow-edges {
  position: absolute;
  inset: 0;
  pointer-events: none;
}
.flow-node-ref {
  font-size: 9.5px;
  color: var(--text-3);
  margin-top: 2px;
}
.flow-config {
  width: 250px;
  flex: none;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: #fff;
  padding: 12px;
  overflow-y: auto;
  max-height: 640px;
}
.node-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex: none;
}
.run-log {
  max-height: 200px;
  overflow-y: auto;
}
.run-row {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 6px 0;
  border-bottom: 1px dashed #f0f4f1;
  font-size: 11.5px;
}
.run-time {
  width: 62px;
  color: var(--text-3);
  font-size: 10.5px;
  flex: none;
}
.run-text {
  color: var(--text-1);
}
.run-status {
  margin-left: auto;
  font-size: 10.5px;
}
.run-status.ok {
  color: #1b8655;
  font-weight: 600;
}
.run-status.pending {
  color: var(--text-3);
}
</style>
