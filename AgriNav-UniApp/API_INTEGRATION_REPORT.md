# 农机作业管控 App 前后端接口对接报告

- 项目：AgriNav-UniApp（uni-app + Vue3 + TS + Vite）
- 后端：AgriNav-CMS（Spring Boot / RuoYi 架构，App 端接口位于 `com.agri.cms.web.controller`）
- 部署环境：远程服务器 `82.156.224.217`，Docker Compose 启动（nginx:80 → cms:8088）
- 对外 API 前缀：`http://82.156.224.217/prod-api`
- 报告日期：2026-11-25
- 对接原则：只对接后端真实存在的接口；不存在的接口不编造、不伪造成功；登录/注册接口不在本次范围内，也未纳入统计。

---

## 0. 结论摘要（已完成后端真实联调）

| 项目 | 结果 |
|---|---|
| 是否修改后端代码 / 数据库 | 否 |
| 是否修改登录、注册、Token 机制 | 否（仅用现有登录接口取 Token 做验证，未改代码） |
| 是否修改页面 UI 布局与样式 | 否（仅修复数据装载与失败隔离逻辑） |
| 前端能否构建 | 能，`npm run build:h5` 通过 |
| 前端能否启动 | 能，`npm run dev:h5` 启动成功（本机 5180） |
| 后端能否连通 | **能**，`http://82.156.224.217/prod-api`（80 端口，nginx 反代） |
| 跨域 | 正常，预检 `OPTIONS` 返回 200 且回显 `Access-Control-Allow-Origin` |
| 实际请求成功接口 | **10 个（去重）** |
| 页面级真实验证 | **4 个页面**（首页、结算列表、结算详情、我的农机） |
| 写操作真实验证 | 0（未创建/修改业务数据） |
| 代码级完成对接 | 19 项 |
| 未接入 | 28 项 |

> 说明：后端 `cms` 容器只 `expose 8088`，未映射到宿主机；公网唯一入口是 nginx 80 的 `/prod-api/`。因此前端基地址必须是 `http://82.156.224.217/prod-api`，**不能用 `:8088`**。

---

## 1. 项目结构与请求体系（已核实）

### 1.1 前端

| 内容 | 实际位置 |
|---|---|
| 页面 | `src/pages/**`（home、task、map、work、settlement、message、exception、agent、mine、login 共 20 个页面） |
| API 模块 | `src/api/*.ts`（request、auth、user、task、work、settlement、message、exception、upload、agent） |
| 状态管理 | `src/stores/*.ts`（user、task、work、app 等 Pinia store） |
| Mock 数据 | `src/mock/*.ts` |
| 配置 | `src/config/index.ts`、`.env.development` |
| 类型定义 | `src/types/index.ts` |

统一请求封装已存在，本次直接复用，未重复创建：

- 基础地址：`API_BASE_URL`（`VITE_API_BASE_URL` 覆盖，末尾自动去除 `/`）。
- 超时：默认 15000ms；上传 60000ms。
- Token：从 storage `app.token` 读取后放入 `Authorization` 请求头。
- 统一处理：401 清空登录态并跳登录页；403 抛“无权限访问”；HTTP 4xx/5xx 与业务 `code != 200` 统一抛错。
- 上传：`uni.uploadFile`，字段名 `file`，`POST /common/upload`。

### 1.2 后端（与 App 相关模块）

| 业务 | Controller |
|---|---|
| 个人资料 | `SysProfileController`（`/system/user/profile`） |
| 地块 | `field/LandPlotController` |
| 农机 | `home/AgriculturalMachineryController` |
| 结算 | `closure/SettlementController` |
| 轨迹 | `trajectory/EffectiveTrajectoryController` |
| 作业类型 | `field/FieldWorkController`（`/fieldWork/list`） |
| 消息 | `system/SysNotificationController` |
| 反馈 | `system/SysFeedbackController` |
| 上传 | `system/CommonController`（`/common/upload`） |

返回结构：`AjaxResult`（`{code,msg,data,...}`）、`TableDataInfo`（`{total,rows}`）；分页参数统一 `pageNum/pageSize`。

---

## 2. 已完成接口对接清单（含真实验证结果）

验证方式：以真实账号 `15888888888` 通过现有登录接口取得 Token（登录本身不计入统计），前端 H5 以真实模式启动（`VITE_USE_MOCK=false`、`VITE_API_BASE_URL=http://82.156.224.217/prod-api`）后逐页验证。

