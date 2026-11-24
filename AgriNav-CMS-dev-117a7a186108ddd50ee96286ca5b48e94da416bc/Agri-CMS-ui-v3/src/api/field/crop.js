import request from '@/utils/request'

export function getCrops() {
  return request({
    url: '/crop/getCrop',
    method: 'get'
  })
}
