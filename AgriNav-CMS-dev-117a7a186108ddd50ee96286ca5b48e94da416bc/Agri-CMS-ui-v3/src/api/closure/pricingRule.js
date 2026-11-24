import request from '@/utils/request'

// 查询单价规则列表（分页）
export function listPricingRule(query) {
  return request({
    url: '/pricingRule/list',
    method: 'get',
    params: query
  })
}

// 查询单价规则详情
export function getPricingRule(id) {
  return request({
    url: '/pricingRule/' + id,
    method: 'get'
  })
}

// 新增单价规则
export function addPricingRule(data) {
  return request({
    url: '/pricingRule',
    method: 'post',
    data: data
  })
}

// 修改单价规则
export function updatePricingRule(data) {
  return request({
    url: '/pricingRule',
    method: 'put',
    data: data
  })
}

// 删除单价规则
export function delPricingRule(ids) {
  return request({
    url: '/pricingRule/' + ids,
    method: 'delete'
  })
}