| # | 业务模块 | 前端页面/功能 | 后端接口 | 方式 | 对接状态 | 实测结果 |
|---|---|---|---|---|---|---|
| C1 | 首页/结算 | 结算汇总、结算列表 | `/settlement/list` | GET | 已对接 | ✅ 返回 total=1，金额 6598.80 |
| C2 | 首页/地块 | 地块概览 | `/landPlot?current&size` | GET | 已对接 | ✅ 返回 data 数组，含 WKT 边界/中心点 |
| C3 | 首页/异常 | 反馈数量 | `/system/feedback/myFeedback` | GET | 已对接 | ✅ 返回 `{total:0,data:[]}` |
| C4 | 个人信息 | 个人资料查询 | `/system/user/profile` | GET | 已对接 | ✅ 返回 userId/nickName/phonenumber；`departmentName` 为数组（已适配） |
| C5 | 地块 | 地块详情（明细回填） | `/landPlot/{id}` | GET | 已对接 | ✅ 按 id 3011 返回“兴安镇真实耕地一号” |
| C6 | 农机 | 我的农机列表 | `/agriculturalMachinery/list` | POST | 已对接 | ✅ total=8；页面渲染 7 张不同卡片（`id` 为 `M001` 字符串，已修复） |
| C7 | 作业 | 历史作业单 | `/effectiveTrajectory?pageNum&pageSize` | POST | 已对接 | ✅ code=200，`{total:0,data:[]}`（当前账号无轨迹） |
| C8 | 轨迹 | 有效轨迹查询 | `/effectiveTrajectory` | POST | 已对接 | ✅ 同上（权限 `trajectory:effective:list` 已通过） |
| C9 | 结算 | 结算单列表 | `/settlement/list` | GET | 已对接 | ✅ 页面显示 ST-20261123-894 / ¥6598.80 |
| C10 | 结算 | 结算单详情 | `/settlement/{id}` | GET | 已对接 | ✅ 雪花 ID 原样传递，详情正常 |
| C11 | 结算 | 结算明细 | `/settlement/items/{id}` | GET | 已对接 | ✅ 3 条明细，地块名与“耕地”均成功回填 |
| C12 | 消息 | 消息列表 | `/system/notification` | GET | 已适配 | ❌ 后端 500：`Failed to evaluate expression 'ss.hasRole('admin')'` |
| C13 | 消息 | 未读数/标记已读 | `/system/notification/unread` 等 | GET/PUT | 已适配 | ❌ 未读数同样 500；已读接口未验证 |
| C14 | 反馈 | 反馈标签 | `/system/feedback/labels` | GET | 已适配 | ⚠️ code=200 但无 `data`（字典数据缺失） |
| C15 | 反馈 | 提交反馈 | `/system/feedback` | POST | 已适配 | 未验证（会写数据；且标签为空，未做写入测试） |
| C16 | 反馈 | 我的反馈 | `/system/feedback/myFeedback` | GET | 已对接 | ✅ 返回空列表，页面空态正常 |
| C17 | 上传 | 图片/文件上传 | `/common/upload` | POST(multipart) | 已对接 | 未验证（当前无真实模式入口） |
| C18 | 结算 | 作业类型名称回填 | `/fieldWork/list` | GET | 新增对接 | ✅ 返回 4001 耕地、4002 旋耕等，明细显示“耕地” |
| C19 | 结算 | 地块名称回填 | `/landPlot/{id}` | GET | 新增对接 | ✅ 见 C5 |

补充说明：

- 结算状态映射：`0 已生成 / 1 已确认 → pending（待结算）`，`2 已付款 → paid（已结算）`。
- 结算明细的 `plotId`（如 3011）与 `land_plot.id` 一致；因“我的地块”列表不包含它，已改为按 id 逐个查详情回填。
- 明细的 `workType` 是编码（4001），名称取自 `/fieldWork/list`，不再显示为 `--`。
- 雪花 ID（19 位）在前端统一按字符串传递，避免 `Number` 精度丢失导致详情 404。

---

## 3. 未接入接口清单

