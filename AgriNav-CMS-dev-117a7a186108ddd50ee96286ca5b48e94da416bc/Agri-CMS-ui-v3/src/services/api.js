/**
 * 统一数据访问层
 * ---------------------------------------------------------------
 * 真实数据（地块 / 农机 / 作业 / 结算 / 报表 / 大屏）-> 老版本若依后端
 * 模拟数据（AI 智能体 / 工具 / 工作流 / 知识库 / 审计）-> 保留 mock
 *
 * 所有函数的名称、参数、返回结构都与原版保持一致，页面无需任何改动。
 * 开关：VITE_USE_REAL_API=true 走真实接口；失败时按 VITE_FALLBACK_MOCK 回退 mock。
 */
import { useAppStore } from '@/stores/app'
import * as db from '@/mock'
import * as remote from './remote'
import {
  landFromBackend, machineFromBackend, workorderFromBackend,
  statementFromBackend, priceRuleFromBackend, farmWorkTaskFromBackend,
  settlementItemFromBackend,
  reviewRequestFromBackend,
  rowsOf, pageOf, toStringId, toStringIdList, toLongIdList, trajectoryPointsFromBackend
} from '@/utils/adapter'

const USE_REAL = String(import.meta.env.VITE_USE_REAL_API ?? 'true') !== 'false'
const FALLBACK = String(import.meta.env.VITE_FALLBACK_MOCK ?? 'true') !== 'false'

const wait = (ms = 260) => new Promise((r) => setTimeout(r, ms))
const clone = (v) => JSON.parse(JSON.stringify(v))
const str = (v, d = '') => (v === undefined || v === null ? d : String(v))

function scope() {
  const store = useAppStore()
  return { role: store.role, district: store.role === 'leader' ? '河东片区' : null }
}
function byDistrict(list, key = 'district') {
  const { district } = scope()
  return district ? list.filter((i) => i[key] === district) : list
}

/** 真实接口优先，失败按配置回退 mock */
async function via(realFn, mockFn) {
  if (USE_REAL) {
    try {
      return await realFn()
    } catch (e) {
      if (!FALLBACK) throw e
      console.warn('[api] 真实接口失败，已回退 mock：', e.message)
    }
  }
  return mockFn()
}

const matchText = (v, k) => String(v ?? '').includes(k)

/* ================= 驾驶舱 ================= */
export async function getDashboard() {
  const mockFn = async () => {
    await wait()
    const tasks = byDistrict(clone(db.tasks))
    const machines = byDistrict(clone(db.machines))
    const online = machines.filter((m) => m.online).length
    return {
      stats: {
        ...db.org.stats,
        todayTasks: tasks.length * 3 + 6,
        doneTasks: tasks.filter((t) => t.status === '已完成').length * 3 + 18,
        pendingTasks: tasks.filter((t) => ['待派单', '待接单', '待执行'].includes(t.status)).length * 2 + 3,
        onlineMachines: online,
        totalMachines: machines.length,
        onlineRate: +((online / machines.length) * 100).toFixed(1)
      },
      insights: clone(db.aiInsights), activities: clone(db.activities), heatPoints: clone(db.heatPoints),
      machines, lands: byDistrict(clone(db.lands)), tasks,
      districtRank: clone(db.reportData.district).sort((a, b) => b.rate - a.rate),
      machineRank: clone(db.reportData.machineEff).slice(0, 5),
      trend: clone(db.reportData.trend7)
    }
  }
  const realFn = async () => {
    // 基础业务数据 + 文档中的真实统计接口（/screen/*、/analysis/*）
    const [landRes, machineRes, workRes, heatRes, plotMonth, trajMonth, finishedRate, activeRate, screenStat, mostCrop, mostProvince] = await Promise.all([
      remote.remoteLandList(),
      remote.remoteMachineList(),
      remote.remoteEffectiveTrajectory({}),
      remote.remoteLandHeatmap().catch(() => null),
      remote.remoteScreen.landPlotNum().catch(() => null),
      remote.remoteScreen.trajectory().catch(() => null),
      remote.remoteScreen.farmWork().catch(() => null),
      remote.remoteScreen.activeUser().catch(() => null),
      remote.remoteScreen.statistics().catch(() => null),
      remote.remoteScreen.mostCrop().catch(() => null),
      remote.remoteScreen.mostFarmWorkProvince().catch(() => null)
    ])
    const lands = byDistrict(rowsOf(landRes).map(landFromBackend))
    const machines = byDistrict(rowsOf(machineRes).map(machineFromBackend))
    const works = rowsOf(workRes).map(workorderFromBackend)
    const online = machines.filter((m) => m.online).length
    const heatPoints = heatRes
      ? rowsOf(heatRes).map((h) => [h.longitude ?? h.lng, h.latitude ?? h.lat, Number(h.weight ?? h.count ?? 0.5)])
      : []

    // 月度序列：接口返回 [{name, value}]，转成图表需要的 {days, area, tasks, abnormal}
    const monthRows = (p) => rowsOf(p).map((x) => ({ name: str(x.name ?? x.month), value: Number(x.value ?? x.count ?? 0) }))
    const plotRows = monthRows(plotMonth)
    const trajRows = monthRows(trajMonth)
    const trendFallback = clone(db.reportData.trend7)
    const trend = plotRows.length || trajRows.length
      ? {
          days: (plotRows.length ? plotRows : trajRows).map((x) => x.name),
          area: plotRows.length ? plotRows.map((x) => x.value) : trendFallback.area,
          tasks: trajRows.length ? trajRows.map((x) => x.value) : trendFallback.tasks,
          abnormal: trendFallback.abnormal
        }
      : trendFallback

    const pick = (p, keys, fallback) => {
      const d = p?.data ?? p ?? {}
      for (const k of keys) if (d[k] !== undefined && d[k] !== null) return d[k]
      return fallback
    }
    const completion = pick(finishedRate, ['percent', 'value', 'data'], db.org.stats.completionRate)
    const activePercent = pick(activeRate, ['percent', 'value', 'data'], db.org.stats.onlineRate)
    const statData = screenStat?.data ?? screenStat ?? {}

    return {
      stats: {
        ...db.org.stats,
        totalArea: +lands.reduce((s, l) => s + l.area, 0).toFixed(1),
        activeLands: lands.length,
        todayTasks: works.length,
        doneTasks: works.filter((w) => ['已完成', '已复核', '已结算'].includes(w.status)).length,
        pendingTasks: works.filter((w) => ['待核算', '待复核'].includes(w.status)).length,
        onlineMachines: online,
        totalMachines: machines.length,
        onlineRate: machines.length ? +((online / machines.length) * 100).toFixed(1) : Number(activePercent) || 0,
        completionRate: Number(completion) || db.org.stats.completionRate,
        mostCrop: mostCrop?.data?.workName || mostCrop?.workName || '-',
        mostFarmWorkProvince: mostProvince?.data?.name || mostProvince?.name || '-',
        screenTotal: statData.landPlotCount ?? statData.totalLandPlot ?? statData.total ?? null
      },
      insights: clone(db.aiInsights), activities: clone(db.activities), heatPoints,
      machines, lands, tasks: clone(db.tasks),
      districtRank: clone(db.reportData.district).sort((a, b) => b.rate - a.rate),
      machineRank: clone(db.reportData.machineEff).slice(0, 5),
      trend
    }
  }
  return via(realFn, mockFn)
}

