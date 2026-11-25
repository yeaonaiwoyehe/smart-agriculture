<template>
  <div class="chat-wrap" :class="{ compact }">
    <div ref="scrollEl" class="chat-scroll">
      <div v-if="messages.length === 0" class="chat-hello">
        <div class="hello-avatar"><el-icon :size="20"><ChatDotRound /></el-icon></div>
        <div class="hello-title">{{ welcomeTitle || `${agentTitle} 已就绪` }}</div>
        <div class="hello-desc">{{ welcomeDesc || greetingText }}</div>
        <div class="hello-note">
          <el-icon :size="13"><Lock /></el-icon>
          身份与数据范围由后端令牌校验，Agent 不是权限主体
        </div>
      </div>

      <div v-for="(m, mi) in messages" :key="mi" class="msg" :class="m.role">
        <div class="msg-avatar">{{ m.role === 'user' ? '我' : agentShort }}</div>
        <div class="msg-body">
          <template v-if="m.role === 'user'">
            <div class="bubble">{{ m.text }}</div>
            <div v-if="m.context" class="msg-context">
              <el-icon :size="11"><Location /></el-icon>
              已结合当前页面：{{ m.context }}
            </div>
          </template>
          <template v-else>
            <div v-if="m.steps && m.steps.length" class="steps">
              <div class="steps-head" @click="m.open = !m.open">
                <el-icon :size="12"><Share /></el-icon>
                执行步骤 {{ doneCount(m) }}/{{ m.steps.length }}
                <span style="margin-left: auto">{{ m.open === false ? '展开' : '收起' }}</span>
              </div>
              <div v-show="m.open !== false" class="steps-body">
                <div v-for="(s, si) in m.steps" :key="si" class="step-row" :class="s.status">
                  <span class="node">
                    <el-icon v-if="s.status === 'done'" :size="9"><Check /></el-icon>
                    <el-icon v-else-if="s.status === 'error'" :size="9"><Close /></el-icon>
                    <span v-else>{{ si + 1 }}</span>
                  </span>
                  <span class="step-text">{{ s.text }}</span>
                  <span v-if="s.tool" class="step-time mono">{{ s.tool.toolId }}</span>
                </div>
              </div>
            </div>

            <div v-if="m.tools && m.tools.length" class="tool-list">
              <div v-for="(t, ti) in m.tools" :key="ti" class="tool-call">
                <div class="tool-call-head" @click="t.open = !t.open">
                  <el-icon :size="11"><Tools /></el-icon>
                  {{ t.name }} · {{ t.toolId }}
                  <span class="tag" :class="t.status === 'done' ? 'green' : t.status === 'error' ? 'red' : 'amber'" style="margin-left: auto; height: 18px; font-size: 10px">
                    {{ t.status === 'done' ? '成功' : t.status === 'error' ? '失败' : '调用中' }}
                  </span>
                </div>
                <div v-show="t.open" class="tool-call-body">{{ pretty(t.result) }}</div>
              </div>
            </div>

            <div v-if="m.text" class="bubble" style="margin-top: 8px" v-html="m.text" />
            <div v-else-if="m.streaming" class="bubble typing" style="margin-top: 8px">
              <span /><span /><span /> 正在调用业务工具…
            </div>

            <div v-if="m.artifacts?.table" class="art-block">
              <table class="art-table">
                <thead><tr><th v-for="c in m.artifacts.table.columns" :key="c">{{ c }}</th></tr></thead>
                <tbody>
                  <tr v-for="(r, ri) in m.artifacts.table.rows" :key="ri">
                    <td v-for="(c, ci) in r" :key="ci">{{ c }}</td>
                  </tr>
                </tbody>
              </table>
            </div>

            <div v-if="m.artifacts?.chart" class="art-block">
              <ChartPanel :option="chartOption(m.artifacts.chart)" :height="168" />
            </div>

            <div v-if="m.artifacts?.citations" class="art-block">
              <div v-for="(c, ci) in m.artifacts.citations" :key="ci" class="citation">
                <div class="citation-head">{{ c.doc }} <span class="mono">{{ c.version }}</span> · {{ c.clause }}</div>
                <div class="citation-text">{{ c.text }}</div>
              </div>
            </div>

            <div v-if="m.artifactLinks?.length" class="msg-actions">
              <button v-for="l in m.artifactLinks" :key="l.text" class="link-btn" @click="$router.push(l.route)">
                {{ l.text }} →
              </button>
            </div>

            <div v-if="m.approval" class="approval-box">
              <div class="approval-title"><el-icon :size="13"><Warning /></el-icon>{{ m.approval.title }}</div>
              <div class="approval-desc">{{ m.approval.desc }}</div>
              <div v-if="!m.approval.decided" class="approval-actions">
                <el-button type="warning" size="small" :loading="m.approval.loading" @click="confirmApproval(m)">确认执行</el-button>
                <el-button size="small" @click="cancelApproval(m)">取消</el-button>
              </div>
              <div v-else class="approval-desc" style="margin-top: 6px">
                <b>{{ m.approval.decided === 'confirmed' ? '已确认执行' : m.approval.decided === 'failed' ? '执行失败' : '已取消' }}</b>
                <span v-if="m.approval.receipt" class="mono"> · 回执 {{ m.approval.receipt }}</span>
              </div>
            </div>

            <div v-if="m.failed" class="retry-row">
              <span>本次调用未完成，请核实业务数据后再重试。</span>
              <button @click="retry(m)">重试</button>
            </div>

            <div class="msg-meta">
              <span>{{ m.time }}</span>
              <span v-if="m.agent">· {{ m.agent }}</span>
              <span v-if="m.artifacts || m.approval" class="tag green no-dot" style="height: 18px; font-size: 10px">数据可追溯</span>
            </div>
          </template>
        </div>
      </div>
    </div>

    <div class="quick-ask">
      <button v-for="q in quickAsks" :key="q" @click="send(q)">{{ q }}</button>
    </div>

    <div class="chat-input">
      <textarea
        v-model="draft"
        :rows="2"
        :placeholder="`向 ${agentTitle} 提问，Enter 发送，Shift+Enter 换行`"
        @keydown.enter.exact.prevent="send()"
      />
      <div class="chat-toolbar">
        <span class="tag no-dot" style="font-size: 10px">工具调用需鉴权</span>
        <span class="grow" />
        <el-button size="small" :disabled="loading" @click="draft = ''">清空</el-button>
        <el-button size="small" type="primary" :loading="loading" :disabled="!draft.trim()" @click="send()">
          发送
        </el-button>
      </div>
    </div>

    <div v-if="assistantMode" class="chat-disclaimer">
      AI 生成内容仅供业务参考，请以系统实际数据和人工审核结果为准。
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, reactive, ref } from 'vue'
import { useAppStore } from '@/stores/app'
import { runAgent, QUICK_ASKS, agentGreeting } from '@/services/agentEngine'
import { runAssistant } from '@/services/assistantEngine'
import { batchDispatch, reviewWorkorder } from '@/services/api'
import ChartPanel from '@/components/ChartPanel.vue'

