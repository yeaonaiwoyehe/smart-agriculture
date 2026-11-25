<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import SfIcon from '@/components/SfIcon.vue'
import type { AgentMessage } from '@/types'
import { AGENT_SUGGESTIONS, speechToText } from '@/api/agent'
import { useAgentStore } from '@/stores/agent'
import { useAppStore } from '@/stores/app'
import { go, goBack } from '@/utils/nav'
import { confirmAction, toast } from '@/utils/ui'

const store = useAgentStore()
const app = useAppStore()
const input = ref('')
const scrollInto = ref('')
const recording = ref(false)

const messages = computed(() => store.messages)
const empty = computed(() => messages.value.length === 0 && !store.thinking)

onShow(() => {
  store.ensureSession()
  const pending = uni.getStorageSync('agent.pendingQuestion')
  if (pending) {
    uni.removeStorageSync('agent.pendingQuestion')
    void handleSend(String(pending))
  }
  scrollToBottom()
})

watch(
  () => [messages.value.length, store.thinking, store.executing],
  () => scrollToBottom()
)

function scrollToBottom() {
  nextTick(() => {
    const list = messages.value
    if (!list.length) return
    scrollInto.value = `msg-${list[list.length - 1].id}`
  })
}

async function handleSend(preset?: string) {
  const text = (preset ?? input.value).trim()
  if (!text || store.busy) return
  if (preset === undefined) input.value = ''
  await store.send(text)
}

async function handleConfirm(msg: AgentMessage) {
  if (!msg.pendingConfirm) return
  const detail = msg.pendingConfirm
  const ok = await confirmAction({
    title: detail.label,
    content: detail.summary,
    confirmText: '确认执行',
    danger: detail.action === 'finishWork'
  })
  if (!ok) return
  const result = await store.confirm(msg.id)
  if (result?.link) {
    setTimeout(() => openLink(result.link!), 500)
  }
  if (detail.action === 'startWork' || detail.action === 'finishWork') {
    void useAppStore().refreshUnread()
  }
}

function handleCancel(msg: AgentMessage) {
  store.cancelConfirm(msg.id)
}

async function handleVoice() {
  if (store.busy || recording.value) return
  recording.value = true
  uni.showLoading({ title: '语音识别中（演示）', mask: true })
  try {
    const text = await speechToText()
    uni.hideLoading()
    recording.value = false
    toast(`识别结果：${text}`)
    await handleSend(text)
  } catch (e) {
    uni.hideLoading()
    recording.value = false
    toast('语音识别失败')
  }
}

function openLink(url?: string) {
  if (!url) return
  go(url)
}

async function newSession() {
  if (messages.value.length) {
    const ok = await confirmAction({
      title: '新建会话',
      content: '当前会话将保存到历史记录，是否开始新的会话？',
      confirmText: '新建'
    })
    if (!ok) return
  }
  store.persist()
  store.newSession()
  input.value = ''
}

function openHistory() {
  store.persist()
  go('/pages/agent/history')
}

function goSettlement() {
  go('/pages/settlement/index')
}

function toolIcon(tool: string) {
  if (tool.includes('task') || tool.includes('faq')) return 'list'
  if (tool.includes('plot') || tool.includes('navigation')) return 'map'
  if (tool.includes('settlement') || tool.includes('work_order')) return 'wallet'
  if (tool.includes('exception')) return 'alert'
  if (tool.includes('start') || tool.includes('end') || tool.includes('status')) return 'satellite'
  if (tool.includes('contact')) return 'phone'
  return 'spark'
}

function toolStatusText(status: string) {
  if (status === 'failed') return '失败'
  if (status === 'running') return '执行中'
  return '完成'
}

function toolStatusColor(status: string) {
  if (status === 'failed') return '#dd5346'
  if (status === 'running') return '#6a7b70'
  return '#16794a'
}
</script>