/* ================= 地块 ================= */
export async function listLands(params = {}) {
  const mockFn = async () => {
    await wait(200)
    let list = byDistrict(clone(db.lands))
    if (params.district && params.district !== '全部') list = list.filter((l) => l.district === params.district)
    if (params.status && params.status !== '全部') list = list.filter((l) => l.status === params.status)
    if (params.crop && params.crop !== '全部') list = list.filter((l) => l.crop === params.crop)
    if (params.keyword) {
      const k = params.keyword.trim()
      list = list.filter((l) => matchText(l.name, k) || matchText(l.code, k) || matchText(l.village, k))
    }
    return list
  }
  const realFn = async () => {
    const query = {}
    if (params.district && params.district !== '全部') query.address = params.district
    const payload = params.keyword ? await remote.remoteLandQuery({ ...query, landName: params.keyword }) : await remote.remoteLandList(query)
    let list = rowsOf(payload).map(landFromBackend)
    if (params.status && params.status !== '全部') list = list.filter((l) => l.status === params.status)
    if (params.crop && params.crop !== '全部') list = list.filter((l) => l.crop === params.crop)
    return byDistrict(list)
  }
  return via(realFn, mockFn)
}

export function getLand(id) {
  const mockFn = () => {
    const item = db.lands.find((l) => l.id === id)
    return Promise.resolve(item ? clone(item) : null)
  }
  const realFn = async () => landFromBackend(await remote.remoteLandGet(id), 0)
  return via(realFn, mockFn)
}

/**
 * 地块矢量导入
 * 后端现状：无可用导入接口。LandPlotController 中不存在 /landPlot/import/geojson、
 * /landPlot/import/shapefile（前端 04-land.js 有定义但后端未实现）。
 * 因此这里不再伪造“真实接口成功”，直接抛出后端待补充错误，避免页面展示虚假成功。
 */
export async function importLands(count = 3) {
  throw new Error('地块导入：后端待补充（缺少 /landPlot/import/* 接口，暂不支持矢量文件导入）')
}

/* 拆分 / 合并：后端 LandPlotController 无对应端点，属“后端待补充”，见 S4 报告第 5 节 */
export async function splitLand(id, parts = 2) {
  throw new Error('地块拆分：后端待补充（缺少拆分接口）')
}
export async function mergeLands(ids = []) {
  throw new Error('地块合并：后端待补充（缺少合并接口）')
}

