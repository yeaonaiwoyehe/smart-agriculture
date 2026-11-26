/**
 * 统一数据访问层
 * ---------------------------------------------------------------
 * 真实数据（地块 / 农机 / 作业 / 结算 / 报表 / 大屏）-> 老版本若依后端
 * 模拟数据（AI 智能体 / 工具 / 工作流 / 知识库 / 审计）-> 保留 mock
 *
 * 所有函数的名称、参数、返回结构都与原版保持一致，页面无需任何改动。
 * 开关：VITE_APP_USE_REAL_API=true 走真实接口；失败时按 VITE_APP_FALLBACK_MOCK 回退 mock。
 */
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import { isAdminUser } from '@/stores/permission'
import dispatchApi from '@/api/07-dispatch'
import { listUser as listUserApi } from '@/api/system/user'
import { listRole as listRoleApi } from '@/api/system/role'
import { list as listLogininforApi } from '@/api/monitor/logininfor'
import { list as listOperlogApi } from '@/api/monitor/operlog'
import { getPermissionAudit } from '@/api/system/permissionAudit'
import { getAgentAuditList, getAgentAuditById } from '@/api/09-agent'
import * as db from '@/mock'
import * as remote from './remote'
import {
  landFromBackend, machineFromBackend, workorderFromBackend,
  statementFromBackend, priceRuleFromBackend, farmWorkTaskFromBackend,
  settlementItemFromBackend,
  reviewRequestFromBackend, workDispatchFromBackend, optionalNum,
  rowsOf, pageOf, toStringId, toStringIdList, toLongIdList, trajectoryPointsFromBackend,
  sysUserFromBackend, sysRoleFromBackend, loginLogFromBackend,
  operationLogFromBackend, permissionAuditFromBackend, agentAuditLogFromBackend
} from '@/utils/adapter'

const USE_REAL = String(import.meta.env.VITE_APP_USE_REAL_API ?? 'true') !== 'false'
const FALLBACK = String(import.meta.env.VITE_APP_FALLBACK_MOCK ?? 'false') !== 'false'

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

/** Dashboard 模块异常 -> 可展示状态；保留原始原因，不回退 mock。 */
function dashboardError(err) {
  if (err?.status === 403 || err?.code === 403) {
    return { kind: 'forbidden', message: '当前账号没有访问该功能的权限。' }
  }
  if (err?.raw || /连接异常/.test(err?.message || '')) {
    return { kind: 'network', message: '网络或服务异常，请稍后重试。' }
  }
  return { kind: 'error', message: err?.payload?.msg || err?.message || '加载失败，请稍后重试。' }
}

/** 单模块请求隔离：一个模块失败不影响其他模块，但不吞掉错误原因。 */
async function dashboardCall(fn) {
  try {
    return { value: await fn(), error: null }
  } catch (err) {
    return { value: null, error: dashboardError(err) }
  }
}

function dashboardNumber(value) {
  if (value === null || value === undefined || value === '') return null
  const n = Number(value)
  return Number.isFinite(n) ? n : null
}

/**
 * 驾驶舱真实数据（S11.2-a）
 * 仅接入已有真实接口，全部移除 mock 打底与演示角色过滤：
 *   - 地块：GET /landPlot（后端仅返回当前账号创建的地块）
 *   - 农机：POST /agriculturalMachinery/list（管理员全量，其他账号本人）
 *   - 热力：GET /landPlot/heatmap
 *   - 地块总面积：GET /screen/getLandPlotsTotalArea
 *   - 作业完成率：GET /screen/getFinishedFarmWorkPercentage
 *   - 用户活跃率：GET /screen/getActiveUserPercentage
 * 各模块独立返回 { value/list, error }，页面据此展示 loading/空/403/错误。
 */