<template>
  <view class="page-root agent-page">
    <view class="agent-top">
      <view class="icon-btn" @tap="goBack('/pages/home/index')">
        <SfIcon name="back" :size="38" color="#10231b" />
      </view>
      <view class="agent-title grow">
        <text class="agent-title-name">作业助手</text>
        <view class="agent-sub">
          <view class="agent-sub-dot" :class="{ 'agent-sub-dot--off': !app.online }" />
          <text>{{ app.online ? '在线 · 本地规则引擎' : '离线 · 仅本地指令' }}</text>
        </view>
      </view>
      <view class="icon-btn" @tap="newSession">
        <SfIcon name="plus" :size="34" color="#10231b" />
      </view>
      <view class="icon-btn" @tap="openHistory">
        <SfIcon name="history" :size="34" color="#10231b" />
      </view>
    </view>

    <view class="pad" style="margin-top: 18rpx">
      <view class="alert-banner">
        <view class="icon-wrap">
          <SfIcon name="info" :size="34" color="#c47a0a" />
        </view>
        <text class="grow banner-text">
          助手后端尚未接入：当前由本地规则引擎演示，工具调用与回执均为演示数据。
        </text>
      </view>
    </view>

    <view v-if="!app.online" class="pad" style="margin-top: 16rpx">
      <view class="alert-banner">
        <view class="icon-wrap">
          <SfIcon name="cloud-off" :size="34" color="#c47a0a" />
        </view>
        <text class="grow banner-text">当前离线：仅支持查看本地记录，网络恢复后可继续对话。</text>
      </view>
    </view>

    <scroll-view
      class="chat-scroll"
      scroll-y
      :scroll-into-view="scrollInto"
      scroll-with-animation
      :show-scrollbar="false"
    >
      <view class="agent-body">
        <view v-if="empty" class="msg msg--welcome">
          <view class="msg-avatar">
            <SfIcon name="leaf" :size="34" color="#c8f45a" />
          </view>
          <view class="msg-col">
            <view class="bubble">
              <text class="bubble-line">你好，我是您的作业助手。</text>
              <text class="bubble-line">查任务、接单、开作业、报异常、查结算，说一句就行。</text>
              <text class="bubble-line">涉及开工和完工，我会先跟您核对，再执行。</text>
            </view>

            <view class="start-card">
              <view class="start-head">
                <text class="start-title">试试这样问</text>
                <text class="start-hint">点击直接发送</text>
              </view>
              <view class="start-grid">
                <view
                  v-for="q in AGENT_SUGGESTIONS"
                  :key="q"
                  class="start-item"
                  @tap="handleSend(q)"
                >
                  <text>{{ q }}</text>
                  <SfIcon name="chevron" :size="24" color="#aab8ac" />
                </view>
              </view>
              <view class="start-links">
                <view class="start-link" @tap="go('/pages/exception/index')">
                  <SfIcon name="alert" :size="26" color="#14513d" />
                  <text>异常上报</text>
                </view>
                <view class="start-link" @tap="goSettlement()">
                  <SfIcon name="wallet" :size="26" color="#14513d" />
                  <text>结算明细</text>
                </view>
                <view class="start-link" @tap="go('/pages/mine/index')">
                  <SfIcon name="phone" :size="26" color="#14513d" />
                  <text>人工协助</text>
                </view>
              </view>
            </view>

            <view class="agent-status">
              <SfIcon name="shield" :size="24" color="#6a7b70" />
              <text>工具调用按当前账号鉴权，只能查询您本人的数据</text>
            </view>
          </view>
        </view>

        <view
          v-for="msg in messages"
          :id="`msg-${msg.id}`"
          :key="msg.id"
          class="msg"
          :class="{ 'msg--user': msg.role === 'user' }"
        >
          <view class="msg-avatar">
            <text v-if="msg.role === 'user'" class="msg-avatar-text">我</text>
            <SfIcon v-else name="spark" :size="32" color="#c8f45a" />
          </view>
          <view class="msg-col">
            <view v-if="msg.toolCall" class="tool-card">
              <view class="tool-card-head">
                <view class="tool-icon">
                  <SfIcon :name="toolIcon(msg.toolCall.name)" :size="28" color="#14513d" />
                </view>
                <text class="tool-name">{{ msg.toolCall.label }}</text>
                <view v-if="msg.toolCall.status === 'running'" class="spinner" />
                <view v-else class="tool-state">
                  <SfIcon
                    :name="msg.toolCall.status === 'failed' ? 'close' : 'check'"
                    :size="26"
                    :color="toolStatusColor(msg.toolCall.status)"
                  />
                  <text :style="{ color: toolStatusColor(msg.toolCall.status) }">
                    {{ toolStatusText(msg.toolCall.status) }}
                  </text>
                </view>
              </view>
              <view class="tool-result">
                <text class="tool-code">{{ msg.toolCall.name }}</text>
              </view>
            </view>

            <view v-if="msg.text" class="bubble">
              <text>{{ msg.text }}</text>
            </view>

            <view v-if="msg.card" class="acard">
              <view class="acard-head">
                <view class="acard-title">
                  <SfIcon name="doc" :size="26" color="#14513d" />
                  <text>{{ msg.card.title }}</text>
                </view>
              </view>
              <view class="acard-body">
                <view v-for="(f, i) in msg.card.fields || []" :key="i" class="acard-row">
                  <text class="acard-k">{{ f.label }}</text>
                  <text class="acard-v">{{ f.value }}</text>
                </view>
                <view v-if="msg.card.type === 'receipt'" class="receipt-stamp">
                  <SfIcon name="check" :size="24" color="#16794a" />
                  <text>已生成业务回执</text>
                </view>
              </view>
              <view v-if="msg.card.link" class="acard-foot">
                <view class="btn btn--soft btn--sm btn--block" @tap="openLink(msg.card.link)">
                  查看详情
                </view>
              </view>
            </view>

            <view v-if="msg.pendingConfirm" class="confirm-box">
              <text class="confirm-title">{{ msg.pendingConfirm.label }}</text>
              <text class="confirm-summary">{{ msg.pendingConfirm.summary }}</text>
              <view class="confirm-actions">
                <view class="btn btn--ghost btn--sm" @tap="handleCancel(msg)">取消</view>
                <view class="btn btn--primary btn--sm" @tap="handleConfirm(msg)">确认执行</view>
              </view>
              <text class="confirm-note">涉及写操作，请核对信息后执行</text>
            </view>
          </view>
        </view>

        <view v-if="store.thinking" class="msg">
          <view class="msg-avatar">
            <SfIcon name="spark" :size="32" color="#c8f45a" />
          </view>
          <view class="msg-col">
            <view class="agent-status">
              <view class="typing-dots">
                <view />
                <view />
                <view />
              </view>
              <text>正在思考…</text>
            </view>
          </view>
        </view>

        <view v-if="!empty" class="assist-row">
          <view class="quick-mini" @tap="go('/pages/exception/index')">
            <SfIcon name="alert" :size="24" color="#14513d" />
            <text>异常上报</text>
          </view>
          <view class="quick-mini" @tap="goSettlement()">
            <SfIcon name="wallet" :size="24" color="#14513d" />
            <text>结算明细</text>
          </view>
          <view class="quick-mini" @tap="go('/pages/mine/index')">
            <SfIcon name="phone" :size="24" color="#14513d" />
            <text>人工协助</text>
          </view>
        </view>

        <view style="height: 32rpx" />
      </view>
    </scroll-view>

    <view class="composer-bar">
      <scroll-view class="quick-row" scroll-x :show-scrollbar="false">
        <view
          v-for="s in AGENT_SUGGESTIONS"
          :key="s"
          class="quick-mini"
          :class="{ 'quick-mini--off': store.busy }"
          @tap="handleSend(s)"
        >
          {{ s }}
        </view>
      </scroll-view>
      <view class="composer-inner">
        <textarea
          v-model="input"
          class="composer-input"
          auto-height
          maxlength="-1"
          :disabled="store.busy"
          placeholder="说说你想做什么，例如：今天有哪些任务"
          placeholder-class="ph"
          confirm-type="send"
          @confirm="handleSend()"
        />
        <view class="mic-btn" :class="{ 'mic-btn--live': recording }" @tap="handleVoice">
          <SfIcon name="mic" :size="42" :color="recording ? '#ffffff' : '#0a2b20'" />
        </view>
        <view class="send-btn" :class="{ 'send-btn--off': !input.trim() || store.busy }" @tap="handleSend()">
          <SfIcon name="send" :size="40" color="#c8f45a" />
        </view>
      </view>
      <text class="composer-hint">语音为演示识别 · 数字均来自业务系统，助手不自行计算</text>
    </view>

    <view v-if="recording" class="voice-layer">
      <view class="voice-ring">
        <view class="voice-ring-pulse" />
        <view class="voice-ring-pulse voice-ring-pulse--2" />
        <view class="voice-core">
          <SfIcon name="mic" :size="80" color="#0a2b20" />
        </view>
      </view>
      <text class="voice-text">正在听，请说…</text>
      <text class="voice-hint">演示环境使用模拟识别，结果会自动填入并发送</text>
    </view>
  </view>