| # | 业务模块 | 前端功能 | 后端是否存在对应接口 | 未接入原因 | 后端需要补充什么 |
|---|---|---|---|---|---|
| N1 | 首页 | 今日任务列表、任务计数 | 否 | 后端无任务中心接口 | 任务列表/统计接口 |
| N2 | 首页 | 当前作业 | 否 | 无作业会话接口 | 当前作业查询接口 |
| N3 | 首页 | 今日作业面积、完成单数 | 否 | 无作业统计接口；结算只能聚合累计值 | 作业统计接口（按日/月） |
| N4 | 个人信息 | 个人资料修改 | 是（`PUT /system/user/profile`） | 该调用位于 `src/api/auth.ts`，属登录/认证模块，本次按要求不改动 | 无（后端已具备） |
| N5 | 地块 | 新增地块 | 是（`POST /landPlot`） | 需管理端权限；App 无新增入口 | 如需开放给机手需授权 |
| N6 | 地块 | 修改地块 | 是（`PUT /landPlot`） | 需管理端权限；App 无入口 | 同上 |
| N7 | 地块 | 删除地块 | 是（`DELETE /landPlot/{id}`） | 需管理端权限；App 无入口 | 同上 |
| N8 | 地块 | 地块热力图 | 是（`GET /landPlot/heatmap`） | 前端无热力图功能 | 无需补充 |
| N9 | 作物 | 作物列表 | 是（`GET /crop/getCrop`） | 接口可访问（admin），当前返回空数组，且前端无作物选择页 | 补充作物种子数据 |
| N10 | 农事类型 | 作业类型列表 | 是（`GET /fieldWork/list`、`/fieldWork/listUnique`） | 需要 `field:work:check` 权限；当前仅用于结算明细回填，无独立字典页 | 面向机手开放只读权限 |
| N11 | 农机 | 农机详情 | 是（列表已含详情字段） | 前端无独立详情页 | 无 |
| N12 | 农机 | 新增农机 | 是（`POST /agriculturalMachinery/add`） | 需管理端权限，multipart 提交；App 无入口 | 如需开放需授权 |
| N13 | 农机 | 修改农机 | 是（`PUT /agriculturalMachinery`） | 需管理端权限；App 无入口 | 同上 |
| N14 | 农机 | 删除农机 | 是（`DELETE /agriculturalMachinery/{id}`） | 需管理端权限；App 无入口 | 同上 |
| N15 | 任务 | 任务列表/筛选 | 否 | 无任务接口 | 任务列表（支持状态筛选、分页） |
| N16 | 任务 | 任务详情 | 否 | 无任务接口 | 任务详情 |
| N17 | 任务 | 接单/取消/完成 | 否 | 无任务状态流转接口 | 接单、取消、完成接口 |
| N18 | 任务 | 任务历史 | 否 | 无任务接口 | 历史任务接口 |
| N19 | 作业 | 当前作业查询 | 否 | 无作业会话接口 | 当前作业接口 |
| N20 | 作业 | 开始/暂停/恢复/结束作业 | 否 | 无作业生命周期接口（仅轨迹接口，不等价） | 作业会话全流程接口 |
| N21 | 轨迹 | 单条轨迹详情/回放 | 部分（列表内含 `points`） | 无单条详情/回放接口 | 轨迹详情接口 |
| N22 | 轨迹 | 原始轨迹上传 | 否 | 原始轨迹由 GPS/JTT808 服务生成，无 App 上传接口 | 如需要新增上报接口 |
| N23 | 轨迹 | 轨迹删除 | 部分（`DELETE /effectiveTrajectory/signOut/{username}`） | 该接口是“注销用户全部轨迹”，与删除单条语义不符 | 单条轨迹删除接口 |
| N24 | 结算 | 结算复核申请 | 部分（`POST /reviewRequest`） | `/reviewRequest` 是轨迹面积复核，不是结算复核 | 结算复核接口 |
| N25 | 结算 | 生成/状态修改/价格规则/审核 | 部分（`/settlement/generate`、`PUT /settlement/status` 等） | 管理端接口且需权限；App 无入口 | 面向机手的结算进度接口 |
| N26 | 消息 | 消息详情 | 否 | 无单条详情接口；列表接口当前 500 | 修复通知接口 + 详情接口 |
| N27 | 消息 | 消息列表/已读 | 是（`/system/notification*`） | **后端 SpEL 报错 500**，任何角色都不可用 | 修复 `ss.hasRole` 表达式或改为 `hasPermi`；补 title/content 字段 |
| N28 | Agent | 对话、多轮、SSE、工具调用、ASR/TTS | 否 | 后端无 Agent/大模型服务 | Agent 会话（含流式）、语音能力 |

---

## 4. 仍然使用 Mock 的功能

