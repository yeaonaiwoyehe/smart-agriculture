<script setup lang="ts">
import { computed } from 'vue'
import SfIcon from '@/components/SfIcon.vue'
import type { Task } from '@/types'
import { TASK_STATUS_TEXT } from '@/utils/constants'
import { formatArea, formatTime } from '@/utils/format'

const props = defineProps<{ task: Task }>()
const emit = defineEmits<{
  (e: 'view', task: Task): void
  (e: 'navigate', task: Task): void
  (e: 'claim', task: Task): void
}>()

const statusClass = computed(() => {
  switch (props.task.status) {
    case 'pending':
      return 'pending'
    case 'claimed':
      return 'todo'
    case 'working':
      return 'doing'
    case 'done':
      return 'done'
    default:
      return 'timeout'
  }
})

const statusText = computed(() => TASK_STATUS_TEXT[props.task.status])
const deadlineLabel = computed(() => `截止 ${formatTime(props.task.deadline)}`)
</script>

<template>
  <view class="card task-card" :class="`task-card--${statusClass}`" @tap="emit('view', task)">
    <view class="task-card-top">
      <view class="grow">
        <view class="plot-name">
          {{ task.plot.plotName }}
          <text class="plot-code">{{ task.plot.plotNo }}</text>
        </view>
        <view class="task-meta">
          <text class="row gap-4">
            <SfIcon name="pin" :size="26" color="#6a7b70" />
            {{ task.distanceKm }} km
          </text>
          <text>{{ task.workTypeName }}</text>
          <text>{{ formatArea(task.area) }} 亩</text>
        </view>
      </view>
      <text class="status" :class="`status--${statusClass}`">{{ statusText }}</text>
    </view>

    <view class="task-req">要求：{{ task.requirement }}</view>

    <view class="task-foot">
      <view class="small muted">
        <text class="task-price">{{ task.unitPrice.toFixed(2) }}</text> 元/亩 ·
        {{ deadlineLabel }}
      </view>
      <view class="row gap-6" @tap.stop>
        <view class="btn btn--soft btn--sm" @tap.stop="emit('navigate', task)">
          <SfIcon name="navigation" :size="30" color="#0e3b2e" />
          <text>导航</text>
        </view>
        <view
          v-if="task.status === 'pending'"
          class="btn btn--lime btn--sm"
          @tap.stop="emit('claim', task)"
        >
          接收任务
        </view>
        <view v-else class="btn btn--ghost btn--sm" @tap.stop="emit('view', task)">查看</view>
      </view>
    </view>
  </view>
</template>

<style scoped>
.gap-4 {
  gap: 8rpx;
}

.task-price {
  color: var(--ink-2);
}
</style>
