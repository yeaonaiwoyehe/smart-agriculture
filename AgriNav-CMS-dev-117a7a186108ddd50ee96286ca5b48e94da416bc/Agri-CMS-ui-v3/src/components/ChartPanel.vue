<template>
  <div ref="el" class="chart-box" :style="{ height: height + 'px' }" />
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  option: { type: Object, required: true },
  height: { type: Number, default: 260 }
})

const el = ref(null)
let chart = null
let observer = null

const PALETTE = ['#146b45', '#1fae6b', '#84cc16', '#e08a12', '#3b7cf0', '#8b5cf6', '#e04b4b']
const BASE = {
  color: PALETTE,
  textStyle: { fontFamily: 'Inter, "PingFang SC", "Microsoft YaHei", sans-serif', color: '#56645d', fontSize: 11 },
  grid: { left: 40, right: 16, top: 28, bottom: 26, containLabel: true },
  tooltip: {
    trigger: 'axis',
    backgroundColor: 'rgba(12,42,31,.94)',
    borderWidth: 0,
    textStyle: { color: '#eafff5', fontSize: 11 },
    padding: [8, 10]
  },
  xAxis: {
    axisLine: { lineStyle: { color: '#e2e9e4' } },
    axisTick: { show: false },
    axisLabel: { color: '#8b9992', fontSize: 10.5 }
  },
  yAxis: {
    splitLine: { lineStyle: { color: '#eef3f0' } },
    axisLabel: { color: '#8b9992', fontSize: 10.5 }
  }
}

function render() {
  if (!chart) return
  chart.setOption({ ...BASE, ...props.option }, true)
}

onMounted(() => {
  chart = echarts.init(el.value)
  render()
  observer = new ResizeObserver(() => chart && chart.resize())
  observer.observe(el.value)
})
watch(() => props.option, render, { deep: true })
onBeforeUnmount(() => {
  observer && observer.disconnect()
  chart && chart.dispose()
  chart = null
})
defineExpose({ chart })
</script>

<style scoped>
.chart-box {
  width: 100%;
}
</style>