/* ================= 农机 / 机手 ================= */
export async function listMachines(params = {}) {
  const mockFn = async () => {
    await wait(200)
    let list = byDistrict(clone(db.machines))
    if (params.district && params.district !== '全部') list = list.filter((m) => m.district === params.district)
    if (params.type && params.type !== '全部') list = list.filter((m) => m.type === params.type)
    if (params.status && params.status !== '全部') list = list.filter((m) => m.workStatus === params.status)
    if (params.keyword) list = list.filter((m) => matchText(m.id, params.keyword) || matchText(m.model, params.keyword) || matchText(m.operatorName, params.keyword))
    return list
  }
  const realFn = async () => {
    // 后端 POST /agriculturalMachinery/list 接受的筛选：name / typeId / status(0|1) /
    // phoneNumber / location / cropId / havingGrainCarrier / myAgriculturalMachinery。
    // 注意：前端 type 是“拖拉机/收割机”文案，后端 typeId 是类型表主键，语义不同不可直传；
    //       前端 workStatus 是作业态文案，后端 status 只有 0(上架)/1(下架)，也不可直传。
    // 因此这里只把「关键字」下推为后端 name 模糊查询，其余仍在结果集内前端过滤。
    const body = {}
    if (params.keyword) body.name = params.keyword
    const payload = await remote.remoteMachineList(body)
    let list = rowsOf(payload).map(machineFromBackend)
    if (params.district && params.district !== '全部') list = list.filter((m) => m.district === params.district)
    if (params.type && params.type !== '全部') list = list.filter((m) => m.type === params.type)
    if (params.status && params.status !== '全部') list = list.filter((m) => m.workStatus === params.status)
    if (params.keyword) {
      const k = params.keyword
      // 后端已按 name 过滤，这里再兜住编号/机手，保证与 mock 分支行为一致
      list = list.filter((m) => matchText(m.id, k) || matchText(m.model, k) || matchText(m.operatorName, k) || matchText(m.code, k))
    }
    return byDistrict(list)
  }
  return via(realFn, mockFn)
}

export async function listOperators(params = {}) {
  const mockFn = async () => {
    await wait(200)
    let list = byDistrict(clone(db.operators))
    if (params.keyword) list = list.filter((o) => matchText(o.name, params.keyword) || matchText(o.phone, params.keyword))
    return list
  }
  // 后端无农机手独立接口（AgriculturalMachineryController 只有农机找活，无机手档案）。
  // 原实现无条件 return mockFn()，即使 VITE_FALLBACK_MOCK=false 也会静默返回假数据；
  // 现改为：开启真实接口且禁止回退时抛错，由页面提示“后端待补充”，不再伪造机手档案。
  if (USE_REAL && !FALLBACK) {
    throw new Error('农机手档案：后端待补充（无农机手档案接口）')
  }
  return mockFn()
}

export async function getMachine(id) {
  const mockFn = async () => {
    await wait(150)
    const machine = db.machines.find((m) => m.id === id)
    if (!machine) return null
    return { ...clone(machine), records: db.workorders.filter((w) => w.machineId === id).map((w) => ({ id: w.id, landName: w.landName, date: w.startAt.slice(0, 10), area: w.effectiveArea, status: w.status })) }
  }
  const realFn = async () => {
    // 后端 AgriculturalMachineryController 无任何 @GetMapping，不存在 GET /agriculturalMachinery/{id}。
    // 原实现直连该路径必然 404，现改为复用 /agriculturalMachinery/list 结果按 id 匹配。
    const payload = await remote.remoteMachineList()
    const row = rowsOf(payload).find((m) => str(m.id) === str(id))
    if (!row) return null
    const machine = machineFromBackend(row, 0)
    // 后端无 machineId 关联字段（TrajectoryInfo 只存 phoneNumber），历史作业无真实来源
    return { ...machine, records: [] }
  }
  return via(realFn, mockFn)
}

/* ================= 任务 / 调度 =================
 *
 * 后端现状（已逐条核对 Controller 源码，见 S6 核查报告第 2 节）：
 *   - 全仓库 47 个 *Controller.java 中【没有 DispatchController】，grep 'dispatch' 命中 0 次，
 *     即 /dispatch、/dispatch/list、/dispatch/batch、/dispatch/stats、/dispatch/timeout 全部不存在。
 *     src/api/07-dispatch.js 里那 9 个函数是依据文档生成的，后端无对应实现。
 *   - 可承载“待派单作业任务”的真实接口只有 FarmWorkController（/farmWork）：
 *       POST   /farmWork/list   查询（body=FarmWorkDTO，query=pageNum/pageSize，返回 {data, total}）
 *       POST   /farmWork/add    新增（@Valid FarmWorkDTO）
 *       PUT    /farmWork        更新（@Valid FarmWorkDTO）
 *       DELETE /farmWork/{id}   下架（status -> NO_LONGER_AVAILABLE=1）
 *   - FarmWorkVO 中【没有】machineId / operatorId / progress / priority 等调度字段，
 *     因此“派给哪台农机、哪个机手、作业进度、优先级”后端无任何存储与接口。
 *
 * 据此本次改造的原则：
 *   1) 列表/新增/修改/撤回 四个动作是后端真实能力，全部接真接口，不再 setTimeout 假成功；
 *   2) 批量派单、AI 调度建议 后端确实没有能力，显式抛出“后端待补充”，不伪造成功；
 *   3) 所有响应统一返回 { list, total }，total 仅在页面显式声明需要分页时使用，
 *      其余调用方（监控大屏 / 助手）取 .list，行为与改造前兼容。
 */

