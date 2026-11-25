# FEATURE_MIGRATION_MATRIX.md — 功能迁移矩阵（第一阶段）

> 图例
> - 现状：✅ 已实现 ｜ 🟡 部分实现 ｜ ❌ 未实现
> - 迁移策略：复用 / 迁移 / 重写 / 新建 / 需后端配合
> - 优先级：P0（演示主链路）｜ P1（完整业务）｜ P2（增强）

---

## 一、账号与认证

| 功能 | 原 Android | 后端 | 新 App 策略 | 优先级 |
|---|---|---|---|---|
| 手机号登录 | ✅ `LoginActivity`，仅手机号 | ✅ `POST /login`（`phoneNumber` + 可选 `captcha`） | 迁移 + 增加验证码 UI；两参数都支持 | P0 |
| 短信验证码发送 | ❌ 无 UI（`/captcha` 已存在但未使用） | ✅ `GET /captcha`，3 分钟，mock 可返回 `mockCode` | 新建验证码按钮与倒计时 | P0 |
| 验证码校验 | ❌ | ✅ 登录内校验 / `GET /validate` | 复用；按 `skip-captcha` 开关决定是否必填 | P0 |
| 自动注册 | ✅ 后端自动注册（角色 id 105） | ✅ `preLoginInsertUser` | 复用，不单独做注册页（可保留入口跳登录） | P0 |
| 微信登录 | ✅ `WxLoginActivity` / 绑定手机 | ✅ `/loginWechatMobile`、`/system/user/bind` | P2 暂缓；需微信开放平台配置 | P2 |
| 手势锁 / 指纹 | ✅ 本地能力 | — | P2 可选，用 uni 插件或系统生物识别 | P2 |
| 退出登录 | ✅ 清本地 | ⚠️ `DELETE /signOut/{username}` 是物理删除，**禁用** | 仅清 token + 跳登录 | P0 |
| 登录态持久化 | ✅ SharedPreferences | ✅ JWT 30 天 + Redis | `uni.setStorageSync` | P0 |
| 个人资料 | ✅ `PersonInfoActivity` | ✅ `GET/PUT /system/user/profile` | 迁移 | P1 |
| 换绑手机号 | ✅ `PhoneChangeActivity` | ✅ `PUT /system/user/profile/phoneNumber` | 迁移（需验证码） | P1 |
| 头像上传 | ✅ | ✅ `POST /system/user/profile/avatar` | `uni.uploadFile` | P2 |

---

## 二、首页与工作台

| 功能 | 原 Android | 后端 | 新 App 策略 | 优先级 |
|---|---|---|---|---|
| 首页聚合 | ✅ `FragmentFront` | 多接口聚合 | 重写为工作台 | P0 |
| 当前日期/机手信息 | ✅ | `/getInfo` | 复用 | P0 |
| 当前作业状态 | ❌ 无「作业中」状态 | ❌ 无作业会话接口 | **新建（前端状态机 + 后端待开发）** | P0 |
| 今日待完成任务 | ❌ | ❌ 无任务接口 | Mock 先行；后端待开发 | P0 |
| 今日累计作业面积 | 🟡 轨迹列表可推算 | 🟡 轨迹/结算数据可查 | 用 Mock；真实接入按轨迹/结算 | P0 |
| 当前任务地块 | ❌ | 🟡 `landPlot` 可查 | Mock + 地块接口 | P0 |
| 当前作业时长 | ❌ | ❌ | 前端计时 + 后端作业单 start_time | P0 |
| 数据同步状态 | 🟡 轨迹重传页 | ❌ 无同步状态接口 | 前端本地队列状态 | P0 |
| 待处理异常提醒 | ❌ | 🟡 `sys_feedback` 可查 | Mock / 反馈接口 | P1 |
| 最近结算信息 | ❌ | ✅ `/settlement/list` | 复用 | P1 |
| 网络/GPS/同步状态 | 🟡 `GpsActivity` | — | `uni.onNetworkStatusChange` + 插件状态 | P0 |
| Agent 快捷入口 | ❌ | ❌ | 新建 | P0 |

