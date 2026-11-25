<template>
  <div :class="classObj" class="app-wrapper" :style="{'--current-color': theme}">
    <div v-if="device==='mobile'&&sidebarState.opened" class="drawer-bg" @click="handleClickOutside"/>
    <sidebar v-if="!sidebarState.hide" class="sidebar-container"/>
    <div :class="{hasTagsView:needTagsView,sidebarHide:sidebarState.hide}" class="main-container">
      <div :class="{'fixed-header':fixedHeader}">
        <navbar/>
        <tags-view v-if="needTagsView"/>
      </div>
      <app-main/>
      <right-panel>
        <settings/>
      </right-panel>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import RightPanel from '@/components/RightPanel'
import { AppMain, Navbar, Settings, Sidebar, TagsView } from './components'
import { useResizeHandler } from './mixin/ResizeHandler'
import { useAppStore } from '@/stores/app'
import { useSettingsStore } from '@/stores/settings'

defineOptions({ name: 'Layout' })

const appStore = useAppStore()
const settingsStore = useSettingsStore()

const theme = computed(() => settingsStore.theme)
const sidebarState = computed(() => appStore.sidebar)
const device = computed(() => appStore.device)
const needTagsView = computed(() => settingsStore.tagsView)
const fixedHeader = computed(() => settingsStore.fixedHeader)

const classObj = computed(() => ({
  hideSidebar: !sidebarState.value.opened,
  openSidebar: sidebarState.value.opened,
  withoutAnimation: sidebarState.value.withoutAnimation,
  mobile: device.value === 'mobile'
}))

useResizeHandler()

function handleClickOutside() {
  appStore.closeSideBar(false)
}
</script>

<style lang="scss" scoped>
@import "@/assets/styles/mixin.scss";
@import "@/assets/styles/variables.scss";

.app-wrapper {
  @include clearfix;
  position: relative;
  height: 100%;
  width: 100%;

  &.mobile.openSidebar {
    position: fixed;
    top: 0;
  }
}

.drawer-bg {
  background: #000;
  opacity: 0.3;
  width: 100%;
  top: 0;
  height: 100%;
  position: absolute;
  z-index: 999;
}

.fixed-header {
  position: fixed;
  top: 0;
  right: 0;
  z-index: 9;
  width: calc(100% - #{$base-sidebar-width});
  transition: width 0.28s;
}

.hideSidebar .fixed-header {
  width: calc(100% - 54px);
}

.sidebarHide .fixed-header {
  width: 100%;
}

.mobile .fixed-header {
  width: 100%;
}
</style>
