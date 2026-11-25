import { defineStore } from 'pinia'
import type { WorkOrder } from '@/types'
import * as workApi from '@/api/work'
import { errorMessage } from '@/utils/ui'

interface WorkState {
  current: WorkOrder | null
  history: WorkOrder[]
  loading: boolean
  error: string
  stats: workApi.WorkStats
}

export const useWorkStore = defineStore('work', {
  state: (): WorkState => ({
    current: null,
    history: [],
    loading: false,
    error: '',
    stats: { todayArea: 0, todayOrders: 0, monthArea: 0 }
  }),
  getters: {
    isWorking: (state) => state.current?.status === 1,
    isSettling: (state) => state.current?.status === 2
  },
  actions: {
    async refresh() {
      this.loading = true
      this.error = ''
      try {
        if (!workApi.WORK_SESSION_SUPPORTED_IN_REAL) {
          this.current = await workApi.fetchCurrentWork()
        } else {
          this.current = await workApi.fetchCurrentWork()
        }
        this.stats = await workApi.fetchWorkStats()
      } catch (e) {
        this.error = errorMessage(e)
      } finally {
        this.loading = false
      }
    },
    async refreshHistory() {
      this.loading = true
      this.error = ''
      try {
        this.history = await workApi.fetchWorkHistory()
      } catch (e) {
        this.error = errorMessage(e)
        this.history = []
      } finally {
        this.loading = false
      }
    },
    async start(taskId: string) {
      const order = await workApi.startWork(taskId)
      this.current = order
      await this.refresh()
      return order
    },
    async finish(orderId: string) {
      const order = await workApi.finishWork(orderId)
      this.current = order
      return order
    },
    async settle(orderId: string) {
      const order = await workApi.confirmSettlement(orderId)
      this.current = order
      await this.refresh()
      return order
    },
    async sync(orderId: string) {
      const count = await workApi.syncPendingPoints(orderId)
      if (this.current) this.current.pendingPoints = 0
      return count
    }
  }
})
