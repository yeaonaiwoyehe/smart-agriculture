<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import SfIcon from '@/components/SfIcon.vue'
import type { NotificationItem } from '@/types'
import { fetchNotifications, markAllRead, markRead } from '@/api/message'
import { useAppStore } from '@/stores/app'
import { NOTICE_TYPE_TEXT } from '@/utils/constants'
import { relativeTime } from '@/utils/format'
import { go, goBack } from '@/utils/nav'
import { errorMessage, toast } from '@/utils/ui'

type Filter = 'all' | NotificationItem['type']

const app = useAppStore()
const list = ref<NotificationItem[]>([])
const filter = ref<Filter>('all')
const loading = ref(false)
const error = ref('')

const tabs: Array<{ key: Filter; label: string }> = [
  { key: 'all', label: '全部' },
  { key: 'task', label: '任务' },
  { key: 'work', label: '作业' },
  { key: 'settlement', label: '结算' },
  { key: 'exception', label: '异常' },
  { key: 'system', label: '系统' }
]

const filtered = computed(() =>
  filter.value === 'all' ? list.value : list.value.filter((n) => n.type === filter.value)
)

const unreadCount = computed(() => list.value.filter((n) => !n.read).length)

onShow(() => {
  void load()
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    list.value = await fetchNotifications()
    await app.refreshUnread()
  } catch (e) {
    error.value = errorMessage(e, '消息加载失败')
  } finally {
    loading.value = false
  }
}

async function handleReadAll() {
  if (!unreadCount.value) {
    toast('没有未读消息')
    return
  }
  try {
    await markAllRead()
    list.value = list.value.map((n) => ({ ...n, read: true }))
    await app.refreshUnread()
    toast('已全部标记为已读', 'success')
  } catch (e) {
    toast(errorMessage(e, '操作失败'))
  }
}

async function openItem(item: NotificationItem) {
  if (!item.read) {
    try {
      await markRead(item.id)
      item.read = true
      await app.refreshUnread()
    } catch (e) {
      // 已读失败不影响查看
    }
  }
  go(`/pages/message/detail?id=${item.id}`)
}
</script>

<template>
  <view class="page-root">
    <view class="page-head">
      <view class="icon-btn" @tap="goBack('/pages/home/index')">
        <SfIcon name="back" :size="40" color="#10231b" />
      </view>
      <text class="page-title grow">消息中心</text>
      <view class="icon-btn" @tap="handleReadAll">
        <SfIcon name="check" :size="38" color="#10231b" />
      </view>
    </view>

    <view class="pad" style="margin-top: 24rpx">
      <view class="card" style="padding: 28rpx 30rpx">
        <view class="row-between">
          <view class="row gap-10 grow">
            <view class="summary-ico">
              <SfIcon name="message" :size="38" color="#14513d" />
            </view>
            <view class="grow">
              <text class="summary-title">
                {{ unreadCount ? `${unreadCount} 条未读消息` : '暂无未读消息' }}
              </text>
              <text class="summary-sub">共 {{ list.length }} 条 · 点击查看详情</text>
            </view>
          </view>
          <view class="btn btn--soft btn--sm" @tap="handleReadAll">全部已读</view>
        </view>
      </view>
    </view>

    <scroll-view class="tabs-scroll" scroll-x :show-scrollbar="false">
      <view class="tabs">
        <view
          v-for="t in tabs"
          :key="t.key"
          class="tab"
          :class="{ 'tab--active': filter === t.key }"
          @tap="filter = t.key"
        >
          {{ t.label }}
        </view>
      </view>
    </scroll-view>

    <view class="section" style="margin-top: 4rpx">
      <view v-if="error" class="alert-banner">
        <view class="icon-wrap">
          <SfIcon name="alert" :size="36" color="#c47a0a" />
        </view>
        <text class="grow">{{ error }}</text>
        <view class="btn btn--soft btn--sm" @tap="load">重试</view>
      </view>

      <view v-if="loading && !list.length" class="card" style="padding: 32rpx">
        <view class="skeleton" style="height: 34rpx; width: 42%" />
        <view class="skeleton" style="height: 24rpx; width: 86%; margin-top: 18rpx" />
        <view class="skeleton" style="height: 24rpx; width: 58%; margin-top: 12rpx" />
      </view>

      <view v-else-if="!filtered.length" class="card empty" style="padding: 56rpx 40rpx">
        <view class="empty-ico">
          <SfIcon name="message" :size="60" color="#1c6a4f" />
        </view>
        <text class="empty-title">暂无消息</text>
        <text class="tiny" style="display: block; margin-top: 8rpx">任务、作业与结算通知会显示在这里</text>
      </view>

      <view
        v-for="item in filtered"
        :key="item.id"
        class="card msg-card"
        :class="{ 'msg-card--unread': !item.read }"
        @tap="openItem(item)"
      >
        <view class="row-between">
          <view class="row gap-8 grow">
            <view v-if="!item.read" class="msg-dot" />
            <text class="msg-title">{{ item.title }}</text>
          </view>
          <text class="plot-code">{{ NOTICE_TYPE_TEXT[item.type] }}</text>
        </view>
        <text class="msg-content">{{ item.content }}</text>
        <view class="msg-foot">
          <text>{{ relativeTime(item.createTime) }}</text>
          <view class="plot-link">
            <text>查看</text>
            <SfIcon name="chevron" :size="24" color="#1c6a4f" />
          </view>
        </view>
      </view>
    </view>

    <view style="height: 48rpx" />
  </view>
</template>

<style scoped>
.summary-ico {
  width: 76rpx;
  height: 76rpx;
  border-radius: 22rpx;
  background: var(--surface-2);
  border: 2rpx solid var(--line);
  display: grid;
  place-items: center;
  flex: none;
}

.summary-title {
  display: block;
  font-size: 30rpx;
  font-weight: 700;
  color: var(--ink);
}

.summary-sub {
  display: block;
  margin-top: 2rpx;
  font-size: 23rpx;
  color: var(--muted);
}

.tabs-scroll {
  width: 100%;
  white-space: nowrap;
  margin-top: 24rpx;
}

.tabs-scroll .tabs {
  display: inline-flex;
  position: static;
  background: transparent;
  padding: 0 32rpx 12rpx;
}

.tabs-scroll .tab {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-right: 12rpx;
  flex: none;
}

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

.msg-card {
  padding: 28rpx 30rpx;
  margin-bottom: 20rpx;
  border-left: 10rpx solid transparent;
}

.msg-card:active {
  transform: scale(0.995);
}

.msg-card--unread {
  border-left-color: var(--lime-deep);
}

.msg-dot {
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
  background: var(--red);
  flex: none;
}

.msg-title {
  font-size: 30rpx;
  font-weight: 700;
  color: var(--ink);
}

.msg-content {
  display: block;
  margin-top: 14rpx;
  font-size: 26rpx;
  color: var(--ink-2);
  line-height: 1.6;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.msg-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  margin-top: 18rpx;
  padding-top: 16rpx;
  border-top: 2rpx dashed var(--line);
  font-size: 21rpx;
  color: var(--muted);
}
</style>
