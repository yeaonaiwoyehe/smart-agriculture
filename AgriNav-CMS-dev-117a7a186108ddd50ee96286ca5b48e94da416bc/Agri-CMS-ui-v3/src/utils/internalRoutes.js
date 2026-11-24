// 外链保留在侧栏菜单中，但不能交给 Vue Router 4 注册为站内路由。
export function internalRoutes(routes) {
  return routes.filter(route => !/^(https?:|mailto:|tel:|\/\/)/i.test(route.path || '')).map(route => ({
    ...route,
    ...(route.children ? { children: internalRoutes(route.children) } : {})
  }))
}