export async function getDashboard() {
  const [landRes, machineRes, heatRes, areaRes, completionRes, activeRes, agentAuditRes] = await Promise.all([
    dashboardCall(() => remote.remoteLandList()),
    dashboardCall(() => remote.remoteMachineList()),
    dashboardCall(() => remote.remoteLandHeatmap()),
    dashboardCall(() => remote.remoteScreen.totalArea()),
    dashboardCall(() => remote.remoteScreen.farmWork()),
    dashboardCall(() => remote.remoteScreen.activeUser()),
    // 仅取真实分页总数，pageSize=1 避免拉取无用明细；total 为后端 TableDataInfo 原值
    dashboardCall(() => listAgentAuditLogs({ pageNum: 1, pageSize: 1 }))
  ])

  const landRows = landRes.value ? rowsOf(landRes.value) : []
  const lands = landRows.map(landFromBackend)

  const machineRows = machineRes.value ? rowsOf(machineRes.value) : []
  const machines = machineRows.map(machineFromBackend)
  const machineTotal = dashboardNumber(machineRes.value?.total)
  const listedCount = machines.filter((m) => m.available).length

  const heatPoints = (heatRes.value ? rowsOf(heatRes.value) : [])
    .map((h) => {
      const lng = Number(h.longitude ?? h.lng)
      const lat = Number(h.latitude ?? h.lat)
      if (!Number.isFinite(lng) || !Number.isFinite(lat)) return null
      const weight = Number(h.weight ?? h.count ?? 1)
      return [lng, lat, Number.isFinite(weight) ? weight : 1]
    })
    .filter(Boolean)

  return {
    lands: {
      list: lands,
      // GET /landPlot 不返回 total，固定请求 1000 条；达到上限即视为可能不完整
      truncated: landRows.length >= 1000,
      error: landRes.error
    },
    machines: {
      list: machines,
      total: machineTotal ?? machines.length,
      listedCount,
      truncated: machines.length >= 1000,
      error: machineRes.error
    },
    heat: { points: heatPoints, error: heatRes.error },
    totalArea: { value: dashboardNumber(areaRes.value?.landArea), error: areaRes.error },
    completionRate: { value: dashboardNumber(completionRes.value?.percent), error: completionRes.error },
    activeRate: { value: dashboardNumber(activeRes.value?.percent), error: activeRes.error },
    // Agent 交互审计记录数（分页 total，不是工具调用次数/tokens）
    agentAudit: { total: dashboardNumber(agentAuditRes.value?.total), error: agentAuditRes.error }
  }
}

/* ================= 地块 ================= */
export async function listLands(params = {}) {
  const query = {}
  if (params.district && params.district !== '全部') query.address = params.district
  const payload = params.keyword ? await remote.remoteLandQuery({ ...query, landName: params.keyword }) : await remote.remoteLandList(query)
  let list = rowsOf(payload).map(landFromBackend)
  if (params.status && params.status !== '全部') list = list.filter((l) => l.status === params.status)
  if (params.crop && params.crop !== '全部') list = list.filter((l) => l.crop === params.crop)
  return byDistrict(list)
}

export async function getLand(id) {
  return landFromBackend(await remote.remoteLandGet(id), 0)
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
    list = list.filter((m) => matchText(m.id, k) || matchText(m.model, k) || matchText(m.operatorName, k) || matchText(m.code, k))
  }
  return byDistrict(list)
}

export async function listOperators(params = {}) {
  const mockFn = async () => {
    await wait(200)
    let list = byDistrict(clone(db.operators))
    if (params.keyword) list = list.filter((o) => matchText(o.name, params.keyword) || matchText(o.phone, params.keyword))
    return list
  }
  // 后端无农机手独立接口（AgriculturalMachineryController 只有农机找活，无机手档案）。
  // 原实现无条件 return mockFn()，即使 VITE_APP_FALLBACK_MOCK=false 也会静默返回假数据；
  // 现改为：开启真实接口且禁止回退时抛错，由页面提示“后端待补充”，不再伪造机手档案。
  if (USE_REAL && !FALLBACK) {
    throw new Error('农机手档案：后端待补充（无农机手档案接口）')
  }
  return mockFn()
}

