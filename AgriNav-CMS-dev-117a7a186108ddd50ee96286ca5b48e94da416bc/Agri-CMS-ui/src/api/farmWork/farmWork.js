import request from '@/utils/request'

// 查询寻找农机列表
export function listFarmWork(query) {
  const {pageNum, pageSize, ...otherParams} = query;
  return request({
    url: '/farmWork/list',
    method: 'post',
    data: otherParams,  // 包含 name 和 phoneNumber 等查询条件
    params: {
      pageNum,
      pageSize
    }
  })
}

// 新增寻找农机
export function addFarmWork(data) {
  return request({
    url: '/farmWork/add',
    method: 'post',
    data: data
  })
}

// 修改寻找农机
export function updateFarmWork(data) {
  return request({
    url: '/farmWork',
    method: 'put',
    data: data
  })
}

// 删除寻找农机
export function delFarmWork(id) {
  return request({
    url: '/farmWork/' + id,
    method: 'delete'
  })
}
