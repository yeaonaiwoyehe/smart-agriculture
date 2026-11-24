<template>
  <div ref="chartEl" :class="className" :style="{height:height,width:width}"/>
</template>

<script setup>
import * as echarts from 'echarts'
import 'echarts/theme/macarons.js' // echarts theme
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import useResizeChart from './mixins/resize'

defineOptions({ name: 'PieChart' })

defineProps({
  className: {
    type: String,
    default: 'chart'
  },
  width: {
    type: String,
    default: '100%'
  },
  height: {
    type: String,
    default: '300px'
  }
})

const chartEl = ref(null)
const chart = ref(null)
useResizeChart(chart)

onMounted(() => {
  nextTick(() => {
    initChart()
  })
})

onBeforeUnmount(() => {
  if (!chart.value) {
    return
  }
  chart.value.dispose()
  chart.value = null
})

function initChart() {
  chart.value = echarts.init(chartEl.value, 'macarons')

  chart.value.setOption({
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b} : {c} ({d}%)'
    },
    legend: {
      left: 'center',
      bottom: '10',
      data: ['Industries', 'Technology', 'Forex', 'Gold', 'Forecasts']
    },
    series: [
      {
        name: 'WEEKLY WRITE ARTICLES',
        type: 'pie',
        roseType: 'radius',
        radius: [15, 95],
        center: ['50%', '38%'],
        data: [
          {value: 320, name: 'Industries'},
          {value: 240, name: 'Technology'},
          {value: 149, name: 'Forex'},
          {value: 100, name: 'Gold'},
          {value: 59, name: 'Forecasts'}
        ],
        animationEasing: 'cubicInOut',
        animationDuration: 2600
      }
    ]
  })
}
</script>
