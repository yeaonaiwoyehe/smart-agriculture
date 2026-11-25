<template>
  <header class="topbar">
    <button class="icon-btn" @click="store.toggleSidebar()">
      <el-icon :size="17"><component :is="collapsed ? Expand : Fold" /></el-icon>
    </button>

    <div class="crumb">
      <span>{{ route.meta.group || '工作台' }}</span>
      <span class="crumb-sep">/</span>
      <b>{{ route.meta.title || '数据总览' }}</b>
    </div>

    <div class="global-search" @click="openSearch">
      <el-icon :size="14"><Search /></el-icon>
      <span>搜索地块 / 农机 / 作业单</span>
      <span class="kbd">Ctrl + K</span>
    </div>

    <div class="topbar-right">
      <span class="scope-pill" :class="`role-${roleMeta.tone}`">
        <el-icon :size="12"><User /></el-icon>
        {{ roleMeta.scope }}
      </span>
      <button class="ai-entry" @click="store.openAssistant('MA-05')">
        <el-icon :size="15"><ChatDotRound /></el-icon>
        AI 助手
      </button>
      <div class="topbar-divider" />
      <el-popover placement="bottom-end" :width="330" trigger="click">
        <template #reference>
          <button class="icon-btn" style="position: relative">
            <el-icon :size="17"><Bell /></el-icon>
            <span style="position: absolute; top: 6px; right: 7px; width: 6px; height: 6px; border-radius: 50%; background: #e04b4b" />
          </button>
        </template>
        <div style="padding: 2px 2px 4px">
          <div style="display: flex; align-items: center; margin-bottom: 8px">
            <b style="font-size: 12.5px">运行告警</b>
            <el-tag size="small" type="danger" style="margin-left: auto">3 条待处理</el-tag>
          </div>
          <div v-for="a in alarms" :key="a.id" style="padding: 7px 0; border-top: 1px solid #f0f4f1">
            <div style="font-size: 11.5px; font-weight: 600">{{ a.title }}</div>
            <div style="font-size: 10.5px; color: #8b9992; margin-top: 2px">{{ a.detail }}</div>
          </div>
          <el-button text size="small" style="margin-top: 4px" @click="$router.push('/system')">查看全部审计告警</el-button>
        </div>
      </el-popover>
      <el-dropdown trigger="click" @command="onUserCommand">
        <div style="display: flex; align-items: center; gap: 8px; cursor: pointer; padding-left: 4px">
          <div class="role-avatar" style="width: 30px; height: 30px">{{ roleMeta.user.slice(0, 1) }}</div>
          <div style="line-height: 1.25">
            <div style="font-size: 12px; font-weight: 600">{{ roleMeta.user }}</div>
            <div style="font-size: 10px; color: #8b9992">{{ roleMeta.name }}</div>
          </div>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item disabled>当前数据范围：{{ roleMeta.scope }}</el-dropdown-item>
            <el-dropdown-item command="system" :disabled="!store.canAccess('system')">权限与审计</el-dropdown-item>
            <el-dropdown-item command="logout" divided>退出演示登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <el-dialog v-model="searchOpen" title="全局搜索" width="560px" :show-close="false" @opened="focusInput">
      <el-input ref="searchInput" v-model="keyword" size="large" placeholder="输入地块名称、地块编码、农机编号或作业单号" clearable>
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <div class="search-results">
        <div v-if="!keyword" class="hint" style="padding: 16px 2px; font-size: 12px">
          支持模糊搜索：如「河东村3号地」「YD-2026-004」「NJ-005」「ZY-20261123-002」
        </div>
        <div v-for="r in results" :key="r.type + r.id" class="search-row" @click="go(r)">
          <span class="tag" :class="r.type === '地块' ? 'green' : r.type === '农机' ? 'blue' : r.type === '任务' ? 'amber' : 'violet'" style="height: 20px">
            {{ r.type }}
          </span>
          <div style="min-width: 0">
            <div style="font-size: 12.5px; font-weight: 600">{{ r.name }}</div>
            <div style="font-size: 10.5px; color: #8b9992">{{ r.id }} · {{ r.meta }}</div>
          </div>
          <el-icon style="margin-left: auto; color: #8b9992"><ArrowRight /></el-icon>
        </div>
        <div v-if="keyword && !results.length" class="empty" style="padding: 22px">没有匹配的结果</div>
      </div>
    </el-dialog>
  </header>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { ElMessage } from 'element-plus'
import { Expand, Fold } from '@element-plus/icons-vue'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import { usePermissionStore } from '@/stores/permission'
import { quickSearch } from '@/services/api'
import { alarms } from '@/mock/platform'

const store = useAppStore()
const userStore = useUserStore()
const permissionStore = usePermissionStore()
const route = useRoute()
const router = useRouter()
const { sidebarCollapsed: collapsed, roleMeta } = storeToRefs(store)

const searchOpen = ref(store.searchOpen)
const keyword = ref('')
const searchInput = ref(null)
const results = computed(() => quickSearch(keyword.value))

watch(
  () => store.searchOpen,
  (v) => {
    searchOpen.value = v
    if (!v) keyword.value = ''
  }
)
watch(searchOpen, (v) => {
  store.searchOpen = v
})

function openSearch() {
  store.searchOpen = true
}
function focusInput() {
  searchInput.value?.focus()
}
function go(r) {
  router.push(r.route)
  searchOpen.value = false
}

async function onUserCommand(cmd) {
  if (cmd === 'system') router.push('/system')
  if (cmd === 'logout') {
    // userStore.logout 内部会去重并发调用，并在接口失败时仍然清理本地态。
    await userStore.logout()
    permissionStore.resetRoutes()
    store.resetAuth()
    ElMessage.success('已退出登录')
    router.replace('/login')
  }
}

function onKey(e) {
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
    e.preventDefault()
    store.searchOpen = true
  }
}
onMounted(() => window.addEventListener('keydown', onKey))
onBeforeUnmount(() => window.removeEventListener('keydown', onKey))
</script>

<style scoped>
.search-results {
  margin-top: 10px;
  max-height: 360px;
  overflow-y: auto;
}
.search-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 8px;
  border-radius: 8px;
  cursor: pointer;
}
.search-row:hover {
  background: var(--brand-50);
}
</style>
