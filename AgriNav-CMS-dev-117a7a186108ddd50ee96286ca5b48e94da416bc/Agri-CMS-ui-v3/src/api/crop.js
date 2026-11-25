import { get, post, del } from '@/utils/request'

// 保留 request 返回的完整响应对象，不在 API 层解包 data。

// 查询可用作物列表。
export function listCrops() {
  return get('/crop/getCrop')
}

// 新增作物，data 作为 JSON 请求体。
export function addCrop(data) {
  return post('/crop/addCrop', data)
}

// 修改作物，使用 POST，data 作为 JSON 请求体。
export function updateCrop(data) {
  return post('/crop/updateCrop', data)
}

// 删除作物，cropId 按后端 @RequestParam 契约放在 URL query 中。
export function deleteCrop(cropId) {
  return del('/crop/deleteCrop', { cropId })
}
