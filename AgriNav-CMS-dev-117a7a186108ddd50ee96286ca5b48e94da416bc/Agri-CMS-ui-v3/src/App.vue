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
#app .theme-picker {
  display: none;
}
</style>
