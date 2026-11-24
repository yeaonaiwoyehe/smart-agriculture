import request from '@/utils/request'

export function listAgriculturalMachineryTypes(pageNum = 1, pageSize = 10) {
  return request({
    url: '/agriculturalMachineryType/list',
    method: 'get',
    params: {pageNum, pageSize}
  })
}
