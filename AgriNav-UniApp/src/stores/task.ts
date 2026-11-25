import { defineStore } from 'pinia'
import type { Task } from '@/types'
import * as taskApi from '@/api/task'
import type { TaskFilter } from '@/api/task'
import { errorMessage } from '@/utils/ui'

interface TaskState {
  list: Task[]
  filter: TaskFilter
  loading: boolean
  error: string
  counts: taskApi.TaskCounts
  loadedOnce: boolean
}

export const useTaskStore = defineStore('task', {
  state: (): TaskState => ({
    list: [],
    filter: 'all',
    loading: false,
    error: '',
    counts: { pending: 0, claimed: 0, working: 0, done: 0, timeout: 0 },
    loadedOnce: false
  }),
  getters: {
    claimedTasks: (state) => state.list.filter((t) => t.status === 'claimed'),
    activeTask: (state) =>
      state.list.find((t) => t.status === 'working') ||
      state.list.find((t) => t.status === 'claimed')
  },
  actions: {
    async fetchList(filter?: TaskFilter) {
      if (filter) this.filter = filter
      this.loading = true
      this.error = ''
      try {
        this.list = await taskApi.fetchTaskList(this.filter)
        this.loadedOnce = true
      } catch (e) {
        this.error = errorMessage(e)
        this.list = []
      } finally {
        this.loading = false
      }
    },
    async fetchCounts() {
      try {
        this.counts = await taskApi.fetchTaskCounts()
      } catch (e) {
        // 真实模式暂不支持，保持默认
      }
    },
    async claim(id: string) {
      return taskApi.claimTask(id)
    },
    async detail(id: string) {
      return taskApi.fetchTaskDetail(id)
    }
  }
})
