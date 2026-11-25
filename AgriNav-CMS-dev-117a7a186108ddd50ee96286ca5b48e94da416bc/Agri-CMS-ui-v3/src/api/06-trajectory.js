/**
 * 06-轨迹管理（18 个接口）
 * 依据 AgriNav接口清单.md 自动生成，方法与路径逐条对应。
 */
import { get, post, put, del, patch } from '@/utils/request'

export const postEffectiveTrajectory = (...args) => post('/effectiveTrajectory', ...args)
export const getEffectiveTrajectoryByPhoneNumber = (...args) => get('/effectiveTrajectory/byPhoneNumber', ...args)
export const postEffectiveTrajectoryConditions = (...args) => post('/effectiveTrajectory/conditions', ...args)
export const removeEffectiveTrajectorySignOutByUsername = (username, ...rest) => del(`/effectiveTrajectory/signOut/${encodeURIComponent(username)}`, ...rest)
export const removeOriginalTrajectoryDeleteByUserName = (...args) => del('/originalTrajectory/deleteByUserName', ...args)
export const removeOriginalTrajectoryQueryOriginalTrajectory = (...args) => del('/originalTrajectory/queryOriginalTrajectory', ...args)
export const getOriginalTrajectoryQueryOriginalTrajectory = (...args) => get('/originalTrajectory/queryOriginalTrajectory', ...args)
export const patchOriginalTrajectoryQueryOriginalTrajectory = (...args) => patch('/originalTrajectory/queryOriginalTrajectory', ...args)
export const postOriginalTrajectoryQueryOriginalTrajectory = (...args) => post('/originalTrajectory/queryOriginalTrajectory', ...args)
export const putOriginalTrajectoryQueryOriginalTrajectory = (...args) => put('/originalTrajectory/queryOriginalTrajectory', ...args)
export const removeOriginalTrajectoryQueryOriginalTrajectoryByPhoneAndTime = (...args) => del('/originalTrajectory/queryOriginalTrajectoryByPhoneAndTime', ...args)
export const getOriginalTrajectoryQueryOriginalTrajectoryByPhoneAndTime = (...args) => get('/originalTrajectory/queryOriginalTrajectoryByPhoneAndTime', ...args)
export const patchOriginalTrajectoryQueryOriginalTrajectoryByPhoneAndTime = (...args) => patch('/originalTrajectory/queryOriginalTrajectoryByPhoneAndTime', ...args)
export const postOriginalTrajectoryQueryOriginalTrajectoryByPhoneAndTime = (...args) => post('/originalTrajectory/queryOriginalTrajectoryByPhoneAndTime', ...args)
export const putOriginalTrajectoryQueryOriginalTrajectoryByPhoneAndTime = (...args) => put('/originalTrajectory/queryOriginalTrajectoryByPhoneAndTime', ...args)
export const postTrajectoryInfo = (...args) => post('/trajectoryInfo', ...args)
export const getTrajectoryInfoCreate = (...args) => get('/trajectoryInfo/create', ...args)
export const getTrajectoryInfoPicture = (...args) => get('/trajectoryInfo/picture', ...args)

export const trajectoryApi = {
  postEffectiveTrajectory,
  getEffectiveTrajectoryByPhoneNumber,
  postEffectiveTrajectoryConditions,
  removeEffectiveTrajectorySignOutByUsername,
  removeOriginalTrajectoryDeleteByUserName,
  removeOriginalTrajectoryQueryOriginalTrajectory,
  getOriginalTrajectoryQueryOriginalTrajectory,
  patchOriginalTrajectoryQueryOriginalTrajectory,
  postOriginalTrajectoryQueryOriginalTrajectory,
  putOriginalTrajectoryQueryOriginalTrajectory,
  removeOriginalTrajectoryQueryOriginalTrajectoryByPhoneAndTime,
  getOriginalTrajectoryQueryOriginalTrajectoryByPhoneAndTime,
  patchOriginalTrajectoryQueryOriginalTrajectoryByPhoneAndTime,
  postOriginalTrajectoryQueryOriginalTrajectoryByPhoneAndTime,
  putOriginalTrajectoryQueryOriginalTrajectoryByPhoneAndTime,
  postTrajectoryInfo,
  getTrajectoryInfoCreate,
  getTrajectoryInfoPicture,
}
export default trajectoryApi
