import request from '@/utils/request'

// 获取所有唯一的工作类型
export function getWorkTypes() {
  return request({
    url: '/fieldWork/listUnique',
    method: 'get'
  })
}
