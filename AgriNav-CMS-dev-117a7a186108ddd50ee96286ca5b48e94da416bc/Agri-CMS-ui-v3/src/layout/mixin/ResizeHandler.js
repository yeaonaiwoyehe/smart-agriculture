import { onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/stores/app'

const { body } = document
const WIDTH = 992 // refer to Bootstrap's responsive design

/**
 * 响应式布局处理（原 Vue2 mixin 迁移为组合式函数）
 */
export function useResizeHandler() {
  const appStore = useAppStore()
  const route = useRoute()

  const isMobile = () => {
    const rect = body.getBoundingClientRect()
    return rect.width - 1 < WIDTH
  }

  const resizeHandler = () => {
    if (!document.hidden) {
      const mobile = isMobile()
      appStore.toggleDevice(mobile ? 'mobile' : 'desktop')
      if (mobile) {
        appStore.closeSideBar(true)
      }
    }
  }

  watch(() => route.fullPath, () => {
    if (appStore.device === 'mobile' && appStore.sidebar.opened) {
      appStore.closeSideBar(false)
    }
  })

  onMounted(() => {
    window.addEventListener('resize', resizeHandler)
    if (isMobile()) {
      appStore.toggleDevice('mobile')
      appStore.closeSideBar(true)
    }
  })

  onBeforeUnmount(() => {
    window.removeEventListener('resize', resizeHandler)
  })
}
