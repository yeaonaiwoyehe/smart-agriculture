/**
 * 04-地块管理（30 个接口）
 * 依据 AgriNav接口清单.md 自动生成，方法与路径逐条对应。
 */
import { get, post, put, del, patch } from '@/utils/request'

export const postLandAddressGetCity = (...args) => post('/landAddress/getCity', ...args)
export const postLandAddressGetDistrict = (...args) => post('/landAddress/getDistrict', ...args)
export const postLandAddressGetHamlet = (...args) => post('/landAddress/getHamlet', ...args)
export const getLandAddressGetProvince = (...args) => get('/landAddress/getProvince', ...args)
export const postLandAddressGetVillage = (...args) => post('/landAddress/getVillage', ...args)
export const getLandPlot = (...args) => get('/landPlot', ...args)
export const postLandPlot = (...args) => post('/landPlot', ...args)
export const putLandPlot = (...args) => put('/landPlot', ...args)
export const removeLandPlotById = (id, ...rest) => del(`/landPlot/${encodeURIComponent(id)}`, ...rest)
export const getLandPlotById = (id, ...rest) => get(`/landPlot/${encodeURIComponent(id)}`, ...rest)
export const putLandPlotById = (id, ...rest) => put(`/landPlot/${encodeURIComponent(id)}`, ...rest)
export const getLandPlotArea = (...args) => get('/landPlot/area', ...args)
export const getLandPlotAreaByAddress = (...args) => get('/landPlot/areaByAddress', ...args)
export const getLandPlotByCoordinate = (...args) => get('/landPlot/byCoordinate', ...args)
export const getLandPlotGroups = (...args) => get('/landPlot/groups', ...args)
export const postLandPlotGroups = (...args) => post('/landPlot/groups', ...args)
export const putLandPlotGroupsAssignment = (...args) => put('/landPlot/groups/assignment', ...args)
export const getLandPlotGroupsAssignments = (...args) => get('/landPlot/groups/assignments', ...args)
export const getLandPlotHeatmap = (...args) => get('/landPlot/heatmap', ...args)
export const getLandPlotHistory = (...args) => get('/landPlot/history', ...args)
export const postLandPlotImportGeojson = (...args) => post('/landPlot/import/geojson', ...args)
export const postLandPlotImportShapefile = (...args) => post('/landPlot/import/shapefile', ...args)
export const getLandPlotQuery = (...args) => get('/landPlot/query', ...args)
export const getLandPlotQueryByAddress = (...args) => get('/landPlot/queryByAddress', ...args)
export const getLandPlotQueryByAddressAll = (...args) => get('/landPlot/queryByAddressAll', ...args)
export const getLandPlotRemeasure = (...args) => get('/landPlot/remeasure', ...args)
export const removeLandPlotSignOutByUsername = (username, ...rest) => del(`/landPlot/signOut/${encodeURIComponent(username)}`, ...rest)
export const postLandPlotExcludeAddLandPlotExclude = (...args) => post('/landPlotExclude/addLandPlotExclude', ...args)
export const postLandPlotExcludeBatchQuery = (...args) => post('/landPlotExclude/batchQuery', ...args)
export const getLandPlotExcludeQueryByLandPlotId = (...args) => get('/landPlotExclude/queryByLandPlotId', ...args)

export const landApi = {
  postLandAddressGetCity,
  postLandAddressGetDistrict,
  postLandAddressGetHamlet,
  getLandAddressGetProvince,
  postLandAddressGetVillage,
  getLandPlot,
  postLandPlot,
  putLandPlot,
  removeLandPlotById,
  getLandPlotById,
  putLandPlotById,
  getLandPlotArea,
  getLandPlotAreaByAddress,
  getLandPlotByCoordinate,
  getLandPlotGroups,
  postLandPlotGroups,
  putLandPlotGroupsAssignment,
  getLandPlotGroupsAssignments,
  getLandPlotHeatmap,
  getLandPlotHistory,
  postLandPlotImportGeojson,
  postLandPlotImportShapefile,
  getLandPlotQuery,
  getLandPlotQueryByAddress,
  getLandPlotQueryByAddressAll,
  getLandPlotRemeasure,
  removeLandPlotSignOutByUsername,
  postLandPlotExcludeAddLandPlotExclude,
  postLandPlotExcludeBatchQuery,
  getLandPlotExcludeQueryByLandPlotId,
}
export default landApi
