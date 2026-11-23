import request from '@/utils/request'

// 分页查询智能体指令审计记录
export function listAudit(query) {
  return request({
    url: '/agent/audit/list',
    method: 'get',
    params: query
  })
}

// 查询审计记录详情
export function getAudit(id) {
  return request({
    url: '/agent/audit/' + id,
    method: 'get'
  })
}
