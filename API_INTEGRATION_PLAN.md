# API_INTEGRATION_PLAN.md — 真实后端接入规划（第一阶段）

> 依据：Spring Boot Controller 实际注解与 DTO；登录专项见 `LOGIN_API_ANALYSIS.md`
> 原则：路径、参数、返回结构以真实代码为准；不存在的接口标记「待开发」，不猜测字段

---

## 一、接入基础约定

### 1.1 地址与环境

| 环境 | Base URL | 说明 |
|---|---|---|
| 开发 | `http://<后端IP>:8088` | 现有 App 使用 `http://10.218.37.215:8088` |
| 生产 | `https://<域名>/prod-api`（Nginx 反代） | 以 `deploy/nginx/nginx.conf` 为准 |
| GPS HTTP | `http://<后端IP>:8100` | 仅媒体/静态资源，业务不直连 |
| GPS TCP/UDP | `<后端IP>:7100` | JTT808 终端接入，非 HTTP |

### 1.2 响应结构

统一 `AjaxResult`（Map）：

```json
{ "code": 200, "msg": "操作成功", "data": {}, "total": 0 }
```

- 分页接口字段可能是 `rows/total` 或 `data/total`，**逐接口确认**（如 `/effectiveTrajectory` 返回 `data/total`）。
- 登录返回 `token`、`rsa_public_key` 平铺顶层。

### 1.3 请求头

| Header | 值 | 必需 |
|---|---|---|
| `Authorization` | `<token>` 或 `Bearer <token>` | 除匿名接口外必需 |
| `source` | `App` | 建议统一携带 |
| `Content-Type` | `application/json`（多数写接口） / `application/x-www-form-urlencoded`（`/login`、`/captcha`） | 按接口 |

### 1.4 鉴权与登录失效

- Token 30 天滑动续期；无 refresh token 接口。
- 401 → 清理本地 → 跳登录页。
- 403 → 提示「无权限」，不要静默失败。

---

## 二、接口分层与文件规划

```
src/api/
├── request.ts        # uni.request 封装、拦截、错误处理
├── auth.ts           # /login /captcha /validate /getInfo /version
├── user.ts           # profile、换绑、头像
├── land.ts           # landPlot、address、crop、fieldWork
├── machinery.ts      # agriculturalMachinery、类型
├── task.ts           # 任务（当前无真实接口，仅 Mock 占位）
├── work.ts           # trajectoryInfo、effectiveTrajectory、workingLandPlot
├── trajectory.ts     # originalTrajectory
├── settlement.ts     # settlement、pricingRule、reviewRequest
├── exception.ts      # feedback + upload
├── message.ts        # notification
├── upload.ts         # /common/upload
└── agent.ts          # 预留 Agent 契约（当前 Mock）
```

页面只调用 `api/*`，不直接 `uni.request`。

---

## 三、真实接口明细（App 相关）

### 3.1 认证

| 接口 | 方法 | 参数 | 返回 | 备注 |
|---|---|---|---|---|
| `/login` | POST | `phoneNumber`、`captcha`（可选） | `token`、`rsa_public_key` | **form-urlencoded / query，不可 JSON** |
| `/captcha` | GET | `phoneNumber` | `msg`，mock 模式含 `mockCode` | 3 分钟有效，重复发送报错 |
| `/validate` | GET | `phoneNumber`、`captcha` | `code/msg` | 单独校验（可选） |
| `/getInfo` | GET | — | `user`、`roles`、`permissions`、`departmentNames` | 需 token |
| `/version` | GET | `version` | `version`、`update` | 匿名 |

请求示例：

```ts
// src/api/auth.ts
export const login = (phoneNumber: string, captcha?: string) =>
  request({
    url: '/login',
    method: 'POST',
    form: true,
    data: captcha ? { phoneNumber, captcha } : { phoneNumber }
  })
```

### 3.2 地块 / 字典

