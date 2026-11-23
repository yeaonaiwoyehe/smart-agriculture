import request from '@/utils/request'

// 分页查询智能体会话列表
export function listConversation(query) {
  return request({
    url: '/agent/conversation/list',
    method: 'get',
    params: query
  })
}

// 查询会话详情（含全部消息）
export function getConversation(id) {
  return request({
    url: '/agent/conversation/' + id,
    method: 'get'
  })
}
