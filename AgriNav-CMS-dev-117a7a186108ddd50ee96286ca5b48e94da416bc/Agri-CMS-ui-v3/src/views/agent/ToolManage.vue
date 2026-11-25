<template>
  <div class="page">
    <PageHeader
      title="Agent 工具管理"
      code="AI-CENTER"
      group="AI 智能体"
      desc="以可视化方式维护 Agent 可调用的业务工具，支持参数定义、权限范围、调用测试与全量日志。"
    >
      <template #actions>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="createOpen = true">新增工具</el-button>
      </template>
    </PageHeader>

    <div class="grid cols-4">
      <StatCard label="工具总数" :value="list.length" unit="个" icon="Tools" foot="覆盖 7 个业务域" />
      <StatCard label="已启用" :value="enabledCount" unit="个" icon="CircleCheck" tone="blue" foot="所有 Agent 共享" />
      <StatCard label="今日调用" value="1,826" unit="次" icon="DataLine" :delta="9.4" foot="较昨日" />
      <StatCard label="平均错误率" value="0.62" unit="%" icon="Warning" tone="amber" foot="T05 派单接口偏高" />
    </div>

    <div class="filter-bar mt-3">
      <el-input v-model="keyword" placeholder="搜索工具名称 / 接口地址" clearable style="width: 240px" :prefix-icon="Search" />
      <el-select v-model="module" style="width: 150px">
        <el-option label="全部模块" value="全部" />
        <el-option v-for="m in modules" :key="m" :value="m" :label="m" />
      </el-select>
      <el-select v-model="scope" style="width: 140px">
        <el-option label="全部权限" value="全部" />
        <el-option v-for="s in ['本片区', '结算范围', '报表范围', '知识域']" :key="s" :value="s" :label="s" />
      </el-select>
      <span class="filter-hint">工具权限由后端令牌与角色共同校验，Agent 无法绕过</span>
    </div>

    <PanelCard title="工具清单" :sub="`共 ${filtered.length} 个工具 · 支持调用测试与日志追溯`" flush>
      <el-table :data="filtered" size="small" style="width: 100%">
        <el-table-column prop="id" label="ID" width="62">
          <template #default="{ row }"><span class="mono">{{ row.id }}</span></template>
        </el-table-column>
        <el-table-column prop="name" label="工具名称" width="140">
          <template #default="{ row }">
            <div class="cell-main">{{ row.name }}</div>
            <div class="cell-sub">{{ row.module }}</div>
          </template>
        </el-table-column>
        <el-table-column label="接口" min-width="220">
          <template #default="{ row }">
            <div style="display: flex; align-items: center; gap: 6px">
              <span class="tag no-dot" :class="row.method === 'GET' ? 'blue' : 'amber'" style="height: 19px; font-size: 10px">{{ row.method }}</span>
              <span class="mono" style="font-size: 11px; color: var(--text-2)">{{ row.endpoint }}</span>
            </div>
            <div class="cell-sub mono">{{ row.params }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="scope" label="权限范围" width="90">
          <template #default="{ row }"><span class="tag gray" style="height: 20px">{{ row.scope }}</span></template>
        </el-table-column>
        <el-table-column prop="calls" label="调用次数" width="90" align="right">
          <template #default="{ row }"><span class="num">{{ row.calls.toLocaleString() }}</span></template>
        </el-table-column>
        <el-table-column prop="errorRate" label="错误率" width="80" align="right">
          <template #default="{ row }">
            <span class="num" :style="{ color: row.errorRate > 1.5 ? '#c53030' : 'var(--text-2)' }">{{ row.errorRate }}%</span>
          </template>
        </el-table-column>
        <el-table-column label="启用" width="66" align="center">
          <template #default="{ row }">
            <el-switch v-model="row.enabled" size="small" @change="toggle(row)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="openTest(row)">测试</el-button>
            <el-button size="small" text @click="openLogs(row)">日志</el-button>
            <el-button size="small" text @click="edit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </PanelCard>

    <el-dialog v-model="testOpen" :title="`调用测试 · ${current?.name}`" width="680px">
      <div class="test-grid">
        <div>
          <div class="ctx-label">请求参数（JSON）</div>
          <el-input v-model="testParams" type="textarea" :rows="9" class="mono" />
          <div class="hint" style="font-size: 10.5px; margin-top: 5px">请求将携带当前用户身份令牌：{{ store.roleMeta.user }} / {{ store.roleMeta.scope }}</div>
          <el-button type="primary" size="small" style="margin-top: 10px" :loading="testing" @click="runTest">发起测试</el-button>
        </div>
        <div>
          <div class="ctx-label">返回结果</div>
          <div class="test-result" :class="{ error: testResult && !testResult.ok }">
            <template v-if="testResult">
              <div class="test-meta">
                <span class="tag" :class="testResult.ok ? 'green' : 'red'">{{ testResult.ok ? '200 OK' : testResult.code }}</span>
                <span class="mono">{{ testResult.latency }} ms</span>
              </div>
              <pre>{{ pretty(testResult.response) }}</pre>
            </template>
            <div v-else class="empty" style="padding: 30px 10px">尚未发起调用</div>
          </div>
        </div>
      </div>
    </el-dialog>

    <el-drawer v-model="logOpen" :title="`工具调用日志 · ${current?.name}`" size="620px">
      <el-table :data="toolLogs" size="small">
        <el-table-column prop="at" label="时间" width="120" />
        <el-table-column prop="agent" label="Agent" width="72" />
        <el-table-column prop="user" label="调用人" width="76" />
        <el-table-column prop="status" label="结果" width="70">
          <template #default="{ row }"><StatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column prop="ms" label="耗时" width="70">
          <template #default="{ row }">{{ row.ms }}ms</template>
        </el-table-column>
        <el-table-column prop="summary" label="参数摘要" show-overflow-tooltip />
      </el-table>
      <div class="hint-line mt-3">
        <el-icon :size="13"><Lock /></el-icon>
        <span>日志保留 180 天，包含调用人、角色、数据范围与返回码，可用于越权追溯。</span>
      </div>
    </el-drawer>

    <el-dialog v-model="createOpen" title="新增工具" width="520px">
      <el-form label-position="top">
        <el-form-item label="工具名称"><el-input v-model="newTool.name" /></el-form-item>
        <el-form-item label="所属模块">
          <el-select v-model="newTool.module" style="width: 100%">
            <el-option v-for="m in modules" :key="m" :value="m" :label="m" />
          </el-select>
        </el-form-item>
        <el-form-item label="接口地址"><el-input v-model="newTool.endpoint" placeholder="/api/xxx/query" class="mono" /></el-form-item>
        <el-form-item label="请求方式">
          <el-radio-group v-model="newTool.method">
            <el-radio value="GET">GET</el-radio>
            <el-radio value="POST">POST</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="权限范围">
          <el-select v-model="newTool.scope" style="width: 100%">
            <el-option v-for="s in ['本片区', '结算范围', '报表范围', '知识域']" :key="s" :value="s" :label="s" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createOpen = false">取消</el-button>
        <el-button type="primary" @click="createTool">创建工具</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAppStore } from '@/stores/app'
