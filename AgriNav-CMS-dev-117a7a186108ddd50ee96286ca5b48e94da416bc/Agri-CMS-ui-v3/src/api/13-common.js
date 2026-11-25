/**
 * 13-通用与测试（11 个接口）
 * 依据 AgriNav接口清单.md 自动生成，方法与路径逐条对应。
 */
import { get, post, put, del, patch } from '@/utils/request'

export const getCommonDownload = (...args) => get('/common/download', ...args)
export const getCommonDownloadResource = (...args) => get('/common/download/resource', ...args)
export const postCommonUpload = (...args) => post('/common/upload', ...args)
export const postCommonUploads = (...args) => post('/common/uploads', ...args)
export const removeTestUserByUserId = (userId, ...rest) => del(`/test/user/${encodeURIComponent(userId)}`, ...rest)
export const getTestUserByUserId = (userId, ...rest) => get(`/test/user/${encodeURIComponent(userId)}`, ...rest)
export const getTestUserList = (...args) => get('/test/user/list', ...args)
export const postTestUserSave = (...args) => post('/test/user/save', ...args)
export const putTestUserUpdate = (...args) => put('/test/user/update', ...args)
export const getVersion = (...args) => get('/version', ...args)
export const postVersion = (...args) => post('/version', ...args)

export const commonApi = {
  getCommonDownload,
  getCommonDownloadResource,
  postCommonUpload,
  postCommonUploads,
  removeTestUserByUserId,
  getTestUserByUserId,
  getTestUserList,
  postTestUserSave,
  putTestUserUpdate,
  getVersion,
  postVersion,
}
export default commonApi
