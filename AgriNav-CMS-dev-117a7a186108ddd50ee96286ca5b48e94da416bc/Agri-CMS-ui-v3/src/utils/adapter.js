/**
 * 数据契约适配层
 * 把老版本若依后端的真实数据（WKT 边界、字符串 ID、Short 状态、经纬度）
 * 转换成新 web 视图模型所需的结构，保证页面零改动。
 */

/** 解析 PostGIS WKT：POLYGON ((lng lat, lng lat, ...)) -> [[lng, lat], ...] */
export function parseWktPoints(wkt) {
  if (!wkt || typeof wkt !== 'string') return []
  const inner = wkt.match(/\(\((.*?)\)\)/s) || wkt.match(/\((.*?)\)/s)
  if (!inner) return []
  return inner[1]
    .split(',')
    .map((pair) => pair.trim().split(/[\s,]+/).map(Number))
    .filter((p) => p.length >= 2 && p.every((n) => !Number.isNaN(n)))
    .map((p) => [p[0], p[1]])
}

/** 解析 WKT POINT -> [lng, lat] */
export function parseWktPoint(wkt) {
  if (!wkt || typeof wkt !== 'string') return null
  const inner = wkt.match(/POINT\s*\(\s*([^)]+)\)/i)
  if (!inner) return null
  const nums = inner[1].trim().split(/[\s,]+/).map(Number)
  return nums.length >= 2 ? [nums[0], nums[1]] : null
}

/** 后端 Point[] / 已有坐标数组 / WKT 统一转换为地图轨迹及逐点时间。 */
export function trajectoryPointsFromBackend(points) {
  if (typeof points === 'string') return { track: parseWktPoints(points), trackTimes: [] }
  const track = []
  const trackTimes = []
  for (const point of Array.isArray(points) ? points : []) {
    const lng = Array.isArray(point) ? point[0] : point?.longitude ?? point?.lng ?? point?.lon
    const lat = Array.isArray(point) ? point[1] : point?.latitude ?? point?.lat
    const x = Number(lng)
    const y = Number(lat)
    if (!Number.isFinite(x) || !Number.isFinite(y)) continue
    track.push([x, y])
    trackTimes.push(Array.isArray(point) ? point[2] || '' : point?.time || '')
  }
  return { track, trackTimes }
}

const num = (v, d = 0) => {
  const n = Number(v)
  return Number.isFinite(n) ? n : d
}
const optionalNum = (v) => {
  if (v === undefined || v === null || v === '') return null
  const n = Number(v)
  return Number.isFinite(n) ? n : null
}
const str = (v, d = '') => (v === undefined || v === null ? d : String(v))

/** ID 在视图模型中统一使用字符串；已经丢失精度的 Number 不继续传播。 */
export function toStringId(value) {
  if (!['string', 'number', 'bigint'].includes(typeof value)) return ''
  if (typeof value === 'number' && !Number.isSafeInteger(value)) return ''
  const text = String(value).trim()
  return /^(?:NaN|Infinity|null|undefined)$/i.test(text) ? '' : text
}

/** List<String> 始终保留字符串，并过滤空值及非法 ID。 */
export function toStringIdList(list) {
  return (Array.isArray(list) ? list : []).map(toStringId).filter(Boolean)
}

/** List<Long>：安全整数提交 Number，超出安全范围的数字字符串原样提交。 */
export function toLongIdList(list) {
  return toStringIdList(list)
    .filter((id) => /^\d+$/.test(id))
    .map((id) => {
      const value = Number(id)
      return Number.isSafeInteger(value) ? value : id
    })
}

/** 后端图片字段 -> 可访问 URL。后端返回相对路径 /profile/... 时自动拼接 API 前缀 */
export function imageUrl(value, fallback = '') {
  if (!value || typeof value !== 'string') return fallback
  if (/^(https?:|data:|blob:)/.test(value)) return value
  if (value.startsWith('/')) {
    // /profile/ 是后端的静态资源路径（老版本由 Nginx /profile -> cms:8088）
    const base = (import.meta.env.VITE_API_BASE || '/dev-api').trim().replace(/\/$/, '')
    return value.startsWith('/profile') ? base + value : value
  }
  return value
}