---

## 三、任务中心

| 功能 | 原 Android | 后端 | 新 App 策略 | 优先级 |
|---|---|---|---|---|
| 任务分类（待接收/待作业/进行中/已完成/已超时） | ❌ | ❌ | Mock 数据驱动；后端待建任务表/接口 | P0 |
| 任务卡片（地块/距离/类型/面积/单价/截止） | ❌ | 🟡 地块/作物/作业类型可查 | Mock + 地块接口拼装 | P0 |
| 任务详情 | ❌ | ❌ | 新建 | P0 |
| 任务认领 | ❌ | ❌ | Mock 先行（返回回执编号）；后端待开发 | P0 |
| 二次确认 | — | — | `uni.showModal` 自定义弹窗 | P0 |
| 地图导航 | ✅ 高德导航 | 🟡 地块坐标 | `uni.openLocation` / 原生地图插件 | P1 |
| 历史任务 | ❌ | 🟡 `trajectory_info` 可当历史作业单 | 迁移轨迹列表 | P1 |
| 任务处理状态 | ❌ | ❌ | Mock / 待后端 | P1 |

> **结论**：任务域后端完全缺失，必须 Mock 先行，并在第二阶段与后端确认任务表模型。

---

## 四、作业控制

| 功能 | 原 Android | 后端 | 新 App 策略 | 优先级 |
|---|---|---|---|---|
| 开始作业 | ❌ 无显式流程 | 🟡 `POST /trajectoryInfo`（upsert），无开工语义 | **需后端新增开工接口或约定**；先 Mock | P0 |
| 结束作业 | ❌ | 🟡 同上，可写 `end_time/area/status` | 需后端核算触发；先 Mock | P0 |
| 作业单号 | 🟡 `trajectory_info_id`（手机号;时间戳） | ✅ `/trajectoryInfo/create` | 复用 | P0 |
| 实时作业时长 | ❌ | ❌ | 前端计时（基于 start_time） | P0 |
| 定位/GPS 状态 | ✅ `JTService` + 高德 | — | 原生插件 | P0 |
| 轨迹采集 | ✅ JTT808 + Netty | ✅ GPS 服务 7100 | 原生插件复用 | P0 |
| 轨迹地图 | ✅ 高德 | 🟡 轨迹点查询 | `map` 组件 + 插件 | P0 |
| 暂停/恢复 | ❌ | ❌ | P2 视业务需要 | P2 |
| 作业面积结果 | 🟡 轨迹面积 | ✅ 有效轨迹/结算 | 只展示后端结果，不让 LLM 计算 | P0 |
| 结束后的待核算状态 | ❌ | 🟡 结算生成 | Mock + 结算接口 | P1 |

---

## 五、地图与轨迹

| 功能 | 原 Android | 后端 | 新 App 策略 | 优先级 |
|---|---|---|---|---|
| 地块位置/边界 | ✅ AMap 多边形 | ✅ `land_plot` + boundary | `map` polygon；复杂场景原生插件 | P0 |
| 当前农机位置 | ✅ | 🟡 `agricultural_machinery_location` | 插件定位 | P1 |
| 作业轨迹 | ✅ | ✅ `/effectiveTrajectory` | 迁移 | P0 |
| 历史/原始轨迹 | ✅ | ✅ `/originalTrajectory/*` | 迁移 | P1 |
| 轨迹回放 | ✅ | 🟡 点位数据 | 插件或 polyline 动画 | P1 |
| 轨迹上传/重传 | ✅ Room + 重传页 | 🟡 有效轨迹生成接口 | 原生插件 + 本地队列 | P0 |
| 定位状态展示 | ✅ `GpsActivity` | — | 插件事件 | P0 |
| 热力图 | ✅ `FragmentFrontLandHeat` | ✅ `/landPlot/heatmap` | `map` 组件或插件 | P2 |

---

## 六、结算

