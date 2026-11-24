import request from '@/utils/request'

// 查询权限列表
export function listPermission() {
  return request({
    url: '/permission/list',
    method: 'get'
  })
}

// 查询权限详细
export function getPermission(componentId) {
  return request({
    url: '/permission/get/' + componentId,
    method: 'get'
  })
}

// 新增权限
export function addPermission(data) {
  return request({
    url: '/permission/add',
    method: 'post',
    data: data
  })
}

// 修改权限
export function updatePermission(data) {
  return request({
    url: '/permission/update',
    method: 'post',
    data: data
  })
}

// 删除权限
export function delPermission(componentId) {
  return request({
    url: '/permission/delete',
    method: 'delete',
    data: { componentId }
  })
}
