<template>
  <span class="tag" :class="tone">{{ text }}</span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  status: { type: String, default: '' },
  label: { type: String, default: '' }
})

const TEXT = {
  online: '在线',
  offline: '离线'
}

const GREEN = ['作业中', '运行中', '启用', '成功', '已付款', '已完成', '已复核', '已结算', '在线', '在职', '已确认', '已结束', '通过']
const AMBER = ['待作业', '待执行', '待接单', '待派单', '待核算', '待复核', '待核对', '待生成', '待付款', '处理中', '灰度中', '未处理', '已拦截']
const RED = ['异常', '超时', '失败', '离线', '维修', '停用', '停运', '驳回']
const BLUE = ['待机', '已处理', '待上传']

const tone = computed(() => {
  const s = props.status
  if (GREEN.includes(s)) return 'green'
  if (RED.includes(s)) return 'red'
  if (AMBER.includes(s)) return 'amber'
  if (BLUE.includes(s)) return 'blue'
  return 'gray'
})
const text = computed(() => props.label || TEXT[props.status] || props.status || '-')
</script>
