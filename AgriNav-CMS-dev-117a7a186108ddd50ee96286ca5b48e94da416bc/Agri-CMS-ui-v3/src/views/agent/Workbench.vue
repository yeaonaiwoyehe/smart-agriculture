<template>
  <div class="page">
    <PageHeader
      title="Agent 工作台"
      code="AI-CENTER"
      group="AI 智能体"
      desc="统一智能体交互入口：自然语言对话、多轮上下文、业务工具调用、执行步骤与人工确认全程可见。"
    >
      <template #actions>
        <el-button :icon="Refresh" @click="reload">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="newSession">新建会话</el-button>
      </template>
    </PageHeader>

    <div class="grid cols-4">
      <StatCard label="可用 Agent" :value="agents.length" unit="个" icon="Cpu" foot="1 个灰度中" />
      <StatCard label="今日会话" value="128" unit="次" icon="ChatDotRound" :delta="18.2" foot="较昨日" />
      <StatCard label="工具调用" value="1,826" unit="次" icon="Tools" tone="blue" :delta="9.4" foot="成功率 98.2%" />
      <StatCard label="待人工确认" value="3" unit="项" icon="Warning" tone="amber" foot="派单 2 · 复核 1" />
    </div>

    <div class="workbench mt-3">
      <aside class="card agent-rail">
        <div class="rail-title">智能体</div>
        <div
          v-for="a in agents"
          :key="a.id"
          class="rail-item"
          :class="{ active: a.id === activeId }"
          @click="activeId = a.id"
        >
          <div class="agent-avatar" style="width: 31px; height: 31px">{{ a.id.replace('MA-', 'M') }}</div>
          <div style="min-width: 0">
            <div class="rail-name">{{ a.name }}</div>
            <div class="rail-meta">{{ a.id }} · {{ a.type }}</div>
          </div>
          <span class="rail-dot" :class="{ gray: a.status !== '运行中' }" />
        </div>
        <div class="rail-foot">
          <div class="hint-line">
            <el-icon :size="12"><Lock /></el-icon>
            <span>Agent 调用业务工具时携带当前用户令牌，后端二次鉴权。</span>
          </div>
        </div>
      </aside>

      <section class="card chat-card">
        <div class="chat-card-head">
          <div class="agent-avatar">{{ activeId.replace('MA-', 'M') }}</div>
          <div>
            <div style="font-size: 14px; font-weight: 700">{{ active?.name }}</div>
            <div class="hint" style="font-size: 10.5px">{{ active?.desc }}</div>
          </div>
          <div style="margin-left: auto; display: flex; align-items: center; gap: 6px">
            <span class="tag green">{{ active?.status }}</span>
            <span class="tag no-dot gray mono">{{ active?.model }}</span>
            <el-tooltip content="清空当前会话"><button class="icon-btn" @click="resetChat"><el-icon><Delete /></el-icon></button></el-tooltip>
          </div>
        </div>
        <div class="chat-card-body">
          <AgentChat ref="chatRef" :key="activeId" :agent-id="activeId" />
        </div>
      </section>

      <aside class="card context-rail">
        <div class="rail-title">运行上下文</div>
        <div class="ctx-block">
          <div class="ctx-label">当前身份</div>
          <div class="ctx-value">{{ store.roleMeta.name }} · {{ store.roleMeta.user }}</div>
          <div class="ctx-sub">数据范围：{{ store.roleMeta.scope }}</div>
        </div>
        <div class="ctx-block">
          <div class="ctx-label">可调用工具（{{ activeTools.length }}）</div>
          <div class="tool-chips">
            <span v-for="t in activeTools" :key="t.id" class="tool-chip" :title="t.endpoint">{{ t.name }}</span>
          </div>
        </div>
        <div class="ctx-block">
          <div class="ctx-label">安全约束</div>
          <ul class="safe-list">
            <li>不直接修改正式业务数据</li>
            <li>派单 / 复核 / 结算需人工确认</li>
            <li>面积、金额、时长来自业务库</li>
            <li>回答可追溯至业务记录或制度条款</li>
          </ul>
        </div>
        <div class="ctx-block" style="flex: 1; min-height: 0">
          <div class="ctx-label">最近会话</div>
          <div v-for="s in sessions" :key="s.id" class="session-row" @click="openSession(s)">
            <div class="session-title">{{ s.title }}</div>
            <div class="session-meta">{{ s.agent }} · {{ s.user }} · {{ s.at.slice(5, 16) }}</div>
          </div>
        </div>
      </aside>
    </div>

    <el-dialog v-model="sessionOpen" :title="sessionDetail?.title" width="640px">
      <div v-if="sessionDetail" class="session-dialog">
        <div class="hint" style="font-size: 11px; margin-bottom: 10px">
          {{ sessionDetail.agent }} · {{ sessionDetail.user }} · {{ sessionDetail.at }} · {{ sessionDetail.turns }} 轮
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
import { Plus, Refresh, Delete } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useAppStore } from '@/stores/app'
import { getAgents, getTools, getCockpitSessions } from '@/services/api'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import AgentChat from '@/components/AgentChat.vue'

