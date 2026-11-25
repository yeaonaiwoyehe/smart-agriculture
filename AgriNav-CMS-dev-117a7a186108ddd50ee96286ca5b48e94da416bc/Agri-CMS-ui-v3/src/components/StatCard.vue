<template>
  <div class="stat-card">
    <div class="stat-top">
      <span class="stat-label">{{ label }}</span>
      <span v-if="icon" class="stat-icon" :class="tone">
        <el-icon :size="15"><component :is="icon" /></el-icon>
      </span>
    </div>
    <div class="stat-value num">
      {{ value }}<span v-if="unit" class="stat-unit">{{ unit }}</span>
    </div>
    <div class="stat-foot">
      <span v-if="delta !== null" class="delta" :class="deltaTone || (Number(delta) >= 0 ? 'up' : 'down')">
        {{ Number(delta) >= 0 ? '↑' : '↓' }} {{ Math.abs(delta) }}{{ deltaUnit }}
      </span>
      <span>{{ foot }}</span>
    </div>
    <svg v-if="spark && spark.length" class="stat-spark" width="96" height="34" viewBox="0 0 96 34">
      <polyline :points="sparkPoints" fill="none" stroke="#1fae6b" stroke-width="1.6" stroke-linejoin="round" />
    </svg>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  label: String,
  value: [String, Number],
  unit: { type: String, default: '' },
  icon: { type: String, default: '' },
  tone: { type: String, default: '' },
  delta: { type: [String, Number], default: null },
  deltaUnit: { type: String, default: '%' },
  deltaTone: { type: String, default: '' },
  foot: { type: String, default: '较昨日' },
  spark: { type: Array, default: () => [] }
})

const sparkPoints = computed(() => {
  const arr = props.spark
  const max = Math.max(...arr)
  const min = Math.min(...arr)
  const range = max - min || 1
  return arr
    .map((v, i) => `${(i / (arr.length - 1)) * 96},${32 - ((v - min) / range) * 26}`)
    .join(' ')
})
</script>
