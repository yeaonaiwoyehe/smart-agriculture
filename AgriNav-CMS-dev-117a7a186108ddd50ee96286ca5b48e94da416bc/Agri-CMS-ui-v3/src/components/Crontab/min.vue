<template>
  <el-form size="small">
    <el-form-item>
      <el-radio v-model='radioValue' :label="1">
        分钟，允许的通配符[, - * /]
      </el-radio>
    </el-form-item>

    <el-form-item>
      <el-radio v-model='radioValue' :label="2">
        周期从
        <el-input-number v-model='cycle01' :min="0" :max="58"/>
        -
        <el-input-number v-model='cycle02' :min="cycle01 ? cycle01 + 1 : 1" :max="59"/>
        分钟
      </el-radio>
    </el-form-item>

    <el-form-item>
      <el-radio v-model='radioValue' :label="3">
        从
        <el-input-number v-model='average01' :min="0" :max="58"/>
        分钟开始，每
        <el-input-number v-model='average02' :min="1" :max="59 - average01 || 0"/>
        分钟执行一次
      </el-radio>
    </el-form-item>

    <el-form-item>
      <el-radio v-model='radioValue' :label="4">
        指定
        <el-select clearable v-model="checkboxList" placeholder="可多选" multiple style="width:100%">
          <el-option v-for="item in 60" :key="item" :value="item-1">{{item-1}}</el-option>
        </el-select>
      </el-radio>
    </el-form-item>
  </el-form>

</template>

<script setup>
import { ref, computed, watch } from 'vue'

defineOptions({ name: 'crontab-min' })

const props = defineProps(['check', 'cron'])
const emit = defineEmits(['update'])

const radioValue = ref(1)
const cycle01 = ref(1)
const cycle02 = ref(2)
const average01 = ref(0)
const average02 = ref(1)
const checkboxList = ref([])
const checkNum = props.check

// 单选按钮值变化时
function radioChange() {
  switch (radioValue.value) {
    case 1:
      emit('update', 'min', '*', 'min');
      break;
    case 2:
      emit('update', 'min', cycleTotal.value, 'min');
      break;
    case 3:
      emit('update', 'min', averageTotal.value, 'min');
      break;
    case 4:
      emit('update', 'min', checkboxString.value, 'min');
      break;
  }
}

// 周期两个值变化时
function cycleChange() {
  if (radioValue.value == '2') {
    emit('update', 'min', cycleTotal.value, 'min');
  }
}

// 平均两个值变化时
function averageChange() {
  if (radioValue.value == '3') {
    emit('update', 'min', averageTotal.value, 'min');
  }
}

// checkbox值变化时
function checkboxChange() {
  if (radioValue.value == '4') {
    emit('update', 'min', checkboxString.value, 'min');
  }
}

// 计算两个周期值
const cycleTotal = computed(() => {
  const cycle01Val = checkNum(cycle01.value, 0, 58)
  const cycle02Val = checkNum(cycle02.value, cycle01Val ? cycle01Val + 1 : 1, 59)
  return cycle01Val + '-' + cycle02Val;
})

// 计算平均用到的值
const averageTotal = computed(() => {
  const average01Val = checkNum(average01.value, 0, 58)
  const average02Val = checkNum(average02.value, 1, 59 - average01Val || 0)
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