export async function getMachine(id) {
  // 后端 AgriculturalMachineryController 无任何 @GetMapping，不存在 GET /agriculturalMachinery/{id}。
  // 复用 /agriculturalMachinery/list 结果按 id 匹配。
  const payload = await remote.remoteMachineList()
  const row = rowsOf(payload).find((m) => str(m.id) === str(id))
  if (!row) return null
  const machine = machineFromBackend(row, 0)
  // 后端无 machineId 关联字段（TrajectoryInfo 只存 phoneNumber），历史作业无真实来源
  return { ...machine, records: [] }
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
  const pageNum = Number(params.pageNum) > 0 ? Number(params.pageNum) : 1
  // 页面未显式分页时取一个足够大的页，保持“一次拿全量”的既有行为
  const pageSize = Number(params.pageSize) > 0 ? Number(params.pageSize) : 1000
  const body = buildFarmWorkQuery(params)
  const payload = await remote.remoteFarmWorkList({ ...body, pageNum, pageSize })
  const { list, total } = pageOf(payload)
  let rows = list.map(farmWorkTaskFromBackend)
  // 前端对后端不支持的条件做结果集内过滤
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
  await remote.remoteFarmWorkDelete(taskId)
  return { id: taskId, message: '任务已下架：状态置为「已结束」，列表不再可接单' }
}

/* ================= 作业调度（/dispatch，10 个真实端点） =================
 * 注意（读触发写）：GET /dispatch/list 与 GET /dispatch/stats 在后端会先执行 markTimeout()，
 * 可能更新派单超时状态，禁止轮询，也不要把它当成纯只读接口。
 * 权限：派单/批量/取消/建议仅 admin；接单/完成由后端校验 assigneeId 本人；
 * 前端按钮显隐不构成权限保障，403 与业务异常必须原样提示后端 msg。
 */

/** 当前登录用户 id（雪花值，按字符串返回） */
function currentDispatchUserId() {
  const userStore = useUserStore()
  return toStringId(userStore.userId ?? userStore.user?.userId)
}

/** 机手 userId 校验：只接受数字字符串，禁止 Number 转换以避免雪花精度丢失 */
function normalizeAssigneeId(value) {
  const raw = value === null || value === undefined ? '' : String(value).trim()
  if (!/^\d+$/.test(raw)) throw new Error('派单失败：机手 userId 非法')
  return raw
}

function normalizeDeadline(value) {
  const raw = value === null || value === undefined ? '' : String(value).trim()
  if (!raw) throw new Error('派单失败：请选择截止时间（格式 yyyy-MM-dd HH:mm:ss）')
  return raw
}

/** 统一取 AjaxResult.data，兼容后端直接返回实体的情况 */
function unwrapData(payload) {
  return payload && typeof payload === 'object' && 'data' in payload ? payload.data : payload
}

/**
 * 派单列表：GET /dispatch/list
 * - 非管理员强制附加当前登录用户 assigneeId，忽略页面传入值；
 * - 缺少 userId 直接抛错，绝不发起无条件查询；
 * - 管理员可传 assigneeId 过滤，也可不传查全部。
 */
export async function listDispatches(params = {}) {
  const query = {
    pageNum: Number(params.pageNum) > 0 ? Number(params.pageNum) : 1,
    pageSize: Number(params.pageSize) > 0 ? Number(params.pageSize) : 10
  }
  if (params.status) query.status = params.status
  if (params.farmWorkId) query.farmWorkId = toStringId(params.farmWorkId)
  if (isAdminUser()) {
    if (params.assigneeId) query.assigneeId = toStringId(params.assigneeId)
  } else {
    const userId = currentDispatchUserId()
    if (!userId) throw new Error('未获取到当前登录用户，无法查询个人派单')
    query.assigneeId = userId
  }
  const payload = await dispatchApi.getDispatchList(query)
  const { list, total } = pageOf(payload)
  return { list: list.map(workDispatchFromBackend), total }
}

/** 派单详情：GET /dispatch/{id} */
export async function getDispatch(id) {
  const dispatchId = toStringId(id)
  if (!dispatchId) throw new Error('查询派单详情失败：缺少合法的派单 id')
  return workDispatchFromBackend(unwrapData(await dispatchApi.getDispatchById(dispatchId)))
}

/** 单选派单：POST /dispatch（仅 admin） */
export async function dispatchOne(payload = {}) {
  const farmWorkId = toStringId(payload.farmWorkId)
  if (!farmWorkId) throw new Error('派单失败：请选择作业需求')
  const body = {
    farmWorkId,
    assigneeId: normalizeAssigneeId(payload.assigneeId),
    deadline: normalizeDeadline(payload.deadline)
  }
  if (payload.remark) body.remark = String(payload.remark)
  const res = await dispatchApi.postDispatch(body)
  return { ...workDispatchFromBackend(unwrapData(res)), message: res?.msg || '' }
}

