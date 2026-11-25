<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    name: string
    size?: number
    color?: string
    sw?: number
  }>(),
  { size: 42, color: '#10231b', sw: 1.8 }
)

const ICONS: Record<string, string> = {
  back: '<path d="M15 18l-6-6 6-6"/>',
  home: '<path d="M4 11 12 4l8 7v8.5a1.5 1.5 0 0 1-1.5 1.5H14v-6h-4v6H5.5A1.5 1.5 0 0 1 4 19.5V11Z"/>',
  mic: '<path d="M12 15a4 4 0 0 0 4-4V6a4 4 0 1 0-8 0v5a4 4 0 0 0 4 4Z"/><path d="M5 11a7 7 0 0 0 14 0"/><path d="M12 18v4"/>',
  send: '<path d="M4 12l16-8-6 16-2.5-6.5L4 12Z"/>',
  play: '<path d="M8 5.5v13l11-6.5-11-6.5Z" fill="currentColor" stroke="none"/>',
  stop: '<rect x="6.5" y="6.5" width="11" height="11" rx="2.5" fill="currentColor" stroke="none"/>',
  pause: '<path d="M9 5v14M15 5v14"/>',
  map: '<path d="M9 4 3.5 6v14L9 18l6 2 5.5-2V4L15 6 9 4Z"/><path d="M9 4v14M15 6v14"/>',
  pin: '<path d="M12 21s7-6.2 7-11a7 7 0 1 0-14 0c0 4.8 7 11 7 11Z"/><circle cx="12" cy="10" r="2.5"/>',
  list: '<path d="M8 6h13M8 12h13M8 18h13M3.5 6h.01M3.5 12h.01M3.5 18h.01"/>',
  wallet: '<path d="M3 7.5A2.5 2.5 0 0 1 5.5 5H19a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H5.5A2.5 2.5 0 0 1 3 16.5v-9Z"/><path d="M3 8h18"/><circle cx="17" cy="13" r="1.2"/>',
  alert: '<path d="M12 3 2.5 20h19L12 3Z"/><path d="M12 9v5M12 17h.01"/>',
  help: '<circle cx="12" cy="12" r="9"/><path d="M9.5 9a2.5 2.5 0 1 1 3.8 2.2c-.8.5-1.3 1-1.3 1.8M12 17h.01"/>',
  user: '<circle cx="12" cy="8" r="4"/><path d="M4 21c0-4 3.6-6 8-6s8 2 8 6"/>',
  refresh: '<path d="M20 11a8 8 0 1 0-2.3 6.3"/><path d="M20 4v7h-7"/>',
  message: '<path d="M21 12a8 8 0 0 1-8 8H5l-2 1 1-4a8 8 0 1 1 17-5Z"/>',
  speaker: '<path d="M11 5 6.5 9H3v6h3.5L11 19V5Z"/><path d="M15 9a4 4 0 0 1 0 6M17.5 6.5a8 8 0 0 1 0 11"/>',
  'speaker-off': '<path d="M11 5 6.5 9H3v6h3.5L11 19V5Z"/><path d="M16 9.5l4 4M20 9.5l-4 4"/>',
  close: '<path d="M6 6l12 12M18 6 6 18"/>',
  check: '<path d="M4.5 12.5 9 17l10.5-10.5"/>',
  clock: '<circle cx="12" cy="12" r="9"/><path d="M12 7v5.5l4 2"/>',
  chevron: '<path d="M9 6l6 6-6 6"/>',
  camera: '<path d="M4 8h3l1.5-2h7L17 8h3v12H4V8Z"/><circle cx="12" cy="14" r="3.5"/>',
  phone: '<path d="M6.5 3h3l1.5 4.5-2 1.5a12 12 0 0 0 6 6l1.5-2L21 14.5v3A2.5 2.5 0 0 1 18.5 20C10.5 20 4 13.5 4 5.5A2.5 2.5 0 0 1 6.5 3Z"/>',
  truck: '<path d="M3 6h10v10H3z"/><path d="M13 10h4l4 4v2h-8"/><circle cx="7" cy="18" r="2"/><circle cx="17.5" cy="18" r="2"/>',
  'cloud-off': '<path d="M4 4l16 16"/><path d="M7.5 17.5A4.5 4.5 0 0 1 7 8.6M9 6.2A5 5 0 0 1 18 10a3.5 3.5 0 0 1 1.7 6.1M6.2 8.3"/>',
  history: '<path d="M3.5 12a8.5 8.5 0 1 0 2.6-6.1"/><path d="M3.5 4.5V10H9"/><path d="M12 8v4.5l3 2"/>',
  plus: '<path d="M12 5v14M5 12h14"/>',
  navigation: '<path d="M21 3 3 10.5l7 2.5 2.5 7L21 3Z"/>',
  satellite: '<path d="M5 14 10 19"/><path d="m8.5 9.5 6 6"/><path d="M13 4l7 7-2.5 2.5-7-7L13 4Z"/><path d="M4.5 14.5 3 16l5 5 1.5-1.5"/>',
  shield: '<path d="M12 3 4.5 6v6c0 4.5 3.2 7.6 7.5 9 4.3-1.4 7.5-4.5 7.5-9V6L12 3Z"/><path d="M8.8 12l2.2 2.2 4.2-4.4"/>',
  logout: '<path d="M14 4h4a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-4"/><path d="M10 8l-4 4 4 4M6 12h9"/>',
  trash: '<path d="M4 7h16M9 7V4h6v3M6 7l1 13h10l1-13M10 11v5M14 11v5"/>',
  wifi: '<path d="M3.5 9a13 13 0 0 1 17 0M6.5 12.5a8.5 8.5 0 0 1 11 0M9.5 16a4 4 0 0 1 5 0"/><circle cx="12" cy="19.5" r="1"/>',
  spark: '<path d="M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8L12 3Z"/><path d="M18.5 16l.8 2.2 2.2.8-2.2.8-.8 2.2-.8-2.2-2.2-.8 2.2-.8.8-2.2Z"/>',
  doc: '<path d="M6 3h8l4 4v14H6V3Z"/><path d="M14 3v4h4M9 12h6M9 16h6"/>',
  ruler: '<path d="M3 9 9 3l12 12-6 6L3 9Z"/><path d="M8 8l2 2M11 5l2 2M5 11l2 2M14 14l2 2M17 11l2 2"/>',
  sync: '<path d="M20 7v5h-5"/><path d="M4 17v-5h5"/><path d="M6.5 8A7 7 0 0 1 19 10.5M17.5 16A7 7 0 0 1 5 13.5"/>',
  edit: '<path d="M4 20h4L19 9l-4-4L4 16v4Z"/><path d="M14.5 5.5l4 4"/>',
  upload: '<path d="M12 16V4M7 9l5-5 5 5"/><path d="M4 16v4h16v-4"/>',
  leaf: '<path d="M4 20c0-9 6-15 16-16 0 10-5 16-14 16"/><path d="M4 20c2-5 5-8 10-10"/>',
  'arrow-right': '<path d="M4 12h16M14 6l6 6-6 6"/>',
  expand: '<path d="M9 4H4v5M15 4h5v5M9 20H4v-5M15 20h5v-5"/>',
  lock: '<rect x="4.5" y="10" width="15" height="10" rx="2.5"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/>',
  info: '<circle cx="12" cy="12" r="9"/><path d="M12 11v5M12 8h.01"/>'
}

const svg = computed(() => {
  const path = ICONS[props.name] || ICONS.info
  const filled = /fill="currentColor"/.test(path)
  const attrs = filled
    ? ''
    : `stroke="${props.color}" stroke-width="${props.sw}" stroke-linecap="round" stroke-linejoin="round"`
  const body = path.replace(/currentColor/g, props.color)
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${props.size}" height="${props.size}" viewBox="0 0 24 24" fill="none" ${attrs}>${body}</svg>`
})

const src = computed(() => `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg.value)}`)
</script>

<template>
  <image
    class="sf-icon"
    :src="src"
    :style="{ width: size + 'rpx', height: size + 'rpx' }"
    mode="aspectFit"
  />
</template>

<style scoped>
.sf-icon {
  display: block;
  flex: none;
}
</style>
