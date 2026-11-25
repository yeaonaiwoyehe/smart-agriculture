<script setup lang="ts">
defineProps<{ text?: string }>()
</script>

<template>
  <view class="empty-state">
    <view class="empty-state__icon" />
    <text class="empty-state__text">{{ text || '暂无数据' }}</text>
  </view>
</template>

<style scoped lang="scss">
.empty-state {
  padding: 90rpx 0;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.empty-state__icon {
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
  border: 4rpx dashed #cfd8d2;
  margin-bottom: 20rpx;
}

.empty-state__text {
  font-size: 28rpx;
  color: $text-weak;
}
</style>