/** 后端 FarmWork status 常量（Constants.AVAILABLE / NO_LONGER_AVAILABLE；list 已排除 DELETE=2） */
export const FARM_WORK_STATUS = {
  AVAILABLE: 0,          // 可接单
  NO_LONGER_AVAILABLE: 1 // 已结束（到期自动结束，或被撤回下架）
}

/** 把页面/其他模块传来的 status 归一成后端 Integer；无法识别时返回 undefined（不下发） */
function toFarmWorkStatus(status) {
  if (status === undefined || status === null || status === '' || status === '全部') return undefined
  const n = Number(status)
  if (Number.isFinite(n) && (n === 0 || n === 1 || n === 2)) return n
  // 兼容旧文案
  if (status === '可接单' || status === '待接单') return FARM_WORK_STATUS.AVAILABLE
  if (status === '已结束' || status === '已完成') return FARM_WORK_STATUS.NO_LONGER_AVAILABLE
  return undefined
}

/**
 * 列表参数 -> FarmWorkDTO。
 * 后端只支持这些条件：name(like) / status(eq) / minArea(ge) / maxArea(le) /
 * endTime(le) / myFarmWork(eq createBy) / orderByCreateTimeDesc /
 * landPlotId[] / cropId[] / workTypeId[] / location[]。
 * 注意：后端【没有】关键字搜索多字段、也没有 machineId/operatorId 过滤，不能凭空构造。
 */
function buildFarmWorkQuery(params = {}) {
  const body = {
    orderByCreateTimeDesc: params.orderByCreateTimeDesc === undefined ? true : !!params.orderByCreateTimeDesc
  }
  // 关键字只能下推到 name（后端唯一支持模糊匹配的文本字段）
  if (params.keyword && String(params.keyword).trim()) body.name = String(params.keyword).trim()
  if (params.name && String(params.name).trim()) body.name = String(params.name).trim()
  const status = toFarmWorkStatus(params.status)
  if (status !== undefined) body.status = status
  if (params.minArea !== undefined && params.minArea !== null && params.minArea !== '') body.minArea = params.minArea
  if (params.maxArea !== undefined && params.maxArea !== null && params.maxArea !== '') body.maxArea = params.maxArea
  if (params.endTime) body.endTime = params.endTime
  if (params.myFarmWork !== undefined && params.myFarmWork !== null) body.myFarmWork = !!params.myFarmWork
  // 查询条件仅在数组非空时下发；ID 类型与非法值在统一适配层处理。
  const pushArray = (key, normalize) => {
    const values = normalize(params[key])
    if (values.length) body[key] = values
  }
  pushArray('landPlotId', toStringIdList)
  pushArray('cropId', toLongIdList)
  pushArray('workTypeId', toLongIdList)
  if (Array.isArray(params.location) && params.location.length) body.location = params.location
  return body
}

/**
 * 查询调度任务列表（真实数据源：POST /farmWork/list）
 * 返回 { list, total }；total 只在后端真的给了 total 时才是数字，否则为 null。
 */
export async function listTasks(params = {}) {
  const mockFn = async () => {
    await wait(200)
    let list = byDistrict(clone(db.tasks))
    if (params.status && params.status !== '全部') list = list.filter((t) => t.status === params.status)
    if (params.type && params.type !== '全部') list = list.filter((t) => t.type === params.type)
    if (params.keyword) list = list.filter((t) => matchText(t.id, params.keyword) || matchText(t.landName, params.keyword) || matchText(t.operatorName, params.keyword))
    // mock 分支无真实分页，明确返回 total=null，避免页面把 length 当总数
    return { list, total: null }
  }
  const realFn = async () => {
    const pageNum = Number(params.pageNum) > 0 ? Number(params.pageNum) : 1
    // 页面未显式分页时取一个足够大的页，保持“一次拿全量”的既有行为
    const pageSize = Number(params.pageSize) > 0 ? Number(params.pageSize) : 1000
    const body = buildFarmWorkQuery(params)
    const payload = await remote.remoteFarmWorkList({ ...body, pageNum, pageSize })
    const { list, total } = pageOf(payload)
    let rows = list.map(farmWorkTaskFromBackend)
    // 前端对后端不支持的条件做结果集内过滤（与 mock 分支语义保持一致）
    if (params.type && params.type !== '全部') {
      rows = rows.filter((t) => t.workTypeName.includes(params.type))
    }
    if (params.keyword) {
      const k = String(params.keyword).trim()
      // 后端已按 name 过滤，这里再兜住 id / 地块名，不涉无人机手（后端无该字段）
      rows = rows.filter((t) => matchText(t.id, k) || t.name.includes(k) || t.landPlotName.some((n) => matchText(n, k)))
    }
    return { list: rows, total }
  }
  return via(realFn, mockFn)
}