| 接口 | 方法 | 参数 | 说明 |
|---|---|---|---|
| `/landPlot` | GET | `current`、`size` | 当前用户地块分页 |
| `/landPlot/{id}` | GET | — | 详情（含边界） |
| `/landPlot/query` | GET | 查询条件 | 条件查询 |
| `/landPlot/byCoordinate` | GET | 经纬度 | 坐标反查 |
| `/landPlot/queryByAddress` | GET | 省市区乡村 | 按地址 |
| `/landPlot/history` | GET | 分页 | 历史地块 |
| `/landPlot/heatmap` | GET | — | 热力图 |
| `/landPlot/area` | GET | — | 面积统计 |
| `/crop/getCrop` | GET | — | 作物 |
| `/fieldWork/getWork` | GET | `cropId` | 作物对应作业类型 |
| `/fieldWork/list` | GET | — | 作业类型 |
| `/landAddress/getProvince` | GET | — | 省 |
| `/landAddress/getCity` 等 | POST | 上级编码 | 市区乡村 |

### 3.3 农机 / 农活

| 接口 | 方法 | 参数 | 说明 |
|---|---|---|---|
| `/agriculturalMachinery/list` | POST | body `AgriculturalMachineryDTO` + `pageNum/pageSize` | 农机分页 |
| `/agriculturalMachineryType/list` | GET | `pageNum/pageSize` | 类型 |
| `/farmWork/list` | POST | body `FarmWorkDTO` + `pageNum/pageSize` | 农活（招工）列表 |
| `/farmWork/{id}` | — | — | **不存在详情 GET 接口**（Controller 只有 add/update/delete/list） |

> 任务中心不能直接复用 `/farmWork`：它是招工信息流，不是平台派单。

### 3.4 作业 / 轨迹

| 接口 | 方法 | 参数 | 说明 |
|---|---|---|---|
| `/trajectoryInfo/create` | GET | `phoneNumber` | 生成作业单号 `手机号;时间戳` |
| `/trajectoryInfo` | POST | body `TrajectoryInfo` | 上传/更新作业单 |
| `/trajectoryInfo/picture` | GET | `phoneNumber`、`startTime`、`endTime` | 缩略图 |
| `/effectiveTrajectory` | POST | `pageNum/pageSize` | 我的有效轨迹 |
| `/effectiveTrajectory/conditions` | POST | body `SortMessage` + 分页 | 条件查询 |
| `/effectiveTrajectory/byPhoneNumber` | GET | `startTime`、`endTime` | 生成有效轨迹 |
| `/originalTrajectory/queryOriginalTrajectory` | POST | body `SortMessage` | 原始轨迹 |
| `/originalTrajectory/queryOriginalTrajectoryByPhoneAndTime` | GET/POST | `phone`、时间 | 按人+时间 |
| `/workingLandPlot` | GET | `pageNum/pageSize/address` | 正在作业地块 |
| `/workingLandPlot/workingTrajectory` | POST | body `WorkingLandPlotDTO` | 地块轨迹 |

`TrajectoryInfo` 关键字段（来自 DDL）：`trajectoryInfoId`、`workType`、`phoneNumber`、`width`、`status`、`startTime`、`endTime`、`trajectoryImg`、`plotId`、`area`。

### 3.5 结算 / 复核

| 接口 | 方法 | 参数 | 说明 |
|---|---|---|---|
| `/settlement/list` | GET | 查询条件 | 结算单列表 |
| `/settlement/{id}` | GET | — | 结算单详情 |
| `/settlement/items/{id}` | GET | — | 明细 |
| `/pricingRule/list` | GET | — | 单价规则 |
| `/reviewRequest` | POST | body `ReviewRequest` | 提交复核 |
| `/reviewRequest/list` | GET | 查询条件 | 我的/全部（看权限） |
| `/reviewRequest/{id}` | GET | — | 详情 |

> `/settlement/generate`、`/settlement/status`、`/reviewRequest/initial|final` 属于管理端，App 不调用。

### 3.6 异常 / 反馈

| 接口 | 方法 | 参数 | 说明 |
|---|---|---|---|
| `/system/feedback` | POST | body `Feedback` | 提交异常/反馈 |
| `/system/feedback/myFeedback` | GET | `pageNum/pageSize` | 我的 |
| `/system/feedback/labels` | GET | — | 标签字典 |
| `/common/upload` | POST | `file`（multipart） | 图片上传 |

`Feedback` 关键字段：`content`、`labels[]`、`phoneNumber`、`username`、`commitTime`、`isChecked`、`reply`。
异常类型可通过 `labels` 传字典 code，或在后端扩展 `type` 字段（需确认）。

### 3.7 消息

