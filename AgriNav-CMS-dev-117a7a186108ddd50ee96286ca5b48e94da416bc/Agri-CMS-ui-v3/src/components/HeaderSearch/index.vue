<template>
  <div :class="{'show':show}" class="header-search">
    <svg-icon class-name="search-icon" icon-class="search" @click.stop="click"/>
    <el-select
      ref="headerSearchSelect"
      v-model="search"
      :remote-method="querySearch"
      filterable
      default-first-option
      remote
      placeholder="Search"
      class="header-search-select"
      @change="change"
    >
      <el-option v-for="option in options" :key="option.item.path" :value="option.item"
                 :label="option.item.title.join(' > ')"/>
    </el-select>
  </div>
</template>

<script setup>
// fuse is a lightweight fuzzy-search module
// make search results more in line with expectations
import Fuse from 'fuse.js/dist/fuse.min.js'
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { usePermissionStore } from '@/stores/permission'

defineOptions({ name: 'HeaderSearch' })

const router = useRouter()
const permissionStore = usePermissionStore()

const search = ref('')
const options = ref([])
const searchPool = ref([])
const show = ref(false)
const fuse = ref(undefined)
const headerSearchSelect = ref(null)

const routes = computed(() => {
  return permissionStore.routes
})

// 浏览器端解析路由路径（替代 node path.resolve）
function resolvePath(...paths) {
  const resolved = []
  paths.filter(p => p !== undefined && p !== '').join('/').split('/').forEach(seg => {
    if (!seg || seg === '.') return
    if (seg === '..') {
      resolved.pop()
    } else {
      resolved.push(seg)
    }
  })
  return '/' + resolved.join('/')
}

watch(routes, () => {
  searchPool.value = generateRoutes(routes.value)
})

watch(searchPool, (list) => {
  initFuse(list)
})

watch(show, (value) => {
  if (value) {
    document.body.addEventListener('click', close)
  } else {
    document.body.removeEventListener('click', close)
  }
})

onMounted(() => {
  searchPool.value = generateRoutes(routes.value)
})

function click() {
  show.value = !show.value
  if (show.value) {
    headerSearchSelect.value && headerSearchSelect.value.focus()
  }
}

function close() {
  headerSearchSelect.value && headerSearchSelect.value.blur()
  options.value = []
  show.value = false
}

function change(val) {
  const path = val.path;
  const query = val.query;
  if (ishttp(val.path)) {
    // http(s):// 路径新窗口打开
    const pindex = path.indexOf("http");
    window.open(path.substr(pindex, path.length), "_blank");
  } else {
    if (query) {
      router.push({path: path, query: JSON.parse(query)});
    } else {
      router.push(path)
    }
  }
  search.value = ''
  options.value = []
  nextTick(() => {
    show.value = false
  })
}

function initFuse(list) {
  fuse.value = new Fuse(list, {
    shouldSort: true,
    threshold: 0.4,
    location: 0,
    distance: 100,
    minMatchCharLength: 1,
    keys: [{
      name: 'title',
      weight: 0.7
    }, {
      name: 'path',
      weight: 0.3
    }]
  })
}

// Filter out the routes that can be displayed in the sidebar
// And generate the internationalized title
function generateRoutes(routes, basePath = '/', prefixTitle = []) {
  let res = []

  for (const router of routes) {
    // skip hidden router
    if (router.hidden) {
      continue
    }

    const data = {
      path: !ishttp(router.path) ? resolvePath(basePath, router.path) : router.path,
      title: [...prefixTitle]
    }

    if (router.meta && router.meta.title) {
      data.title = [...data.title, router.meta.title]

      if (router.redirect !== 'noRedirect') {
        // only push the routes with title
        // special case: need to exclude parent router without redirect
        res.push(data)
      }
    }

    if (router.query) {
      data.query = router.query
    }

    // recursive child routes
    if (router.children) {
      const tempRoutes = generateRoutes(router.children, data.path, data.title)
      if (tempRoutes.length >= 1) {
        res = [...res, ...tempRoutes]
      }
    }
  }
  return res
}

function querySearch(query) {
  if (query !== '') {
    options.value = fuse.value.search(query)
  } else {
    options.value = []
  }
}

function ishttp(url) {
  return url.indexOf('http://') !== -1 || url.indexOf('https://') !== -1
}
</script>

<style lang="scss" scoped>
.header-search {
  font-size: 0 !important;

  .search-icon {
    cursor: pointer;
    font-size: 18px;
    vertical-align: middle;
  }

  .header-search-select {
    font-size: 18px;
    transition: width 0.2s;
    width: 0;
    overflow: hidden;
    background: transparent;
    border-radius: 0;
    display: inline-block;
    vertical-align: middle;

    ::v-deep .el-input__inner {
      border-radius: 0;
      border: 0;
      padding-left: 0;
      padding-right: 0;
      box-shadow: none !important;
      border-bottom: 1px solid #d9d9d9;
      vertical-align: middle;
    }
  }

  &.show {
    .header-search-select {
      width: 210px;
      margin-left: 10px;
    }
  }
}
</style>
