# 农机作业管控 App 迁移 uni-app 可行性分析报告

> 分析对象：`AgriNav-APP-master-*`（Android 原生）、`AgriNav-CMS-dev-*`（Spring Boot 后端）、`AgriNav-GPS-dev-*`（JTT808 接入服务）
> 分析方式：只读代码，未修改任何文件、配置、依赖、数据库或目录结构
> 结论时效：基于当前工作区实际代码，凡代码中未证实的均标注为「待确认」，不做推测性补全

---

## 一、现有项目结构分析

### 1.1 工作区顶层

| 目录 | 作用 | 技术栈 |
| --- | --- | --- |
| `AgriNav-APP-master-*` | 农机手使用的 Android 客户端 | 原生 Java + Gradle |
| `AgriNav-CMS-dev-*` | 管理后台 + 业务后端 + Web 前端 | Spring Boot 3.2.6 + Vue 3（已迁移） |
| `AgriNav-GPS-dev-*` | JTT808 协议接入服务（车载终端/GPS） | Spring Boot |
| `deploy/` | Nginx、Docker、构建脚本 | Nginx + Docker Compose |

### 1.2 Android 客户端结构

```
app/src/main/java/cn/agrinav/
├── AgriNavApplication.java / MainActivity.java   单 Activity + 5 个底部 Fragment
├── base/BaseActivity.java
├── user/            登录、注册、微信登录、手势锁、绑定手机号
├── front/           首页广场：热力图、找地、找活、农机挂牌、系统消息
├── measure/         地块测量：绘制、面积计算、地块详情、上传
├── trajectory/      轨迹：采集服务、原始/作业轨迹、重传
├── personal/        个人中心：资料、安全设置、反馈、通讯、GPS
├── utils/           OkHttp、加密、JTT808 协议、Netty、权限
└── ...
```

- 构建：Gradle 8.6 + AGP 8.4.0，`compileSdk 34 / minSdk 26 / targetSdk 34`，Java 8
- 视图：`viewBinding` + `dataBinding`，无 MVVM、无 ViewModel、无依赖注入
- 接口地址：`app/build.gradle` 读取 `app/endpoint.properties`，注入 `server_url`、`jtt808_ip`

### 1.3 后端结构

多模块 Maven：`Agri-CMS-admin`（Web 层/启动）、`system`（领域与业务）、`framework`（安全/配置）、`common`、`quartz`、`generator`。

- Spring Boot 3.2.6，JDK 17
- MyBatis-Plus + PageHelper，Druid 连接池
- Spring Security + JWT（无状态），Redis 存登录态
- PostgreSQL（矢量地块依赖空间能力，实际库类型以部署环境为准）

### 1.4 模块调用关系（实际链路）

```
Android App ──HTTP(OkHttp, Authorization)──> CMS /landPlot /farmWork /settlement ...
     │
     └──TCP(JTT808 over Netty)──> GPS 服务 :7100 ──落库──> trajectory_points
                                        │
                                        └──> 有效轨迹计算 ──> effective_point_buffer / settlement_item
```

App 同时走两条链路：**业务数据走 HTTP**，**位置点位走 JTT808 TCP 长连接**。这是迁移中最大的架构差异点。

---

## 二、现有移动端功能清单

### 2.1 页面与职责（按模块）

