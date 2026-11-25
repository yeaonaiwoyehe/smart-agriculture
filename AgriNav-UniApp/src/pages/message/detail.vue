<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import SfIcon from '@/components/SfIcon.vue'
import type { NotificationItem } from '@/types'
import { fetchNotificationDetail, markRead } from '@/api/message'
import { NOTICE_TYPE_TEXT } from '@/utils/constants'
import { go, goBack } from '@/utils/nav'
import { errorMessage } from '@/utils/ui'

const item = ref<NotificationItem | null>(null)
const loading = ref(true)
const error = ref('')

onLoad(async (query) => {
  const id = String(query?.id ?? '')
  if (!id) {
    error.value = '缺少消息编号'
    loading.value = false
    return
  }
  try {
    item.value = await fetchNotificationDetail(id)
    if (!item.value.read) {
      await markRead(id)
      item.value.read = true
    }
  } catch (e) {
    error.value = errorMessage(e, '消息加载失败')
  } finally {
    loading.value = false
  }
})

function openBiz() {
  if (!item.value) return
  const { type, bizId } = item.value
  if (type === 'task' && bizId) {
    go(`/pages/task/detail?id=${bizId}`)
  } else if (type === 'work' && bizId) {
    go(`/pages/work/detail?id=${encodeURIComponent(bizId)}`)
  } else if (type === 'settlement' && bizId) {
    go(`/pages/settlement/detail?id=${bizId}`)
  } else if (type === 'exception' && bizId) {
    go(`/pages/exception/detail?id=${bizId}`)
  }
}
</script>

<template>
  <view class="page-root">
    <view class="page-head">
      <view class="icon-btn" @tap="goBack('/pages/message/index')">
        <SfIcon name="back" :size="40" color="#10231b" />
      </view>
      <text class="page-title grow">消息详情</text>
    </view>

    <view v-if="loading" class="pad" style="margin-top: 24rpx">
      <view class="card" style="padding: 32rpx">
        <view class="skeleton" style="height: 40rpx; width: 60%" />
        <view class="skeleton" style="height: 24rpx; width: 34%; margin-top: 18rpx" />
        <view class="skeleton" style="height: 24rpx; width: 100%; margin-top: 28rpx" />
        <view class="skeleton" style="height: 24rpx; width: 88%; margin-top: 12rpx" />
        <view class="skeleton" style="height: 24rpx; width: 72%; margin-top: 12rpx" />
      </view>
    </view>

    <view v-else-if="error" class="pad" style="margin-top: 24rpx">
      <view class="alert-banner">
        <view class="icon-wrap">
          <SfIcon name="alert" :size="36" color="#c47a0a" />
        </view>
        <text class="grow">{{ error }}</text>
        <view class="btn btn--soft btn--sm" @tap="goBack('/pages/message/index')">返回</view>
      </view>
    </view>

    <template v-else-if="item">
      <view class="section" style="margin-top: 24rpx">
        <view class="card" style="padding: 32rpx">
          <view class="row-between">
            <text class="msg-title grow">{{ item.title }}</text>
            <text class="plot-code">{{ NOTICE_TYPE_TEXT[item.type] }}</text>
          </view>
          <view class="msg-time">
            <SfIcon name="clock" :size="24" color="#8d9c91" />
            <text>{{ item.createTime }}</text>
          </view>
          <view class="divider" />
          <text class="msg-content">{{ item.content }}</text>
        </view>
      </view>

      <view v-if="item.bizId" class="pad" style="margin-top: 28rpx">
        <view class="btn btn--primary btn--block" @tap="openBiz">
          <SfIcon name="doc" :size="34" color="#ffffff" />
          <text>查看关联单据</text>
        </view>
      </view>

      <view class="notice" style="margin: 28rpx 36rpx 48rpx">
        <SfIcon name="shield" :size="28" color="#6a7b70" />
        <text>消息由业务系统推送，涉及金额与面积以关联单据为准。</text>
      </view>
    </template>
  </view>
</template>

<style scoped>
.msg-title {
  font-size: 36rpx;
  font-weight: 800;
  line-height: 1.4;
  color: var(--ink);
}

.msg-time {
  display: flex;
  align-items: center;
  gap: 8rpx;
  margin-top: 12rpx;
  font-size: 22rpx;
  color: var(--muted);
}

.msg-content {
  display: block;
  font-size: 29rpx;
  line-height: 1.8;
  color: var(--ink-2);
}
</style>