/**
 * 列表查询的兼容包装：返回数组。
 * 助手 assistantEngine.js 等历史调用方按数组消费，这里提供数组版本。
 */
export async function listTaskRows(params = {}) {
  const { list } = await listTasks(params)
  return list
}

/**
 * 撤回 / 下架任务 -> DELETE /farmWork/{id}
 * 后端行为：status 置为 NO_LONGER_AVAILABLE(1)，是软下架，不做物理删除。
 * 因此这里明确返回“已下架”语义，不再说“已撤销并通知农机手”（后端没有通知能力）。
 */
export async function revokeTask(id) {
  const taskId = toStringId(id)
  if (!taskId) throw new Error('撤回任务失败：缺少合法的任务 id')
  const mockFn = async () => {
    await wait(400)
    return { id: taskId, message: '任务已下架（模拟数据）' }
  }
  const realFn = async () => {
    await remote.remoteFarmWorkDelete(taskId)
    return { id: taskId, message: '任务已下架：状态置为「已结束」，列表不再可接单' }
  }
  return via(realFn, mockFn)
}

/**
 * 批量派单：后端【没有】任何批量派单接口（无 DispatchController，FarmWork 也无批量状态变更端点）。
 * 原实现 setTimeout 后返回“已向 N 个任务下发派单”，属于无请求的假成功，本次移除。
 */
export async function batchDispatch(ids = []) {
  throw new Error('批量派单：后端待补充（不存在 /dispatch/batch 及任何批量派单接口，无法下发至农机手）')
}

/**
 * AI 调度建议：后端【没有】推荐/建议类接口，也没有机器学习调度能力。
 * 原实现无条件返回 3 条硬编码建议（db.dispatchSuggestions），本次改为显式空能力。
 */
export async function getDispatchSuggestions() {
  throw new Error('AI 调度建议：后端待补充（无调度建议/推荐接口，无法基于真实距离与效率生成建议）')
}

/**
 * 新建调度任务 -> POST /farmWork/add
 * 必填（后端 @Valid）：name / area / endTime / phoneNumber
 * 后端还有一条业务校验：endTime 必须晚于当前时间，否则返回 code=500「截止时间不得小于当前时间」。
 * 注意：后端不接收 machineId / operatorId / priority / note，提交它们不会报错但也不会被保存，
 * 因此这里显式不发送，避免“看起来派了农机其实没存”。
 */
export async function createTask(payload = {}) {
  const body = {
    name: payload.name,
    area: payload.area,
    endTime: payload.endTime,
    phoneNumber: payload.phoneNumber,
    landPlotId: toStringIdList(payload.landPlotId),
    cropId: toLongIdList(payload.cropId),
    workTypeId: toLongIdList(payload.workTypeId)
  }
  const mockFn = async () => {
    await wait(500)
    return { id: `RW-20261123-${String(100 + Math.floor(Math.random() * 800))}`, ...payload }
  }
  const realFn = async () => {
    // 后端 add 只返回 AjaxResult.success()，不回传新 id；新增后需重新查询列表才能拿到真实 id
    await remote.remoteFarmWorkAdd(body)
    return { ok: true, message: '作业需求已发布' }
  }
  return via(realFn, mockFn)
}

/**
 * 修改调度任务 -> PUT /farmWork
 * 同样要求 name / area / endTime / phoneNumber 齐全，并带上 id。
 * 用于“调整截止时间 / 面积 / 联系方式”等后端真实支持的字段。
 */
export async function updateTask(payload = {}) {
  const id = toStringId(payload.id)
  if (!id) throw new Error('修改任务失败：缺少合法的任务 id')
  const body = {
    id,
    name: payload.name,
    area: payload.area,
    endTime: payload.endTime,
    phoneNumber: payload.phoneNumber,
    // 更新会删除并重建关联，空数组必须保留，既支持清空关联，也避免 DTO 对 null 执行 forEach。
    landPlotId: toStringIdList(payload.landPlotId),
    cropId: toLongIdList(payload.cropId),
    workTypeId: toLongIdList(payload.workTypeId)
  }
  const mockFn = async () => {
    await wait(400)
    return { ...payload }
  }
  const realFn = async () => {
    await remote.remoteFarmWorkUpdate(body)
    return { ok: true, message: '作业需求已更新' }
  }
  return via(realFn, mockFn)
}

/**
 * 作业类型字典 -> GET /fieldWork/listUnique（WorkType：id / workName）
 * 页面原先从 @/mock 的 org.workTypes 取 5 个写死的文案，本次改为真实字典。
 * 该接口带 @PreAuthorize('field:work:check')，无权限时后端返回 code=403，页面需给出空态提示。
 */
export async function listWorkTypes() {
  const mockFn = async () => {
    await wait(120)
    return db.org.workTypes.map((name, i) => ({ id: `MOCK-WT-${i + 1}`, name }))
  }
  const realFn = async () => {
    const payload = await remote.remoteWorkTypeList()
    return rowsOf(payload).map((r) => ({
      id: toStringId(r.id),
      name: r.workName || r.name || ''
    })).filter((w) => w.id && w.name)
  }
  return via(realFn, mockFn)
}

