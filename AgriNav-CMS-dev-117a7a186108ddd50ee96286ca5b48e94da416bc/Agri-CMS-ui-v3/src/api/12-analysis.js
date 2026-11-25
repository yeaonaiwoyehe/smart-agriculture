/**
 * 12-统计分析（29 个接口）
 * 依据 AgriNav接口清单.md 自动生成，方法与路径逐条对应。
 */
import { get, post, put, del, patch } from '@/utils/request'

export const getAnalysisTrajectoryOverview = (...args) => get('/analysis/trajectory/overview', ...args)
export const getAnalysisTrajectoryQualityByPlotId = (plotId, ...rest) => get(`/analysis/trajectory/quality/${encodeURIComponent(plotId)}`, ...rest)
export const getAnalysisTrajectoryTimeRange = (...args) => get('/analysis/trajectory/time-range', ...args)
export const getAnalysisUserAuthenticity = (...args) => get('/analysis/user/authenticity', ...args)
export const getAnalysisUserAuthenticityAll = (...args) => get('/analysis/user/authenticity/all', ...args)
export const getAnalysisUserHighRisk = (...args) => get('/analysis/user/high-risk', ...args)
export const getAnalysisUserOne = (...args) => get('/analysis/user/one', ...args)
export const getAnalysisUserOverview = (...args) => get('/analysis/user/overview', ...args)
export const getAnalysisUserTopActive = (...args) => get('/analysis/user/top-active', ...args)
export const getScreenGetActiveUserPercentage = (...args) => get('/screen/getActiveUserPercentage', ...args)
export const getScreenGetBiggestFieldWorkType = (...args) => get('/screen/getBiggestFieldWorkType', ...args)
export const getScreenGetCropSituation = (...args) => get('/screen/getCropSituation', ...args)
export const getScreenGetFarmWorkInfo = (...args) => get('/screen/getFarmWorkInfo', ...args)
export const getScreenGetFinishedFarmWorkPercentage = (...args) => get('/screen/getFinishedFarmWorkPercentage', ...args)
export const getScreenGetLandPlotCountByProvice = (...args) => get('/screen/getLandPlotCountByProvice', ...args)
export const getScreenGetLandPlotCountInDetailedProvince = (...args) => get('/screen/getLandPlotCountInDetailedProvince', ...args)
export const getScreenGetLandPlotNumByMonth = (...args) => get('/screen/getLandPlotNumByMonth', ...args)
export const getScreenGetLandPlotsTotalArea = (...args) => get('/screen/getLandPlotsTotalArea', ...args)
export const getScreenGetLandPlotsWithinProvince = (...args) => get('/screen/getLandPlotsWithinProvince', ...args)
export const getScreenGetMostCrop = (...args) => get('/screen/getMostCrop', ...args)
export const getScreenGetMostFarmWorkProvince = (...args) => get('/screen/getMostFarmWorkProvince', ...args)
export const getScreenGetMostFieldWorkType = (...args) => get('/screen/getMostFieldWorkType', ...args)
export const getScreenGetScreenStatistics = (...args) => get('/screen/getScreenStatistics', ...args)
export const getScreenGetTrajectoryNumByMonth = (...args) => get('/screen/getTrajectoryNumByMonth', ...args)
export const getScreenRefresh = (...args) => get('/screen/refresh', ...args)
export const getStaticUserDau = (...args) => get('/static/user/dau', ...args)
export const getStaticUserLc = (...args) => get('/static/user/lc', ...args)
export const getStaticUserLocation = (...args) => get('/static/user/location', ...args)
export const getStaticUserRc = (...args) => get('/static/user/rc', ...args)

export const analysisApi = {
  getAnalysisTrajectoryOverview,
  getAnalysisTrajectoryQualityByPlotId,
  getAnalysisTrajectoryTimeRange,
  getAnalysisUserAuthenticity,
  getAnalysisUserAuthenticityAll,
  getAnalysisUserHighRisk,
  getAnalysisUserOne,
  getAnalysisUserOverview,
  getAnalysisUserTopActive,
  getScreenGetActiveUserPercentage,
  getScreenGetBiggestFieldWorkType,
  getScreenGetCropSituation,
  getScreenGetFarmWorkInfo,
  getScreenGetFinishedFarmWorkPercentage,
  getScreenGetLandPlotCountByProvice,
  getScreenGetLandPlotCountInDetailedProvince,
  getScreenGetLandPlotNumByMonth,
  getScreenGetLandPlotsTotalArea,
  getScreenGetLandPlotsWithinProvince,
  getScreenGetMostCrop,
  getScreenGetMostFarmWorkProvince,
  getScreenGetMostFieldWorkType,
  getScreenGetScreenStatistics,
  getScreenGetTrajectoryNumByMonth,
  getScreenRefresh,
  getStaticUserDau,
  getStaticUserLc,
  getStaticUserLocation,
  getStaticUserRc,
}
export default analysisApi