Mock 总开关：`VITE_USE_MOCK`（`src/config/index.ts`，运行期可用 `app.useMock` 覆盖，「我的 → 设置」可切换）。

| # | 页面 | 功能 | Mock 数据位置 | 保留原因 | 后续需要的接口 |
|---|---|---|---|---|---|
| M1 | 首页 | 今日任务、任务计数 | `src/mock/task.ts` | 后端无任务接口 | 任务列表/统计 |
| M2 | 首页/作业 | 当前作业 | `src/mock/work.ts` | 无作业会话接口 | 当前作业 |
| M3 | 首页 | 今日作业面积、完成单数 | `src/mock/work.ts` | 无作业统计接口 | 作业统计 |
| M4 | 任务 | 列表/筛选/详情/接单 | `src/mock/task.ts` | 后端无任务中心 | 任务 CRUD 与状态流转 |
| M5 | 作业 | 开始/暂停/恢复/结束/确认结算 | `src/mock/work.ts` | 无作业生命周期接口 | 作业会话接口 |
| M6 | 作业/地图 | 作业详情、轨迹回放 | `src/mock/work.ts`、`src/mock/trajectory.ts` | 无单条轨迹详情/回放接口 | 轨迹详情 |
| M7 | 地图/首页 | 地块新增/修改/删除 | `src/mock/db.ts` | App 无入口；写接口需管理端权限 | 无需新增（授权即可） |
| M8 | 结算详情 | 复核申请 | `src/mock/settlement.ts` | 后端无结算复核接口 | 结算复核 |
| M9 | 消息 | 列表/详情/已读 | `src/mock/message.ts` | 后端通知接口 500，任何角色不可用 | 修复并补齐字段 |
| M10 | 异常上报 | 现场图片 | `src/mock/exception.ts` | 后端 `Feedback` 无图片字段 | 反馈图片字段与上传绑定 |
| M11 | Agent | 对话、规则应答、历史 | `src/mock/agent.ts` | 后端无 Agent 服务 | Agent 会话接口 |
| M12 | 全站 | 演示数据总开关 | `src/mock/db.ts` | 保证无后端时仍可演示 | — |

> 真实模式下，未支持功能会返回明确的“后端暂未提供…”错误并展示失败态，不会伪造成功。

---

## 5. 后端需要补充的接口（建议，均尚未实现）

### 5.1 任务中心

| 项 | 建议 |
|---|---|
| 对应功能 | 首页今日任务、任务列表/详情、接单/取消/完成、任务历史 |
| 前端操作 | 查询列表（按状态筛选、分页）、查看详情、接单、取消、完成 |
| 后端现状 | 无任何任务 Controller/实体接口 |
| 建议接口 | `GET /app/task/list?status&pageNum&pageSize`、`GET /app/task/{id}`、`POST /app/task/{id}/claim`、`POST /app/task/{id}/cancel`、`POST /app/task/{id}/finish` |
| 建议返回 | 列表 `{total, rows:[{taskId, taskNo, plotId, plotName, workTypeName, area, planDate, status, unitPrice}]}` |
| 业务逻辑 | 接单需校验任务未被认领、当前用户与农机绑定；状态机：待接单→进行中→已完成/已取消 |

### 5.2 作业会话（作业控制中心）

| 项 | 建议 |
|---|---|
| 对应功能 | 当前作业、开始、暂停、恢复、结束、作业时长/面积 |
| 后端现状 | 仅有轨迹查询/生成，无作业会话 |
| 建议接口 | `GET /app/work/current`、`POST /app/work/start`、`POST /app/work/{id}/pause`、`POST /app/work/{id}/resume`、`POST /app/work/{id}/finish` |
| 建议参数 | `{plotId, machineryId, workType, startTime}` / `{trajectoryInfoId, endTime, claimedArea}` |
| 业务逻辑 | 与轨迹服务联动，结束时生成有效轨迹并进入结算候选 |

### 5.3 首页统计

| 项 | 建议 |
|---|---|
| 对应功能 | 今日任务数、待接/进行中数量、今日作业面积、累计面积、消息提醒 |
| 建议接口 | `GET /app/home/overview` |
| 建议返回 | `{todayTaskCount, pendingTaskCount, workingTaskCount, todayArea, totalArea, unreadCount, currentWork}` |
| 业务逻辑 | 按当前登录用户聚合任务、轨迹、结算数据 |

### 5.4 作物/作业类型字典

