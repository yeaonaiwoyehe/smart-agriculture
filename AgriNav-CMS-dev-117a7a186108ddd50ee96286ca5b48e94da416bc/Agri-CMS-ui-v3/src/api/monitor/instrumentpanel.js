import request from '@/utils/request'

// 获取用户信息
export function getUserInfo() {
  return request({
    url: '/screen/getActiveUserPercentage',
    method: 'get'
  })
}

// 获取地块总面积
export function getLandPlotsTotalArea() {
  return request({
    url: '/screen/getLandPlotsTotalArea',
    method: 'get'
  })
}

// 获取农事工作信息
export function getFarmWorkInfo() {
  return request({
    url: '/screen/getFinishedFarmWorkPercentage',
    method: 'get'
  })
}

// 获取轨迹数据
export function getTrajectoryData() {
  return request({
    url: '/screen/getTrajectoryNumByMonth',
    method: 'get'
  })
}

// 获取地块数据
export function getLandPlotNumByMonth() {
  return request({
    url: '/screen/getLandPlotNumByMonth',
    method: 'get'
  })
}

// 获取作物工作详情
export function getCropWorkDetail() {
  return request({
    url: '/screen/getScreenStatistics',
    method: 'get'
  })
}

// 获取省份数据
export function getLandPlotCountByProvince() {
  return request({
    url: '/screen/getLandPlotCountByProvice',
    method: 'get'
  })
}

// 获取发布最多农活的省份
export function getMostFarmWorkProvince() {
  return request({
    url: '/screen/getMostFarmWorkProvince',
    method: 'get'
  })
}

// 获取工作面积最大的田地操作
export function getMostAreaCropWork() {
  return request({
    url: '/screen/getBiggestFieldWorkType',
    method: 'get'
  })
}

// 获取工作次数最多的田地操作
export function getMostCountCropWork() {
  return request({
    url: '/screen/getMostFieldWorkType',
    method: 'get'
  })
}

// 获取最热门的作物
export function getMostCrop() {
  return request({
    url: '/screen/getMostCrop',
    method: 'get'
  })
}

// 获取作物情况
export function getCropSituation() {
  return request({
    url: '/screen/getCropSituation',
    method: 'get'
  })
}