| 接口 | 方法 | 参数 | 说明 |
|---|---|---|---|
| `/system/notification` | GET | — | 列表 |
| `/system/notification/unread` | GET | — | 未读 |
| `/system/notification/{id}` | PUT | — | 已读 |
| `/system/notification` | PUT | — | 全部已读 |
| `/system/notification/{id}` | DELETE | — | 删除 |

> **风险**：类级 `@PreAuthorize("ss.hasRole('admin')")`（缺少 `@`），普通机手可能 403/500，需后端修复后再接。

### 3.8 用户

| 接口 | 方法 | 参数 |
|---|---|---|
| `/system/user/profile` | GET / PUT | GET 无参；PUT body `SysUser` |
| `/system/user/profile/phoneNumber` | PUT | `phoneNumber`、`captcha` |
| `/system/user/profile/avatar` | POST | `avatarfile` |
| `/system/user/profile/updatePwd` | PUT | `oldPassword`、`newPassword` |
| `/system/user/check/{phoneNumber}` | GET | — |
| `/system/user/bind` | POST | body `AuthUser` + `phoneNumber`、`captcha` |

---

## 四、请求封装规范

```ts
// src/api/request.ts（示意）
const BASE = import.meta.env.VITE_API_BASE_URL

export default function request<T = any>(opt: {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  data?: any
  form?: boolean
  header?: Record<string, string>
}): Promise<T> {
  return new Promise((resolve, reject) => {
    uni.request({
      url: BASE + opt.url,
      method: opt.method || 'GET',
      data: opt.data,
      header: {
        source: 'App',
        ...(opt.form ? { 'Content-Type': 'application/x-www-form-urlencoded' } : {}),
        ...(uni.getStorageSync('token')
          ? { Authorization: uni.getStorageSync('token') }
          : {}),
        ...opt.header
      },
      success: (res) => {
        const body: any = res.data || {}
        if (res.statusCode === 401 || body.code === 401) {
          uni.removeStorageSync('token')
          uni.reLaunch({ url: '/pages/login/index' })
          return reject(new Error('登录已失效'))
        }
        if (res.statusCode === 403 || body.code === 403) {
          return reject(new Error('无权限访问'))
        }
        if (body.code !== 200) {
          return reject(new Error(body.msg || '请求失败'))
        }
        resolve(body as T)
      },
      fail: (err) => reject(new Error(err.errMsg || '网络异常'))
    })
  })
}
```

约定：
- 超时 15s；上传 60s。
- 统一 loading 可选，页面自行控制。
- 不在此层弹 Toast（由页面决定），但 401 统一处理。
- 日志只在开发环境打印 URL 与 code，不打印 token。

---

## 五、Mock 与真实接口切换

| 层 | 职责 |
|---|---|
| `src/mock/*` | 返回与真实接口同结构的 Promise，含延迟与失败分支 |
| `src/api/*` | 通过 `VITE_USE_MOCK` 决定调用 mock 还是 request |
| 页面 | 只调 api，不感知来源 |

```ts
// src/api/task.ts
import { USE_MOCK } from '@/config'
import { mockTasks } from '@/mock/task'
import request from './request'

export const getTodayTasks = () =>
  USE_MOCK ? mockTasks.today() : request({ url: '/task/today' }) // 后端待开发
```

约束：
- Mock 必须模拟真实业务字段与状态流转，不能只返回字符串。
- Mock 成功不得伪装成真实后端成功；UI 需有「演示数据」标识（可全局角标）。
- 切换真实接口时只改 `api` 层，页面不动。

### 5.1 Mock 文件与职责

| 文件 | 提供 |
|---|---|
| `mock/auth.ts` | 登录、验证码、用户信息、权限 |
| `mock/task.ts` | 任务列表（5 种状态）、任务详情、认领回执 |
| `mock/work.ts` | 当前作业、开工/完工、作业单、时长、面积结果 |
| `mock/trajectory.ts` | 地块边界、轨迹点序列、回放数据 |
| `mock/settlement.ts` | 结算汇总、结算单、明细、扣减原因、复核结果 |
| `mock/exception.ts` | 异常类型、工单提交、回执、工单状态 |
| `mock/message.ts` | 消息列表、未读数、已读 |
| `mock/agent.ts` | 意图规则、工具定义、工具返回、话术 |
| `mock/user.ts` | 个人资料、农机、统计 |

### 5.2 Mock 数据模型（与后端字段对齐）

