import request from '@/utils/request'

// 默认只做文件预检，正式导入必须显式传入 false。
export function importGeoJsonLandPlots(file, dryRun = true, encoding = 'UTF-8') {
  const data = new FormData()
  data.append('file', file)
  return request({
    url: /\.zip$/i.test(file.name) ? '/landPlot/import/shapefile' : '/landPlot/import/geojson',
    method: 'post',
    params: { dryRun, encoding },
    data,
    timeout: 60000,
    returnBusinessErrors: true
  })
}

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
