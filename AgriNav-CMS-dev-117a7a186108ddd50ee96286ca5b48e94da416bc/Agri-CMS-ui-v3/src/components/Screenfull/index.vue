<template>
  <div>
    <svg-icon :icon-class="isFullscreen?'exit-fullscreen':'fullscreen'" @click="click"/>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, getCurrentInstance } from 'vue'
import screenfull from 'screenfull'

defineOptions({ name: 'Screenfull' })

const { proxy } = getCurrentInstance()
const isFullscreen = ref(false)

function click() {
  if (!screenfull.isEnabled) {
    proxy.$modal.msgWarning('你的浏览器不支持全屏')
    return false
  }
  screenfull.toggle()
}

function change() {
  isFullscreen.value = screenfull.isFullscreen
}

function init() {
  if (screenfull.isEnabled) {
    screenfull.on('change', change)
  }
}

function destroy() {
  if (screenfull.isEnabled) {
    screenfull.off('change', change)
  }
}

onMounted(init)
onBeforeUnmount(destroy)
</script>

<style scoped>
.screenfull-svg {
  display: inline-block;
  cursor: pointer;
  fill: #5a5e66;;
  width: 20px;
  height: 20px;
  vertical-align: 10px;
}
</style>
