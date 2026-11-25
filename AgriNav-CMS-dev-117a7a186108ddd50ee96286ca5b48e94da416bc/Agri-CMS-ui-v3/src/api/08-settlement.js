/**
 * 08-内部结算（16 个接口）
 * 依据 AgriNav接口清单.md 自动生成，方法与路径逐条对应。
 */
import { get, post, put, del, patch } from '@/utils/request'

export const postPricingRule = (...args) => post('/pricingRule', ...args)
export const putPricingRule = (...args) => put('/pricingRule', ...args)
export const getPricingRuleById = (id, ...rest) => get(`/pricingRule/${encodeURIComponent(id)}`, ...rest)
export const removePricingRuleByIds = (ids, ...rest) => del(`/pricingRule/${encodeURIComponent(ids)}`, ...rest)
export const getPricingRuleList = (...args) => get('/pricingRule/list', ...args)
export const postReviewRequest = (...args) => post('/reviewRequest', ...args)
export const getReviewRequestById = (id, ...rest) => get(`/reviewRequest/${encodeURIComponent(id)}`, ...rest)
export const putReviewRequestFinal = (...args) => put('/reviewRequest/final', ...args)
export const putReviewRequestInitial = (...args) => put('/reviewRequest/initial', ...args)
export const getReviewRequestList = (...args) => get('/reviewRequest/list', ...args)
export const getSettlementById = (id, ...rest) => get(`/settlement/${encodeURIComponent(id)}`, ...rest)
export const postSettlementExportById = (id, ...rest) => post(`/settlement/export/${encodeURIComponent(id)}`, ...rest)
export const postSettlementGenerate = (...args) => post('/settlement/generate', ...args)
export const getSettlementItemsById = (id, ...rest) => get(`/settlement/items/${encodeURIComponent(id)}`, ...rest)
export const getSettlementList = (...args) => get('/settlement/list', ...args)
export const putSettlementStatus = (...args) => put('/settlement/status', ...args)

export const settlementApi = {
  postPricingRule,
  putPricingRule,
  getPricingRuleById,
  removePricingRuleByIds,
  getPricingRuleList,
  postReviewRequest,
  getReviewRequestById,
  putReviewRequestFinal,
  putReviewRequestInitial,
  getReviewRequestList,
  getSettlementById,
  postSettlementExportById,
  postSettlementGenerate,
  getSettlementItemsById,
  getSettlementList,
  putSettlementStatus,
}
export default settlementApi
