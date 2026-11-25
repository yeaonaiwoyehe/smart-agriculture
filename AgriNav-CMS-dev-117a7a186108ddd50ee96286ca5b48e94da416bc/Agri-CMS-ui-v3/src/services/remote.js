/**
 * 真实后端接口集合
 * ---------------------------------------------------------------
 * 全部方法来自 src/api（依据《AgriNav接口清单.md》308 个接口生成）。
 * 这里保留旧的文件名与别名，供 services/api.js 及历史代码调用；
 * 路径、HTTP 方法与文档逐条对应，字段转换仍由 utils/adapter.js 负责。
 */
import { get, post, put, del } from '@/utils/request'
import * as api from '@/api'

/* ---------- 地块 ---------- */
export const remoteLandList = (params = {}) => get('/landPlot', { current: 1, size: 1000, ...params })
export const remoteLandQuery = (params = {}) => api.getLandPlotQuery(params)
export const remoteLandGet = (id) => api.getLandPlotById(id)
export const remoteLandHeatmap = () => api.getLandPlotHeatmap()
export const remoteLandArea = (address) => api.getLandPlotArea({ address })

/* ---------- 农机 / 机手 ---------- */
export const remoteMachineList = (data = {}, pageNum = 1, pageSize = 1000) =>
  post('/agriculturalMachinery/list', data, { pageNum, pageSize })
/**
 * 【不可用，已废弃】AgriculturalMachineryController 中没有任何 @GetMapping，
 * 后端不存在 GET /agriculturalMachinery/{id}，调用必然 404。
 * 农机详情已改为复用 /agriculturalMachinery/list 结果按 id 匹配（见 services/api.js getMachine）。
 * 保留导出仅为避免破坏既有 import，请勿在新代码中调用。
 */
export const remoteMachineGet = (id) => get(`/agriculturalMachinery/${encodeURIComponent(id)}`)
export const remoteMachineTypeList = () => api.getAgriculturalMachineryTypeList()
export const remoteFarmWorkList = (query = {}) => {
  const { pageNum = 1, pageSize = 1000, ...rest } = query
  return post('/farmWork/list', rest, { pageNum, pageSize })
}
/**
 * 寻找农机（作业需求）真实写接口，来自 FarmWorkController.java：
 *   POST   /farmWork/add      新增，@PreAuthorize('home:farmWork:add')
 *   PUT    /farmWork          更新，@PreAuthorize('home:farmWork:update')
 *   DELETE /farmWork/{id}     下架（status 置为 NO_LONGER_AVAILABLE=1），@PreAuthorize('home:farmWork:delete')
 * 三者请求/响应契约明确，因此可直接接入，不需要 mock 兜底。
 */
export const remoteFarmWorkAdd = (data) => post('/farmWork/add', data)
export const remoteFarmWorkUpdate = (data) => put('/farmWork', data)
export const remoteFarmWorkDelete = (id) => del(`/farmWork/${encodeURIComponent(id)}`)

/* ---------- 作业类型（唯一类型字典） ---------- */
/** GET /fieldWork/listUnique -> WorkType 列表（id / workName / createTime / updateTime / delete） */
export const remoteWorkTypeList = () => get('/fieldWork/listUnique')

/* ---------- 作业 / 轨迹 ---------- */
export const remoteWorkingLandPlot = (params = {}) => get('/workingLandPlot', { pageNum: 1, pageSize: 1000, ...params })
export const remoteEffectiveTrajectory = (data = {}) => {
  const { pageNum = 1, pageSize = 1000, ...body } = data
  return post('/effectiveTrajectory/conditions', body, { pageNum, pageSize })
}
export const remoteOriginalTrajectory = (params = {}) =>
  get('/originalTrajectory/queryOriginalTrajectoryByPhoneAndTime', params)
/** 文档：GET /landPlot/byCoordinate（按经纬度查地块） */
export const remoteTrajectoryByCoord = (params = {}) => api.getLandPlotByCoordinate(params)
/** 文档无 /trajectoryInfo/list，改用 POST /effectiveTrajectory 查询当前用户轨迹 */
export const remoteTrajectoryInfo = (data = {}) => api.postEffectiveTrajectory(data)

/* ---------- 结算 / 复核 / 单价 ---------- */
export const remoteSettlementList = (params = {}) => api.getSettlementList(params)
export const remoteSettlementDetail = (id) => api.getSettlementById(String(id))
export const remoteSettlementItems = (id) => api.getSettlementItemsById(String(id))
export const remoteSettlementGenerate = (params = {}) => post('/settlement/generate', undefined, params)
/**
 * 结算状态更新：后端 PUT /settlement/status 接收 @RequestParam Long id + Short status。
 * 雪花 Long 主键先转字符串防丢精度。
 */
export const remoteSettlementConfirm = (id, status) => put('/settlement/status', undefined, { id: String(id), status })
export const remoteReviewList = (params = {}) => api.getReviewRequestList(params)
export const remoteReviewSubmit = (data) => post('/reviewRequest', data)
/**
 * 复核接口后端使用 @RequestParam Long id 接收 ReviewRequest 主键。
 * 雪花 Long 需先转字符串再交给 request 层处理，避免 JS Number 丢精度。
 */
export const remoteReviewInitial = (id, pass, comment) => put('/reviewRequest/initial', undefined, { id: String(id), pass, comment })
export const remoteReviewFinal = (id, pass, comment) => put('/reviewRequest/final', undefined, { id: String(id), pass, comment })
export const remotePriceRuleList = (params = {}) => api.getPricingRuleList(params)
export const remotePriceRuleUpdate = (data) => put('/pricingRule', data)

/* ---------- 大屏统计 ---------- */
export const remoteScreen = {
  activeUser: () => api.getScreenGetActiveUserPercentage(),
  totalArea: () => api.getScreenGetLandPlotsTotalArea(),
  farmWork: () => api.getScreenGetFinishedFarmWorkPercentage(),
  trajectory: () => api.getScreenGetTrajectoryNumByMonth(),
  landPlotNum: () => api.getScreenGetLandPlotNumByMonth(),
  statistics: () => api.getScreenGetScreenStatistics(),
  provinceCount: () => api.getScreenGetLandPlotCountByProvice(),
  mostFarmWorkProvince: () => api.getScreenGetMostFarmWorkProvince(),
  biggestFieldWorkType: () => api.getScreenGetBiggestFieldWorkType(),
  mostFieldWorkType: () => api.getScreenGetMostFieldWorkType(),
  mostCrop: () => api.getScreenGetMostCrop(),
  cropSituation: () => api.getScreenGetCropSituation()
}

/* ---------- 登录 ---------- */
export const remoteLogin = (username, password, code, uuid) =>
  post('/login', { username, password, code, uuid })

/** 暴露全量接口，供需要直接调用文档接口的页面使用 */
export const allApi = api
