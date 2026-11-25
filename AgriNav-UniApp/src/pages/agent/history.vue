<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import SfIcon from '@/components/SfIcon.vue'
import type { AgentSession } from '@/types'
import { deleteSession, listSessions } from '@/api/agent'
import { useAgentStore } from '@/stores/agent'
import { formatDate } from '@/utils/format'
import { goBack } from '@/utils/nav'
import { confirmAction, toast } from '@/utils/ui'

const sessions = ref<AgentSession[]>([])
const store = useAgentStore()

onShow(() => {
  refresh()
})

function refresh() {
  sessions.value = listSessions()
}

function open(item: AgentSession) {
  const ok = store.loadSession(item.id)
  if (!ok) {
    toast('会话已失效')
    return
  }
  goBack('/pages/agent/index')
}

async function remove(item: AgentSession) {
  const yes = await confirmAction({
    title: '删除会话',
    content: '删除后无法恢复，是否继续？',
    confirmText: '删除',
    danger: true
  })
  if (!yes) return
  deleteSession(item.id)
  if (store.session?.id === item.id) {
    store.newSession()
  }
  refresh()
  toast('已删除')
}

function lastText(item: AgentSession): string {
  const last = item.messages[item.messages.length - 1]
  return last?.text || '暂无消息'
}
</script>

<template>
  <view class="page-root">
    <view class="page-head">
      <view class="icon-btn" @tap="goBack('/pages/agent/index')">
        <SfIcon name="back" :size="40" color="#10231b" />
      </view>
      <text class="page-title grow">历史会话</text>
      <view class="icon-btn" @tap="refresh">
        <SfIcon name="refresh" :size="38" color="#10231b" />
      </view>
    </view>

    <view class="section" style="margin-top: 24rpx">
      <view class="section-head">
        <text class="section-title">全部会话</text>
        <text class="tiny muted">{{ sessions.length }} 个</text>
      </view>

      <view v-if="!sessions.length" class="card empty" style="padding: 56rpx 40rpx">
        <view class="empty-ico">
          <SfIcon name="history" :size="60" color="#1c6a4f" />
        </view>
        <text class="empty-title">暂无历史会话</text>
        <text class="tiny" style="display: block; margin-top: 8rpx">
          与助手的对话会按账号隔离保存在这里
        </text>
      </view>

      <view v-for="item in sessions" :key="item.id" class="card session" @tap="open(item)">
        <view class="row gap-10">
          <view class="session-ico">
            <SfIcon name="message" :size="38" color="#14513d" />
          </view>
          <view class="grow">
            <text class="session-title">{{ item.title || '新会话' }}</text>
            <text class="session-preview">{{ lastText(item) }}</text>
          </view>
        </view>
        <view class="session-foot">
          <view class="row gap-8">
            <text class="tiny muted">{{ formatDate(item.createTime) }}</text>
            <text class="tiny muted">· {{ item.messages.length }} 条消息</text>
          </view>
          <view class="btn btn--danger btn--sm" @tap.stop="remove(item)">
            <SfIcon name="trash" :size="26" color="#a92f24" />
            <text>删除</text>
          </view>
        </view>
      </view>
    </view>

    <view class="notice" style="margin: 28rpx 36rpx 48rpx">
      <SfIcon name="shield" :size="28" color="#6a7b70" />
      <text>会话按账号隔离保存；工具调用均校验身份，涉及写操作需二次确认。</text>
    </view>
  </view>
</template>

<style scoped>
.empty-ico {
  display: flex;
  justify-content: center;
  margin-bottom: 20rpx;
}

.empty-title {
  display: block;
  font-size: 30rpx;
  font-weight: 700;
  color: var(--ink-2);
}

.session {
  padding: 28rpx 30rpx;
  margin-bottom: 20rpx;
}

.session:active {
  transform: scale(0.995);
}

.session-ico {
  width: 76rpx;
  height: 76rpx;
  border-radius: 22rpx;
  background: var(--surface-2);
  border: 2rpx solid var(--line);
  display: grid;
  place-items: center;
  flex: none;
}

.session-title {
  display: block;
  font-size: 30rpx;
  font-weight: 700;
  color: var(--ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-preview {
  display: block;
  margin-top: 6rpx;
  font-size: 23rpx;
  color: var(--muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  margin-top: 18rpx;
  padding-top: 16rpx;
  border-top: 2rpx dashed var(--line);
}

.session-foot .btn--sm {
  height: 60rpx;
  padding: 0 20rpx;
  border-radius: 18rpx;
}
</style>
