<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import SfIcon from '@/components/SfIcon.vue'
import type { FeedbackItem } from '@/types'
import { fetchFeedbackDetail } from '@/api/exception'
import { FEEDBACK_STATUS_TEXT } from '@/utils/constants'
import { goBack } from '@/utils/nav'
import { errorMessage } from '@/utils/ui'

const ticket = ref<FeedbackItem | null>(null)
const loading = ref(true)
const error = ref('')

onLoad(async (query) => {
  const id = String(query?.id || '')
  if (!id) {
    error.value = '缺少工单编号'
    loading.value = false
    return
  }
  try {
    ticket.value = await fetchFeedbackDetail(id)
  } catch (e) {
    error.value = errorMessage(e, '工单加载失败')
  } finally {
    loading.value = false
  }
})

function statusClass(): string {
  if (!ticket.value) return 'status--todo'
  if (ticket.value.status === 'resolved') return 'status--ok'
  if (ticket.value.status === 'processing') return 'status--review'
  return 'status--todo'
}
</script>

<template>
  <view class="page-root">
    <view class="page-head">
      <view class="icon-btn" @tap="goBack('/pages/exception/index')">
        <SfIcon name="back" :size="40" color="#10231b" />
      </view>
      <text class="page-title grow">工单详情</text>
    </view>

    <view v-if="loading" class="pad" style="margin-top: 24rpx">
      <view class="card" style="padding: 32rpx">
        <view class="skeleton" style="height: 38rpx; width: 48%" />
        <view class="skeleton" style="height: 24rpx; width: 66%; margin-top: 18rpx" />
        <view class="skeleton" style="height: 24rpx; width: 52%; margin-top: 12rpx" />
        <view class="skeleton" style="height: 140rpx; margin-top: 26rpx" />
      </view>
    </view>

    <view v-else-if="error" class="pad" style="margin-top: 24rpx">
      <view class="alert-banner">
        <view class="icon-wrap">
          <SfIcon name="alert" :size="36" color="#c47a0a" />
        </view>
        <text class="grow">{{ error }}</text>
        <view class="btn btn--soft btn--sm" @tap="goBack('/pages/exception/index')">返回</view>
      </view>
    </view>

    <template v-else-if="ticket">
      <view class="section" style="margin-top: 24rpx">
        <view class="card" style="padding: 30rpx">
          <view class="row-between">
            <text class="plot-name">{{ ticket.typeName }}</text>
            <text class="status" :class="statusClass()">
              {{ FEEDBACK_STATUS_TEXT[ticket.status] }}
            </text>
          </view>
          <view class="receipt-line">
            <text class="receipt-no">{{ ticket.receiptNo }}</text>
            <text class="tiny muted">{{ ticket.createTime }}</text>
          </view>
          <view class="divider" />
          <text class="content">{{ ticket.content }}</text>
          <view v-if="ticket.images.length" class="images">
            <image
              v-for="(img, idx) in ticket.images"
              :key="idx"
              :src="img"
              class="image"
              mode="aspectFill"
            />
          </view>
        </view>
      </view>

      <view class="section">
        <view class="section-head">
          <text class="section-title">处理进度</text>
        </view>
        <view class="card" style="padding: 30rpx">
          <view class="timeline">
            <view class="timeline-item">
              <view class="timeline-dot timeline-dot--on" />
              <view class="grow">
                <text class="timeline-title">已提交</text>
                <text class="timeline-desc">{{ ticket.createTime }}</text>
              </view>
            </view>
            <view class="timeline-item">
              <view class="timeline-dot" :class="{ 'timeline-dot--on': ticket.status !== 'submitted' }" />
              <view class="grow">
                <text class="timeline-title">处理中</text>
                <text class="timeline-desc">
                  {{ ticket.status !== 'submitted' ? '工作人员已受理' : '等待受理' }}
                </text>
              </view>
            </view>
            <view class="timeline-item timeline-item--last">
              <view class="timeline-dot" :class="{ 'timeline-dot--on': ticket.status === 'resolved' }" />
              <view class="grow">
                <text class="timeline-title">已解决</text>
                <text class="timeline-desc">
                  {{ ticket.status === 'resolved' ? ticket.reply || '问题已处理完成' : '待处理' }}
                </text>
              </view>
            </view>
          </view>
        </view>
      </view>

      <view v-if="ticket.reply" class="section">
        <view class="section-head">
          <text class="section-title">处理回复</text>
        </view>
        <view class="notice">
          <SfIcon name="message" :size="30" color="#1c6a4f" />
          <text>{{ ticket.reply }}</text>
        </view>
      </view>
    </template>

    <view style="height: 48rpx" />
  </view>
</template>

<style scoped>
.receipt-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  margin-top: 14rpx;
}

.receipt-no {
  font-size: 23rpx;
  font-weight: 700;
  color: var(--green-700);
  background: var(--surface-2);
  border: 2rpx solid var(--line);
  border-radius: 12rpx;
  padding: 4rpx 12rpx;
  letter-spacing: 0.04em;
}

.content {
  display: block;
  font-size: 29rpx;
  line-height: 1.75;
  color: var(--ink-2);
}

.images {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  margin-top: 22rpx;
}

.image {
  width: 180rpx;
  height: 180rpx;
  border-radius: 20rpx;
  background: var(--surface-2);
}

.timeline {
  display: flex;
  flex-direction: column;
}

.timeline-item {
  display: flex;
  align-items: flex-start;
  gap: 20rpx;
  padding-bottom: 30rpx;
  position: relative;
}

.timeline-item::before {
  content: '';
  position: absolute;
  left: 10rpx;
  top: 30rpx;
  bottom: 0;
  width: 2rpx;
  background: var(--line);
}

.timeline-item--last {
  padding-bottom: 0;
}

.timeline-item--last::before {
  display: none;
}

.timeline-dot {
  width: 22rpx;
  height: 22rpx;
  border-radius: 50%;
  background: #d5ddd8;
  margin-top: 8rpx;
  flex: none;
  position: relative;
  z-index: 1;
  border: 4rpx solid var(--bg);
  box-sizing: border-box;
}

.timeline-dot--on {
  background: var(--green-600);
  box-shadow: 0 0 0 6rpx rgba(28, 106, 79, 0.14);
}

.timeline-title {
  display: block;
  font-size: 28rpx;
  font-weight: 700;
  color: var(--ink);
}

.timeline-desc {
  display: block;
  margin-top: 4rpx;
  font-size: 23rpx;
  color: var(--muted);
  line-height: 1.55;
}
</style>