/**
 * 批量派单：POST /dispatch/batch（仅 admin）
 * 后端任一子单失败会整体报错，且已成功的子单不回滚，
 * 调用方必须提示“可能已部分成功，请刷新列表核对”并重新加载列表与统计。
 */
export async function dispatchBatch(payload = {}) {
  const farmWorkIds = toStringIdList(payload.farmWorkIds)
  if (!farmWorkIds.length) throw new Error('批量派单失败：请至少选择一条作业需求')
  const body = {
    farmWorkIds,
    assigneeId: normalizeAssigneeId(payload.assigneeId),
    deadline: normalizeDeadline(payload.deadline)
  }
  if (payload.remark) body.remark = String(payload.remark)
  const res = await dispatchApi.postDispatchBatch(body)
  const list = rowsOf(unwrapData(res)).map(workDispatchFromBackend)
  return { list, count: list.length, message: res?.msg || '' }
}

/** 接单：PUT /dispatch/{id}/accept（仅 assigneeId 本人） */
export async function acceptDispatch(id) {
  const dispatchId = toStringId(id)
  if (!dispatchId) throw new Error('接单失败：缺少合法的派单 id')
  await dispatchApi.putDispatchAcceptById(dispatchId)
  return { id: dispatchId }
}

/** 完成：PUT /dispatch/{id}/complete（仅 assigneeId 本人，须先接单） */
export async function completeDispatch(id) {
  const dispatchId = toStringId(id)
  if (!dispatchId) throw new Error('完成派单失败：缺少合法的派单 id')
  await dispatchApi.putDispatchCompleteById(dispatchId)
  return { id: dispatchId }
}

/** 取消：PUT /dispatch/{id}/cancel（仅 admin） */
export async function cancelDispatch(id) {
  const dispatchId = toStringId(id)
  if (!dispatchId) throw new Error('取消派单失败：缺少合法的派单 id')
  await dispatchApi.putDispatchCancelById(dispatchId)
  return { id: dispatchId }
}

/** 刷新超时：POST /dispatch/timeout（后端允许登录用户调用，前端仅向 admin 展示入口） */
export async function refreshDispatchTimeout() {
  const res = await dispatchApi.postDispatchTimeout()
  const count = Number(unwrapData(res))
  return { count: Number.isFinite(count) ? count : 0, message: res?.msg || '' }
}

/**
 * 全局派单统计：GET /dispatch/stats
 * 后端无参数且不按人过滤，机手账号不提供“个人统计”，
 * 避免把全局数字伪装成个人数据（按人统计接口后端待补充）。
 */
export async function getDispatchStats() {
  if (!isAdminUser()) {
    throw new Error('派单统计为全局口径，机手账号不提供（后端无按人统计接口）')
  }
  const data = unwrapData(await dispatchApi.getDispatchStats()) || {}
  return {
    total: Number(data.total) || 0,
    completed: Number(data.completed) || 0,
    accepted: Number(data.accepted) || 0,
    dispatched: Number(data.dispatched) || 0,
    timeout: Number(data.timeout) || 0,
    cancelled: Number(data.cancelled) || 0,
    completionRate: optionalNum(data.completionRate),
    timeoutRate: optionalNum(data.timeoutRate)
  }
}

/**
 * 调度建议：GET /dispatch/suggestion（仅 admin）
 * operatorCandidates 是后端基于全量用户的评估结果，未按机手角色过滤：
 * 原样返回，调用方必须标注说明，不得前端过滤或伪造机手数据。
 */
export async function getDispatchSuggestions() {
  const data = unwrapData(await dispatchApi.getDispatchSuggestion()) || {}
  const strList = (v) => (Array.isArray(v) ? v.filter((x) => x !== null && x !== undefined && x !== '') : [])
  const pendingTasks = (Array.isArray(data.pendingTasks) ? data.pendingTasks : []).map((t) => ({
    ...t,
    farmWorkId: toStringId(t.farmWorkId),
    plotId: toStringId(t.plotId)
  }))
  const operatorCandidates = (Array.isArray(data.operatorCandidates) ? data.operatorCandidates : []).map((o) => ({
    ...o,
    userId: toStringId(o.userId)
  }))
  return {
    pendingTasks,
    operatorCandidates,
    machineryOptions: Array.isArray(data.machineryOptions) ? data.machineryOptions : [],
    recommendations: strList(data.recommendations),
    dataGaps: strList(data.dataGaps)
  }
}

