import { defineStore } from 'pinia'
import { users } from '@/mock/platform'
import { useUserStore } from '@/stores/user'
import { usePermissionStore } from '@/stores/permission'

const ROLE_META = {
  admin: { name: '公司管理员', short: '管理员', scope: '全公司数据', tone: 'admin', user: '王建民' },
  leader: { name: '片区生产队长', short: '生产队长', scope: '河东片区', tone: 'leader', user: '王强' },
  finance: { name: '财务人员', short: '财务', scope: '结算与单价', tone: 'finance', user: '刘敏' },
  operator: { name: '农机手', short: '农机手', scope: '仅本人数据', tone: 'operator', user: '陈立新' }
}

// 仅用于旧版角色切换器的展示映射；菜单权限不再依赖这张静态表。
const MENU_ROLES = {
  dashboard: ['admin', 'leader', 'finance', 'operator'],
  lands: ['admin', 'leader'],
  machines: ['admin', 'leader'],
  dispatch: ['admin', 'leader'],
  monitor: ['admin', 'leader'],
  workorders: ['admin', 'leader'],
  settlement: ['admin', 'finance'],
  reports: ['admin', 'leader', 'finance'],
  workbench: ['admin', 'leader', 'finance'],
  agentManage: ['admin'],
  agentTools: ['admin'],
  agentWorkflow: ['admin'],
  agentKnowledge: ['admin', 'leader', 'finance'],
  agentMonitor: ['admin', 'leader'],
  system: ['admin']
}

function mapBackendRole(roles) {
  if (roles.includes('admin')) return 'admin'
  if (roles.some((role) => ['leader', '生产队长'].includes(role))) return 'leader'
  if (roles.some((role) => ['finance', '财务'].includes(role))) return 'finance'
  return 'operator'
}

export const useAppStore = defineStore('app', {
  state: () => ({
    loggedIn: false,
    role: 'operator',
    userInfo: null,
    departmentNames: [],
    backendReady: false,
    sidebarCollapsed: false,
    searchOpen: false,
    assistantOpen: false,
    assistantAgent: 'MA-05',
    navBadges: { workorders: 3, dispatch: 4, system: 1 }
  }),
  getters: {
    roleMeta: (s) => {
      const meta = ROLE_META[s.role] || ROLE_META.operator
      return {
        ...meta,
        user: s.userInfo?.userName || s.userInfo?.nickName || meta.user,
        scope: s.departmentNames?.length ? s.departmentNames.join('、') : meta.scope
      }
    },
    roleOptions: () => Object.entries(ROLE_META).map(([value, v]) => ({ value, ...v })),
    currentUser: (s) => s.userInfo || users.find((u) => u.role === s.role) || users[0],
    districtScope: (s) => (s.role === 'leader' ? '河东片区' : '全公司')
  },
  actions: {
    applyUserInfo(user, roles, departments) {
      this.userInfo = user || null
      this.departmentNames = Array.isArray(departments) ? departments : []
      this.role = mapBackendRole(Array.isArray(roles) ? roles : [])
      this.backendReady = true
    },
    markAuthenticated() {
      this.loggedIn = true
    },
    resetAuth() {
      this.loggedIn = false
      this.role = 'operator'
      this.userInfo = null
      this.departmentNames = []
      this.backendReady = false
    },
    login() {
      this.loggedIn = true
    },
    logout() {
      this.resetAuth()
    },
    setRole(role) {
      // 角色切换器仅影响展示；真实权限始终来自后端。
      if (ROLE_META[role]) this.role = role
    },
    canAccess(key) {
      if (key === 'dashboard') return true

      const permissionStore = usePermissionStore()
      const userStore = useUserStore()
      if (!permissionStore.loaded) return false

      const isAdmin = userStore.roles.includes('admin') || userStore.permissions.includes('*:*:*')
      if (isAdmin) return true

      return permissionStore.allowedMenuKeys.includes(key)
    },
    toggleSidebar() {
      this.sidebarCollapsed = !this.sidebarCollapsed
    },
    openAssistant(agent = 'MA-05') {
      this.assistantAgent = agent
      this.assistantOpen = true
    }
  }
})
