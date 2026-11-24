<template>
  <el-form size="small">
    <el-form-item>
      <el-radio v-model='radioValue' :label="1">
        小时，允许的通配符[, - * /]
      </el-radio>
    </el-form-item>

    <el-form-item>
      <el-radio v-model='radioValue' :label="2">
        周期从
        <el-input-number v-model='cycle01' :min="0" :max="22"/>
        -
        <el-input-number v-model='cycle02' :min="cycle01 ? cycle01 + 1 : 1" :max="23"/>
        小时
      </el-radio>
    </el-form-item>

    <el-form-item>
      <el-radio v-model='radioValue' :label="3">
        从
        <el-input-number v-model='average01' :min="0" :max="22"/>
        小时开始，每
        <el-input-number v-model='average02' :min="1" :max="23 - average01 || 0"/>
        小时执行一次
      </el-radio>
    </el-form-item>

    <el-form-item>
      <el-radio v-model='radioValue' :label="4">
        指定
        <el-select clearable v-model="checkboxList" placeholder="可多选" multiple style="width:100%">
          <el-option v-for="item in 24" :key="item" :value="item-1">{{item-1}}</el-option>
        </el-select>
      </el-radio>
    </el-form-item>
  </el-form>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

defineOptions({ name: 'crontab-hour' })

const props = defineProps(['check', 'cron'])
const emit = defineEmits(['update'])

const radioValue = ref(1)
const cycle01 = ref(0)
const cycle02 = ref(1)
const average01 = ref(0)
const average02 = ref(1)
const checkboxList = ref([])
const checkNum = props.check

// 单选按钮值变化时
function radioChange() {
  if (props.cron.min === '*') {
    emit('update', 'min', '0', 'hour');
  }
  if (props.cron.second === '*') {
    emit('update', 'second', '0', 'hour');
  }
  switch (radioValue.value) {
    case 1:
      emit('update', 'hour', '*')
      break;
    case 2:
      emit('update', 'hour', cycleTotal.value);
      break;
    case 3:
      emit('update', 'hour', averageTotal.value);
      break;
    case 4:
      emit('update', 'hour', checkboxString.value);
      break;
  }
}

// 周期两个值变化时
function cycleChange() {
  if (radioValue.value == '2') {
    emit('update', 'hour', cycleTotal.value);
  }
}

// 平均两个值变化时
function averageChange() {
  if (radioValue.value == '3') {
    emit('update', 'hour', averageTotal.value);
  }
}

// checkbox值变化时
function checkboxChange() {
  if (radioValue.value == '4') {
    emit('update', 'hour', checkboxString.value);
  }
}

// 计算两个周期值
const cycleTotal = computed(() => {
  const cycle01Val = checkNum(cycle01.value, 0, 22)
  const cycle02Val = checkNum(cycle02.value, cycle01Val ? cycle01Val + 1 : 1, 23)
  return cycle01Val + '-' + cycle02Val;
})

// 计算平均用到的值
const averageTotal = computed(() => {
  const average01Val = checkNum(average01.value, 0, 22)
  const average02Val = checkNum(average02.value, 1, 23 - average01Val || 0)
  return average01Val + '/' + average02Val;
})

// 计算勾选的checkbox值合集
const checkboxString = computed(() => {
  let str = checkboxList.value.join();
  return str == '' ? '*' : str;
})

watch(radioValue, radioChange)
watch(cycleTotal, cycleChange)
watch(averageTotal, averageChange)
watch(checkboxString, checkboxChange)

defineExpose({
  radioValue,
  cycle01,
  cycle02,
  average01,
  average02,
  checkboxList
})
</script>
