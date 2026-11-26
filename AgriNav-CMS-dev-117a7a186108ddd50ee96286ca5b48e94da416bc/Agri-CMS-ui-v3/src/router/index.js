import { createRouter, createWebHashHistory } from 'vue-router'
import MainLayout from '@/layouts/MainLayout.vue'

// 侧边栏展示结构保持不变；实际可访问性由后端 /getRouters 派生的权限决定。
export const menuGroups = [
  {
    label: '运营总览',
    items: [{ key: 'dashboard', title: '智能运营驾驶舱', path: '/dashboard', icon: 'Odometer' }]
  },
  {
    label: '业务管理',
    items: [
      { key: 'lands', title: '地块管理', path: '/lands', icon: 'MapLocation' },
      { key: 'machines', title: '农机与农机手', path: '/machines', icon: 'Van' },
      { key: 'farmwork', title: '农田作业管理', path: '/farmwork', icon: 'Document' },
      { key: 'dispatch', title: '作业调度管理', path: '/dispatch', icon: 'Promotion' },
      { key: 'monitor', title: '实时作业监控', path: '/monitor', icon: 'Aim' },
      { key: 'workorders', title: '轨迹与面积核算', path: '/workorders', icon: 'Tickets' },
      { key: 'settlement', title: '内部结算管理', path: '/settlement', icon: 'Wallet' },
      { key: 'reports', title: '统计报表', path: '/reports', icon: 'TrendCharts' }
    ]
  },
  {
    label: 'AI 智能体',
    items: [
      { key: 'workbench', title: 'Agent 工作台', path: '/agent/workbench', icon: 'ChatDotRound' },
      { key: 'agentManage', title: 'Agent 管理', path: '/agent/manage', icon: 'Cpu' },
      { key: 'agentTools', title: '工具管理', path: '/agent/tools', icon: 'Tools' },
      { key: 'agentWorkflow', title: '工作流编排', path: '/agent/workflow', icon: 'Share' },
      { key: 'agentKnowledge', title: '知识库管理', path: '/agent/knowledge', icon: 'Collection' },
      { key: 'agentMonitor', title: '运行监控', path: '/agent/monitor', icon: 'Monitor' }
    ]
  },
  {
    label: '系统管理',
    items: [{ key: 'system', title: '权限与审计', path: '/system', icon: 'Lock' }]
  }
]

// 始终可用：登录页 + 布局壳 + 首页。这样登录成功后一定有可跳转的落点。
export const constantRoutes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    name: 'RootLayout',
    component: MainLayout,
    redirect: '/login',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/dashboard/Dashboard.vue'),
        meta: { title: '数据总览', code: 'MP-00', group: '运营总览', menuKey: 'dashboard' }
      }
    ]
  }
]

// 业务路由由 permission store 根据后端 /getRouters 动态 addRoute。
// 组件与路径完全沿用现有定义，不改动页面文件与 UI。
export const businessRoutes = [
  {
    path: 'lands',
    name: 'lands',
    component: () => import('@/views/land/LandList.vue'),
    meta: { title: '地块管理', code: 'MP-01', group: '业务管理', menuKey: 'lands' }
  },
  {
    path: 'machines',
    name: 'machines',
    component: () => import('@/views/machine/MachineList.vue'),
    meta: { title: '农机与农机手', code: 'MP-02', group: '业务管理', menuKey: 'machines' }
  },
  {
    path: 'farmwork',
    name: 'farmwork',
    component: () => import('@/views/farmWork/FarmWorkList.vue'),
    meta: { title: '农田作业管理', code: 'MP-05A', group: '业务管理', menuKey: 'farmwork' }
  },
  {
    path: 'dispatch',
    name: 'dispatch',
    component: () => import('@/views/dispatch/DispatchBoard.vue'),
    meta: { title: '作业调度管理', code: 'MP-03', group: '业务管理', menuKey: 'dispatch' }
  },
  {
    path: 'monitor',
    name: 'monitor',
    component: () => import('@/views/monitor/Monitor.vue'),
    meta: { title: '实时作业监控', code: 'MP-03', group: '业务管理', menuKey: 'monitor' }
  },
  {
    path: 'workorders',
    name: 'workorders',
    component: () => import('@/views/workorder/WorkorderList.vue'),
    meta: { title: '轨迹与面积核算', code: 'MP-03', group: '业务管理', menuKey: 'workorders' }
  },
  {
    path: 'settlement',
    name: 'settlement',
    component: () => import('@/views/settlement/Settlement.vue'),
    meta: { title: '内部结算管理', code: 'MP-04', group: '业务管理', menuKey: 'settlement' }
  },
  {
    path: 'reports',
    name: 'reports',
    component: () => import('@/views/report/Report.vue'),
    meta: { title: '统计报表', code: 'MP-05', group: '业务管理', menuKey: 'reports' }
  },
  {
    path: 'agent/workbench',
    name: 'agent-workbench',
    component: () => import('@/views/agent/Workbench.vue'),
    meta: { title: 'Agent 工作台', code: 'AI-CENTER', group: 'AI 智能体', menuKey: 'workbench' }
  },
  {
    path: 'agent/manage',
    name: 'agent-manage',
    component: () => import('@/views/agent/AgentManage.vue'),
    meta: { title: 'Agent 管理', code: 'AI-CENTER', group: 'AI 智能体', menuKey: 'agentManage' }
  },
  {
    path: 'agent/tools',
    name: 'agent-tools',
    component: () => import('@/views/agent/ToolManage.vue'),
    meta: { title: '工具管理', code: 'AI-CENTER', group: 'AI 智能体', menuKey: 'agentTools' }
  },
  {
    path: 'agent/workflow',
    name: 'agent-workflow',
    component: () => import('@/views/agent/WorkflowDesigner.vue'),
    meta: { title: '工作流编排', code: 'AI-CENTER', group: 'AI 智能体', menuKey: 'agentWorkflow' }
  },
  {
    path: 'agent/knowledge',
    name: 'agent-knowledge',
    component: () => import('@/views/agent/KnowledgeBase.vue'),
    meta: { title: '知识库管理', code: 'AI-CENTER', group: 'AI 智能体', menuKey: 'agentKnowledge' }
  },
  {
    path: 'agent/monitor',
    name: 'agent-monitor',
    component: () => import('@/views/agent/AgentMonitor.vue'),
    meta: { title: 'Agent 运行监控', code: 'AI-CENTER', group: 'AI 智能体', menuKey: 'agentMonitor' }
  },
  {
    path: 'system',
    name: 'system',
    component: () => import('@/views/system/SystemManage.vue'),
    meta: { title: '权限与审计', code: 'MP-06', group: '系统管理', menuKey: 'system' }
  }
]

// 兜底路由必须最后添加，否则会遮蔽动态注册的业务路由。
export const notFoundRoute = {
  path: ':pathMatch(.*)*',
  name: 'not-found',
  component: () => import('@/views/NotFound.vue'),
  meta: { title: '页面不存在' }
}

const router = createRouter({
  history: createWebHashHistory(),
  routes: constantRoutes,
  scrollBehavior: () => ({ top: 0 })
})

router.afterEach((to) => {
  document.title = to.meta?.title
    ? `${to.meta.title} · 田界智航`
    : '田界智航 · 农机作业管控智能管理平台'
})

export default router
