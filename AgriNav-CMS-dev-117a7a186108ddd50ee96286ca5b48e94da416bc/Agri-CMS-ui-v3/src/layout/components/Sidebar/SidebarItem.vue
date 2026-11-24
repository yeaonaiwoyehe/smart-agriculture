<template>
  <div v-if="!item.hidden">
    <template
      v-if="showSingleChild">
      <app-link v-if="onlyOneChild.meta" :to="resolvePath(onlyOneChild.path, onlyOneChild.query)">
        <el-menu-item :index="resolvePath(onlyOneChild.path)" :class="{'submenu-title-noDropdown':!isNest}">
          <item :icon="onlyOneChild.meta.icon||(item.meta&&item.meta.icon)"/>
          <template #title>
            <span
              :title="onlyOneChild.meta.title && onlyOneChild.meta.title.length > 5 ? onlyOneChild.meta.title : undefined">{{ onlyOneChild.meta.title }}</span>
          </template>
        </el-menu-item>
      </app-link>
    </template>

    <el-sub-menu v-else :index="resolvePath(item.path)" teleported>
      <template #title>
        <item v-if="item.meta" :icon="item.meta && item.meta.icon" :title="item.meta.title"/>
      </template>
      <sidebar-item
        v-for="(child, index) in item.children"
        :key="child.path + index"
        :is-nest="true"
        :item="child"
        :base-path="resolvePath(child.path)"
        class="nest-menu"
      />
    </el-sub-menu>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { isExternal } from '@/utils/validate'
import Item from './Item'
import AppLink from './Link'

defineOptions({ name: 'SidebarItem' })

const props = defineProps({
  // route object
  item: {
    type: Object,
    required: true
  },
  isNest: {
    type: Boolean,
    default: false
  },
  basePath: {
    type: String,
    default: ''
  }
})

const showingChildren = computed(() => (props.item.children || []).filter(child => !child.hidden))
const onlyOneChild = computed(() => showingChildren.value.length === 1
  ? showingChildren.value[0]
  : { ...props.item, path: '', noShowingChildren: true })
const showSingleChild = computed(() => showingChildren.value.length <= 1
  && (!onlyOneChild.value.children || onlyOneChild.value.noShowingChildren)
  && !props.item.alwaysShow)

/**
 * 等价于原 path-browserify 的 path.resolve（POSIX 语义）
 */
function pathResolve(...paths) {
  let resolved = ''
  for (let i = paths.length - 1; i >= 0; i--) {
    const p = paths[i]
    if (typeof p !== 'string' || p === '') continue
    resolved = p + '/' + resolved
    if (p.charAt(0) === '/') break
  }
  const isAbsolute = resolved.charAt(0) === '/'
  const parts = []
  resolved.split('/').forEach(part => {
    if (!part || part === '.') return
    if (part === '..') {
      if (parts.length) parts.pop()
    } else {
      parts.push(part)
    }
  })
  return '/' + parts.join('/')
}

function resolvePath(routePath, routeQuery) {
  if (isExternal(routePath)) {
    return routePath
  }
  if (isExternal(props.basePath)) {
    return props.basePath
  }
  if (routeQuery) {
    let query = JSON.parse(routeQuery)
    return { path: pathResolve(props.basePath, routePath), query: query }
  }
  return pathResolve(props.basePath, routePath)
}
</script>