</template>

<style scoped>
.agent-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-sizing: border-box;
}

.agent-title-name {
  font-size: 33rpx;
  font-weight: 800;
  color: var(--ink);
  line-height: 1.25;
}

.agent-sub {
  display: flex;
  align-items: center;
  gap: 8rpx;
  margin-top: 2rpx;
  font-size: 22rpx;
  color: var(--muted);
}

.agent-sub-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  background: #2fbf71;
  flex: none;
}

.agent-sub-dot--off {
  background: var(--red);
}

.banner-text {
  font-size: 25rpx;
  line-height: 1.5;
}

.chat-scroll {
  flex: 1;
  height: 0;
  width: 100%;
}

.agent-body {
  padding: 28rpx 28rpx 12rpx;
  display: flex;
  flex-direction: column;
  gap: 28rpx;
}

.bubble-line {
  display: block;
}

.bubble-line + .bubble-line {
  margin-top: 6rpx;
}

.msg--welcome .msg-col {
  max-width: 100%;
  flex: 1;
  min-width: 0;
}

.start-card {
  background: #fff;
  border: 2rpx solid rgba(14, 59, 46, 0.07);
  border-radius: 32rpx;
  padding: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(12, 42, 31, 0.04);
}

.start-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 18rpx;
  padding: 0 4rpx;
}