/**
 * 兼容旧调用：AgentChat 的 AI 审批传入的是地块 id 数组，缺少机手与时限，
 * 不能当作 farmWorkIds 直接派单，因此显式拒绝而不是伪造成功。
 */
export async function batchDispatch(input = []) {
  if (Array.isArray(input)) {
    throw new Error('AI 批量派单已停用：缺少机手与截止时间，请在作业调度台选择作业需求后派单')
  }
  return dispatchBatch(input)
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
  // 后端 add 只返回 AjaxResult.success()，不回传新 id；新增后需重新查询列表才能拿到真实 id
  await remote.remoteFarmWorkAdd(body)
  return { ok: true, message: '作业需求已发布' }
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
  await remote.remoteFarmWorkUpdate(body)
  return { ok: true, message: '作业需求已更新' }
}

/**
 * 作业类型字典 -> GET /fieldWork/listUnique（WorkType：id / workName）
 * 页面原先从 @/mock 的 org.workTypes 取 5 个写死的文案，本次改为真实字典。
 * 该接口带 @PreAuthorize('field:work:check')，无权限时后端返回 code=403，页面需给出空态提示。
 */
export async function listWorkTypes() {
  const payload = await remote.remoteWorkTypeList()
  return rowsOf(payload).map((r) => ({
    id: toStringId(r.id),
    name: r.workName || r.name || ''
  })).filter((w) => w.id && w.name)
}

/* ================= 作业单 / 轨迹 =================
 * 口径说明：后端没有作业单（WorkOrder）表与接口。本函数返回的是
 * POST /effectiveTrajectory/conditions 的有效轨迹记录，经 workorderFromBackend
 * 映射成页面所需的视图字段（id 由轨迹 id 派生）。它只能作为“有效作业轨迹”展示，
 * 不得用于作业单数量、完成率等业务统计，也不代表已结算作业单。
 */
