<template>
  <div class="app-shell">
    <Sidebar />
    <div class="main">
      <Topbar />
      <div ref="scrollRef" class="page-scroll">
        <router-view />
      </div>
    </div>
    <GlobalAssistant />
  </div>
</template>

<script setup>
import { nextTick, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import Sidebar from './Sidebar.vue'
import Topbar from './Topbar.vue'
import GlobalAssistant from './GlobalAssistant.vue'

const store = useAppStore()
const route = useRoute()
const router = useRouter()
const scrollRef = ref(null)

watch(
  () => route.fullPath,
  () => nextTick(() => scrollRef.value && scrollRef.value.scrollTo({ top: 0 }))
)

watch(
  () => store.role,
  () => {
    const key = route.meta?.menuKey
    if (key && !store.canAccess(key)) router.push('/dashboard')
  }
)
</script>