| 功能 | 原 Android | 后端 | 新 App 策略 | 优先级 |
|---|---|---|---|---|
| 累计/本月面积 | ❌ | 🟡 结算/轨迹聚合 | 复用接口 + 前端展示 | P1 |
| 待结算/已结算金额 | ❌ | ✅ `/settlement/list` | 新建页面 | P1 |
| 结算周期 | ❌ | ✅ `settlement.period_type/start/end` | 复用 | P1 |
| 作业单列表 | ❌ | ✅ `/settlement/items/{id}` | 复用 | P1 |
| 结算明细 | ❌ | ✅ | 复用 | P1 |
| 扣减原因 | ❌ | 🟡 `settlement_item.area` 与轨迹差异，无独立原因字段 | 需后端补字段或前端解释规则 | P1 |
| 提交复核 | ❌ | ✅ `POST /reviewRequest` | 新建表单 | P1 |
| 数字来源约束 | — | 后端返回 | 禁止 LLM 计算 | P0 |

---

## 七、异常上报

| 功能 | 原 Android | 后端 | 新 App 策略 | 优先级 |
|---|---|---|---|---|
| 异常类型（故障/地块/道路/环境/其他） | 🟡 反馈有标签 | 🟡 `sys_feedback` + `sys_feedback_label` | 复用字典，扩展异常类型 | P0 |
| 文字描述 | ✅ `FeedbackActivity` | ✅ `POST /system/feedback` | 迁移 | P0 |
| 语音描述 | ❌ | ❌ | 新建（插件 ASR） | P1 |
| 图片上传 | 🟡 | ✅ `/common/upload` | `uni.uploadFile` | P0 |
| 自动整理描述 | ❌ | ❌ | Agent 功能，Mock 先行 | P1 |
| 二次确认 | ❌ | — | 新建 | P0 |
| 工单编号 | ❌ | 🟡 feedback.id（UUID） | 复用 id 作为回执号 | P0 |
| 工单状态查询 | ✅ 我的反馈 | ✅ `/system/feedback/myFeedback` | 迁移 | P1 |
| 联系队长/人工 | ✅ `CommunicationActivity` | — | 拨号/企微入口 | P2 |

---

## 八、消息中心

| 功能 | 原 Android | 后端 | 新 App 策略 | 优先级 |
|---|---|---|---|---|
| 消息列表 | ✅ `ReceiveSysMessageActivity` | 🟡 `/system/notification`（权限风险） | 迁移；后端修复权限后接入 | P1 |
| 未读标记/未读数 | ✅ | ✅ `/unread` | 复用 | P1 |
| 全部已读 | ✅ | ✅ `PUT /system/notification` | 复用 | P1 |
| 消息详情 | ✅ | ✅ 列表内数据 | 复用 | P1 |
| 消息分类 | 🟡 | 🟡 `sys_notification` 有类型字段 | 前端分组 | P2 |

---

## 九、个人中心

| 功能 | 原 Android | 后端 | 新 App 策略 | 优先级 |
|---|---|---|---|---|
| 头像/姓名/手机号 | ✅ | ✅ `/getInfo`、`/system/user/profile` | 迁移 | P0 |
| 农机信息 | ✅ | ✅ `/agriculturalMachinery/list` | 迁移 | P1 |
| 账号安全 | ✅ | ✅ 换绑/改密 | 迁移 | P1 |
| GPS 状态 | ✅ | — | 插件 | P1 |
| App 版本/升级 | ✅ | ✅ `/version` | 复用 | P2 |
| 使用帮助 | ✅ `ProtocolActivity` | — | 静态页 | P2 |
| 联系人工 | ✅ | — | 拨号/客服 | P2 |
| 退出登录 | ✅ | — | 清本地 | P0 |

---

## 十、AI Agent（全部新建）