/** 用 id 稳定地分配一张本地地块实拍图（后端图片缺失时的兜底） */
export function landImage(row = {}, index = 0) {
  const fromBackend = imageUrl(row.image)
  if (fromBackend) return fromBackend
  const key = String(row.id ?? index)
  const digits = key.match(/\d+/)
  const n = digits ? Number(digits[0]) : index + 1
  return `/images/${((n - 1) % 10) + 1001}.jpg`
}

/** 用农机 id 分配本地农机实拍图 */
export function machineImage(row = {}, index = 0) {
  const fromBackend = imageUrl(row.image || row.machineImage)
  if (fromBackend) return fromBackend
  const key = String(row.id ?? index)
  const m = key.match(/(\d+)/)
  const n = m ? Number(m[1]) : index + 1
  return `/images/M${String(((n - 1) % 8) + 1).padStart(3, '0')}_1.jpg`
}

/** 依据地址字符串推断片区（backend addressPrefix/address -> web district） */
export function inferDistrict(address) {
  if (!address) return ''
  const text = String(address)
  const districts = ['河东片区', '河西片区', '南滩片区', '北塬片区']
  const hit = districts.find((d) => text.includes(d)) ||
    (text.includes('河东') ? '河东片区' :
     text.includes('河西') ? '河西片区' :
     text.includes('南滩') ? '南滩片区' :
     text.includes('北塬') ? '北塬片区' : '')
  return hit || text.split(/[市县区镇乡村]/)[0] || ''
}

/** 后端 LandPlot -> web 地块视图模型 */
export function landFromBackend(row = {}, index = 0) {
  const points = parseWktPoints(row.boundary)
  const center = parseWktPoint(row.centerPoint) || (points.length
    ? [
        points.reduce((s, p) => s + p[0], 0) / points.length,
        points.reduce((s, p) => s + p[1], 0) / points.length
      ]
    : [0, 0])
  const id = str(row.id)
  return {
    id,
    code: row.landCode || `YD-${id}`,
    name: row.landName || '未命名地块',
    district: row.district || inferDistrict(row.address),
    village: row.village || '',
    // 后端 landType 是地块性质分类（"插花地" / "新增"），不是作物，不能映射到 crop
    landType: row.landType || '',
    // 后端 LandPlot 实体无作物字段，作物需由 crop_work / FarmWorkCrop 关联得出，此处不臆造
    crop: row.crop || '—',
    area: num(row.landArea),
    status: row.status || '待作业',
    price: num(row.price),
    width: num(row.width),
    grade: row.grade || '一类',
    owner: row.ownerName || '',
    soil: row.soil || '',
    slope: row.slope || '',
    points,
    center,
    // 真实经纬度单独保留，供高德地图使用
    lnglat: points,
    centerLngLat: center,
    version: row.version || 'V1.0',
    updatedAt: row.updateTime || row.createTime || '',
    lastWork: row.lastWork || '',
    workCount: num(row.workCount),
    quality: row.quality || '',
    history: row.history || [],
    boundaryWkt: row.boundary || '',
    image: landImage(row, index),
    raw: row
  }
}

/**
 * 后端 AgriculturalMachinery（语义为“农机找活”发布信息）-> web 农机视图模型
 *
 * 重要：后端实体仅含 id / name / typeId / phoneNumber / status / createBy / createTime /
 * updateBy / updateTime / havingGrainCarrier / endTime / addressPrefix。
 * status 取值仅为 Constants.AVAILABLE=0（上架/有效）与 NO_LONGER_AVAILABLE=1（已下架），
 * 不是“作业中/待机/离线/维修”作业态；后者后端无字段，不得伪造。
 */
