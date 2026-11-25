<template>
  <teleport to="body">
    <transition name="ga-fab">
      <button v-if="!open" class="ga-fab" aria-label="打开农机智能助手" @click="openPanel">
        <span class="ga-fab-ring" />
        <svg viewBox="0 0 24 24" class="ga-robot" aria-hidden="true">
          <rect x="4" y="7.5" width="16" height="11.5" rx="4" />
          <circle cx="9.2" cy="13" r="1.3" fill="currentColor" stroke="none" />
          <circle cx="14.8" cy="13" r="1.3" fill="currentColor" stroke="none" />
          <path d="M12 3.2v4.3M8.8 19v2M15.2 19v2M2.6 12.6v2.4M21.4 12.6v2.4" />
        </svg>
        <span class="ga-fab-dot" />
      </button>
    </transition>

    <transition name="ga-panel">
      <section v-if="open" class="ga-panel" role="dialog" aria-label="农机智能助手">
        <header class="ga-head">
          <div class="ga-head-icon">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <rect x="4" y="7.5" width="16" height="11.5" rx="4" />
              <circle cx="9.2" cy="13" r="1.3" fill="currentColor" stroke="none" />
              <circle cx="14.8" cy="13" r="1.3" fill="currentColor" stroke="none" />
              <path d="M12 3.2v4.3M8.8 19v2M15.2 19v2" />
            </svg>
          </div>
          <div class="ga-head-text">
            <div class="ga-title">
              农机智能助手
              <span class="ga-online"><i />在线</span>
            </div>
            <div class="ga-sub">AI Agent 智能服务</div>
          </div>
          <div class="ga-head-actions">
            <button title="新建对话" aria-label="新建对话" @click="newChat">
              <el-icon :size="15"><Plus /></el-icon>
            </button>
            <button title="最小化" aria-label="最小化" @click="open = false">
              <el-icon :size="15"><Minus /></el-icon>
            </button>
            <button title="关闭" aria-label="关闭" @click="closePanel">
              <el-icon :size="15"><Close /></el-icon>
            </button>
          </div>
        </header>

        <div class="ga-context">
          <span class="ga-page">
            <el-icon :size="12"><Location /></el-icon>
            当前页面 · {{ pageContext.title }}
          </span>
          <el-select v-model="agentId" size="small" class="ga-agent">
            <el-option v-for="a in agents" :key="a.id" :value="a.id" :label="`${a.id} ${a.name}`" />
          </el-select>
        </div>

        <div class="ga-chat">
          <AgentChat
            ref="chatRef"
            :key="agentId"
            :agent-id="agentId"
            :page-context="pageContext"
            :welcome-title="welcomeTitle"
            :welcome-desc="welcomeDesc"
            :preset-quick-asks="quickAsks"
            compact
            assistant-mode
          />
        </div>
      </section>
    </transition>
  </teleport>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { agents } from '@/mock/platform'
import AgentChat from '@/components/AgentChat.vue'

const store = useAppStore()
const route = useRoute()
const { assistantOpen: open } = storeToRefs(store)
const agentId = ref(store.assistantAgent)
const chatRef = ref(null)

const welcomeTitle = '您好，我是农机智能助手。'
const welcomeDesc = '我可以帮助您查询作业任务、分析作业异常、查看结算数据以及提供调度建议。'
const quickAsks = [
  '今天有哪些待作业地块？',
  '当前农机作业情况如何？',
  '帮我分析异常轨迹。',
  '查询本月结算数据。',
  '有哪些待处理事项？'
]

const pageContext = computed(() => ({
  key: route.meta?.menuKey || '',
  title: route.meta?.title || '管理平台',
  path: route.path,
  queryId: route.query?.id || ''
}))

watch(
  () => store.assistantAgent,
  (v) => (agentId.value = v)
)
watch(agentId, (v) => (store.assistantAgent = v))

function openPanel() {
  store.assistantOpen = true
}
function closePanel() {
  chatRef.value?.reset()
  open.value = false
}
function newChat() {
  chatRef.value?.reset()
  agentId.value = 'MA-05'
}

function onKeydown(e) {
  if (e.key === 'Escape' && open.value) open.value = false
}
onMounted(() => window.addEventListener('keydown', onKeydown))
onBeforeUnmount(() => window.removeEventListener('keydown', onKeydown))
</script>