const props = defineProps({
  agentId: { type: String, default: 'MA-05' },
  compact: { type: Boolean, default: false },
  assistantMode: { type: Boolean, default: false },
  pageContext: { type: Object, default: () => ({}) },
  welcomeTitle: { type: String, default: '' },
  welcomeDesc: { type: String, default: '' },
  presetQuickAsks: { type: Array, default: () => [] }
})

const store = useAppStore()
const messages = ref([])
const draft = ref('')
const loading = ref(false)
const scrollEl = ref(null)

const AGENT_TITLE = { 'MA-01': '调度建议 Agent', 'MA-02': '异常研判 Agent', 'MA-03': '制度与口径问答', 'MA-04': '数据分析 Agent', 'MA-05': '综合运营 Agent' }
const agentTitle = computed(() => AGENT_TITLE[props.agentId] || '智能体')
const agentShort = computed(() => (props.agentId || 'AI').replace('MA-', 'M'))
const greetingText = computed(() => agentGreeting(props.agentId))
const quickAsks = computed(() =>
  props.presetQuickAsks.length ? props.presetQuickAsks : QUICK_ASKS[props.agentId] || QUICK_ASKS['MA-05']
)

const now = () => new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
const doneCount = (m) => m.steps.filter((s) => s.status === 'done').length
const pretty = (v) => {
  if (v === undefined || v === null) return '等待返回…'
  try {
    return JSON.stringify(v, null, 2)
  } catch {
    return String(v)
  }
}

