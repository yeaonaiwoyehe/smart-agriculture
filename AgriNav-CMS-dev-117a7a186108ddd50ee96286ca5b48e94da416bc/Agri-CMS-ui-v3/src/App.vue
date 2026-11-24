<template>
  <div id="app">
    <router-view />
    <theme-picker />
  </div>
</template>

<script setup>
import ThemePicker from '@/components/ThemePicker/index.vue'
</script>

<style scoped>
/* 用 :deep 穿透：ThemePicker 最终 DOM 上没有本组件的 scoped 属性，普通选择器匹配不到会导致隐藏失效 */
#app :deep(.theme-picker) {
  display: none;
}
</style>