<style scoped>
.ga-fab {
  position: fixed;
  right: 26px;
  bottom: 26px;
  width: 60px;
  height: 60px;
  border-radius: 50%;
  border: none;
  cursor: pointer;
  display: grid;
  place-items: center;
  color: #fff;
  background: linear-gradient(140deg, #1fae6b 0%, #146b45 78%);
  box-shadow: 0 10px 26px rgba(20, 107, 69, 0.32), 0 2px 8px rgba(20, 60, 40, 0.18);
  z-index: 1600;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}
.ga-fab:hover {
  transform: translateY(-2px) scale(1.05);
  box-shadow: 0 14px 32px rgba(20, 107, 69, 0.4);
}
.ga-fab:active {
  transform: scale(0.97);
}
.ga-robot {
  width: 27px;
  height: 27px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.7;
  stroke-linecap: round;
  stroke-linejoin: round;
}
.ga-fab-ring {
  position: absolute;
  inset: -5px;
  border-radius: 50%;
  border: 2px solid rgba(31, 174, 107, 0.38);
  animation: ga-breathe 3.2s ease-out infinite;
}
@keyframes ga-breathe {
  0% {
    transform: scale(0.92);
    opacity: 0.75;
  }
  70% {
    transform: scale(1.16);
    opacity: 0;
  }
  100% {
    opacity: 0;
  }
}
.ga-fab-dot {
  position: absolute;
  top: 9px;
  right: 9px;
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #84cc16;
  border: 2px solid #146b45;
}

.ga-panel {
  position: fixed;
  right: 26px;
  bottom: 26px;
  width: 400px;
  height: 580px;
  max-width: calc(100vw - 32px);
  max-height: calc(100vh - 96px);
  display: flex;
  flex-direction: column;
  background: #f7faf8;
  border: 1px solid var(--border);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 24px 60px rgba(18, 52, 36, 0.24), 0 6px 18px rgba(18, 52, 36, 0.12);
  z-index: 1600;
}

.ga-head {
  flex: none;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 13px 14px;
  color: #fff;
  background: linear-gradient(130deg, #146b45 0%, #1b8a58 62%, #1fae6b 100%);
}
.ga-head-icon {
  width: 36px;
  height: 36px;
  border-radius: 11px;
  display: grid;
  place-items: center;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.24);
}
.ga-head-icon svg {
  width: 21px;
  height: 21px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}
.ga-head-text {
  min-width: 0;
}
.ga-title {
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.2px;
  display: flex;
  align-items: center;
  gap: 7px;
}
.ga-online {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 10px;
  font-weight: 500;
  padding: 1px 7px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.18);
}
.ga-online i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #b7f07a;
  box-shadow: 0 0 0 2px rgba(183, 240, 122, 0.25);
}
.ga-sub {
  margin-top: 2px;
  font-size: 10.5px;
  color: rgba(255, 255, 255, 0.78);
}
.ga-head-actions {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 2px;
}
.ga-head-actions button {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  border: none;
  background: transparent;
  color: rgba(255, 255, 255, 0.9);
  display: grid;
  place-items: center;
  cursor: pointer;
  transition: background 0.15s;
}
.ga-head-actions button:hover {
  background: rgba(255, 255, 255, 0.18);
}

.ga-context {
  flex: none;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #fff;
  border-bottom: 1px solid var(--border);
}
.ga-page {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: var(--text-2);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.ga-agent {
  width: 138px;
  margin-left: auto;
}
.ga-chat {
  flex: 1;
  min-height: 0;
  display: flex;
  padding: 10px 12px 0;
}

.ga-fab-enter-active,
.ga-fab-leave-active {
  transition: opacity 0.16s ease, transform 0.16s ease;
}
.ga-fab-enter-from,
.ga-fab-leave-to {
  opacity: 0;
  transform: scale(0.86);
}
.ga-panel-enter-active,
.ga-panel-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
  transform-origin: bottom right;
}
.ga-panel-enter-from,
.ga-panel-leave-to {
  opacity: 0;
  transform: translateY(10px) scale(0.97);
}

@media (max-width: 620px) {
  .ga-fab {
    right: 16px;
    bottom: 16px;
  }
  .ga-panel {
    right: 8px;
    bottom: 8px;
    width: calc(100vw - 16px);
    height: calc(100vh - 16px);
    max-height: calc(100vh - 16px);
    border-radius: 14px;
  }
}
</style>
