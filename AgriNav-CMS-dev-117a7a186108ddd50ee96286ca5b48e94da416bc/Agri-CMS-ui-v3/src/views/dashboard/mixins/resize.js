import { debounce } from '@/utils'
import { onActivated, onBeforeUnmount, onDeactivated, onMounted } from 'vue'

/**
 * 图表自适应组合式函数（替代原 resize mixin）
 * @param {import('vue').Ref} chartRef 保存 echarts 实例的 ref
 */
export default function useResizeChart(chartRef) {
  let sidebarElm = null
  let resizeHandler = null

  const resize = () => {
    const chart = chartRef.value
    chart && chart.resize()
  }

  const sidebarResizeHandler = (e) => {
    if (e.propertyName === 'width') {
      resizeHandler()
    }
  }

  const initListener = () => {
    resizeHandler = debounce(() => {
      resize()
    }, 100)
    window.addEventListener('resize', resizeHandler)

    sidebarElm = document.getElementsByClassName('sidebar-container')[0]
    sidebarElm && sidebarElm.addEventListener('transitionend', sidebarResizeHandler)
  }

  const destroyListener = () => {
    window.removeEventListener('resize', resizeHandler)
    resizeHandler = null

    sidebarElm && sidebarElm.removeEventListener('transitionend', sidebarResizeHandler)
  }

  onMounted(() => {
    initListener()
  })

  onActivated(() => {
    if (!resizeHandler) {
      // avoid duplication init
      initListener()
    }

    // when keep-alive chart activated, auto resize
    resize()
  })

  onBeforeUnmount(() => {
    destroyListener()
  })

  onDeactivated(() => {
    destroyListener()
  })

  return { resize, initListener, destroyListener }
}
