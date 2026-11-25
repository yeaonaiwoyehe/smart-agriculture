import request from '@/utils/request'

// 查询变更日志列表
export function listChangeLog(releaseNoteId) {
  return request({
    url: `/system/changelog/list/${releaseNoteId}`,
    method: 'get'
  })
}

// 查询变更日志详细
export function getChangeLog(id) {
  return request({
    url: `/system/changelog/${id}`,
    method: 'get'
  })
}

// 新增变更日志
export function addChangeLog(data) {
  return request({
    url: '/system/changelog',
    method: 'post',
    data: data
  })
}

// 修改变更日志
export function updateChangeLog(data) {
  return request({
    url: '/system/changelog',
    method: 'put',
    data: data
  })
}

// 删除变更日志
export function delChangeLog(id) {
  return request({
    url: `/system/changelog/${id}`,
    method: 'delete'
  })
} 