/* ================= 作业单 / 轨迹 ================= */
export async function listWorkorders(params = {}) {
  const mockFn = async () => {
    await wait(220)
    let list = byDistrict(clone(db.workorders))
    if (params.status && params.status !== '全部') list = list.filter((w) => w.status === params.status)
    if (params.district && params.district !== '全部') list = list.filter((w) => w.district === params.district)
    if (params.keyword) list = list.filter((w) => matchText(w.id, params.keyword) || matchText(w.landName, params.keyword) || matchText(w.operatorName, params.keyword))
    return list
  }
  const realFn = async () => {
    const query = {
      pageNum: Number(params.pageNum) > 0 ? Number(params.pageNum) : 1,
      pageSize: Number(params.pageSize) > 0 ? Number(params.pageSize) : 1000
    }
    for (const key of ['localDate', 'beginTime', 'endTime', 'workType', 'address', 'phone']) {
      if (params[key] !== undefined && params[key] !== null && params[key] !== '') query[key] = params[key]
    }
    const payload = await remote.remoteEffectiveTrajectory(query)
    let list = rowsOf(payload).map(workorderFromBackend)
    if (params.status && params.status !== '全部') list = list.filter((w) => w.status === params.status)
    if (params.district && params.district !== '全部') list = list.filter((w) => w.district === params.district)
    if (params.keyword) list = list.filter((w) => matchText(w.id, params.keyword) || matchText(w.landName, params.keyword) || matchText(w.operatorName, params.keyword))
    return byDistrict(list)
  }
  return via(realFn, mockFn)
}

export async function getWorkorder(id, preloadedList = null) {
  if (preloadedList && Array.isArray(preloadedList)) {
    const hit = preloadedList.find((w) => w.id === id)
    if (hit) return hit
  }
  const mockFn = async () => {
    await wait(180)
    const item = db.workorders.find((w) => w.id === id)
    return item ? clone(item) : null
  }
  const realFn = async () => {
    const list = rowsOf(await remote.remoteEffectiveTrajectory({})).map(workorderFromBackend)
    return list.find((w) => w.id === id) || null
  }
  return via(realFn, mockFn)
}

/* ================= 复核申请 ================= */
export async function listReviewRequests(params = {}) {
  const realFn = async () => {
    const payload = await remote.remoteReviewList(params)
    return rowsOf(payload).map(reviewRequestFromBackend)
  }
  // 复核无 mock 兜底；空数组代表无复核申请。
  if (!import.meta.env.VITE_USE_REAL_API) return []
  return realFn()
}

/**
 * 对指定轨迹完整走复核流程。
 * 后端流程（ReviewRequestServiceImpl）：
 *   1. POST /reviewRequest 提交申请（body 带 trajectoryInfoId，后端自动回填轨迹信息）
 *   2. PUT  /reviewRequest/initial  队长初核（param: ReviewRequest.id / pass / comment）
 *   3. PUT  /reviewRequest/final    管理员终审（同上）
 *
 * 当前页面定位为"队长初核通过"与"驳回退回"两个动作，因此默认走 initial 分支。
 * 若该轨迹尚未存在复核申请，则先提交一条初核申请，再立即执行初核。
 */
export async function reviewWorkorder(trajectoryInfoId, action, note = '') {
  const pass = action === 'pass'
  // 先查复核单是否已存在
  let reviewId = null
  let reviewList = []
  try { reviewList = await listReviewRequests({ pageNum: 1, pageSize: 1000 }) } catch { reviewList = [] }
  const hit = reviewList.find((r) => r.trajectoryInfoId === trajectoryInfoId)
  if (hit) {
    reviewId = hit.id
  } else {
    // 不存在 → 提交初核申请
    const submitPayload = await remote.remoteReviewSubmit({
      trajectoryInfoId,
      reason: note || (pass ? '队长审核通过' : '队长审核驳回')
    })
    // 后端 submit 返回空 success，没有直接返回复核单主键；重新查一次
    reviewList = await listReviewRequests({ pageNum: 1, pageSize: 1000 })
    const fresh = reviewList.find((r) => r.trajectoryInfoId === trajectoryInfoId)
    if (!fresh) throw new Error('复核申请已提交但未查询到对应复核单')
    reviewId = fresh.id
  }
  // 执行初核（队长角色）
  await remote.remoteReviewInitial(reviewId, pass, note || (pass ? '队长审核通过' : '队长审核驳回'))
  return { id: trajectoryInfoId, reviewId, action, note, message: pass ? '初核通过，已进入终审队列' : '已驳回并退回片区重新处理' }
}

/** 原始轨迹只使用真实接口；无数据返回空轨迹，不生成模拟点位。 */
export async function getOriginalTrajectory({ phone, startTime, endTime } = {}) {
  if (!phone || !startTime || !endTime) {
    throw new Error('查询原始轨迹需要手机号、开始时间和结束时间')
  }
  const payload = await remote.remoteOriginalTrajectory({ phone, startTime, endTime })
  return trajectoryPointsFromBackend(rowsOf(payload))
}

