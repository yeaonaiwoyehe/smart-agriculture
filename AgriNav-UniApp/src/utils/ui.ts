export function toast(title: string, icon: 'none' | 'success' | 'error' = 'none'): void {
  uni.showToast({ title, icon, duration: 2000 })
}

export function showLoading(title = '加载中'): void {
  uni.showLoading({ title, mask: true })
}

export function hideLoading(): void {
  uni.hideLoading()
}

export interface ConfirmOptions {
  title?: string
  content: string
  confirmText?: string
  cancelText?: string
  danger?: boolean
}

/** 写操作二次确认 */
export function confirmAction(opt: ConfirmOptions): Promise<boolean> {
  return new Promise((resolve) => {
    uni.showModal({
      title: opt.title || '请确认',
      content: opt.content,
      confirmText: opt.confirmText || '确认',
      cancelText: opt.cancelText || '取消',
      confirmColor: opt.danger ? '#C0392B' : '#1F7A4D',
      success: (res) => resolve(!!res.confirm),
      fail: () => resolve(false)
    })
  })
}

export function errorMessage(err: unknown, fallback = '操作失败，请稍后重试'): string {
  if (err instanceof Error && err.message) return err.message
  if (typeof err === 'string' && err) return err
  return fallback
}

export function navigate(url: string): void {
  uni.navigateTo({ url })
}

export function back(): void {
  uni.navigateBack({ delta: 1 })
}
