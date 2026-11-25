<script setup lang="ts">
import SfIcon from './SfIcon.vue'

withDefaults(
  defineProps<{
    running: boolean
    canStart?: boolean
    startLabel?: string
    endLabel?: string
  }>(),
  { canStart: true, startLabel: '开始作业', endLabel: '结束作业' }
)

const emit = defineEmits<{
  (e: 'start'): void
  (e: 'end'): void
}>()
</script>

<template>
  <view class="dock">
    <view class="dock-inner">
      <view
        class="dock-btn dock-btn--start"
        :class="{ 'dock-btn--disabled': running || !canStart }"
        @tap="!running && canStart && emit('start')"
      >
        <SfIcon name="play" :size="40" color="#0a2b20" />
        <text>{{ startLabel }}</text>
      </view>
      <view
        class="dock-btn dock-btn--end"
        :class="{ 'dock-btn--disabled': !running }"
        @tap="running && emit('end')"
      >
        <SfIcon name="stop" :size="38" color="#ffffff" />
        <text>{{ endLabel }}</text>
      </view>
    </view>
  </view>
</template>