| 模块 | 页面（类） | 主要功能 | 数据来源 | 状态 |
| --- | --- | --- | --- | --- |
| 登录 | `LoginActivity` | 手机号 + 验证码登录、token 落盘、拉取用户信息 | `POST /login`、`GET getInfo` | 已实现 |
| 登录 | `RegisterActivity`、`VerificationCodeActivity` | 注册、验证码 | `/register`、短信验证码 | 已实现 |
| 登录 | `WxLoginActivity`、`WxLoginBindPhoneActivity` | 微信登录与绑定手机号 | 微信 SDK + `/loginWechatMobile` | 已实现（依赖微信开放平台配置） |
| 登录 | `GestureActivity`、`FingerprintLoginActivity` | 手势/指纹本地解锁 | 本地存储 + Biometric | 已实现（本地能力） |
| 首页 | `MainActivity` | 5 Tab 容器 | — | 已实现 |
| 首页 | `FragmentFront` | 首页聚合入口 | 多接口 | 已实现 |
| 首页 | `FragmentFrontLandHeat` | 地块热力图 | `/landPlot` 聚合数据 | 已实现（渲染依赖 AMap） |
| 首页 | `FindFarmActivity`、`FarmDetailActivity` | 找地列表/详情、地块多边形展示 | `/landPlot`、`/landPlot/{id}` | 已实现 |
| 首页 | `FindJobActivity`、`JobDetailActivity` | 找活列表/详情 | `/farmWork/list`、`/farmWork/{id}` | 已实现 |
| 首页 | `AddTaskActivity`、`UpdateTaskActivity` | 发布/编辑作业需求 | `/farmWork` | 已实现 |
| 首页 | `FreeAgriculturalMachineryActivity`、`DemandFarmWorkActivity` | 农机挂牌、需求列表 | `/agriculturalMachinery/list`、`/farmWork/list` | 已实现 |
| 首页 | `ScanCodeActivity`、`ScanAfterActivity` | 扫码进入地块/农机 | ZXing + 查询接口 | 已实现 |
| 测量 | `FragmentMeasure`、`MeasureActivity` | 地图打点/画多边形、面积计算 | 本地计算 + `/landPlot` | 已实现 |
| 测量 | `MeasureInfoActivity`、`PlotInfoActivity`、`ItemPlotDetail` | 测量结果、地块详情、排除区域 | `/landPlot`、`/landPlot/exclude` | 已实现 |
| 轨迹 | `FragmentTrajectory`、`TrajectoryActivity` | 原始轨迹列表与地图回放 | Room + `/originalTrajectory/*` | 已实现 |
| 轨迹 | `WorkTrajectoryActivity`、`ItemActivity` | 作业轨迹、单条详情 | 本地 Room + 接口 | 已实现 |
| 轨迹 | `TrajectoryReuploadActivity` | 未上传轨迹重传、3 天清理 | Room + JTT808 | 已实现 |
| 消息 | `ReceiveSysMessageActivity`、`SysMsgDetailActivity` | 站内消息列表/详情/未读数 | `/system/notification*` | 已实现 |
| 个人 | `PersonInfoActivity`、`EditNameActivity` | 资料查看/改名 | `/system/user/profile` | 已实现 |
| 个人 | `SecurityActivity`、`PhoneChangeActivity`、`AuthCodeActivity` | 安全设置、换绑手机 | `/system/user/profile/phoneNumber`、`/system/user/bind` | 已实现 |
| 个人 | `FeedbackActivity`、`ShowFeedbackActivity` | 反馈提交与查看 | `/system/feedback*` | 已实现 |
| 个人 | `CommunicationActivity`、`GpsActivity`、`BackInterfaceActivity` | 客服联系、GPS 状态、返回入口 | 部分本地 | 已实现 |
| 个人 | `ProtocolActivity` | 协议展示（Markwon） | 本地资源 | 已实现 |

### 2.2 当前明确缺失的能力（代码中不存在）

| 能力 | 证据 |
| --- | --- |
| 智能体对话界面 | 全仓库无 chat/对话 Activity，无大模型 SDK 依赖 |
| 语音输入（ASR） | 无语音 SDK、无录音权限调用 |
| 语音播报（TTS） | 无 TTS 依赖与调用 |
| 作业开始/结束的显式业务流程 | 无「开始作业/结束作业」按钮级实现，轨迹靠后台服务持续采集 |
| 结算查看页 | App 内无 settlement 相关 Activity；后端接口已存在 |
| 离线任务队列 | 仅轨迹有本地缓存，业务请求无离线队列 |

> 说明：`trajectory` 模块解决的是「点位采集与回传」，不等于 PRD 中的「作业单开工/完工」流程。二者需在新架构中区分。

---

## 三、后端真实 API 清单

### 3.1 认证与安全（已验证）

| 项 | 实际值 | 依据 |
| --- | --- | --- |
| Token Header | `Authorization` | `application.yml` → `token.header: Authorization` |
| Token 前缀 | 后端解析时去除 `Bearer `，也接受裸 token | `TokenService.getToken()` |
| 有效期 | `expireTime: 2592000000`（30 天） | `application.yml` |
| 登录入参 | `phoneNumber`、`captcha`（可选） | `SysLoginController.login` |
| 登录返回 | `token` + RSA 公钥字段 | `SysLoginService.defaultLogin` |
| 匿名放行 | `/login`、`/register`、`/captcha`、`/landPlot/**`、`/captchaQR`、`/validateQR`、`/render`、`/loginOAuth`、`/loginWechatMobile`、`/loginWithoutSecret`、`/loginQR`、`/validate`、`/system/user/check/**`、`/system/user/bind` | `SecurityConfig` L111-112 |
| 请求加密 | App 侧 RSA + AES，白名单路径不加密 | `EncryptConfig`（`/system/key/exchange`、`/system/user/bind`、`/system/user/profile` 等） |

