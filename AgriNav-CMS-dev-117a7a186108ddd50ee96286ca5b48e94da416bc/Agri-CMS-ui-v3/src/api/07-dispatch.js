/**
 * 07-作业调度（10 个真实接口）
 * 依据后端 DispatchController.java（Agri-CMS-admin/.../web/controller/dispatch/DispatchController.java）
 * 逐条核对，类级 @RequestMapping("/dispatch") 下共 10 个端点：
 *
 *   POST   /dispatch                 单选派单（仅 admin）
 *   POST   /dispatch/batch           批量派单（仅 admin）
 *   GET    /dispatch/list            分页查询（登录态；后端会先执行 markTimeout()，属“读触发写”）
 *   GET    /dispatch/{id}            派单详情
 *   PUT    /dispatch/{id}/accept     接单（仅 assigneeId 本人）
 *   PUT    /dispatch/{id}/complete   完成（仅 assigneeId 本人）
 *   PUT    /dispatch/{id}/cancel     取消（仅 admin）
 *   POST   /dispatch/timeout         刷新超时（登录态；前端仅向 admin 展示入口）
 *   GET    /dispatch/stats           全局统计（无参数；后端会先执行 markTimeout()）
 *   GET    /dispatch/suggestion      调度建议（仅 admin）
 *
 * 注意：
 *   - 不是 11 个端点，也不存在 /dispatch/my、/dispatch/export 等路径；
 *   - 所有写操作仍需后端鉴权，前端按钮显隐不构成权限保障；
 *   - 雪花 ID 一律按字符串处理，禁止 Number 化。
 */
import { get, post, put } from '@/utils/request'

export const postDispatch = (...args) => post('/dispatch', ...args)
export const getDispatchById = (id, ...rest) => get(`/dispatch/${encodeURIComponent(id)}`, ...rest)
export const putDispatchAcceptById = (id, ...rest) => put(`/dispatch/${encodeURIComponent(id)}/accept`, ...rest)
export const putDispatchCancelById = (id, ...rest) => put(`/dispatch/${encodeURIComponent(id)}/cancel`, ...rest)
export const putDispatchCompleteById = (id, ...rest) => put(`/dispatch/${encodeURIComponent(id)}/complete`, ...rest)
export const postDispatchBatch = (...args) => post('/dispatch/batch', ...args)
export const getDispatchList = (...args) => get('/dispatch/list', ...args)
export const getDispatchStats = (...args) => get('/dispatch/stats', ...args)
export const postDispatchTimeout = (...args) => post('/dispatch/timeout', ...args)
export const getDispatchSuggestion = (...args) => get('/dispatch/suggestion', ...args)

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
  getDispatchSuggestion,
}
export default dispatchApi
