<template>
  <div>
    <svg-icon icon-class="question" @click="goto"/>
  </div>
</template>

<script setup>
import { ref } from 'vue'

defineOptions({ name: 'RuoYiDoc' })

const url = ref('http://doc.ruoyi.vip/ruoyi-vue')

function goto() {
  window.open(url.value)
}
</script>
