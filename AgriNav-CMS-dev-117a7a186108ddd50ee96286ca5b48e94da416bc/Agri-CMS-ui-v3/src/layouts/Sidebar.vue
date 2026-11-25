<template>
  <aside class="sidebar" :class="{ 'is-collapsed': collapsed }">
    <div class="brand">
      <div class="brand-mark">
        <el-icon :size="19"><Sunny /></el-icon>
      </div>
      <div class="brand-text">
        <div class="brand-name">田界智航</div>
        <div class="brand-sub">农机作业智能管理平台</div>
      </div>
    </div>

    <nav class="nav-scroll">
      <div v-for="group in groups" :key="group.label" class="nav-section">
        <!-- 分组标题：可点击展开/收起 -->
        <button
          type="button"
          class="nav-group-toggle"
          :class="{ 'is-open': isGroupOpen(group.label), 'is-collapsed': collapsed }"
          :title="collapsed ? group.label : ''"
          @click="toggleGroup(group.label)"
        >
          <span class="nav-group-label">{{ group.label }}</span>
          <el-icon class="group-caret" :size="12"><ArrowDown /></el-icon>
        </button>
        <!-- 折叠态；侧栏收起时强制展开，避免只显示为图标却无分组提示 -->
        <div v-show="isGroupOpen(group.label) || collapsed" class="nav-group-body">
          <router-link
            v-for="item in group.items"
            :key="item.key"
            :to="item.path"
            class="nav-item"
            :class="{ active: route.path === item.path }"
            :title="collapsed ? item.title : ''"
          >
            <el-icon class="nav-icon" :size="16"><component :is="icons[item.icon]" /></el-icon>
            <span class="nav-label">{{ item.title }}</span>
            <span v-if="badges[item.key]" class="nav-badge">{{ badges[item.key] }}</span>
          </router-link>
        </div>
      </div>
    </nav>

    <div class="sidebar-foot">
      <el-dropdown trigger="click" placement="top-start" @command="changeRole">
        <div class="role-card">
          <div class="role-avatar">{{ roleMeta.user.slice(0, 1) }}</div>
          <div class="role-info">
            <div class="role-name">{{ roleMeta.user }} · {{ roleMeta.short }}</div>
            <div class="role-scope">{{ roleMeta.scope }}</div>
          </div>
          <el-icon v-if="!collapsed" :size="12" style="margin-left: auto; opacity: 0.6"><ArrowUp /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item v-for="r in roleOptions" :key="r.value" :command="r.value">
              {{ r.name }} · {{ r.scope }}
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </aside>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { storeToRefs } from 'pinia'
import { ElMessage } from 'element-plus'
import * as Icons from '@element-plus/icons-vue'
import { useAppStore } from '@/stores/app'
import { menuGroups } from '@/router'

const store = useAppStore()
const route = useRoute()
const { sidebarCollapsed: collapsed, navBadges: badges, roleMeta } = storeToRefs(store)
const roleOptions = computed(() => store.roleOptions)
const icons = Icons

/* ---------------------------------------------------------------
 * 分组映射函数：平铺菜单 -> 分组菜单
 * 当前 menuGroups 本身已是分组结构；这里做兼容处理：
 *  - 若传入项已带 items（分组结构），原样返回
 *  - 若传入的是平铺数组，按 path / name / key 关键字归入四个大组
 * 这样无论后端将来返回平铺数组还是现有静态结构，菜单都不会报错。
 * --------------------------------------------------------------- */
const GROUP_RULES = [
  {
    label: '运营总览',
    order: 0,
    match: (i) => /dashboard|\/index|^home|总览|驾驶舱|首页/i.test(`${i.path} ${i.name} ${i.key} ${i.title}`)
  },
  {
    label: '业务管理',
    order: 1,
    match: (i) => /land|machine|dispatch|monitor|workorder|settlement|report|plot|farm|地块|农机|调度|监控|轨迹|核算|结算|报表|作业/i.test(`${i.path} ${i.name} ${i.key} ${i.title}`)
  },
  {
    label: 'AI 智能体',
    order: 2,
    match: (i) => /agent|workflow|knowledge|tool|llm|智能体|工作流|知识库|工具/i.test(`${i.path} ${i.name} ${i.key} ${i.title}`)
  },
  {
    label: '系统管理',
    order: 3,
    match: (i) => /system|permission|user|role|menu|dept|config|log|audit|系统|权限|审计|监控|工具/i.test(`${i.path} ${i.name} ${i.key} ${i.title}`)
  }
]

function normalizeGroups(source = []) {
  if (!Array.isArray(source) || !source.length) return []
  // 已是分组结构（带 items），直接使用
  if (source.some((g) => Array.isArray(g.items))) return source
  // 平铺数组：按规则分组
  const buckets = GROUP_RULES.map((r) => ({ label: r.label, order: r.order, items: [] }))
  const other = { label: '系统管理', order: 3, items: [] }
  source.forEach((item) => {
    const rule = GROUP_RULES.find((r) => r.match(item))
    const target = rule ? buckets.find((b) => b.label === rule.label) : other
    target.items.push(item)
  })
  return [...buckets, other]
    .filter((g) => g.items.length)
    .sort((a, b) => a.order - b.order)
}

// 依次：分组映射 -> 权限过滤 -> 去掉空组；不存在的菜单项自动被过滤，不会渲染
const groups = computed(() =>
  normalizeGroups(menuGroups)
    .map((g) => ({ ...g, items: (g.items || []).filter((i) => store.canAccess(i.key)) }))
    .filter((g) => g.items.length)
)

/* 折叠状态：默认全部折叠（只显示一级分组标题） */
const openGroups = ref({})
function isGroupOpen(label) {
  return !!openGroups.value[label]
}
function toggleGroup(label) {
  openGroups.value = { ...openGroups.value, [label]: !isGroupOpen(label) }
}

function changeRole(role) {
  if (role === store.role) return
  store.setRole(role)
  ElMessage.success(`已切换为「${store.roleMeta.name}」，数据范围：${store.roleMeta.scope}`)
}
</script>

<style scoped>
/* 新增：分组折叠面板（沿用深绿色侧边栏风格） */
.nav-section + .nav-section {
  margin-top: 2px;
}
.nav-group-toggle {
  display: flex;
  align-items: center;
  width: 100%;
  padding: 0;
  border: 0;
  background: transparent;
  color: inherit;
  cursor: pointer;
  font-family: inherit;
}
.nav-group-toggle :deep(.nav-group-label) {
  flex: 1;
  text-align: left;
  transition: color 0.16s;
}
.nav-group-toggle:hover :deep(.nav-group-label) {
  color: rgba(226, 245, 236, 0.72);
}
.nav-group-toggle :deep(.group-caret) {
  flex: none;
  margin-right: 10px;
  color: rgba(214, 238, 226, 0.4);
  transform: rotate(-90deg);
  transition: transform 0.2s ease, color 0.16s;
}
.nav-group-toggle.is-open :deep(.group-caret) {
  transform: rotate(0deg);
}
.nav-group-toggle:hover :deep(.group-caret) {
  color: rgba(226, 245, 236, 0.72);
}
/* 侧栏收起时隐藏箭头与分组标题的交互提示 */
.nav-group-toggle.is-collapsed {
  cursor: default;
}
.nav-group-toggle.is-collapsed :deep(.group-caret) {
  display: none;
}
.nav-group-body {
  overflow: hidden;
}
</style>