/* ================= 结算 ================= */
/**
 * 结算列表筛选：后端 SettlementServiceImpl.selectList 支持
 *   settlementNo LIKE、phoneNumber EQ、periodType EQ、status EQ。
 * 返回 TableDataInfo (List + total)；periodType=1 月 / 2 季；status 0 已生成 / 1 已确认 / 2 已付款。
 */
export async function listStatements(params = {}) {
  const mockFn = async () => {
    await wait(220)
    let list = clone(db.statements)
    if (params.status !== undefined && params.status !== null && params.status !== '') {
      const raw = list.map((s) => s.statusRaw)
      list = list.filter((s) => (s.statusRaw ?? s.status) === Number(params.status))
    }
    if (params.keyword) {
      const k = params.keyword.trim()
      list = list.filter((s) => s.id.includes(k) || s.operatorName.includes(k) || s.phoneNumber.includes(k))
    }
    return list
  }
  const realFn = async () => {
    const query = {}
    if (params.settlementNo) query.settlementNo = params.settlementNo
    if (params.phoneNumber) query.phoneNumber = params.phoneNumber
    if (params.periodType !== undefined && params.periodType !== null && params.periodType !== '') query.periodType = Number(params.periodType)
    if (params.status !== undefined && params.status !== null && params.status !== '') query.status = Number(params.status)
    const payload = await remote.remoteSettlementList(query)
    return rowsOf(payload).map(statementFromBackend)
  }
  return via(realFn, mockFn)
}

/** 结算单详情：后端 GET /settlement/{id}，参数为结算单 Long 主键（已序列化为字符串） */
export async function getStatement(id, preloadedList = null) {
  if (preloadedList && Array.isArray(preloadedList)) {
    // 优先用已经加载的列表数据；避免每次重扫一遍全量
    const hit = preloadedList.find((s) => s.id === id || String(s.raw?.id) === String(id))
    if (hit) return hit
  }
  const mockFn = async () => {
    await wait(160)
    const item = db.statements.find((s) => s.id === id)
    return item ? clone(item) : null
  }
  const realFn = async () => {
    // 用 settlementNo 或 Long 主键去查独立详情接口
    const payload = await remote.remoteSettlementDetail(id)
    const item = rowsOf(payload)[0] || (payload && typeof payload === 'object' && !Array.isArray(payload) ? payload : null)
    return item ? statementFromBackend(item) : null
  }
  return via(realFn, mockFn)
}

/** 结算明细：后端 GET /settlement/items/{id}，参数结算单 Long 主键 */
export async function getStatementItems(id) {
  const mockFn = async () => {
    await wait(140)
    return clone(db.statementItems || [])
  }
  const realFn = async () => {
    const payload = await remote.remoteSettlementItems(id)
    return rowsOf(payload).map(settlementItemFromBackend)
  }
  return via(realFn, mockFn)
}

/**
 * 生成结算单：后端 POST /settlement/generate
 *   4 个 RequestParam 都是必填：
 *     periodType(Short: 1 月 / 2 季)
 *     periodStart(LocalDate ISO 格式 yyyy-MM-dd)
 *     periodEnd(LocalDate)
 *     phoneNumber(机手手机号)
 * 后端会：
 *   1. 校验周期 + 手机号，防重复生成（同机手 + 同周期唯一）
 *   2. 查周期内该机手 status=0 的有效轨迹 TrajectoryInfo
 *   3. 按轨迹 workType + 启用 PricingRule 匹配单价
 *   4. 生成本次 Settlement + 多条 SettlementItem
 * 返回生成后的 Settlement 实体。
 */
export async function generateStatements({ periodType, periodStart, periodEnd, phoneNumber } = {}) {
  if (!phoneNumber) throw new Error('机手手机号必填')
  if (!periodType) throw new Error('结算周期类型必填')
  if (!periodStart || !periodEnd) throw new Error('结算周期起止日期必填')
  const mockFn = async () => {
    await wait(900)
    return { periodType, periodStart, periodEnd, phoneNumber, count: 3, amount: 128640, message: '已生成 3 份结算单（演示）' }
  }
  const realFn = async () => {
    const payload = await remote.remoteSettlementGenerate({ periodType: Number(periodType), periodStart, periodEnd, phoneNumber })
    const result = payload?.data || payload
    const period = `${periodStart} ~ ${periodEnd}`
    const count = 1
    const amount = result?.totalAmount ?? 0
    return { period, periodType, periodStart, periodEnd, phoneNumber, count, amount, message: `已为 ${phoneNumber} 生成 ${period} 结算单` }
  }
  return via(realFn, mockFn)
}

/**
 * 结算状态更新：后端 PUT /settlement/status
 *   RequestParam id(结算单 Long 主键) + status(Short: 0=已生成 / 1=已确认 / 2=已付款)
 */
