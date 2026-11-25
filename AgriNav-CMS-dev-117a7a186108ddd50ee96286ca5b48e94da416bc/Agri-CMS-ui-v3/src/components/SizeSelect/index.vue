<template>
  <el-dropdown trigger="click" @command="handleSetSize">
    <div>
      <svg-icon class-name="size-icon" icon-class="size"/>
    </div>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item v-for="item of sizeOptions" :key="item.value" :disabled="size===item.value"
                          :command="item.value">
          {{ item.label }}
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup>
import { ref, computed, nextTick, getCurrentInstance } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { useTagsViewStore } from '@/stores/tagsView'

const { proxy } = getCurrentInstance()
const appStore = useAppStore()
const tagsViewStore = useTagsViewStore()
const route = useRoute()
const router = useRouter()

const sizeOptions = ref([
  {label: 'Default', value: 'default'},
  {label: 'Medium', value: 'medium'},
  {label: 'Small', value: 'small'},
  {label: 'Mini', value: 'mini'}
])

const size = computed(() => {
  return appStore.size
})

function handleSetSize(size) {
  appStore.setSize(size)
  refreshView()
  proxy.$modal.msgSuccess('Switch Size Success')
}

function refreshView() {
  // In order to make the cached page re-rendered
  tagsViewStore.delAllCachedViews(route)

  const {fullPath} = route

  nextTick(() => {
    router.replace({
      path: '/redirect' + fullPath
    })
  })
}
</script>