function scrollToEnd() {
  nextTick(() => {
    if (scrollEl.value) scrollEl.value.scrollTop = scrollEl.value.scrollHeight
  })
}

function chartOption(spec) {
  return {
    grid: { left: 36, right: 12, top: 24, bottom: 22, containLabel: true },
    xAxis: { type: 'category', data: spec.categories },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#eef3f0' } } },
    series: spec.series.map((s) => ({
      name: s.name,
      type: spec.type || 'bar',
      data: s.data,
      smooth: true,
      barWidth: 14,
      areaStyle: spec.type === 'line' ? { color: 'rgba(31,174,107,.12)' } : undefined,
      itemStyle: { borderRadius: [4, 4, 0, 0] }
    }))
  }
}

async function send(text, skipUser = false) {
  const content = (text || draft.value).trim()
  if (!content || loading.value) return
  draft.value = ''
  if (!skipUser) {
    messages.value.push({
      role: 'user',
      text: content,
      time: now(),
      context: props.assistantMode ? props.pageContext?.title : ''
    })
  }
  const msg = reactive({
    role: 'assistant',
    agent: agentShort.value,
    text: '',
    time: now(),
    streaming: true,
    steps: [],
    tools: [],
    open: true,
    lastText: content
  })
  messages.value.push(msg)
  loading.value = true
  scrollToEnd()

  const emit = (e) => {
    if (e.type === 'plan') msg.steps = e.steps
    if (e.type === 'step') msg.steps = [...e.steps]
    if (e.type === 'tool-start') {
      msg.tools.push({ toolId: e.toolId, name: e.name, status: 'running', payload: e.payload, open: false })
    }
    if (e.type === 'tool-end') {
      const t = [...msg.tools].reverse().find((x) => x.toolId === e.toolId && x.status === 'running')
      if (t) {
        t.status = e.toolId === 'T05' ? 'error' : 'done'
        t.result = e.result
      }
    }
    if (e.type === 'reply') {
      msg.text = e.reply.content
      msg.artifacts = e.reply.artifacts || null
      msg.artifactLinks = e.reply.artifacts?.links || []
      msg.approval = e.reply.approval || null
      msg.streaming = false
    }
    scrollToEnd()
  }

  try {
    if (props.assistantMode) {
      await runAssistant({ agentId: props.agentId, message: content, role: store.role, context: props.pageContext || {}, emit })
    } else {
      await runAgent({ agentId: props.agentId, message: content, role: store.role, emit })
    }
  } catch (err) {
    const reason = err?.payload?.msg || err?.msg || err?.message || '未知错误'
    msg.text = `工具调用失败：${reason}。请核实当前业务状态后再决定是否重试。`
    msg.streaming = false
    msg.failed = true
    msg.steps.forEach((s) => (s.status = s.status === 'pending' ? 'error' : s.status))
  } finally {
    loading.value = false
    scrollToEnd()
  }
}

function retry(m) {
  const i = messages.value.indexOf(m)
  if (i >= 0) messages.value.splice(i, 1)
  if (m.lastText) send(m.lastText, true)
}