export function machineFromBackend(row = {}, index = 0) {
  const id = str(row.id)
  const status = num(row.status, 0)
  const available = status === 0
  const pos = row.pos || [40 + ((index * 137) % 900), 40 + ((index * 89) % 520)]
  const lng = num(row.longitude ?? row.lng, NaN)
  const lat = num(row.latitude ?? row.lat, NaN)
  return {
    id,
    code: row.code || id,
    // 后端 name 是“农机找活名称”，只有名称，无型号/编号字段
    model: row.name || row.model || '未命名',
    type: row.typeName || row.type || '农机',
    district: row.district || inferDistrict(row.addressPrefix),
    operatorId: str(row.operatorId),
    // 后端无农机手表，phoneNumber 是发布人联系方式，不能当作机手姓名
    operatorName: row.operatorName || '',
    phone: row.phoneNumber || '',
    online: row.online !== undefined ? !!row.online : available,
    // 只有上架/下架两态可用；真实作业态后端无来源
    available,
    workStatus: row.workStatus || (available ? '待机' : '离线'),
    totalArea: num(row.totalArea),
    efficiency: num(row.efficiency),
    todayArea: num(row.todayArea),
    speed: num(row.speed),
    fuel: num(row.fuel),
    pos,
    lnglat: Number.isFinite(lng) && Number.isFinite(lat) ? [lng, lat] : null,
    landId: str(row.landId),
    landName: row.landName || '',
    lastActive: row.endTime || row.updateTime || '',
    endTime: row.endTime || '',
    havingGrainCarrier: !!row.havingGrainCarrier,
    purchase: row.purchase || '',
    maintainAt: row.maintainAt || '',
    image: machineImage(row, index),
    raw: row
  }
}

/**
 * 后端 FarmWorkVO（POST /farmWork/list，业务语义为“发布找农机的作业需求”）-> 调度任务视图模型
 *
 * 这是后端唯一能够承载“待派单作业任务”的真实数据源。必须严格按 FarmWorkVO 实际字段映射：
 *   id(String) / name / area(BigDecimal) / endTime(LocalDateTime) / phoneNumber /
 *   createTime / createBy(用户名) / status(Integer) / updateTime / updateBy /
 *   cropId(List<Long>) / workTypeId(List<Long>) / landPlotId(List<String>) /
 *   cropName / workTypeName / landPlotName / landPlotImage / landAddress（后 5 个为 List<String>）
 *
 * 后端【不存在】的字段一律不得伪造：
 *   machineId / machineName / operatorId / operatorName / progress / priority / distance / efficiency / score
 * 页面据此展示“未分配”或显式空态，而不是给出看似真实的假值。
 *
 * status 语义（Constants：AVAILABLE=0、NO_LONGER_AVAILABLE=1；FarmWorkServiceImpl.DELETE=2 已被 list 排除）：
 *   0 -> 可接单（待接单）
 *   1 -> 已结束（可能是到期自动结束，也可能是被撤回下架）
 * 后端没有“待派单/待执行/作业中/超时”这些状态，前端不得凭空映射。
 */
export function farmWorkTaskFromBackend(row = {}) {
  const statusNum = row.status === null || row.status === undefined || row.status === ''
    ? null
    : Number(row.status)
  const landPlotId = toStringIdList(row.landPlotId)
  const landPlotName = Array.isArray(row.landPlotName) ? row.landPlotName.filter(Boolean) : []
  return {
    id: toStringId(row.id),
    name: row.name || '',
    // 后端 area 是 BigDecimal，语义为作业亩数
    area: num(row.area),
    endTime: row.endTime || '',
    deadline: row.endTime || '',
    phoneNumber: row.phoneNumber || '',
    createTime: row.createTime || '',
    createdAt: row.createTime || '',
    createBy: row.createBy || '',
    updateTime: row.updateTime || '',
    updateBy: row.updateBy || '',
    status: statusNum,
    // 关联：作业需求 - 地块 是多对多，列表语义上是数组，不能塌缩成单个 landId
    landPlotId,
    landPlotName,
    landAddress: Array.isArray(row.landAddress) ? row.landAddress.filter(Boolean) : [],
    landPlotImage: Array.isArray(row.landPlotImage) ? row.landPlotImage.filter(Boolean) : [],
    cropId: toStringIdList(row.cropId),
    cropName: Array.isArray(row.cropName) ? row.cropName.filter(Boolean) : [],
    workTypeId: toStringIdList(row.workTypeId),
    workTypeName: Array.isArray(row.workTypeName) ? row.workTypeName.filter(Boolean) : [],
    /* ---- 以下三个是「派生的便捷字段」，仅取首元素，不代表后端只有一个地块 ---- */
    // 首个关联地块 id：仅为兼容按单地块检索的历史调用方（如 monitor 的 activeTask 匹配）
    landId: landPlotId[0] || '',
    landName: landPlotName[0] || '',
    type: (Array.isArray(row.workTypeName) ? row.workTypeName.filter(Boolean)[0] : '') || '',
    raw: row
  }
}

