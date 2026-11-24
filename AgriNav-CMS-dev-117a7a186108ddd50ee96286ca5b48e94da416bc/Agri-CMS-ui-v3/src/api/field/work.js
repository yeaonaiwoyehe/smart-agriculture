import request from '@/utils/request'

// 查询正在作业的地块列表
export function queryWorkingLandPlot(params) {
  return request({
    url: '/workingLandPlot',
    method: 'get',
    params: {
      pageNum: params.pageNum,
      pageSize: params.pageSize,
      address: params.address
    }
  })
}