async function confirmApproval(m) {
  m.approval.loading = true
  try {
    let res
    // 注意：batchDispatch 在当前后端不存在对应接口，会抛出「后端待补充」，
    // 这里必须如实回执失败，不能把异常吞掉后仍输出“已执行并写入审计日志”。
    if (m.approval.payload?.landIds) res = await batchDispatch(m.approval.payload.landIds)
    else if (m.approval.payload?.workorderId) res = await reviewWorkorder(m.approval.payload.workorderId, 'pass', '用户确认采纳 Agent 研判')
    else throw new Error('未识别的审批操作，未执行任何写入')
    m.approval.decided = 'confirmed'
    m.approval.receipt = res.receipt || ''
    messages.value.push({
      role: 'assistant',
      agent: agentShort.value,
      text: `操作结果：${res.message || '已完成'}${m.approval.receipt ? `。回执编号 <b>${m.approval.receipt}</b>` : ''}。`,
      time: now(),
      steps: [],
      tools: []
    })
  } catch (error) {
    m.approval.decided = 'failed'
    messages.value.push({
      role: 'assistant',
      agent: agentShort.value,
      text: `执行失败：${error?.payload?.msg || error?.msg || error?.message || '未知错误'}。请通过业务页面核实当前状态后再决定是否重试。`,
      time: now(),
      steps: [],
      tools: []
    })
  } finally {
    m.approval.loading = false
    scrollToEnd()
  }
}

function cancelApproval(m) {
  m.approval.decided = 'cancelled'
  messages.value.push({
    role: 'assistant',
    agent: agentShort.value,
    text: '已取消，本次建议未写入任何正式业务数据。你可以继续调整条件后重新生成。',
    time: now(),
    steps: [],
    tools: []
  })
  scrollToEnd()
}

function reset() {
  messages.value = []
}
defineExpose({ send, reset })
</script>

<style scoped>
.chat-hello {
  padding: 18px 8px 12px;
  text-align: center;
}
.hello-avatar {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  margin: 0 auto 10px;
  display: grid;
  place-items: center;
  background: var(--ai-gradient);
  color: #fff;
  box-shadow: var(--ai-glow);
}
.hello-title {
  font-size: 15px;
  font-weight: 700;
}
.hello-desc {
  margin: 6px auto 0;
  max-width: 420px;
  font-size: 12px;
  color: var(--text-2);
  line-height: 1.65;
}
.hello-note {
  margin-top: 10px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 10.5px;
  color: var(--text-3);
}
.tool-list {
  margin-top: 6px;
}
.typing {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  color: var(--text-3);
}
.typing span {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--brand-500);
  animation: blink 1.2s infinite;
}
.typing span:nth-child(2) {
  animation-delay: 0.2s;
}
.typing span:nth-child(3) {
  animation-delay: 0.4s;
}
@keyframes blink {
  0%,
  100% {
    opacity: 0.25;
  }
  50% {
    opacity: 1;
  }
}
.art-block {
  margin-top: 8px;
  border: 1px solid var(--border);
  border-radius: 9px;
  overflow: auto;
  background: #fff;
}
.art-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 11px;
  min-width: 460px;
}
.art-table th {
  background: var(--surface-2);
  color: var(--text-2);
  font-weight: 600;
  text-align: left;
  padding: 6px 8px;
  border-bottom: 1px solid var(--border);
  white-space: nowrap;
}
.art-table td {
  padding: 6px 8px;
  border-bottom: 1px solid #f0f4f1;
  color: var(--text-1);
  white-space: nowrap;
}
.citation {
  padding: 8px 10px;
  border-bottom: 1px solid #f0f4f1;
}
.citation:last-child {
  border-bottom: none;
}
.citation-head {
  font-size: 10.5px;
  font-weight: 650;
  color: var(--brand-700);
}
.citation-text {
  margin-top: 3px;
  font-size: 11px;
  color: var(--text-2);
  line-height: 1.6;
}
.compact .msg-body {
  max-width: 88%;
}
.msg-context {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 4px;
  justify-content: flex-end;
  font-size: 10px;
  color: var(--text-3);
}
.retry-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 6px;
  font-size: 11px;
  color: var(--danger, #d93f3f);
}
.retry-row button {
  border: 1px solid var(--border);
  background: #fff;
  border-radius: 6px;
  padding: 2px 8px;
  font-size: 11px;
  color: var(--brand-700, #0f5537);
  cursor: pointer;
}
.retry-row button:hover {
  background: var(--brand-50, #e8f5ee);
}
.chat-disclaimer {
  padding: 6px 10px 8px;
  font-size: 10px;
  line-height: 1.5;
  color: var(--text-3);
  text-align: center;
  border-top: 1px solid #eef3f0;
  background: #fff;
}
</style>
