/**
 * 05-作业类型与作物（10 个接口）
 * 依据 AgriNav接口清单.md 自动生成，方法与路径逐条对应。
 */
import { get, post, put, del, patch } from '@/utils/request'

export const postCropAddCrop = (...args) => post('/crop/addCrop', ...args)
export const removeCropDeleteCrop = (...args) => del('/crop/deleteCrop', ...args)
export const getCropGetCrop = (...args) => get('/crop/getCrop', ...args)
export const postCropUpdateCrop = (...args) => post('/crop/updateCrop', ...args)
export const postFieldWorkAddWork = (...args) => post('/fieldWork/addWork', ...args)
export const removeFieldWorkDeleteWork = (...args) => del('/fieldWork/deleteWork', ...args)
export const getFieldWorkGetWork = (...args) => get('/fieldWork/getWork', ...args)
export const getFieldWorkList = (...args) => get('/fieldWork/list', ...args)
export const getFieldWorkListUnique = (...args) => get('/fieldWork/listUnique', ...args)
export const postFieldWorkUpdateWork = (...args) => post('/fieldWork/updateWork', ...args)

export const cropApi = {
  postCropAddCrop,
  removeCropDeleteCrop,
  getCropGetCrop,
  postCropUpdateCrop,
  postFieldWorkAddWork,
  removeFieldWorkDeleteWork,
  getFieldWorkGetWork,
  getFieldWorkList,
  getFieldWorkListUnique,
  postFieldWorkUpdateWork,
}
export default cropApi
