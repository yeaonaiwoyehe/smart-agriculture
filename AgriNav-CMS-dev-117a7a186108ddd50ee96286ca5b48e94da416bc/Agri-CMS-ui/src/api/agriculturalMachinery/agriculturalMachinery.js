import request from '@/utils/request'

// 查询农机找活列表
export function listAgriculturalMachinery(data, pageNum, pageSize) {
  return request({
    url: '/agriculturalMachinery/list',
    method: 'post',
    data: data,
    params: {
      pageNum: pageNum,
      pageSize: pageSize
    }
  })
}

// 查询农机找活详细
export function getAgriculturalMachinery(id) {
  return request({
    url: '/agriculturalMachinery/' + id,
    method: 'get'
  })
}

// 新增农机找活
export function addAgriculturalMachinery(data) {
  return request({
    url: '/agriculturalMachinery/add',
    method: 'post',
    data: data,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 修改农机找活
export function updateAgriculturalMachinery(data) {
  return request({
    url: '/agriculturalMachinery',
    method: 'put',
    data: data
  })
}

// 删除农机找活
export function delAgriculturalMachinery(id) {
  return request({
    url: '/agriculturalMachinery/' + id,
    method: 'delete'
  })
}
