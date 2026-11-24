import request from '@/utils/request'

// 查询发布记录列表
export function listReleaseNote(version) {
  return request({
    url: '/system/releaseNote/list',
    method: 'get',
    params: {
      version: version
    }
  })
}

// 查询发布记录详细
export function getReleaseNote(id) {
  return request({
    url: `/system/releaseNote/${id}`,
    method: 'get'
  })
}

// 新增发布记录
export function addReleaseNote(data) {
  return request({
    url: '/system/releaseNote',
    method: 'post',
    data: data
  })
}

// 修改发布记录
export function updateReleaseNote(data) {
  return request({
    url: '/system/releaseNote',
    method: 'put',
    data: data
  })
}

// 删除发布记录
export function delReleaseNote(id) {
  return request({
    url: `/system/releaseNote/${id}`,
    method: 'delete'
  })
} 