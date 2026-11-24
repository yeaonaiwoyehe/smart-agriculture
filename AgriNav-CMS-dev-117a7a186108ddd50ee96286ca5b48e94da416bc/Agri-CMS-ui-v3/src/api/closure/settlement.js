import request from '@/utils/request'

// 查询结算单列表（分页）
export function listSettlement(query) {
  return request({
    url: '/settlement/list',
    method: 'get',
    params: query
  })
}

// 查询结算单详情
export function getSettlement(id) {
  return request({
    url: '/settlement/' + id,
    method: 'get'
  })
}

// 查询结算单明细
export function getSettlementItems(id) {
  return request({
    url: '/settlement/items/' + id,
    method: 'get'
  })
}

// 生成结算单
export function generateSettlement(params) {
  return request({
    url: '/settlement/generate',
    method: 'post',
    params: params
  })
}

// 更新结算单状态
export function updateSettlementStatus(id, status) {
  return request({
    url: '/settlement/status',
    method: 'put',
    params: { id, status }
  })
}

// 导出结算明细（blob）
export function exportSettlement(id) {
  return request({
    url: '/settlement/export/' + id,
    method: 'post',
    responseType: 'blob'
  })
}
