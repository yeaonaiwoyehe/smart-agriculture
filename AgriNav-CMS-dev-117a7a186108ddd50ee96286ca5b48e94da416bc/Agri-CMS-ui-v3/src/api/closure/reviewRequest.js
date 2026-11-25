import request from '@/utils/request'

// 查询复核申请列表（分页）
export function listReviewRequest(query) {
  return request({
    url: '/reviewRequest/list',
    method: 'get',
    params: query
  })
}

// 查询复核申请详情
export function getReviewRequest(id) {
  return request({
    url: '/reviewRequest/' + id,
    method: 'get'
  })
}

// 提交复核申请
export function submitReviewRequest(data) {
  return request({
    url: '/reviewRequest',
    method: 'post',
    data: data
  })
}

// 队长初核
export function initialReview(id, pass, comment) {
  return request({
    url: '/reviewRequest/initial',
    method: 'put',
    params: { id, pass, comment }
  })
}

// 管理员终审
export function finalReview(id, pass, comment) {
  return request({
    url: '/reviewRequest/final',
    method: 'put',
    params: { id, pass, comment }
  })
}