/** 后端 ReviewRequest -> web 复核申请视图模型。用于轨迹与复核流程关联。 */
export function reviewRequestFromBackend(row = {}) {
  return {
    id: toStringId(row.id),
    reviewNo: row.reviewNo || '',
    trajectoryInfoId: row.trajectoryInfoId || '',
    phoneNumber: row.phoneNumber || '',
    plotId: toStringId(row.plotId),
    workType: row.workType || '',
    systemArea: optionalNum(row.systemArea),
    claimedArea: optionalNum(row.claimedArea),
    reason: row.reason || '',
    status: row.status === null || row.status === undefined ? null : Number(row.status),
    statusLabel: reviewStatusToLabel(row.status),
    initialReviewer: row.initialReviewer || '',
    initialComment: row.initialComment || '',
    initialTime: row.initialTime || '',
    finalReviewer: row.finalReviewer || '',
    finalComment: row.finalComment || '',
    finalTime: row.finalTime || '',
    createBy: row.createBy || '',
    createTime: row.createTime || '',
    updateTime: row.updateTime || '',
    raw: row
  }
}
const REVIEW_STATUS = { 0: '待初核', 1: '初核通过', 2: '初核驳回', 3: '终审通过', 4: '终审驳回' }
const reviewStatusToLabel = (s) => REVIEW_STATUS[Number(s)] || ''

/** 后端 Trajectory VO -> web 作业单视图模型。缺失业务字段保留为空，不伪造核算结果。 */
export function workorderFromBackend(row = {}) {
  const id = toStringId(row.trajectoryInfoId ?? row.id)
  const parsed = trajectoryPointsFromBackend(row.points ?? row.boundary)
  const track = Array.isArray(row.track) ? row.track : parsed.track
  const trackTimes = Array.isArray(row.trackTimes) ? row.trackTimes : parsed.trackTimes
  const startAt = row.startAt || row.startTime || row.createTime || ''
  const endAt = row.endAt || row.endTime || ''
  const startText = String(startAt)
  const endText = String(endAt)
  const start = startAt ? new Date(startText.includes('T') ? startText : startText.replace(/-/g, '/')).getTime() : NaN
  const end = endAt ? new Date(endText.includes('T') ? endText : endText.replace(/-/g, '/')).getTime() : NaN
  const derivedDuration = Number.isFinite(start) && Number.isFinite(end) && end >= start
    ? Math.round((end - start) / 60000)
    : null
  return {
    id,
    taskId: toStringId(row.taskId),
    landId: toStringId(row.plotId ?? row.landId),
    landName: row.landName || row.location || '',
    district: row.district || inferDistrict(row.location),
    crop: row.crop || '',
    machineId: toStringId(row.machineId),
    operatorId: toStringId(row.operatorId),
    operatorName: row.operatorName || '',
    phoneNumber: row.phoneNumber || '',
    type: row.workType || row.type || '',
    width: num(row.width),
    price: num(row.price),
    status: row.statusText || '',
    startAt,
    endAt,
    duration: optionalNum(row.duration) ?? derivedDuration,
    pointCount: optionalNum(row.pointCount) ?? track.length,
    gpsArea: optionalNum(row.gpsArea),
    effectiveArea: optionalNum(row.area ?? row.effectiveArea),
    landArea: optionalNum(row.landArea),
    diffRate: optionalNum(row.diffRate),
    anomalies: Array.isArray(row.anomalies) ? row.anomalies : [],
    track,
    trackTimes,
    landPoints: row.landPoints || [],
    reviewedBy: row.reviewedBy || '',
    reviewedAt: row.reviewedAt || '',
    reviewNote: row.reviewNote || '',
    logs: row.logs || [],
    lnglatTrack: track,
    raw: row
  }
}