const store = useAppStore()
const agents = ref([])
const tools = ref([])
const sessions = ref([])
const activeId = ref('MA-01')
const chatRef = ref(null)
const sessionOpen = ref(false)
const sessionDetail = ref(null)

const active = computed(() => agents.value.find((a) => a.id === activeId.value))
const activeTools = computed(() => tools.value.filter((t) => active.value?.tools.includes(t.id)))

async function load() {
  const [a, t, s] = await Promise.all([getAgents(), getTools(), getCockpitSessions()])
  agents.value = a
  tools.value = t
  sessions.value = s
}
onMounted(load)

function reload() {
  load()
  ElMessage.success('Agent 列表与会话上下文已刷新')
}
function newSession() {
  resetChat()
  ElMessage.success('已创建新会话，上下文已隔离')
}
function resetChat() {
  chatRef.value?.reset()
}
function openSession(s) {
  sessionDetail.value = s
  sessionOpen.value = true
}
</script>

<style scoped>
.workbench {
  display: grid;
  grid-template-columns: 244px minmax(0, 1fr) 292px;
  gap: 14px;
  height: 636px;
  min-height: 0;
}
.agent-rail,
.context-rail {
  padding: 12px;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}
.rail-title {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.6px;
  color: var(--text-3);
  padding: 2px 4px 8px;
}
.rail-item {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 8px;
  border-radius: 9px;
  cursor: pointer;
  margin-bottom: 3px;
  border: 1px solid transparent;
}
.rail-item:hover {
  background: var(--surface-2);
}
.rail-item.active {
  background: var(--brand-50);
  border-color: #cbe9d8;
}
.rail-name {
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.rail-meta {
  font-size: 9.5px;
  color: var(--text-3);
  white-space: nowrap;
}
.rail-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #1fae6b;
  margin-left: auto;
  flex: none;
}
.rail-dot.gray {
  background: #a8b4ae;
}
.rail-foot {
  margin-top: auto;
  padding-top: 10px;
}
.chat-card {
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}
.chat-card-head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 15px;
  border-bottom: 1px solid var(--border);
  background: linear-gradient(180deg, #f7fcf9, #fff);
  flex: none;
}
.chat-card-body {
  flex: 1;
  min-height: 0;
  display: flex;
  padding: 10px 14px 12px;
}
.ctx-block {
  padding: 10px 4px;
  border-bottom: 1px dashed var(--border);
}
.ctx-block:last-child {
  border-bottom: none;
}
.ctx-label {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.4px;
  color: var(--text-3);
  margin-bottom: 6px;
}
.ctx-value {
  font-size: 12px;
  font-weight: 600;
}
.ctx-sub {
  font-size: 10.5px;
  color: var(--text-3);
  margin-top: 2px;
}
.tool-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}
.tool-chip {
  font-size: 10px;
  padding: 2px 7px;
  border-radius: 5px;
  background: var(--surface-3);
  color: var(--text-2);
  font-family: var(--font-mono);
}
.safe-list {
  margin: 0;
  padding-left: 15px;
  font-size: 10.5px;
  color: var(--text-2);
  line-height: 1.7;
}
.session-row {
  padding: 7px 5px;
  border-radius: 6px;
  cursor: pointer;
  margin-bottom: 2px;
}
.session-row:hover {
  background: var(--surface-2);
}
.session-title {
  font-size: 11.5px;
  font-weight: 550;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.session-meta {
  font-size: 9.5px;
  color: var(--text-3);
  margin-top: 1px;
}
</style>
