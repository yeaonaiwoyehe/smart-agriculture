/**
 * 02-农机管理（9 个接口）
 * 依据 AgriNav接口清单.md 自动生成，方法与路径逐条对应。
 */
import { get, post, put, del, patch } from '@/utils/request'

export const putAgriculturalMachinery = (...args) => put('/agriculturalMachinery', ...args)
export const removeAgriculturalMachineryById = (id, ...rest) => del(`/agriculturalMachinery/${encodeURIComponent(id)}`, ...rest)
export const postAgriculturalMachineryAdd = (...args) => post('/agriculturalMachinery/add', ...args)
export const postAgriculturalMachineryList = (...args) => post('/agriculturalMachinery/list', ...args)
export const removeAgriculturalMachinerySignOutByUsername = (username, ...rest) => del(`/agriculturalMachinery/signOut/${encodeURIComponent(username)}`, ...rest)
export const putAgriculturalMachineryType = (...args) => put('/agriculturalMachineryType', ...args)
export const removeAgriculturalMachineryTypeById = (id, ...rest) => del(`/agriculturalMachineryType/${encodeURIComponent(id)}`, ...rest)
export const postAgriculturalMachineryTypeAdd = (...args) => post('/agriculturalMachineryType/add', ...args)
export const getAgriculturalMachineryTypeList = (...args) => get('/agriculturalMachineryType/list', ...args)

export const machineryApi = {
  putAgriculturalMachinery,
  removeAgriculturalMachineryById,
  postAgriculturalMachineryAdd,
  postAgriculturalMachineryList,
  removeAgriculturalMachinerySignOutByUsername,
  putAgriculturalMachineryType,
  removeAgriculturalMachineryTypeById,
  postAgriculturalMachineryTypeAdd,
  getAgriculturalMachineryTypeList,
}
export default machineryApi