| 项 | 建议 |
|---|---|
| 对应功能 | 作物列表、作业类型下拉 |
| 现状 | `/crop/getCrop` 返回空数组；`/fieldWork/list` 可返回 4001 耕地等，但需 `field:work:check` 权限 |
| 建议 | 补齐作物数据；提供机手可访问的只读字典接口，返回 `[{code, name}]` |

### 5.5 结算复核

| 项 | 建议 |
|---|---|
| 对应功能 | 结算明细“申请复核” |
| 后端现状 | `/reviewRequest` 是轨迹面积复核，字段与语义均不匹配 |
| 建议接口 | `POST /app/settlement/review` |
| 建议参数 | `{settlementId, itemId, reason, images[]}` |
| 建议返回 | `{reviewId, receiptNo, status}` |

### 5.6 机手消息中心（当前故障）

| 项 | 建议 |
|---|---|
| 对应功能 | 消息列表/详情/已读/未读数 |
| 现状 | 类级 `@PreAuthorize("ss.hasRole('admin')")` 在运行时抛 `Failed to evaluate expression`，接口 500；`Notification` 无 title/content |
| 建议 | 修复表达式（改用 `@ss.hasPermi(...)` 或补齐 `ss.hasRole` 实现）；新增机手可见接口，统一返回 `{id, title, content, type, read, createTime}`；`/unread` 返回数量 |

### 5.7 反馈图片与详情

| 项 | 建议 |
|---|---|
| 对应功能 | 异常上报现场照片、反馈详情 |
| 现状 | `Feedback` 无图片字段；`/system/feedback/labels` 返回空 data；无单条详情接口 |
| 建议 | 增加 `images` 字段；补齐反馈字典数据；新增 `GET /system/feedback/{id}` |

### 5.8 Agent 智能助手

| 项 | 建议 |
|---|---|
| 对应功能 | 对话、多轮上下文、SSE 流式、工具调用、历史、ASR/TTS |
| 现状 | 无任何 Agent/大模型服务 |
| 建议 | 提供 `/app/agent/chat`（SSE）、`/app/agent/history`、`/app/agent/asr`、`/app/agent/tts` |

---

## 6. 联调问题与实测记录

1. **8088 不可直连（设计如此）**：`cms` 容器只 `expose: 8088`，未映射宿主机；公网入口为 nginx 80 的 `/prod-api/`。使用 `:8088` 会一直连接失败。
2. **已确认可达**：`http://82.156.224.217/prod-api` 返回 200；`/captcha?phoneNumber=...` 返回 `mockCode`（演示短信）。
3. **消息接口 500**：`/system/notification` 与 `/unread` 均返回 `{"msg":"Failed to evaluate expression 'ss.hasRole('admin')'","code":500}`，属后端表达式错误，管理员同样不可用。
4. **反馈标签为空**：`/system/feedback/labels` 返回 `{"msg":"","code":200}`，无 `data`，疑似字典数据未初始化。
5. **作物数据为空**：`/crop/getCrop` 返回 `data: []`。
6. **雪花 ID 精度**：结算单/明细 ID 为 19 位字符串，前端已全部改为字符串传递（否则详情 404）。
7. **农机主键为字符串**：如 `M001`，原 `Number()` 映射会得到 `NaN`，已改为字符串。
8. **个人资料部门为数组**：`departmentName` 实测为 `["若依科技","深圳总公司","研发部门"]`，已拼接展示。
9. **结算明细无名称字段**：`plotId` 与 `workType` 均为编码，已分别通过 `/landPlot/{id}` 与 `/fieldWork/list` 回填。
10. **轨迹接口可用但无数据**：`POST /effectiveTrajectory` 返回 `{total:0,data:[]}`，说明权限通过，字段映射待有数据时再核验。
11. **跨域正常**：`OPTIONS` 预检 200，`Access-Control-Allow-Origin` 回显 `http://localhost:5180`，允许凭证。
12. **结算字段不足**：`Settlement` 无已付金额；`SettlementItem` 无地块名、作业类型名、扣减字段（已用关联接口回填名称，其余显示 `--`）。
13. **农机字段不足**：无车牌、品牌、功率、幅宽；`status` 语义为“在架/过期”，前端按可用/不可用映射。
14. **地块字段不足**：无地块编号与作物；`boundary/centerPoint` 为 WKT 字符串，已在前端解析。
15. **反馈归属过滤**：`myFeedback` 按 `phone_number = username` 查询，若账号与手机号不一致会查不到数据。

