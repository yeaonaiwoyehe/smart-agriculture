<template>
  <div ref="rightPanel" class="rightPanel-container">
    <div class="rightPanel-background"/>
    <div class="rightPanel">
      <div class="rightPanel-items">
        <slot/>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useSettingsStore } from '@/stores/settings'

defineOptions({ name: 'RightPanel' })

const props = defineProps({
  clickNotClose: {
    default: false,
    type: Boolean
  }
})

const settingsStore = useSettingsStore()
const rightPanel = ref(null)

const show = computed({
  get() {
    return settingsStore.showSettings
  },
  set(val) {
    settingsStore.changeSetting({
      key: 'showSettings',
      value: val
    })
  }
})

function addEventClick() {
  window.addEventListener('click', closeSidebar)
}

function closeSidebar(evt) {
  const parent = evt.target.closest('.el-drawer__body')
  if (!parent) {
    show.value = false
    window.removeEventListener('click', closeSidebar)
  }
}

watch(show, (value) => {
  if (value && !props.clickNotClose) {
    addEventClick()
  }
})

onMounted(() => {
  addEventClick()
})

onBeforeUnmount(() => {
  const elx = rightPanel.value
  elx && elx.remove()
})
</script>

<style lang="scss" scoped>
.rightPanel-background {
  position: fixed;
  top: 0;
  left: 0;
  opacity: 0;
  transition: opacity .3s cubic-bezier(.7, .3, .1, 1);
  background: rgba(0, 0, 0, .2);
  z-index: -1;
}

.rightPanel {
  width: 100%;
  max-width: 260px;
  height: 100vh;
  position: fixed;
  top: 0;
  right: 0;
  box-shadow: 0px 0px 15px 0px rgba(0, 0, 0, .05);
  transition: all .25s cubic-bezier(.7, .3, .1, 1);
  transform: translate(100%);
  background: #fff;
  z-index: 40000;
}

.handle-button {
  width: 48px;
  height: 48px;
  position: absolute;
  left: -48px;
  text-align: center;
  font-size: 24px;
  border-radius: 6px 0 0 6px !important;
  z-index: 0;
  pointer-events: auto;
  cursor: pointer;
  color: #fff;
  line-height: 48px;

  i {
    font-size: 24px;
    line-height: 48px;
  }
}
</style>
