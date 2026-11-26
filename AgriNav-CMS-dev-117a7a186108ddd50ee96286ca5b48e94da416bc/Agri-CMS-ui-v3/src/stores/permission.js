import { defineStore } from 'pinia'
import router, { businessRoutes, notFoundRoute } from '@/router'
import { getRouters } from '@/api/menu'
import { useUserStore } from '@/stores/user'

// 后端 sys_menu 里仍保留老若依菜单（system/xxx、monitor/xxx、tool/xxx），
// 与新版页面不是一一对应。这里只把“可明确识别”的菜单映射为新版 menuKey，
// 不做猜测式全量替换；管理员则保留全部新版页面入口。
const SYSTEM_MENU_PATTERN = /system\/|monitor\/|tool\/|系统管理|系统监控|系统工具|用户管理|角色管理|菜单管理|部门管理|岗位管理|字典管理|参数设置|通知公告|日志管理|在线用户|定时任务|服务监控|数据监控/

const ROUTE_ALIASES = {
  lands: ['/lands', 'landplot', '地块'],
  machines: ['/machines', 'agriculturalmachinery', '农机'],
  // 农田作业管理（农户发布作业需求）：只匹配农田作业自身语义，
  // 刻意不含「作业」这种宽泛词，避免与作业调度/作业单/轨迹核算混淆。
  farmwork: ['/farmwork', 'farmwork', 'farm-work', '农田作业'],
  dispatch: ['/dispatch', '作业调度', '派单'],
  monitor: ['/monitor', 'realtime', '实时监控'],
  workorders: ['/workorders', 'workorder', 'effectivetrajectory', '作业单', '轨迹'],
  settlement: ['/settlement', '结算'],
  reports: ['/reports', 'report', '报表', '统计'],
  workbench: ['/agent/workbench', 'agent-workbench', '工作台'],
  agentManage: ['/agent/manage', 'agent-manage', 'agent管理'],
  agentTools: ['/agent/tools', '工具管理'],
  agentWorkflow: ['/agent/workflow', '工作流'],
  agentKnowledge: ['/agent/knowledge', '知识库'],
  agentMonitor: ['/agent/monitor', 'agent监控']
}

function collectAllowedMenuKeys(routers) {
  const keys = new Set()
  const walk = (list) => {
    ;(list || []).forEach((item) => {
      const haystack = [item.path, item.name, item.component, item.meta?.title, item.meta?.path]
        .filter(Boolean)
        .join(' ')
        .toLowerCase()

      // 老若依系统类菜单统一归到新版“权限与审计”，避免误开实时监控等业务页。
      if (SYSTEM_MENU_PATTERN.test(haystack)) keys.add('system')
      else {
        Object.entries(ROUTE_ALIASES).forEach(([key, aliases]) => {
          if (aliases.some((alias) => haystack.includes(alias.toLowerCase()))) keys.add(key)
        })
      }

      if (Array.isArray(item.children)) walk(item.children)
    })
  }
  walk(routers)
  return Array.from(keys)
}

// 管理员判定集中在此处，避免多处重复判断。
// 业务规则保持不变：admin 角色或通配权限仍使用全部 businessRoutes。
export function isAdminUser() {
  const userStore = useUserStore()
  return userStore.roles.includes('admin') || userStore.permissions.includes('*:*:*')
}

export const usePermissionStore = defineStore('permission', {
  state: () => ({
    backendRouters: [],
    allowedMenuKeys: [],
    addedRouteNames: [],
    loaded: false
  }),
  actions: {
    async generateRoutes() {
      console.debug('[AUTH-TRACE] permissionStore.generateRoutes: ENTER', 'loadedBefore=', this.loaded)
      // 先把已加载标志置回 false：重试或强制刷新时，即使中途失败也不会残留“已就绪”状态。
      this.loaded = false
      // 第一阶段登录/权限链路必须真实请求，禁止 Mock 回退。
      console.debug('[AUTH-TRACE] generateRoutes: BEFORE getRouters')
      const res = await getRouters()
      console.debug('[AUTH-TRACE] generateRoutes: AFTER getRouters', 'hasRes=', !!res, 'dataIsArray=', Array.isArray(res?.data), 'dataLen=', Array.isArray(res?.data) ? res.data.length : -1)
      const backendRouters = Array.isArray(res?.data) ? res.data : []
      const isAdmin = isAdminUser()
      console.debug('[AUTH-TRACE] generateRoutes: isAdmin=', isAdmin)

      const allowedMenuKeys = isAdmin
        ? businessRoutes.map((route) => route.meta.menuKey)
        : collectAllowedMenuKeys(backendRouters)

      const routesToAdd = isAdmin
        ? businessRoutes
        : businessRoutes.filter((route) => allowedMenuKeys.includes(route.meta.menuKey))

      this.addToRouter(routesToAdd)
      this.backendRouters = backendRouters
      this.allowedMenuKeys = allowedMenuKeys
      this.loaded = true
      console.debug('[AUTH-TRACE] generateRoutes: DONE routesRegistered', 'loaded=', this.loaded, 'routesToAddLen=', routesToAdd.length, 'addedRouteNamesLen=', this.addedRouteNames.length)
      return routesToAdd
    },

    addToRouter(routes) {
      routes.forEach((route) => {
        if (!router.hasRoute(route.name)) router.addRoute('RootLayout', route)
        if (!this.addedRouteNames.includes(route.name)) this.addedRouteNames.push(route.name)
      })
      if (!router.hasRoute(notFoundRoute.name)) router.addRoute('RootLayout', notFoundRoute)
      if (!this.addedRouteNames.includes(notFoundRoute.name)) this.addedRouteNames.push(notFoundRoute.name)
    },

    resetRoutes() {
      this.addedRouteNames.forEach((name) => {
        if (router.hasRoute(name)) router.removeRoute(name)
      })
      this.addedRouteNames = []
      this.backendRouters = []
      this.allowedMenuKeys = []
      this.loaded = false
    }
  }
})
