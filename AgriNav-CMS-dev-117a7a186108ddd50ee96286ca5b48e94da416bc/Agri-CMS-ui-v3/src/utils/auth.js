// Token 管理统一委托给 utils/request.js，保证全站只有一个 Admin-Token 存储位置。
// 旧代码继续从本文件 import，无需改动业务组件。
import { getToken as readToken, setToken as writeToken } from '@/utils/request'

export const getToken = readToken
export const setToken = writeToken
export const removeToken = () => writeToken('')
