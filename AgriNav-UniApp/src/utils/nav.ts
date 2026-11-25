export const ROOT_PAGES = [
  '/pages/home/index',
  '/pages/task/index',
  '/pages/settlement/index',
  '/pages/mine/index'
]

/** 根页面用 reLaunch（相当于切换 Tab），其余用 navigateTo */
export function go(url: string): void {
  const path = url.split('?')[0]
  if (ROOT_PAGES.includes(path)) {
    if (url.includes('?')) {
      // 根页面带参数时以参数形式重新进入
      uni.reLaunch({ url })
    } else {
      uni.reLaunch({ url })
    }
    return
  }
  uni.navigateTo({ url })
}

export function goBack(fallback = '/pages/home/index'): void {
  const pages = getCurrentPages()
  if (pages.length > 1) {
    uni.navigateBack({ delta: 1 })
  } else {
    uni.reLaunch({ url: fallback })
  }
}

export function goAgent(question?: string): void {
  if (question) {
    uni.setStorageSync('agent.pendingQuestion', question)
  }
  uni.navigateTo({ url: '/pages/agent/index' })
}