### 3.2 业务接口（来自 Controller 实际注解）

> 统一前缀以部署为准（开发 `/dev-api`，生产 `/prod-api`）。下表省略前缀。

**地块 / 作物 / 作业类型**

| 方法 | 路径 | 说明 | 权限注解 |
| --- | --- | --- | --- |
| GET | `/landPlot/list` | 分页查询地块 | 匿名可访问（`/landPlot/**`） |
| GET | `/landPlot/{id}` | 地块详情（含边界） | 匿名可访问 |
| POST | `/landPlot` | 新增地块 | 登录 |
| PUT | `/landPlot` | 修改地块 | 登录 |
| DELETE | `/landPlot/{ids}` | 删除地块 | 登录 |
| GET | `/landPlot/heatmap` | 热力图数据 | 匿名可访问 |
| GET | `/crop/getCrop` | 作物列表 | 登录 |
| GET | `/crop/list` | 作物列表（另一入口） | 登录 |
| GET | `/fieldWork/getWork?cropId=` | 指定作物的作业类型 | `field:work:check` |
| GET | `/fieldWork/list` | 全部作业类型 | `field:work:check` |
| GET | `/fieldWork/listUnique` | 去重作业类型（App 在用） | `field:work:check` |

**农机 / 派单**

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/agriculturalMachinery/list` | 农机分页 |
| POST | `/agriculturalMachinery/add` | 新增农机 |
| PUT | `/agriculturalMachinery` | 修改农机 |
| DELETE | `/agriculturalMachinery/{id}` | 删除农机 |
| GET | `/agriculturalMachineryType/list` | 农机类型 |
| POST | `/farmWork/list` | 派单/需求列表（App 在用） |
| GET | `/farmWork/{id}` | 需求详情（App 在用） |
| POST | `/farmWork/add` | 新增需求 |
| PUT | `/farmWork` | 修改需求（App 在用） |
| DELETE | `/farmWork/{id}` | 删除需求 |

**轨迹**

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/trajectoryInfo` | 上传轨迹元信息（App 在用） |
| GET | `/trajectoryInfo/create` | 创建轨迹任务 |
| GET | `/trajectoryInfo/picture` | 轨迹缩略图 |
| POST | `/effectiveTrajectory` | 上传有效轨迹 |
| POST | `/effectiveTrajectory/conditions` | 条件查询 |
| GET | `/effectiveTrajectory/byPhoneNumber` | 按手机号查询 |
| POST/GET | `/originalTrajectory/queryOriginalTrajectory` | 原始轨迹查询 |
| POST/GET | `/originalTrajectory/queryOriginalTrajectoryByPhoneAndTime` | 按人+时间段查询 |
| DELETE | `/originalTrajectory/deleteByUserName` | 清理某人轨迹 |