/**
 * 后端 Settlement -> web 结算单视图模型
 *
 * 后端 Settlement 实体真实字段：
 *   id(雪花Long, 序列化为字符串), settlementNo,
 *   periodType(Short: 1=月 2=季), periodStart(LocalDate), periodEnd(LocalDate),
 *   phoneNumber(String),
 *   totalArea(BigDecimal), totalAmount(BigDecimal),
 *   status(Short: 0=已生成 1=已确认 2=已付款),
 *   createBy, createTime, updateTime
 *
 * 后端不存在的字段（前端不得伪造）：operatorName / operatorId / district / deduction / operator。
 */
export function statementFromBackend(row = {}) {
  const id = str(row.settlementNo ?? row.id)
  const statusMap = { 0: '已生成', 1: '已确认', 2: '已付款' }
  return {
    id,
    // 周期：前后端都没有"半月"概念，后端只有 periodType(1/2) + periodStart/periodEnd
    periodType: optionalNum(row.periodType),
    periodStart: row.periodStart || '',
    periodEnd: row.periodEnd || '',
    period: row.periodStart && row.periodEnd ? `${row.periodStart} ~ ${row.periodEnd}` : '',
    cycle: '', // 后端无"半月/自然月"展示字段，前端不伪造
    phoneNumber: row.phoneNumber || '',
    operatorId: row.phoneNumber || '',
    operatorName: row.phoneNumber || '',
    district: '', // 后端无片区
    area: optionalNum(row.totalArea),
    amount: optionalNum(row.totalAmount),
    deduction: null, // 后端无扣减概念，生成时 totalAmount = Σ(area × unitPrice)
    status: row.statusText || statusMap[num(row.status, -1)] || '',
    statusRaw: row.status === null || row.status === undefined ? null : Number(row.status),
    createdAt: row.createTime || '',
    updatedAt: row.updateTime || '',
    createBy: row.createBy || '',
    raw: row
  }
}

/** SettlementItem 明细（列表 API 不带地块名称等信息，需额外 join 或用 plotId 关联） */
export function settlementItemFromBackend(row = {}) {
  return {
    id: str(row.id),
    settlementId: str(row.settlementId),
    trajectoryInfoId: row.trajectoryInfoId || '',
    plotId: str(row.plotId),
    type: row.workType || '',
    area: optionalNum(row.area),
    price: optionalNum(row.unitPrice),
    amount: optionalNum(row.amount),
    deduction: null, // SettlementItem 无扣减
    reason: '', // 无扣减原因
    createTime: row.createTime || '',
    raw: row
  }
}

/** 后端 PricingRule -> web 单价规则视图模型 */
export function priceRuleFromBackend(row = {}) {
  return {
    id: str(row.id),
    name: row.name || row.ruleName || '',
    type: row.workType || row.type || '',
    grade: row.grade || '一类',
    price: num(row.price),
    unit: row.unit || '元/亩',
    effective: row.effectiveDate || row.effective || '',
    status: row.statusText || (num(row.status, 1) === 1 ? '启用' : '停用'),
    raw: row
  }
}

/** 后端通用分页响应 -> 数组 */
export function rowsOf(payload) {
  if (!payload) return []
  if (Array.isArray(payload)) return payload
  const data = payload.data
  if (Array.isArray(data)) return data
  return payload.rows || data?.rows || payload.list || data?.list || []
}

/**
 * 后端通用分页响应 -> { list, total }
 * 只有在后端确实返回 total 时才给出数字；后端未返回 total 时 total 为 null，
 * 由调用方按“后端未提供总数”处理，不得用 list.length 冒充。
 */
export function pageOf(payload) {
  const list = rowsOf(payload)
  const raw = payload?.total
  const total = raw === undefined || raw === null || raw === '' ? null : num(raw, 0)
  return { list, total }
}
