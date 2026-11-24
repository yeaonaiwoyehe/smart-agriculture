<template>
  <div v-if="!item.hidden">
    <template
      v-if="hasOneShowingChild(item.children,item) && (!onlyOneChild.children||onlyOneChild.noShowingChildren)&&!item.alwaysShow">
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
import { ref } from 'vue'
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

const onlyOneChild = ref(null)

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
  return (isAbsolute ? '/' : '') + parts.join('/')
}

function hasOneShowingChild(children = [], parent) {
  if (!children) {
    children = []
  }
  const showingChildren = children.filter(item => {
    if (item.hidden) {
      return false
    } else {
      // Temp set(will be used if only has one showing child)
      onlyOneChild.value = item
      return true
    }
  })

  // When there is only one child router, the child router is displayed by default
  if (showingChildren.length === 1) {
    return true
  }

  // Show parent if there are no child router to display
  if (showingChildren.length === 0) {
    onlyOneChild.value = { ...parent, path: '', noShowingChildren: true }
    return true
  }

  return false
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