export async function listWorkorders(params = {}) {
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

export async function getWorkorder(id, preloadedList = null) {
  if (preloadedList && Array.isArray(preloadedList)) {
    const hit = preloadedList.find((w) => w.id === id)
    if (hit) return hit
  }
  // 与 listWorkorders 相同：数据源为有效轨迹，不是独立作业单详情接口。
  const list = rowsOf(await remote.remoteEffectiveTrajectory({})).map(workorderFromBackend)
  return list.find((w) => w.id === id) || null
}

/* ================= 复核申请 ================= */
export async function listReviewRequests(params = {}) {
  const realFn = async () => {
    const payload = await remote.remoteReviewList(params)
    return rowsOf(payload).map(reviewRequestFromBackend)
  }
  // 复核无 mock 兜底；空数组代表无复核申请。
  if (String(import.meta.env.VITE_APP_USE_REAL_API ?? 'true') === 'false') return []
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
  const query = { ...pageQuery(params) }
  if (params.settlementNo) query.settlementNo = params.settlementNo
  if (params.phoneNumber) query.phoneNumber = params.phoneNumber
  if (params.periodType !== undefined && params.periodType !== null && params.periodType !== '') query.periodType = Number(params.periodType)
  if (params.status !== undefined && params.status !== null && params.status !== '') query.status = Number(params.status)
  const payload = await remote.remoteSettlementList(query)
  const { list, total } = pageOf(payload)
  return { list: list.map(statementFromBackend), total }
}

/** 结算单详情：后端 GET /settlement/{id}，参数为结算单 Long 主键（已序列化为字符串） */
export async function getStatement(id, preloadedList = null) {
  if (preloadedList && Array.isArray(preloadedList)) {
    // 优先用已经加载的列表数据；避免每次重扫一遍全量
    const hit = preloadedList.find((s) => s.id === id || String(s.raw?.id) === String(id))
    if (hit) return hit
  }
  // 用 settlementNo 或 Long 主键去查独立详情接口
  const payload = await remote.remoteSettlementDetail(id)
  const item = rowsOf(payload)[0] || (payload && typeof payload === 'object' && !Array.isArray(payload) ? payload : null)
  return item ? statementFromBackend(item) : null
}

/** 结算明细：后端 GET /settlement/items/{id}，参数结算单 Long 主键 */
export async function getStatementItems(id) {
  const payload = await remote.remoteSettlementItems(id)
  return rowsOf(payload).map(settlementItemFromBackend)
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
  const payload = await remote.remoteSettlementGenerate({ periodType: Number(periodType), periodStart, periodEnd, phoneNumber })
  const result = payload?.data || payload
  const period = `${periodStart} ~ ${periodEnd}`
  const count = 1
  const amount = result?.totalAmount ?? 0
  return { period, periodType, periodStart, periodEnd, phoneNumber, count, amount, message: `已为 ${phoneNumber} 生成 ${period} 结算单` }
}

/**
 * 结算状态更新：后端 PUT /settlement/status
 *   RequestParam id(结算单 Long 主键) + status(Short: 0=已生成 / 1=已确认 / 2=已付款)
 */
export async function confirmStatement(id, status) {
  await remote.remoteSettlementConfirm(id, Number(status))
  return { id, status }
}

/**
 * 单价规则列表：后端 GET /pricingRule/list
 * 扣减规则（deductRules）后端无对应表，前端不再伪造，改为空数组。
 */
export async function listPriceRules(params = {}) {
  const payload = await remote.remotePriceRuleList(pageQuery(params))
  const { list, total } = pageOf(payload)
  return { rules: list.map(priceRuleFromBackend), total, deductions: [] }
}

/**
 * 更新单价规则：后端 PUT /pricingRule @RequestBody PricingRule
 * PricingRule 实体包含：id, workType(Long), minArea, maxArea, unitPrice, grade, status(0 启用 / 1 停用)
 */
export async function updatePriceRule(rule) {
  await remote.remotePriceRuleUpdate(rule)
  return { id: rule?.id, message: '单价规则已更新，已记录审计日志' }
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

/* ==========================================================================
 * 系统管理真实数据访问层（S10.2）
 * --------------------------------------------------------------------------
 * 约束：
 *   - 仅新增真实 Service，不替换 getSystemData() / getAgentMonitor()；
 *   - 列表统一返回 { list, total }，total 为 null 表示后端未提供总数（不伪造）；
 *   - 服务端分页：把 pageNum/pageSize 原样交给后端，不在前端 slice 模拟；
 *   - 保留原始请求错误，不把 403 转成功，不 catch 后返回 mock；
 *   - 管理员判断统一复用 isAdminUser()，未加载时其返回 false，天然 fail-closed。
 * ========================================================================== */

const SYS_USER_FILTERS = ['userName', 'phonenumber', 'status', 'deptId']
const SYS_ROLE_FILTERS = ['roleName', 'roleKey', 'status']
const LOGIN_LOG_FILTERS = ['userName', 'status', 'ipaddr']
const OPER_LOG_FILTERS = ['title', 'operName', 'businessType', 'status']
const AGENT_AUDIT_FILTERS = ['userName', 'intent', 'status', 'side']

/** 统一分页参数，未传时给后端约定的默认值（pageNum=1/pageSize=10）。 */
function pageQuery(params = {}) {
  const pageNum = Number(params.pageNum) > 0 ? Number(params.pageNum) : 1
  const pageSize = Number(params.pageSize) > 0 ? Number(params.pageSize) : 10
  return { pageNum, pageSize }
}

/** 只透传契约允许的筛选条件，空值不发送。 */
function pickFilters(params = {}, allowed = []) {
  const query = {}
  allowed.forEach((key) => {
    const value = params[key]
    if (value !== undefined && value !== null && value !== '') query[key] = value
  })
  return query
}

/**
 * 若依 BaseEntity.params 日期区间。
 * 后端参数名形如 params.beginTime / params.endTime，自定义 request 层按扁平查询串序列化，
 * 因此使用 Spring Map 绑定语法 params[beginTime] / params[endTime]。
 */
function dateRangeQuery(params = {}) {
  const query = {}
  if (params.beginTime) query['params[beginTime]'] = params.beginTime
  if (params.endTime) query['params[endTime]'] = params.endTime
  return query
}

/**
 * 用户列表：GET /system/user/list
 * @param {object} params pageNum / pageSize / userName / phonenumber / status / deptId
 * @returns {Promise<{ list: object[], total: number|null }>}
 */
export async function listSysUsers(params = {}) {
  const query = { ...pageQuery(params), ...pickFilters(params, SYS_USER_FILTERS) }
  const payload = await listUserApi(query)
  const { list, total } = pageOf(payload)
  return { list: list.map(sysUserFromBackend), total }
}

/**
 * 角色列表：GET /system/role/list
 * @param {object} params pageNum / pageSize / roleName / roleKey / status
 * @returns {Promise<{ list: object[], total: number|null }>}
 */
export async function listSysRoles(params = {}) {
  const query = { ...pageQuery(params), ...pickFilters(params, SYS_ROLE_FILTERS) }
  const payload = await listRoleApi(query)
  const { list, total } = pageOf(payload)
  return { list: list.map(sysRoleFromBackend), total }
}

/**
 * 登录日志：GET /monitor/logininfor/list
 * @param {object} params pageNum / pageSize / userName / status / ipaddr / beginTime / endTime
 * @returns {Promise<{ list: object[], total: number|null }>}
 */
export async function listLoginLogs(params = {}) {
  const query = {
    ...pageQuery(params),
    ...pickFilters(params, LOGIN_LOG_FILTERS),
    ...dateRangeQuery(params)
  }
  const payload = await listLogininforApi(query)
  const { list, total } = pageOf(payload)
  return { list: list.map(loginLogFromBackend), total }
}

/**
 * 操作日志：GET /monitor/operlog/list
 * @param {object} params pageNum / pageSize / title / operName / businessType / status / beginTime / endTime
 * @returns {Promise<{ list: object[], total: number|null }>}
 */
export async function listOperationLogs(params = {}) {
  const query = {
    ...pageQuery(params),
    ...pickFilters(params, OPER_LOG_FILTERS),
    ...dateRangeQuery(params)
  }
  const payload = await listOperlogApi(query)
  const { list, total } = pageOf(payload)
  return { list: list.map(operationLogFromBackend), total }
}

/**
 * 权限审计快照：GET /permission/audit
 * 仅管理员可用；非管理员或登录信息未加载时直接拒绝，不发送请求。
 * @returns {Promise<object>} permissionAuditFromBackend 转换后的单一审计对象
 */
export async function getPermissionAuditSnapshot() {
  if (!isAdminUser()) {
    const err = new Error('仅管理员可查看权限审计')
    err.code = 403
    throw err
  }
  const payload = await getPermissionAudit()
  return permissionAuditFromBackend(unwrapData(payload))
}

/**
 * Agent 调用日志：GET /agent/audit/list
 * 该接口的 beginTime / endTime 为顶层参数，不是 params[]。
 * @param {object} params pageNum / pageSize / userName / intent / status / side / beginTime / endTime
 * @returns {Promise<{ list: object[], total: number|null }>}
 */
export async function listAgentAuditLogs(params = {}) {
  const query = { ...pageQuery(params), ...pickFilters(params, AGENT_AUDIT_FILTERS) }
  if (params.beginTime) query.beginTime = params.beginTime
  if (params.endTime) query.endTime = params.endTime
  const payload = await getAgentAuditList(query)
  const { list, total } = pageOf(payload)
  return { list: list.map(agentAuditLogFromBackend), total }
}

/**
 * Agent 调用日志详情：GET /agent/audit/{id}
 * @param {string|number} id 审计记录 id（雪花值按字符串处理）
 * @returns {Promise<object>} agentAuditLogFromBackend 转换后的单条记录
 */
export async function getAgentAuditLogDetail(id) {
  const auditId = toStringId(id)
  if (!auditId) throw new Error('查询 Agent 审计详情失败：缺少合法的记录 id')
  const payload = await getAgentAuditById(auditId)
  return agentAuditLogFromBackend(unwrapData(payload))
}