**结算 / 复核 / 反馈 / 通知 / 公共**

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/settlement/list`、`/settlement/{id}`、`/settlement/items/{id}` | 结算单与明细 |
| POST | `/settlement/generate` | 生成结算 |
| PUT | `/settlement/status` | 改状态 |
| GET/POST | `/pricingRule/*` | 单价规则 |
| GET/POST | `/reviewRequest/*` | 复核申请 |
| POST | `/system/feedback` | 提交反馈 |
| GET | `/system/feedback/myFeedback` | 我的反馈 |
| GET | `/system/feedback/labels` | 反馈标签 |
| GET | `/system/notification` | 消息列表 |
| GET | `/system/notification/unread` | 未读数 |
| PUT | `/system/notification/{id}` | 标记已读 |
| GET | `/system/notification/all` | 全部消息（App 在用） |
| POST | `/common/upload`、`/common/uploads` | 文件上传 |
| GET | `/version` | 版本号 |
| POST | `/system/key/exchange` | 公钥交换 |
| GET | `/system/key/public` | 获取公钥 |
| PUT | `/system/user/profile` | 修改资料 |
| PUT | `/system/user/profile/phoneNumber` | 换绑手机 |
| POST | `/system/user/profile/avatar` | 换头像 |
| POST | `/webSocket`（未验证路径） | **未在代码中找到可用的 WebSocket/SSE 实现** |

**数据范围**：`/landPlot/**` 匿名开放，但 Service 内按登录用户/部门过滤；农机与派单接口通过 `@PreAuthorize` 控制。App 端应只展示当前账号可见数据，具体过滤逻辑需在阶段三逐个接口核对（未逐行验证，列为待确认项）。

---

## 四、Agent 功能及工具调用清单

### 4.1 核查结论

**当前工作区不存在任何可运行的 Agent 实现。**

核查范围与证据：

| 检查项 | 结果 |
| --- | --- |
| `SseEmitter` / `text/event-stream` / `EventSource` | 未命中 |
| `llm` / `deepseek` / `qwen` / `openai` / `dashscope` / `langchain` / `langgraph` | 未命中 |
| function calling / tools / prompt 配置 | 未命中 |
| Maven / npm / requirements 中的大模型、向量库依赖 | 未命中 |
| 独立 agent / ai 服务模块 | 不存在 |
| App 内对话、ASR、TTS 代码或依赖 | 不存在 |
| Nginx 的 SSE 代理配置 | 不存在 |

### 4.2 对迁移的直接影响

1. 「Agent 能力迁移」实际是**从零建设**，不是迁移。原任务描述中「必须保留已有 Agent 业务能力」在当前代码里没有对象可保留。
2. PRD 中 AG-01~AG-05 依赖的三件事目前都缺：
   - 后端：意图识别 + 工具编排 + SSE 输出接口
   - 前端：对话 UI + 流式渲染 + 卡片消息
   - 数据：任务/结算/异常的取数接口（**这部分已有**，见第三节）
3. 因此建议把 Agent 拆成两个独立阶段：先做「对话 UI + 直接调用现有 REST 的结构化卡片」，再做「LLM 意图识别 + 工具编排」。

> 如果团队认为 Agent 已在其他分支/仓库实现，请提供代码位置，本报告结论需重新评估。

---

## 五、数据库与业务模型分析

### 5.1 已在代码中显式声明表名的实体

| 表 | 实体 | 业务含义 |
| --- | --- | --- |
| `land_plot` | `LandPlot` | 地块主表（边界、面积、作物、归属） |
| `land_plot_exclude` | `LandPlotExclude` | 地块排除区域 |
| `boundary` | `Boundary` | 边界点集 |
| `address` | `LandAddress` | 省/市/县/村行政区 |
| `crop` | `Crop` | 作物字典 |
| `work` | `FieldWork` | 作业类型 |
| `crop_work` | `CropWork` | 作物-作业类型关联 |
| `agricultural_machinery` | `AgriculturalMachinery` | 农机 |
| `agricultural_machinery_image` | `AgriculturalMachineryImage` | 农机图片 |
| `agricultural_machinery_crop` | `AgriculturalMachineryCrop` | 农机适用作物 |
| `agricultural_machinery_location` | `AgriculturalMachineryLocation` | 农机位置 |
| `trajectory_info` | `TrajectoryInfo` | 轨迹元信息（作业单维度） |
| `trajectory_points` | `TrajectoryPoints` | 原始轨迹点 |
| `effective_point_buffer` | `EffectivePointBuffer` | 有效轨迹点缓冲 |
| `settlement` | `Settlement` | 结算单 |
| `settlement_item` | `SettlementItem` | 结算明细 |
| `pricing_rule` | `PricingRule` | 单价规则 |
| `review_request` | `ReviewRequest` | 复核申请 |
| `sys_feedback` / `sys_feedback_label` | `Feedback` 等 | 反馈与标签 |
| `sys_release_note` / `sys_change_log` | 发布说明、变更日志 |
| `sys_user_wechat` | 微信绑定关系 |

### 5.2 未显式声明表名（按驼峰默认映射推断，需确认）

| 实体 | 推断表名 | 说明 |
| --- | --- | --- |
| `FarmWork` | `farm_work` | 作业需求/派单 |
| `AgriculturalMachineryType` | `agricultural_machinery_type` | 农机类型字典 |
| `ScreenStatistics` | `screen_statistics` | 大屏统计快照 |

### 5.3 核心关系（依据字段与服务逻辑）

```
sys_user 1 ── n farm_work ── n trajectory_info ── n trajectory_points
land_plot 1 ── n farm_work        land_plot 1 ── n land_plot_exclude
farm_work 1 ── n settlement_item ── 1 settlement ── n review_request
crop 1 ── n crop_work ── 1 work
```

> 建表 SQL：`sql/` 下只有若依基础表 `ry_20240629.sql` 与 `quartz.sql`，**业务表没有建表脚本入库**，说明业务库由已有环境维护。迁移前需导出实际表结构（待确认项）。

---

## 六、Android 原生能力迁移分析

| # | 能力 | 现状实现 | uni-app 可行性 | 建议方案 | 风险 |
| --- | --- | --- | --- | --- | --- |
| 1 | 后台持续 GPS 定位 | 前台服务 + 高德定位 SDK | **不可直接用** `uni.getLocation`（无法保证后台持续） | Android 原生插件：前台服务 + AMap Location | 高，厂商省电策略会杀服务 |
| 2 | 轨迹点采集与上传 | JTT808 + Netty TCP | **不可直接实现**（uni-app 无 TCP 长连接） | 原生插件内复用现有 `jtt808` + `JTService` 代码 | 高，需与 GPS 服务联调 |
| 3 | Android 前台服务 | `FOREGROUND_SERVICE_LOCATION` | 需原生插件 | 插件内 `startForeground` + 通知栏 | 中 |
| 4 | 定位权限/后台定位 | Manifest 已声明 | 部分可用：`plus.android.requestPermissions` | 插件内统一申请并处理「仅本次允许」 | 中，Android 11+ 需二次引导 |
| 5 | 断网轨迹缓存 | Room 本地库 | 可用 `plus.sqlite`，但批量写入性能弱 | 原生插件内沿用 Room，或 plus.sqlite + 批量事务 | 中 |
| 6 | 恢复网络自动同步 | 重传页 + 手动 | 可用 `uni.onNetworkStatusChange` 触发 | 状态机 + 幂等上传 | 中 |
| 7 | 防重复上传/重复建单 | 代码中未见幂等键 | 可实现 | 客户端生成 `clientTraceId`，服务端唯一约束（**需后端配合**） | 高，涉及后端改动 |
| 8 | 语音输入/播报 | 不存在 | 需插件 | 讯飞/百度 SDK 原生插件；或系统 TTS | 中，噪声环境识别率 |
| 9 | 地图与地块边界 | AMap AAR | `map` 组件可用，但多边形/热力/轨迹回放能力弱 | 优先 `map` 组件 + `polygon`/`polyline`；复杂场景用原生地图插件 | 中，视觉与性能需实测 |

### 6.1 可直接用 uni-app 实现

- 登录/注册/验证码、个人资料、消息列表、反馈、找地找活列表、表单与列表页
- 本地 token 存储（`uni.setStorage` / `plus.storage`）
- 网络请求（`uni.request`）、文件上传（`uni.uploadFile`）
- 简易地图展示与折线/多边形绘制（`<map>` 组件）
- 网络状态监听、页面路由、下拉刷新

### 6.2 必须写 Android 原生插件

| 插件 | 必要性 | 可复用代码 |
| --- | --- | --- |
| 后台定位 + 轨迹采集 | 必须 | `trajectory/service/JTService.java`、`utils/jtt808/**`、`utils/netty/**` |
| 本地轨迹库 | 强烈建议 | `trajectory/dao/*`、`AbstractTrajectoryDatabase` |
| 语音识别/播报 | 必须（若保留语音交互） | 无，需接第三方 SDK |
| 高德高级地图能力（热力/轨迹回放/大量覆盖物） | 视需求 | `measure/utils/PolygonUtils`、地图绘制逻辑 |

### 6.3 必须保留在原生侧、不可搬到 JS 的部分

- RSA/AES 加密的密钥生命周期（`utils/encryption/*`）：可用 JS 实现，但密钥安全等级下降，建议保留原生
- JTT808 报文编解码与 Netty 连接管理
- 前台服务与通知

---

## 七、uni-app 新架构设计

### 7.1 技术选型

| 层 | 选型 | 说明 |
| --- | --- | --- |
| 框架 | uni-app（Vue 3 + Vite） | 与现有 Web 端 Vue 3 技术栈一致，团队可复用经验 |
| 语言 | TypeScript | 接口多、字段多，类型能显著降低联调成本；建议启用 |
| 状态 | Pinia | 与 Web 端一致 |
| 请求 | `uni.request` 封装 | 统一拦截、加密、错误码、401 |
| 上传 | `uni.uploadFile` | 对应 `/common/upload` |
| 路由 | uni-app 页面路由（`pages.json`） | 不用 vue-router |
| 原生 | uni_modules 原生插件（Java/Kotlin） | 定位、轨迹、语音 |
| 地图 | `<map>` 组件 + 原生插件兜底 | Key 需在 `manifest.json` 配置 |

### 7.2 目录结构（建议）

```
src/
├── api/                 # 按后端模块拆：auth、landPlot、farmWork、trajectory、settlement、feedback、notification
├── components/          # 业务组件：TaskCard、PlotCard、SettlementCard、ChatBubble、VoiceButton
├── composables/         # useAuth、useLocation、useNetSync、useChatStream
├── pages/
│   ├── login/           # 登录、验证码、绑定手机
│   ├── home/            # 工作台（今日任务、在线状态）
│   ├── task/            # 任务列表、任务详情、作业中
│   ├── chat/            # 智能体对话
│   ├── settlement/      # 结算列表、结算详情
│   ├── feedback/        # 反馈提交与记录
│   ├── message/         # 站内消息
│   └── mine/            # 个人中心、设置、安全
├── stores/              # user、app、task、trace、chat、dict
├── utils/               # request、crypto、format、storage、network
├── uni_modules/         # 原生插件：agri-location、agri-jtt808、agri-voice
├── static/
├── App.vue
├── main.ts
├── manifest.json
└── pages.json
```

### 7.3 请求封装

- 统一 `baseURL`（按环境切换）、`Authorization` 注入、`source: App` 头（后端依赖此头区分端）
- 保留现有加密链路：请求前取 RSA 公钥 → AES 加密 body → 白名单路径不加密
- 统一 `code !== 200` 错误提示；401 → 清理 token + 跳登录
- 提供 `request<T>()` 泛型，返回值与后端 `Result<T>` 对齐

### 7.4 登录与 Token

- 登录 → 存 `token` + `rsaPublicKey` → 拉 `getInfo` → 存用户与权限
- Token 放 `uni.setStorageSync`（非 Cookie），请求头 `Authorization: Bearer <token>`
- 30 天有效期，不做前端自动续期（后端无 refresh 接口），过期引导重新登录

### 7.5 Agent 对话模块（新建）

由于后端暂无 Agent，架构上分两步落地：

**第一步（不依赖大模型）**：对话 UI + 结构化卡片 + 直接调用现有 REST。
- 用户点选/语音说「今天有啥活」→ 前端直接调 `/farmWork/list` → 渲染任务卡片
- 这一层先跑通，指标可验证，且不受模型质量影响

**第二步（接入 LLM）**：需要后端新增接口（本阶段不改后端，仅提出契约）
- `POST /agent/chat`（SSE）：入参 `{ sessionId, message, context }`，输出 `text/event-stream`
- 事件类型建议：`token`（增量文本）、`tool_call`（工具名+参数）、`tool_result`（结构化数据）、`card`（卡片渲染指令）、`done`
- 工具白名单必须绑定真实接口：`queryTasks` → `/farmWork/list`、`querySettlement` → `/settlement/*`、`reportException` → `/system/feedback`
- 硬约束：数值一律由工具返回，模型不得计算；写操作必须二次确认并回传回执号

**SSE 在 uni-app 中的实现**（关键难点）
- `uni.request` 不支持流式读取，方案按优先级：
  1. `plus.net.XMLHttpRequest` + `onreadystatechange` 读取 `responseText` 增量（可跑通，需处理分包）
  2. 原生插件用 OkHttp `EventSource`/`ResponseBody.source()` 逐行解析（最稳）
  3. WebSocket 替代 SSE（需后端支持，当前无）
- 建议直接做方案 2，避免 H5 与 App 行为不一致

### 7.6 GPS 与轨迹方案

- 原生插件 `agri-location`：
  - 前台服务常驻，通知栏展示「作业中」
  - AMap Location 连续定位（间隔、精度可配）
  - 点位批量入本地库 → 按 JTT808 组包 → TCP 发送
- 与现有服务端协议保持一致，不改 GPS 服务
- 作业会话：开始作业生成 `sessionId`，结束作业关闭并触发一次对账请求

### 7.7 离线与同步

- 本地库：`plus.sqlite` 或插件内 Room；表结构对齐 `trajectory_points` + 上传状态字段
- 状态机：`pending → uploading → uploaded → failed(可重试)`
- 同步触发：网络恢复、App 前台、手动重试
- 幂等：客户端生成 `clientTraceId`（UUID）+ 服务端唯一索引；上传前查重
- 上限：本地保留 7 天（现实现为 3 天清理，可按需调整）

---

## 八、分阶段迁移计划

### 阶段一：项目初始化

| 项 | 内容 |
| --- | --- |
| 任务 | 创建 uni-app Vue3+TS 工程；配置 manifest（包名 `cn.agrinav`、权限、高德 Key）；接入 Pinia、请求封装、环境变量 |
| 涉及 | `manifest.json`、`pages.json`、`src/utils/request.ts`、`vite.config.ts` |
| 依赖 | HBuilderX 或 CLI 环境；高德 Android Key；后端可访问 |
| 验收 | 真机能打开空白工程；`uni.request` 能打通 `/version`；打包 APK 成功 |
| 风险 | 签名与包名冲突、地图 Key 绑定包名 |

### 阶段二：基础页面与 UI 迁移

| 项 | 内容 |
| --- | --- |
| 任务 | 登录/验证码、首页工作台、任务列表/详情、消息、个人中心、反馈表单 |
| 涉及 | `pages/login`、`pages/home`、`pages/task`、`pages/message`、`pages/mine`、`pages/feedback` |
| 依赖 | 阶段一完成；UI 规范确认 |
| 验收 | 页面可跳转、表单可校验、与旧 App 功能一一对应 |
| 风险 | 旧页面交互逻辑散落在 Activity，需逐页读代码还原 |

### 阶段三：真实 API 接入

| 项 | 内容 |
| --- | --- |
| 任务 | 按模块实现 api 层；登录态、加密、401、分页、上传全部跑通；逐接口核对数据范围 |
| 涉及 | `src/api/*`、`utils/request.ts`、`utils/crypto.ts` |
| 依赖 | 后端 8088 可用；测试账号 |
| 验收 | 每个接口在真机返回真实数据；无 mock；权限越界有正确提示 |
| 风险 | 加密链路（RSA 公钥刷新时机）易出错；`/landPlot/**` 匿名开放但数据过滤依赖登录态 |

### 阶段四：Agent 能力建设

| 项 | 内容 |
| --- | --- |
| 任务 | 先做卡片式对话（直连 REST）；再等后端 SSE 接口就绪后接 LLM |
| 涉及 | `pages/chat`、`components/ChatBubble`、`composables/useChatStream` |
| 依赖 | **后端新增 Agent 接口（当前不存在）** |
| 验收 | 对话可查任务/结算并跳转单据；数值与接口一致；写操作有确认与回执 |
| 风险 | 后端无接口 → 阶段四前半可做、后半阻塞 |

### 阶段五：GPS 与离线能力

| 项 | 内容 |
| --- | --- |
| 任务 | 开发 `agri-location`、`agri-jtt808` 插件；轨迹采集→本地库→上传→重传；作业会话管理 |
| 涉及 | `uni_modules/agri-location`、`uni_modules/agri-jtt808`、`stores/trace.ts` |
| 依赖 | 阶段三完成；GPS 服务 7100 可达；真机 |
| 验收 | 锁屏/切后台 30 分钟持续采点；断网 4 小时不丢；恢复后自动补传且不重复 |
| 风险 | **最高**：厂商后台限制、Android 12+ 前台服务限制、模拟器无法验证 |

### 阶段六：真机联调与验收

| 项 | 内容 |
| --- | --- |
| 任务 | 全链路联调、弱网测试、耗电测试、升级兼容、旧数据迁移 |
| 验收 | 关键路径 100% 通过；崩溃率、耗电、定位精度达标 |
| 风险 | 不同厂商 ROM 差异大，需覆盖 3 款以上机型 |

---

## 九、需要保留与可能废弃的代码

### 9.1 强烈建议保留（直接复用）

| 代码 | 用途 | 复用方式 |
| --- | --- | --- |
| `utils/jtt808/**` | 报文编解码 | 原样搬进原生插件 |
| `utils/netty/**` | TCP 连接与编解码 | 原样搬进原生插件 |
| `trajectory/service/JTService.java` | 采集与发送调度 | 改造为插件 Service |
| `trajectory/dao/*`、`database/*` | Room 轨迹库 | 插件内继续使用 |
| `utils/encryption/*` | RSA/AES | 插件或 JS 重写，逻辑保持一致 |
| AMap AAR | 地图与定位 | 插件内继续引用 |
| 后端全部接口 | 业务能力 | 不改，前端适配 |

### 9.2 需要重写

- 全部 Activity/Fragment 的 UI 与交互 → Vue 页面
- `OkHttpUtils` → `uni.request` 封装
- `QueryUtils` 等分散的接口调用 → 统一 api 层
- 自绘手势锁、扫码页 → uni-app 组件或插件
- 消息、反馈、个人中心等纯展示逻辑

### 9.3 可废弃

- `FragmentAdapter`、`ViewPagerAdapter` 等 View 层适配器
- `viewBinding`/`dataBinding` 相关布局与绑定代码
- `DialogUtils`（改用 uni-popup / uni.showModal）
- `FixiOSBug` 类似的历史兼容代码
- 旧的 XML 布局资源（迁移完成后）

---

## 十、技术风险与待确认问题

### 10.1 风险排序

| 级别 | 风险 | 说明 | 缓解 |
| --- | --- | --- | --- |
| P0 | Agent 后端不存在 | 阶段四无接口可接 | 先做卡片对话，或同步立项后端 Agent 服务 |
| P0 | 后台定位被系统杀死 | 直接影响作业计费 | 原生插件 + 引导用户加白名单 + 心跳检测 |
| P0 | 断网数据丢失 | 直接影响结算 | 本地先落库再上传，落库成功才允许结束作业 |
| P1 | 重复上传/重复建单 | 结算金额错误 | `clientTraceId` + 服务端唯一约束（需后端配合） |
| P1 | SSE 在 uni-app 不可用 | 流式体验 | 原生插件解析流 |
| P1 | 地图能力弱于原生 | 热力/回放效果下降 | 复杂页面用原生地图插件 |
| P2 | 加密链路不一致 | 登录失败或请求 400 | 逐接口比对旧 App 抓包 |
| P2 | Table 结构无 SQL 脚本 | 本地库字段对不齐 | 向 DBA 导出实际结构 |

### 10.2 待确认问题

1. 后端是否计划新增 Agent 服务？接口契约由谁定？
2. 业务库真实表结构能否导出（当前 `sql/` 只有若依基础表）？
3. 是否允许后端做幂等改造（唯一索引/去重表）？
4. 语音识别/播报是否必须保留？供应商是否已采购（讯飞/百度）？
5. 目标机型与 Android 版本覆盖范围？
6. 是否需要保留微信登录、手势锁、指纹登录？
7. 地图是否必须沿用高德（涉及 Key 与合规）？
8. 旧 App 的历史轨迹数据是否需要迁移到新端本地？

---

## 十一、最终结论

### 可以直接复用

- 后端全部 REST 接口（无需重写，前端适配即可）
- JTT808 / Netty 轨迹链路代码（搬进原生插件）
- Room 轨迹库结构
- RSA/AES 加密逻辑
- 高德 AAR 与地图配置
- 现有业务规则（面积核算、结算口径由后端保证）

### 需要重写

- 全部页面与交互（Activity → Vue 页面）
- 网络层（OkHttp → `uni.request` 封装）
- 状态管理（无 → Pinia）
- 接口调用组织（散落代码 → `src/api` 分层）
- 消息/反馈/个人中心等展示层

### 需要编写 Android 原生插件

1. **agri-location**：前台服务 + 持续定位 + 权限引导
2. **agri-jtt808**：TCP 连接 + 报文编解码 + 轨迹上传与重传
3. **agri-voice**：ASR + TTS（若保留语音交互）
4. **agri-map**（可选）：热力图、轨迹回放等高级渲染

### 最大的技术风险

**后台持续定位与离线轨迹的可靠性**。它同时决定「作业面积是否准确」和「机手收入是否可信」，且受 Android 厂商省电策略影响，无法仅靠 JS 层解决，必须原生插件 + 真机长时测试。

其次是 **Agent 后端缺失**：当前代码中不存在任何 Agent/LLM/SSE 实现，阶段四存在硬依赖，需与后端团队确认排期。

### 推荐的实际开发顺序

1. 初始化工程 + 打通 `/version`（1 条链路验证）
2. 登录 + 加密 + `getInfo`（认证链路验证）
3. 首页/任务/消息/个人中心（覆盖 70% 页面工作量）
4. 找地找活 + 地图展示（验证地图组件够不够用）
5. 原生定位插件 + 轨迹采集（最高风险，尽早开始）
6. 离线库 + 同步 + 幂等（与后端联调）
7. 结算查看 + 反馈
8. Agent 卡片对话 → 等后端 SSE 就绪后接 LLM
9. 真机全量回归

> 本阶段仅完成分析，未修改任何代码。待方案确认后再进入编码。
