<script setup lang="ts">
import { computed } from 'vue'
import { useAppStore } from '@/stores/app'

const app = useAppStore()

const mock = computed(() => app.mockEnabled)

function openMessage() {
  uni.navigateTo({ url: '/pages/message/index' })
}
</script>

<template>
  <view class="status-bar">
    <view class="status-item" :class="{ 'status-item--bad': !app.online }">
      <view class="dot" :class="app.online ? 'dot--ok' : 'dot--bad'" />
      <text>{{ app.networkLabel }}</text>
    </view>
    <view class="status-item" :class="{ 'status-item--warn': app.gpsPermission !== 'granted' }">
      <view
        class="dot"
        :class="app.gpsPermission === 'granted' ? 'dot--ok' : 'dot--warn'"
      />
      <text>{{ app.gpsLabel }}</text>
    </view>
    <view class="status-item status-item--tap" @tap="openMessage">
      <view class="dot" :class="app.unreadCount > 0 ? 'dot--warn' : 'dot--ok'" />
      <text>{{ app.unreadCount }} 条未读</text>
    </view>
    <text v-if="mock" class="status-demo">演示数据</text>
  </view>
</template>

<style scoped lang="scss">
.status-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 20rpx;
  padding: 14rpx 20rpx;
  background: #ffffff;
  border-radius: 999rpx;
  border: 1rpx solid $border;
  margin-bottom: 24rpx;
}

.status-item {
  display: flex;
  align-items: center;
  font-size: 22rpx;
  color: $text-sub;
}

.status-item--bad {
  color: $danger;
}

.status-item--warn {
  color: $warning;
}

.status-item--tap:active {
  opacity: 0.6;
}

.dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  margin-right: 8rpx;
  background: $text-weak;
}

.dot--ok {
  background: $success;
}

.dot--warn {
  background: $accent;
}

.dot--bad {
  background: $danger;
}

.status-demo {
  margin-left: auto;
  font-size: 20rpx;
  color: $warning;
  background: #fbf1dc;
  border: 1rpx solid #efdcb3;
  border-radius: 6rpx;
  padding: 2rpx 10rpx;
}
</style>