```ts
// 任务（后端待建，字段为建议契约）
interface Task {
  id: string
  taskNo: string            // 任务编号
  plotId: number
  plotName: string
  plotNo: string
  address: string
  distanceKm: number
  workTypeId: string        // 对应 fieldWork
  workTypeName: string
  area: number              // 亩
  unitPrice: number         // 元/亩
  status: 'pending' | 'claimed' | 'working' | 'done' | 'timeout'
  deadline: string
  requirement?: string
}

// 作业单（对齐 trajectory_info）
interface WorkOrder {
  trajectoryInfoId: string
  plotId: number
  workType: string
  phoneNumber: string
  status: 0 | 1 | 2
  startTime: string
  endTime?: string
  area?: number
  trajectoryId?: string
}

// 结算明细（对齐 settlement_item）
interface SettlementItem {
  settlementId: number
  trajectoryInfoId: string
  plotId: number
  workType: string
  area: number
  unitPrice: number
  amount: number
}
```

### 5.3 Mock 行为要求

1. **延迟**：200–600ms 随机，模拟网络。
2. **失败分支**：按 `scenario` 开关注入超时/业务失败，便于测试错误态。
3. **状态流转**：认领 → 待作业；开工 → 进行中；完工 → 待核算 → 已完成。
4. **数字来源**：面积/金额由 Mock 服务计算并返回，Agent 只展示不计算。
5. **回执**：认领/开工/完工/上报均生成 `receiptNo`，格式与真实业务一致。
6. **持久化**：Mock 状态写入 `uni.storage`，刷新/重启后保持，支持重置。
7. **时间**：使用当前时间生成，截止时间相对今天，保证演示真实感。
8. **隔离**：Mock 数据不得混入真实接口返回；切换后清空 Mock 缓存。

---

## 六、接口缺口（必须由后端补齐或产品确认）

| 缺口 | 现状 | 建议 |
|---|---|---|
| 今日任务/任务列表/任务详情 | 无 | 新增 `task` 表与 `/app/task/*` |
| 任务认领 + 回执 | 无 | `POST /app/task/{id}/claim`，返回回执号 |
| 开始作业 | 无专用接口 | `POST /app/work/start`，返回作业单号 |
| 结束作业 + 核算 | 无专用接口 | `POST /app/work/{id}/finish`，返回面积/状态 |
| 作业状态查询 | 无 | `GET /app/work/current` |
| 异常工单 | 仅 feedback | 扩展类型/状态，或新增 `exception_order` |
| 通知接口权限 | 类级 admin | 修复为方法级或去掉类级注解 |
| 幂等上传 | 无 | `clientTraceId` + 唯一索引 |
| Agent / SSE | 无 | 新服务，契约见下 |

### Agent 契约（预留，不属本阶段开发）

```
POST /agent/chat            Content-Type: application/json
{ "sessionId": "...", "message": "...", "context": { "taskId": "..." } }

响应（SSE）:
event: token      data: {"text":"..."}
event: tool_call  data: {"name":"queryTasks","args":{}}
event: tool_result data: {"name":"queryTasks","data":[...]}
event: card       data: {"type":"task","payload":{}}
event: done       data: {"receipt":"..."}
```

硬约束：工具必须走后端鉴权；数值由工具返回；写操作二次确认。

---

## 七、接入顺序

1. `/version`（验证网络）。
2. `/login` + `/getInfo`（验证认证与 token）。
3. `/landPlot`、`/crop/getCrop`、`/fieldWork/*`（基础字典）。
4. `/trajectoryInfo/*`、`/effectiveTrajectory/*`（作业与轨迹）。
5. `/settlement/*`（结算）。
6. `/system/feedback` + `/common/upload`（异常）。
7. `/system/notification`（待权限修复）。
8. 任务域与 Agent（后端就绪后）。

---

## 八、联调检查清单

- [ ] `/login` 使用 form-urlencoded，未使用 JSON。
- [ ] Token 存裸值，请求头自动注入。
- [ ] 401 能回登录页，403 有提示。
- [ ] 分页字段按接口逐个适配。
- [ ] Mock 开关可用，真实接口可一键切换。
- [ ] 上传走 `uni.uploadFile`，字段名与后端一致（`file`/`avatarfile`）。
- [ ] 不调用 `/signOut/{username}` 等物理删除接口。
- [ ] 所有写操作有二次确认与回执展示。