.start-title {
  font-size: 26rpx;
  font-weight: 800;
  color: var(--ink-2);
}

.start-hint {
  font-size: 21rpx;
  color: var(--muted-2);
}

.start-grid {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.start-item {
  min-width: 0;
  height: 80rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
  padding: 0 22rpx;
  border-radius: 20rpx;
  background: var(--surface-2);
  border: 2rpx solid var(--line);
  font-size: 25rpx;
  font-weight: 600;
  color: var(--ink-2);
  box-sizing: border-box;
}

.start-item text {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.start-item:active {
  background: var(--lime-soft);
  border-color: var(--lime-deep);
}

.start-links {
  display: flex;
  gap: 12rpx;
  margin-top: 18rpx;
  padding-top: 18rpx;
  border-top: 2rpx dashed var(--line);
}

.start-link {
  flex: 1;
  min-width: 0;
  height: 68rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  border-radius: 18rpx;
  font-size: 23rpx;
  font-weight: 700;
  color: var(--green-700);
}

.start-link:active {
  background: var(--surface-2);
}

.start-link text {
  white-space: nowrap;
}

.msg-avatar-text {
  font-size: 26rpx;
  font-weight: 800;
}

.tool-state {
  display: flex;
  align-items: center;
  gap: 6rpx;
  font-size: 21rpx;
  font-weight: 700;
}

.tool-code {
  font-size: 21rpx;
  color: var(--muted);
  letter-spacing: 0.02em;
}

.acard-title text {
  font-size: 24rpx;
  font-weight: 800;
  color: var(--green-700);
  letter-spacing: 0.03em;
}

.acard-k {
  color: var(--muted);
  flex: none;
  font-size: 25rpx;
}

.acard-v {
  font-weight: 700;
  text-align: right;
  font-size: 25rpx;
  word-break: break-all;
  color: var(--ink);
}

.receipt-stamp {
  margin-top: 14rpx;
}

.receipt-stamp text {
  font-size: 22rpx;
  font-weight: 700;
}

.confirm-box {
  background: var(--amber-soft);
  border: 2rpx solid #f7d59b;
  border-radius: 30rpx;
  padding: 24rpx 26rpx;
}

.confirm-title {
  display: block;
  font-size: 28rpx;
  font-weight: 800;
  color: #9a5c00;
}

.confirm-summary {
  display: block;
  margin-top: 10rpx;
  font-size: 25rpx;
  color: #7b5300;
  line-height: 1.6;
}

.confirm-actions {
  display: flex;
  gap: 18rpx;
  margin-top: 20rpx;
}

.confirm-actions .btn {
  flex: 1;
}

.confirm-note {
  display: block;
  margin-top: 14rpx;
  font-size: 21rpx;
  color: #a3762b;
}

.typing-dots {
  display: inline-flex;
  align-items: center;
  gap: 6rpx;
}

.typing-dots view {
  width: 8rpx;
  height: 8rpx;
  border-radius: 50%;
  background: var(--muted);
  animation: dotBlink 1s infinite;
}

.typing-dots view:nth-child(2) {
  animation-delay: 0.15s;
}

.typing-dots view:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes dotBlink {
  0%,
  80%,
  100% {
    opacity: 0.25;
  }
  40% {
    opacity: 1;
  }
}

.assist-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14rpx;
}

.assist-row .quick-mini {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  padding: 0 12rpx;
}

.assist-row .quick-mini text {
  white-space: nowrap;
}

.composer-bar {
  flex: none;
  padding: 12rpx 24rpx calc(18rpx + env(safe-area-inset-bottom, 0px));
  background: linear-gradient(180deg, rgba(239, 242, 230, 0) 0%, var(--bg) 18%, var(--bg) 100%);
}

.quick-row {
  width: 100%;
  white-space: nowrap;
  padding-bottom: 8rpx;
}

.quick-row .quick-mini {
  display: inline-flex;
  align-items: center;
  margin-right: 14rpx;
}

.quick-mini--off {
  opacity: 0.5;
}

.composer-inner {
  display: flex;
  align-items: flex-end;
  gap: 14rpx;
  background: #ffffff;
  border: 2rpx solid var(--line);
  border-radius: 40rpx;
  padding: 12rpx 14rpx 12rpx 28rpx;
  box-shadow: 0 12rpx 40rpx rgba(12, 42, 31, 0.08);
}

.composer-input {
  flex: 1;
  min-height: 80rpx;
  max-height: 176rpx;
  padding: 18rpx 0;
  font-size: 30rpx;
  line-height: 1.4;
  background: transparent;
}

.composer-hint {
  display: block;
  margin-top: 10rpx;
  font-size: 20rpx;
  color: var(--muted-2);
  text-align: center;
}

.send-btn--off {
  opacity: 0.35;
}

.voice-layer {
  position: fixed;
}

.voice-ring-pulse {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  border: 4rpx solid rgba(200, 244, 90, 0.4);
  animation: voicePulse 2s infinite;
}

.voice-ring-pulse--2 {
  animation-delay: 0.8s;
}

@keyframes voicePulse {
  0% {
    transform: scale(0.6);
    opacity: 0.9;
  }
  100% {
    transform: scale(1.5);
    opacity: 0;
  }
}

.ph {
  color: #8d9c91;
}
</style>
