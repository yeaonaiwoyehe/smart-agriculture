<template>
  <el-color-picker
    v-model="theme"
    :predefine="['#409EFF', '#1890ff', '#304156','#212121','#11a983', '#13c2c2', '#6959CD', '#f5222d', ]"
    class="theme-picker"
    popper-class="theme-picker-dropdown"
  />
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useSettingsStore } from '@/stores/settings'

const ORIGINAL_THEME = '#409EFF' // default color

const emit = defineEmits(['change'])
const settingsStore = useSettingsStore()

const theme = ref('')

const defaultTheme = computed(() => {
  return settingsStore.theme
})

const toHex = (num) => {
  const str = num.toString(16)
  return str.length === 1 ? '0' + str : str
}

// 根据主色生成 Element Plus 亮色/暗色变量
const getThemeCluster = (color) => {
  const red = parseInt(color.slice(0, 2), 16)
  const green = parseInt(color.slice(2, 4), 16)
  const blue = parseInt(color.slice(4, 6), 16)

  const mix = (tint) => {
    return `#${toHex(red + Math.round(tint * (255 - red)))}${toHex(green + Math.round(tint * (255 - green)))}${toHex(blue + Math.round(tint * (255 - blue)))}`
  }

  return {
    light3: mix(0.3),
    light5: mix(0.5),
    light7: mix(0.7),
    light8: mix(0.8),
    light9: mix(0.9),
    dark2: `#${toHex(Math.round(0.8 * red))}${toHex(Math.round(0.8 * green))}${toHex(Math.round(0.8 * blue))}`
  }
}

const setTheme = (val) => {
  if (typeof val !== 'string') return
  const cluster = getThemeCluster(val.replace('#', ''))
  const root = document.documentElement
  root.style.setProperty('--el-color-primary', val)
  root.style.setProperty('--el-color-primary-light-3', cluster.light3)
  root.style.setProperty('--el-color-primary-light-5', cluster.light5)
  root.style.setProperty('--el-color-primary-light-7', cluster.light7)
  root.style.setProperty('--el-color-primary-light-8', cluster.light8)
  root.style.setProperty('--el-color-primary-light-9', cluster.light9)
  root.style.setProperty('--el-color-primary-dark-2', cluster.dark2)

  emit('change', val)
}

watch(defaultTheme, (val) => {
  theme.value = val
}, { immediate: true })

watch(theme, (val) => {
  setTheme(val)
})

if (defaultTheme.value !== ORIGINAL_THEME) {
  setTheme(defaultTheme.value)
}
</script>

<style>
.theme-message,
.theme-picker-dropdown {
  z-index: 99999 !important;
}

.theme-picker .el-color-picker__trigger {
  height: 26px !important;
  width: 26px !important;
  padding: 2px;
}

.theme-picker-dropdown .el-color-dropdown__link-btn {
  display: none;
}
</style>