export async function confirmStatement(id, status) {
  const mockFn = async () => { await wait(300); return { id, status } }
  const realFn = async () => {
    await remote.remoteSettlementConfirm(id, Number(status))
    return { id, status }
  }
  return via(realFn, mockFn)
}

/**
 * 单价规则列表：后端 GET /pricingRule/list
 * 扣减规则（deductRules）后端无对应表，前端不再伪造，改为空数组。
 */
export async function listPriceRules() {
  const mockFn = async () => { await wait(180); return { rules: clone(db.priceRules), deductions: [] } }
  const realFn = async () => ({ rules: rowsOf(await remote.remotePriceRuleList()).map(priceRuleFromBackend), deductions: [] })
  return via(realFn, mockFn)
}

/**
 * 更新单价规则：后端 PUT /pricingRule @RequestBody PricingRule
 * PricingRule 实体包含：id, workType(Long), minArea, maxArea, unitPrice, grade, status(0 启用 / 1 停用)
 */
export async function updatePriceRule(rule) {
  const mockFn = async () => { await wait(400); return { id: rule?.id, message: '单价规则已更新（演示）' } }
  const realFn = async () => {
    await remote.remotePriceRuleUpdate(rule)
    return { id: rule?.id, message: '单价规则已更新，已记录审计日志' }
  }
  return via(realFn, mockFn)
}

/* ================= 报表 ================= */
export async function getReport(dimension = 'week') {
  const mockFn = async () => {
    await wait(350)
    const role = scope()
    let district = clone(db.reportData.district)
    if (role.district) district = district.filter((d) => d.name === role.district)
    return { ...clone(db.reportData), district, dimension }
  }
  const realFn = async () => {
    const [plots, traj, work, crop] = await Promise.all([
      remote.remoteScreen.landPlotNum().catch(() => null),
      remote.remoteScreen.trajectory().catch(() => null),
      remote.remoteScreen.farmWork().catch(() => null),
      remote.remoteScreen.statistics().catch(() => null)
    ])
    return {
      ...clone(db.reportData),
      district: clone(db.reportData.district),
      cardStats: { plots: plots?.data ?? null, trajectory: traj?.data ?? null, work: work?.data ?? null, crop: crop?.data ?? null },
      dimension
    }
  }
  return via(realFn, mockFn)
}

/* ================= 系统 / 权限 / 审计（后端无对应前端视图模型，保留 mock） ================= */
export async function getSystemData() {
  await wait(300)
  return {
    users: clone(db.users), roles: clone(db.roles), loginLogs: clone(db.loginLogs),
    operationLogs: clone(db.operationLogs), auditLogs: clone(db.auditLogs), alarms: clone(db.alarms)
  }
}

/* ================= Agent 中心（老后端无对应接口，全部保留 mock） ================= */
export async function getAgents() { await wait(220); return clone(db.agents) }
export async function saveAgent(agent) { await wait(420); return { ...agent, message: 'Agent 配置已保存' } }
export async function getTools() { await wait(200); return clone(db.tools) }
export async function testTool(toolId, params = '{}') {
  await wait(650)
  const tool = db.tools.find((t) => t.id === toolId)
  return {
    toolId,
    ok: toolId !== 'T05',
    code: toolId === 'T05' ? 504 : 200,
    latency: Math.round(120 + Math.random() * 380),
    request: params,
    response: toolId === 'T05'
      ? { error: 'UPSTREAM_TIMEOUT', message: '派单服务响应超时，已回滚，未写入正式数据' }
      : { code: 200, data: { source: tool?.module, scope: tool?.scope, count: Math.floor(Math.random() * 8) + 1, sample: tool?.result } }
  }
}
export async function getWorkflows() { await wait(200); return clone(db.workflows) }
export async function saveWorkflow(flow) { await wait(450); return { ...flow, message: '工作流已保存并发布' } }
export async function getKnowledge() { await wait(220); return { docs: clone(db.knowledgeDocs), chunks: clone(db.knowledgeChunks) } }
export async function searchKnowledge(query, role) {
  await wait(500)
  const q = query.toLowerCase()
  const hits = db.knowledgeChunks
    .filter((c) => c.roles.includes(role))
    .filter((c) => !q || c.text.includes(query) || c.doc.includes(query) || c.clause.includes(query) || ['结算', '周期', '面积', '扣减', '复核', '派单', '单价'].some((k) => query.includes(k)))
  return hits.slice(0, 3)
}
export async function getAgentMonitor() {
  await wait(260)
  return {
    agents: clone(db.agents), logs: clone(db.agentLogs), sessions: clone(db.sessions),
    totals: { calls: 5226, successRate: 98.2, avgLatency: 2.6, tokens: 3892000, toolCalls: 18642, failures: 58 }
  }
}
export async function getCockpitSessions() { await wait(150); return clone(db.sessions) }

export function quickSearch(keyword) {
  const k = (keyword || '').trim().toLowerCase()
  if (!k) return []
  return db.quickSearchIndex
    .filter((i) => i.name.toLowerCase().includes(k) || i.id.toLowerCase().includes(k) || i.meta.includes(k))
    .slice(0, 8)
}
