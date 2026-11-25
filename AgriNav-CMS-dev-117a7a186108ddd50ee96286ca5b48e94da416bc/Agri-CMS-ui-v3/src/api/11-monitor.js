/**
 * 11-监控运维（32 个接口）
 * 依据 AgriNav接口清单.md 自动生成，方法与路径逐条对应。
 */
import { get, post, put, del, patch } from '@/utils/request'

export const getMonitorCache = (...args) => get('/monitor/cache', ...args)
export const removeMonitorCacheClearCacheAll = (...args) => del('/monitor/cache/clearCacheAll', ...args)
export const removeMonitorCacheClearCacheKeyByCacheKey = (cacheKey, ...rest) => del(`/monitor/cache/clearCacheKey/${encodeURIComponent(cacheKey)}`, ...rest)
export const removeMonitorCacheClearCacheNameByCacheName = (cacheName, ...rest) => del(`/monitor/cache/clearCacheName/${encodeURIComponent(cacheName)}`, ...rest)
export const getMonitorCacheGetKeysByCacheName = (cacheName, ...rest) => get(`/monitor/cache/getKeys/${encodeURIComponent(cacheName)}`, ...rest)
export const getMonitorCacheGetNames = (...args) => get('/monitor/cache/getNames', ...args)
export const getMonitorCacheGetValueByCacheNameAndCacheKey = (cacheName, cacheKey, ...rest) => get(`/monitor/cache/getValue/${encodeURIComponent(cacheName)}/${encodeURIComponent(cacheKey)}`, ...rest)
export const postMonitorJob = (...args) => post('/monitor/job', ...args)
export const putMonitorJob = (...args) => put('/monitor/job', ...args)
export const getMonitorJobByJobId = (jobId, ...rest) => get(`/monitor/job/${encodeURIComponent(jobId)}`, ...rest)
export const removeMonitorJobByJobIds = (jobIds, ...rest) => del(`/monitor/job/${encodeURIComponent(jobIds)}`, ...rest)
export const putMonitorJobChangeStatus = (...args) => put('/monitor/job/changeStatus', ...args)
export const postMonitorJobExport = (...args) => post('/monitor/job/export', ...args)
export const getMonitorJobList = (...args) => get('/monitor/job/list', ...args)
export const putMonitorJobRun = (...args) => put('/monitor/job/run', ...args)
export const getMonitorJobLogByJobLogId = (jobLogId, ...rest) => get(`/monitor/jobLog/${encodeURIComponent(jobLogId)}`, ...rest)
export const removeMonitorJobLogByJobLogIds = (jobLogIds, ...rest) => del(`/monitor/jobLog/${encodeURIComponent(jobLogIds)}`, ...rest)
export const removeMonitorJobLogClean = (...args) => del('/monitor/jobLog/clean', ...args)
export const postMonitorJobLogExport = (...args) => post('/monitor/jobLog/export', ...args)
export const getMonitorJobLogList = (...args) => get('/monitor/jobLog/list', ...args)
export const removeMonitorLogininforByInfoIds = (infoIds, ...rest) => del(`/monitor/logininfor/${encodeURIComponent(infoIds)}`, ...rest)
export const removeMonitorLogininforClean = (...args) => del('/monitor/logininfor/clean', ...args)
export const postMonitorLogininforExport = (...args) => post('/monitor/logininfor/export', ...args)
export const getMonitorLogininforList = (...args) => get('/monitor/logininfor/list', ...args)
export const getMonitorLogininforUnlockByUserName = (userName, ...rest) => get(`/monitor/logininfor/unlock/${encodeURIComponent(userName)}`, ...rest)
export const removeMonitorOnlineByTokenId = (tokenId, ...rest) => del(`/monitor/online/${encodeURIComponent(tokenId)}`, ...rest)
export const getMonitorOnlineList = (...args) => get('/monitor/online/list', ...args)
export const removeMonitorOperlogByOperIds = (operIds, ...rest) => del(`/monitor/operlog/${encodeURIComponent(operIds)}`, ...rest)
export const removeMonitorOperlogClean = (...args) => del('/monitor/operlog/clean', ...args)
export const postMonitorOperlogExport = (...args) => post('/monitor/operlog/export', ...args)
export const getMonitorOperlogList = (...args) => get('/monitor/operlog/list', ...args)
export const getMonitorServer = (...args) => get('/monitor/server', ...args)

export const monitorApi = {
  getMonitorCache,
  removeMonitorCacheClearCacheAll,
  removeMonitorCacheClearCacheKeyByCacheKey,
  removeMonitorCacheClearCacheNameByCacheName,
  getMonitorCacheGetKeysByCacheName,
  getMonitorCacheGetNames,
  getMonitorCacheGetValueByCacheNameAndCacheKey,
  postMonitorJob,
  putMonitorJob,
  getMonitorJobByJobId,
  removeMonitorJobByJobIds,
  putMonitorJobChangeStatus,
  postMonitorJobExport,
  getMonitorJobList,
  putMonitorJobRun,
  getMonitorJobLogByJobLogId,
  removeMonitorJobLogByJobLogIds,
  removeMonitorJobLogClean,
  postMonitorJobLogExport,
  getMonitorJobLogList,
  removeMonitorLogininforByInfoIds,
  removeMonitorLogininforClean,
  postMonitorLogininforExport,
  getMonitorLogininforList,
  getMonitorLogininforUnlockByUserName,
  removeMonitorOnlineByTokenId,
  getMonitorOnlineList,
  removeMonitorOperlogByOperIds,
  removeMonitorOperlogClean,
  postMonitorOperlogExport,
  getMonitorOperlogList,
  getMonitorServer,
}
export default monitorApi
