/**
 * 03-农活招工（7 个接口）
 * 依据 AgriNav接口清单.md 自动生成，方法与路径逐条对应。
 */
import { get, post, put, del, patch } from '@/utils/request'

export const putFarmWork = (...args) => put('/farmWork', ...args)
export const removeFarmWorkById = (id, ...rest) => del(`/farmWork/${encodeURIComponent(id)}`, ...rest)
export const postFarmWorkAdd = (...args) => post('/farmWork/add', ...args)
export const postFarmWorkList = (...args) => post('/farmWork/list', ...args)
export const removeFarmWorkSignOutByUsername = (username, ...rest) => del(`/farmWork/signOut/${encodeURIComponent(username)}`, ...rest)
export const getWorkingLandPlot = (...args) => get('/workingLandPlot', ...args)
export const postWorkingLandPlotWorkingTrajectory = (...args) => post('/workingLandPlot/workingTrajectory', ...args)

export const farmworkApi = {
  putFarmWork,
  removeFarmWorkById,
  postFarmWorkAdd,
  postFarmWorkList,
  removeFarmWorkSignOutByUsername,
  getWorkingLandPlot,
  postWorkingLandPlotWorkingTrajectory,
}
export default farmworkApi