---

## 7. 最终统计

统计口径：以“前端页面/功能需要的一次后端交互”为 1 项；同一接口被多个页面使用时按功能项分别列出，但“实际请求成功接口”按去重接口计；登录、注册、`/getInfo` 不计入。

| 指标 | 数量 | 说明 |
|---|---|---|
| 前端识别出的接口需求总数 | 45 | 含查询、写操作、统计、上传、Agent |
| 已完成代码对接 | 19 | 见第 2 节 C1–C19 |
| 实际请求成功（去重接口） | 10 | profile、landPlot 列表/详情、农机列表、结算列表/详情/明细、有效轨迹、我的反馈、作业类型 |
| 已验证返回数据正确展示 | 4 个页面 | 首页、结算列表、结算详情、我的农机 |
| 已验证写操作成功 | 0 | 未创建/修改任何业务数据 |
| 未接入 | 28 | 见第 3 节 N1–N28 |
| 仍使用 Mock 的功能 | 12 | 见第 4 节 M1–M12（不含登录） |
| 因权限/故障/数据缺失无法验证 | 6 项 | 通知列表、通知未读/已读、反馈标签、提交反馈、文件上传（另消息详情随列表一并不可用） |

---

## 8. 启动与测试说明

### 8.1 前端启动

```bash
cd AgriNav-UniApp
npm install          # 已安装可跳过
npm run dev:h5       # H5 调试，端口以控制台输出为准（本机 5180）
npm run build:h5     # 构建验证
```

### 8.2 连接当前远程后端（实测可用）

`.env.development`：

```
VITE_API_BASE_URL=http://82.156.224.217/prod-api
VITE_USE_MOCK=false
```

也可以不改文件，临时用环境变量启动（本次验证即用此方式）：

```powershell
$env:VITE_API_BASE_URL='http://82.156.224.217/prod-api'
$env:VITE_USE_MOCK='false'
npm run dev:h5
```

登录沿用现有页面：手机号 `15888888888`，验证码可留空或使用 `/captcha` 返回的 `mockCode`。

### 8.3 已真实验证的路径

1. 登录 → 首页（统计数字渲染，任务/当前作业仍为 0 或演示数据）。
2. 结算中心 → 显示 `ST-20261123-894`、`¥6598.80`。
3. 结算详情 → 3 条明细，地块名“兴安镇真实耕地一号/二号”，作业类型“耕地”，金额与面积正确。
4. 我的 → 我的农机 → 渲染 7 张不同农机卡片。

### 8.4 预期失败项（不是对接错误）

- 消息中心：后端 500（`ss.hasRole` 表达式错误），页面显示失败态与重试。
- 异常上报：标签接口无数据，分类为空；现场图片不会保存（后端无字段）。
- 任务/作业控制/轨迹回放/Agent：后端无对应接口，真实模式下明确提示不支持或显示空态。

---

## 9. 本次改动文件清单（仅前端，未动后端与数据库）

| 文件 | 改动 |
|---|---|
| `src/api/user.ts` | 个人资料部门数组适配；农机字符串主键；地块列表取数与 WKT `POINT/POLYGON` 解析 |
| `src/api/settlement.ts` | 状态语义修正；雪花 ID 字符串；详情合并明细；`/landPlot/{id}` 回填地块名；`/fieldWork/list` 回填作业类型 |
| `src/api/message.ts` | 枚举类型映射、未读数取列表长度、`id` 兼容字符串、字段兜底 |
| `src/api/exception.ts` | 标签按 `SysDictData` 映射；提交 `labels:[{dictCode}]`；图片不可保存说明 |
| `src/api/work.ts` | 有效轨迹字段映射（点串、时间、面积、状态） |
| `src/types/index.ts` | `Settlement.settlementId`、`SettlementItem.itemId` 兼容字符串；`Machinery.id` 兼容字符串 |
| `src/mock/settlement.ts` | `ReviewPayload` ID 类型放宽为字符串 |
| `src/pages/settlement/detail.vue` | 结算单 ID 按字符串接收与传递 |
| `src/pages/exception/index.vue` | 默认选中后端第一个标签；真实模式不保存图片并提示 |
| `src/pages/home/index.vue` | 任务接口不可用时隔离失败 |
| `src/pages/map/index.vue` | 作业详情不可用时不影响地块地图展示 |