import { getTools, testTool, getAgentMonitor } from '@/services/api'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import PanelCard from '@/components/PanelCard.vue'
import StatusTag from '@/components/StatusTag.vue'

const store = useAppStore()
const list = ref([])
const keyword = ref('')
const module = ref('全部')
const scope = ref('全部')
const testOpen = ref(false)
const logOpen = ref(false)
const createOpen = ref(false)
const testing = ref(false)
const current = ref(null)
const testParams = ref('{}')
const testResult = ref(null)
const logs = ref([])
const newTool = reactive({ name: '', module: '地块管理', endpoint: '', method: 'GET', scope: '本片区' })

const modules = computed(() => [...new Set(list.value.map((t) => t.module))])
const enabledCount = computed(() => list.value.filter((t) => t.enabled).length)
const filtered = computed(() =>
  list.value.filter((t) => {
    const k = keyword.value.trim()
    const okK = !k || t.name.includes(k) || t.endpoint.includes(k) || t.id.toLowerCase().includes(k.toLowerCase())
    return okK && (module.value === '全部' || t.module === module.value) && (scope.value === '全部' || t.scope === scope.value)
  })
)
const toolLogs = computed(() =>
  logs.value.slice(0, 8).map((l, i) => ({
    at: l.at,
    agent: l.agent,
    user: l.user,
    status: l.result,
    ms: l.ms,
    summary: current.value ? `${current.value.params.split(' / ')[0]} = ${['河东片区', 'LD002', 'NJ-002', '2026-11'][i % 4]}` : '-'
  }))
)

async function load() {
  const [t, m] = await Promise.all([getTools(), getAgentMonitor()])
  list.value = t
  logs.value = m.logs
}
onMounted(load)

const pretty = (v) => (v ? JSON.stringify(v, null, 2) : '')
function openTest(row) {
  current.value = row
  testParams.value = JSON.stringify({ scope: row.scope, params: row.params.split(' / ') }, null, 2)
  testResult.value = null
  testOpen.value = true
}
async function runTest() {
  testing.value = true
  try {
    testResult.value = await testTool(current.value.id, testParams.value)
    if (!testResult.value.ok) ElMessage.warning('工具返回异常，已触发异常处理与回滚')
    else ElMessage.success('调用成功')
  } finally {
    testing.value = false
  }
}
function openLogs(row) {
  current.value = row
  logOpen.value = true
}
function edit(row) {
  current.value = row
  Object.assign(newTool, row)
  createOpen.value = true
}
function toggle(row) {
  ElMessage.success(`工具 ${row.id} 已${row.enabled ? '启用' : '停用'}，配置已记录审计日志`)
}
async function createTool() {
  if (!newTool.name || !newTool.endpoint) return ElMessage.warning('请填写名称与接口地址')
  try {
    await ElMessageBox.confirm('新增工具后需为 Agent 授权才能调用，是否继续？', '新增工具', { type: 'info' })
    list.value.push({
      id: `T${String(list.value.length + 1).padStart(2, '0')}`,
      ...newTool,
      params: '待定义',
      result: '待定义',
      enabled: false,
      calls: 0,
      errorRate: 0,
      desc: '新建工具',
      updatedAt: '2026-11-23 10:20'
    })
    createOpen.value = false
    ElMessage.success('工具已创建，默认停用')
  } catch {}
}
</script>

<style scoped>
.ctx-label {
  font-size: 10.5px;
  font-weight: 700;
  color: var(--text-3);
  margin-bottom: 5px;
  letter-spacing: 0.4px;
}
.test-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}
.test-result {
  border: 1px solid var(--border);
  border-radius: 9px;
  background: #0f221a;
  min-height: 208px;
  max-height: 300px;
  overflow: auto;
}
.test-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 10px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  color: #b9f0d2;
  font-size: 10.5px;
}
.test-result pre {
  margin: 0;
  padding: 10px;
  color: #b9f0d2;
  font-family: var(--font-mono);
  font-size: 10.5px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}
.test-result.error pre {
  color: #ffb4b4;
}
</style>
