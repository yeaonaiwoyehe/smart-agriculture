/**
 * 07-作业调度（9 个接口）
 * 依据 AgriNav接口清单.md 自动生成，方法与路径逐条对应。
 *
 * ⚠️【S6 核查结论：后端全部未实现，本文件当前不可用，请勿在新代码中调用】
 * 逐条核对后端源码（Agri-CMS-admin/src 下 47 个 *Controller.java）：
 *   - 不存在 DispatchController；对全部 Controller 执行 grep 'dispatch|Dispatch' 命中 0 次。
 *   - 因此下列 9 个路径（/dispatch、/dispatch/{id}、/accept、/cancel、/complete、
 *     /dispatch/batch、/dispatch/list、/dispatch/stats、/dispatch/timeout）均返回 404，
 *     它们是依据文档生成的占位定义，不代表后端能力。
 *
 * 真实的调度相关能力只在 FarmWorkController（/farmWork）：
 *   POST /farmWork/list、POST /farmWork/add、PUT /farmWork、DELETE /farmWork/{id}
 * 前端实际接入见 services/api.js 的 listTasks / createTask / updateTask / revokeTask。
 *
 * 按约束（未经单独授权不得删除旧 API 文件）本文件保留不删，仅作标注。
 */
import { get, post, put, del, patch } from '@/utils/request'

export const postDispatch = (...args) => post('/dispatch', ...args)
export const getDispatchById = (id, ...rest) => get(`/dispatch/${encodeURIComponent(id)}`, ...rest)
export const putDispatchAcceptById = (id, ...rest) => put(`/dispatch/${encodeURIComponent(id)}/accept`, ...rest)
export const putDispatchCancelById = (id, ...rest) => put(`/dispatch/${encodeURIComponent(id)}/cancel`, ...rest)
export const putDispatchCompleteById = (id, ...rest) => put(`/dispatch/${encodeURIComponent(id)}/complete`, ...rest)
export const postDispatchBatch = (...args) => post('/dispatch/batch', ...args)
export const getDispatchList = (...args) => get('/dispatch/list', ...args)
export const getDispatchStats = (...args) => get('/dispatch/stats', ...args)
export const postDispatchTimeout = (...args) => post('/dispatch/timeout', ...args)

export const dispatchApi = {
  postDispatch,
  getDispatchById,
  putDispatchAcceptById,
  putDispatchCancelById,
  putDispatchCompleteById,
  postDispatchBatch,
  getDispatchList,
  getDispatchStats,
  postDispatchTimeout,
}
export default dispatchApi
