import { get, post, del } from '@/utils/request'

// 保留 request 返回的完整响应对象，不在 API 层解包 data。

// 查询 FieldWork 列表；与 /fieldWork/listUnique 的 WorkType 数据不同源。
export function listFieldWorks() {
  return get('/fieldWork/list')
}

// 按作物查询关联的作业项，cropId 是必填 URL query 参数。
export function listFieldWorkOptions(cropId) {
  return get('/fieldWork/getWork', { cropId })
}

/**
 * 待后端确认：POST /fieldWork/addWork。
 * FieldWorkController.java:56 同时声明了 @RequestBody FieldWork work
 * 和 @RequestBody Long cropId，当前不能据此确定一个合法的请求体。
 * 按 S1 确认方案保留 addFieldWork(work, cropId) 签名作为显式失败占位。
 * work、cropId 仅为待确认的参数，本函数不会发送 HTTP 请求或伪造成功响应。
 * 不将 cropId 擅自移到 query，也不拼接两个 JSON 或猜测合并请求体。
 * 后端确认单一请求体或 query 契约后，再用现有 post 封装实现此函数。
 */
export async function addFieldWork(work, cropId) {
  const error = new Error('POST /fieldWork/addWork 的请求契约待后端确认，暂不发送请求')
  error.code = 'BACKEND_CONTRACT_PENDING'
  throw error
}

// 修改作业项，使用 POST，data 作为 JSON 请求体。
export function updateFieldWork(data) {
  return post('/fieldWork/updateWork', data)
}

// 删除作业项，workId 按后端 @RequestParam 契约放在 URL query 中。
export function deleteFieldWork(workId) {
  return del('/fieldWork/deleteWork', { workId })
}
