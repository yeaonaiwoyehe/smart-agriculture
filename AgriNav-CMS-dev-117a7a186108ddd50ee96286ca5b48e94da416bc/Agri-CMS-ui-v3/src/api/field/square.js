import request from '@/utils/request'

// 查询地块列表（分页）
export function listLandPlot(current, size) {
  return request({
    url: '/landPlot',
    method: 'get',
    params: {current, size}
  })
}

// 条件查询地块列表
export function queryLandPlot(query) {
  return request({
    url: '/landPlot/query',
    method: 'get',
    params: query
  })
}

// 查询地块详细
export function getLandPlot(id) {
  return request({
    url: '/landPlot/' + id,
    method: 'get'
  })
}

// 新增地块
export function addLandPlot(data) {
  return request({
    url: '/landPlot',
    method: 'post',
    data: data,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 修改地块
export function updateLandPlot(data) {
  return request({
    url: '/landPlot',
    method: 'put',
    data: data,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 删除地块
export function delLandPlot(id) {
  return request({
    url: '/landPlot/' + id,
    method: 'delete'
  })
}

// 导出地块（如果需要的话）
export function exportLandPlot(query) {
  return request({
    url: '/landPlot/export',
    method: 'get',
    params: query
  })
}

// 热力图
export function getHeatmapData() {
  return request({
    url: '/landPlot/heatmap',
    method: 'get'
  })
}

// 根据地址显示的各个位置的已有地块面积
export function getAreaByAddress(address) {
  return request({
    url: '/landPlot/area',
    method: 'get',
    params: {address}
  })
}

// 获取地区面积统计数据
export function getAreaStatistics(addressCode, level) {
  return request({
    url: '/landPlot/area',
    method: 'get',
    params: {
      addressCode,
      level
    }
  })
}


// 恢复地块
export function restoreLandPlot(id) {
  return request({
    url: '/landPlot/' + id,
    method: 'put'
  })
}
