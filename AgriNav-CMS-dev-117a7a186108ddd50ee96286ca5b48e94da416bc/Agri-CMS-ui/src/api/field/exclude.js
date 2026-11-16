import request from '@/utils/request'

// 查询地块无用区域列表
export function listLandPlotExclude(landPlotId) {
  return request({
    url: '/landPlotExclude/queryByLandPlotId',
    method: 'get',
    params: { landPlotId }
  })
}

// 新增地块无用区域
export function addLandPlotExclude(data) {
  return request({
    url: '/landPlotExclude/addLandPlotExclude',
    method: 'post',
    data: data
  })
}