| 能力 | 原 Android | 后端 | 策略 | 优先级 |
|---|---|---|---|---|
| 对话 UI | ❌ | ❌ | 新建 | P0 |
| 多轮会话/历史 | ❌ | ❌ | 本地 + Mock；后端待建 | P0 |
| 意图识别 | ❌ | ❌ | Mock 规则匹配（关键词） | P0 |
| 工具调用（查任务/查结算/上报） | ❌ | ❌ | Mock 工具层，直接读 Mock 数据 | P0 |
| 工具执行状态展示 | ❌ | ❌ | 新建组件 | P0 |
| 业务卡片 | ❌ | ❌ | 新建组件 | P0 |
| 二次确认 | ❌ | — | 新建 | P0 |
| 执行回执 | ❌ | — | Mock 生成编号 | P0 |
| 语音输入（ASR） | ❌ | ❌ | 插件 + Mock 识别 | P1 |
| 语音播报（TTS） | ❌ | ❌ | 插件/系统 TTS | P1 |
| SSE 流式输出 | ❌ | ❌ | 预留 `api/agent.ts` 接口；当前 Mock | P2 |
| 真实 LLM 后端 | ❌ | ❌ | 后端待立项 | P2 |

### AG-01 ~ AG-05 与工具映射

| Agent | 工具 | 当前可接真实接口 | Mock 数据源 |
|---|---|---|---|
| AG-01 任务助手 | `queryTodayTasks` / `queryTaskDetail` / `navigateToPlot` | ❌（无任务接口） | `mock/task.ts` |
| AG-02 作业助手 | `startWork` / `finishWork` / `queryWorkStatus` | 🟡（`/trajectoryInfo` 可复用但无开工语义） | `mock/work.ts` |
| AG-03 结算助手 | `querySettlement` / `querySettlementItems` / `explainDeduction` | ✅ `/settlement/*` | `mock/settlement.ts` |
| AG-04 异常助手 | `submitException` / `queryExceptionStatus` | ✅ `/system/feedback` + `/common/upload` | `mock/exception.ts` |
| AG-05 FAQ | `searchFaq` / `contactSupport` | ❌ | 本地 FAQ 文案 |

---

## 十一、离线与同步

| 功能 | 原 Android | 后端 | 新 App 策略 | 优先级 |
|---|---|---|---|---|
| 网络状态 | 🟡 | — | `uni.onNetworkStatusChange` | P0 |
| 离线查看任务/作业 | ❌ | — | Mock + 本地缓存 | P0 |
| 离线开始/结束作业 | ❌ | — | 本地状态机，恢复后补传 | P1 |
| 待上传轨迹 | ✅ Room | ✅ GPS 服务 | 原生插件队列 | P0 |
| 同步进度/失败重试 | 🟡 手动重传 | ❌ 无幂等接口 | 前端队列 + 待后端幂等 | P1 |
| 防重复上传 | ❌ | ❌ | `clientTraceId` 方案（需后端配合） | P1 |

---

## 十二、汇总统计

| 分类 | 数量 | 说明 |
|---|---|---|
| 可直接复用后端 | 14 类 | 登录、用户、地块、作物、作业类型、农机、轨迹、结算、复核、反馈、通知、上传、版本 |
| 需前端重写/新建页面 | 约 20 个 | 见 `APP_PAGE_DESIGN.md` |
| 后端需新增 | 4 类 | 任务/作业单流程、异常工单扩展、Agent/SSE、通知权限修复 |
| 必须原生插件 | 4 个 | 定位、JTT808、本地库、语音（见 `NATIVE_PLUGIN_PLAN.md`） |
| 完全从零 | 1 个 | AI Agent 体系 |

---

## 十三、迁移红线

1. 不删除原 Android 项目，保留其 GPS/JTT808/Room/加密代码作为插件复用源。
2. 不修改现有后端业务规则；新增能力以新接口/新表方式并存。
3. Mock 数据与真实数据严格分层，页面不得直接写死业务数据。
4. 面积、金额、单价、时长一律由 Mock 业务服务或真实接口返回，禁止 LLM 计算。
5. 所有写操作（认领、开工、完工、上报、复核）必须二次确认并生成回执。
