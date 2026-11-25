# PROJECT_ANALYSIS.md — 现有项目分析报告（第一阶段）

> 范围：当前工作区全部代码，只读分析
> 原则：只写代码中能证实的内容；无法确认的一律标注「待核实」；不编造接口、字段、数据
> 关联文档：`LOGIN_API_ANALYSIS.md`（登录专项）、`UNIAPP_MIGRATION_ANALYSIS.md`（迁移可行性）

---

## 一、工作区项目识别

| 目录 | 职责 | 技术栈 | 状态 |
|---|---|---|---|
| `AgriNav-APP-master-fe62b419d49d777bb1ea58b6f8b0df5bde26a55f` | 农机手 Android 客户端 | Java + Gradle + AMap + Room + OkHttp + JTT808/Netty | 可运行，作为功能参考 |
| `AgriNav-CMS-dev-117a7a186108ddd50ee96286ca5b48e94da416bc` | 业务后端 + 管理后台 | Spring Boot 3.2.6 / JDK17 / MyBatis-Plus / Security+JWT / Redis / PostgreSQL | 可运行 |
| `AgriNav-GPS-dev-ec449519968cd61472a454f6fe6889d89a43c8b3` | JTT808 接入服务 | Spring Boot + Netty + ES + PostgreSQL | 可运行 |
| `database/` | 数据库 DDL 与种子数据 | PostgreSQL + PostGIS | 脚本齐备 |
| `deploy/` | 容器化部署 | Docker Compose + Nginx | 可部署 |

**结论：当前工作区不存在独立的 Agent / LLM / 语音服务模块**（全仓库检索 `SseEmitter`、`text/event-stream`、`openai`、`deepseek`、`qwen`、`langchain`、`function calling` 均无命中）。PRD 中的智能体能力属于**从零建设**，不是迁移。

---

## 二、后端项目结构

### 2.1 Maven 模块

| 模块 | 职责 |
|---|---|
| `Agri-CMS-admin` | 启动类、全部 Controller、`application*.yml` |
| `Agri-CMS-framework` | Spring Security、JWT、登录服务、过滤器、拦截器 |
| `Agri-CMS-system` | 领域实体、Service、Mapper、业务实现 |
| `Agri-CMS-common` | 通用工具、常量、异常、AjaxResult、Redis、SMS |
| `Agri-CMS-quartz` | 定时任务 |
| `Agri-CMS-generator` | 代码生成 |
| `Agri-CMS-ui-v3`（同仓库前端） | Vue 3 + Element Plus 管理后台 |

### 2.2 运行参数

| 项 | 值 | 依据 |
|---|---|---|
| HTTP 端口 | `8088` | `application.yml` L20 |
| context-path | `/` | `application.yml` L23 |
| Token 头 | `Authorization` | `application.yml` L72 |
| Token 有效期 | 30 天（`2592000000` ms，滑动续期） | `application.yml` L76 |
| 登录态存储 | Redis `LoginUser` | `TokenService` |
| 数据库 | PostgreSQL（空间数据用 PostGIS） | `database/agrinav_postgis_ddl.sql` |
| 缓存 | Redis | `application-dev.yml` L54-59 |

### 2.3 安全与加密现状

- Spring Security 无状态鉴权；匿名白名单见 `SecurityConfig` L108-118。
- **传输加密当前全局关闭**：`EncryptConfig.ENCRYPT_PATHS = ["/**"]`、`EXCLUDE_PATHS = ["/**"]`，排除项优先 → `isEncryptPath()` 恒为 false。
- App 端 `EncryptConfig.checkPath()` 直接 `return false`，两端一致。
- 登录细节见 `LOGIN_API_ANALYSIS.md`。

---

## 三、真实后端接口全景

> 路径均为「类级 `@RequestMapping` + 方法级注解」拼接后的真实值，省略服务地址。App 使用需带 `Authorization`（匿名接口除外）。

### 3.1 认证与用户

| 方法 | 路径 | 参数 | 说明 |
|---|---|---|---|
| POST | `/login` | `phoneNumber`（必填）、`captcha`（可选） | 手机号登录/自动注册，返回 `token`、`rsa_public_key` |
| GET | `/captcha` | `phoneNumber` | 发送短信验证码（3 分钟） |
| GET | `/validate` | `phoneNumber`、`captcha` | 单独校验验证码 |
| GET | `/getInfo` | — | 当前用户 + 角色 + 权限 + 部门名 |
| GET | `/system/user/profile` | — | 个人资料 |
| PUT | `/system/user/profile` | body `SysUser` | 修改昵称/邮箱/手机号/性别 |
| PUT | `/system/user/profile/phoneNumber` | `phoneNumber`、`captcha` | 换绑手机号（返回新 token） |
| POST | `/system/user/profile/avatar` | `avatarfile` | 头像上传（MinIO） |
| PUT | `/system/user/profile/updatePwd` | `oldPassword`、`newPassword` | 修改密码 |
| GET | `/system/user/check/{phoneNumber}` | — | 手机号是否已存在 |
| POST | `/system/user/bind` | `AuthUser` body + `phoneNumber`、`captcha` | 微信绑定手机号 |
| GET | `/version` | `version` | 版本检查（匿名） |
| POST | `/system/key/exchange` | 加密 AES key + `source` 头 | 密钥交换（当前加密关闭，可不调） |

### 3.2 地块 / 行政区 / 作物 / 作业类型

| 方法 | 路径 | 参数 | 说明 |
|---|---|---|---|
| GET | `/landPlot` | `current`、`size` | 当前用户/部门地块分页 |
| GET | `/landPlot/{id}` | — | 地块详情 |
| GET | `/landPlot/query` | 查询条件 | 条件查询 |
| GET | `/landPlot/queryByAddress` | 行政区条件 | 按地址查询 |
| GET | `/landPlot/queryByAddressAll` | 行政区条件 | 按地址查询全部 |
| GET | `/landPlot/byCoordinate` | 经纬度 | 坐标反查地块 |
| GET | `/landPlot/heatmap` | — | 热力图数据 |
| GET | `/landPlot/area` | — | 面积统计 |
| GET | `/landPlot/areaByAddress` | 地址 | 按地址面积统计 |
| GET | `/landPlot/history` | 分页 | 历史删除地块 |
| GET | `/landPlot/remeasure` | `id` | 复测历史 |
| POST | `/landPlot` | `landPlot` JSON + `file` | 新增地块 |
| PUT | `/landPlot` | `landPlot` JSON + `file` | 修改地块 |
| DELETE | `/landPlot/{id}` | — | 逻辑删除 |
| POST | `/landPlotExclude/addLandPlotExclude` | — | 新增排除区域 |
| GET | `/landAddress/getProvince` | — | 省 |
| POST | `/landAddress/getCity` / `getDistrict` / `getVillage` / `getHamlet` | 上级编码 | 市区乡村 |
| GET | `/crop/getCrop` | — | 作物列表 |
| GET | `/fieldWork/getWork` | `cropId` | 指定作物的作业类型 |
| GET | `/fieldWork/list` | — | 全部作业类型 |
| GET | `/fieldWork/listUnique` | — | 去重作业类型 |

> 更正：`/landPlot/**` 在 `SecurityConfig` 中匿名放行，但 Controller 内仍通过 `getUserId()` 过滤数据；**不存在 `/landPlot/list` 接口**。

### 3.3 农机 / 农活（招工）

| 方法 | 路径 | 参数 | 说明 |
|---|---|---|---|
| POST | `/agriculturalMachinery/list` | body `AgriculturalMachineryDTO` + `pageNum/pageSize` | 农机分页（App 在用） |
| POST | `/agriculturalMachinery/add` | `agriculturalMachinery` JSON + 图片 | 新增农机 |
| PUT | `/agriculturalMachinery` | 同上 | 修改农机 |
| DELETE | `/agriculturalMachinery/{id}` | — | 下架/删除 |
| GET | `/agriculturalMachineryType/list` | `pageNum/pageSize` | 农机类型 |
| POST | `/farmWork/list` | body `FarmWorkDTO` + `pageNum/pageSize` | 农活（招工）列表 |
| POST | `/farmWork/add` | body `FarmWorkDTO` | 发布农活需求 |
| PUT | `/farmWork` | body `FarmWorkDTO` | 修改需求 |
| DELETE | `/farmWork/{id}` | — | 下架需求 |

> **重要**：`farm_work` 是「农活招工/寻找农机」信息表，**不是** PRD 中的「平台派给机手的作业任务」。二者不能混为一谈。

### 3.4 轨迹 / 作业单

| 方法 | 路径 | 参数 | 说明 |
|---|---|---|---|
| POST | `/trajectoryInfo` | body `TrajectoryInfo` | 上传/更新作业单（一条轨迹元信息） |
| GET | `/trajectoryInfo/create` | `phoneNumber` | 生成轨迹 ID（`手机号;时间戳`） |
| GET | `/trajectoryInfo/picture` | `phoneNumber`、`startTime`、`endTime` | 轨迹缩略图（匿名） |
| POST | `/effectiveTrajectory` | `pageNum/pageSize` | 当前用户作业轨迹分页 |
| POST | `/effectiveTrajectory/conditions` | body `SortMessage` + 分页 | 条件查询 |
| GET | `/effectiveTrajectory/byPhoneNumber` | `startTime`、`endTime` | 按当前用户生成有效轨迹 |
| POST | `/originalTrajectory/queryOriginalTrajectory` | body `SortMessage` | 原始轨迹查询 |
| GET/POST | `/originalTrajectory/queryOriginalTrajectoryByPhoneAndTime` | `phone`、时间 | 按人+时间段 |
| DELETE | `/originalTrajectory/deleteByUserName` | `phone` | 删除某人轨迹 |
| GET | `/workingLandPlot` | `pageNum/pageSize/address` | 正在作业地块 |
| POST | `/workingLandPlot/workingTrajectory` | body `WorkingLandPlotDTO` | 地块对应作业轨迹 |

**数据模型关键点**：`trajectory_info` 实际承担「作业单」角色，字段含 `trajectory_info_id / work_type / phone_number / plot_id / area / status / start_time / end_time / trajectory_img`；`settlement_item.trajectory_info_id` 即指作业单 ID。

### 3.5 结算 / 复核 / 反馈 / 通知 / 文件

| 方法 | 路径 | 参数 | 说明 |
|---|---|---|---|
| GET | `/settlement/list` | 查询条件 | 结算单列表 |
| GET | `/settlement/{id}` | — | 结算单详情 |
| GET | `/settlement/items/{id}` | — | 结算明细 |
| POST | `/settlement/generate` | 参数 | 生成结算单 |
| PUT | `/settlement/status` | 参数 | 更新结算状态 |
| POST | `/settlement/export/{id}` | — | 导出明细 |
| GET | `/pricingRule/list` / `/{id}` | — | 单价规则 |
| GET | `/reviewRequest/list` / `/{id}` | — | 复核申请列表/详情 |
| POST | `/reviewRequest` | body `ReviewRequest` | 提交复核申请 |
| PUT | `/reviewRequest/initial` / `/final` | `id`、`pass`、`comment` | 初核/终审（管理端） |
| POST | `/system/feedback` | body `Feedback` | 提交反馈 |
| GET | `/system/feedback/myFeedback` | `pageNum/pageSize` | 我的反馈 |
| GET | `/system/feedback/labels` | — | 反馈标签 |
| POST | `/system/feedback/list` / `/check` | body | 管理端 |
| GET | `/system/notification` | — | 通知列表 |
| GET | `/system/notification/unread` | — | 未读列表 |
| PUT | `/system/notification/{id}` | — | 标记已读 |
| PUT | `/system/notification` | — | 全部已读 |
| DELETE | `/system/notification/{id}` | — | 删除 |
| POST | `/common/upload` | `file` | 单文件上传 |
| POST | `/common/uploads` | `files` | 批量上传 |
| GET | `/common/download` | `fileName`、`delete` | 下载 |

### 3.6 接口风险标注（必须重视）

| 风险 | 依据 | 影响 |
|---|---|---|
| `/system/notification` 类上写的是 `@PreAuthorize("ss.hasRole('admin')")`（缺少 `@`），SpEL 可能解析失败或仅管理员可用 | `SysNotificationController` L21 | 普通机手调用可能 403 / 500，**待核实并在后端修复** |
| `/system/feedback` 类上 `@Anonymous`，但方法内调用 `getLoginUser()` | `SysFeedbackController` L34、L57 | 无 token 调用可能空指针，App 必须带 token |
| 轨迹/结算接口大量使用 `@PreAuthorize` 细粒度权限 | 如 `trajectory:effective:list` | 机手角色需具备对应权限，否则 403；需核对 `sys_menu`/角色授权 |
| 无「开始作业 / 结束作业」专用接口 | 只有 `POST /trajectoryInfo`（upsert） | PRD 的开工/完工流程**需新增后端接口或约定复用** |

---

## 四、Android 客户端现状

### 4.1 架构

- 单 Activity + 5 个底部 Fragment：首页 `FragmentFront`、地块/测量 `FragmentMeasure`、轨迹 `FragmentTrajectory`、个人中心 `FragmentPersonal`，入口 `BaseActivity`。
- 无 MVVM / ViewModel / 依赖注入；`viewBinding + dataBinding`。
- 网络：`OkHttpUtils` + 全局拦截器（加 `source: App`、按 `EncryptConfig` 决定加解密）。
- 本地库：Room（轨迹点缓存）。
- 定位：高德定位 SDK + 前台服务。
- 通信：`utils/jtt808/**` 报文编解码 + `utils/netty/**` TCP 客户端，`trajectory/service/JTService` 调度采集与上报。

### 4.2 页面清单（真实类）

| 模块 | 页面 |
|---|---|
| 登录 | `LoginActivity`、`RegisterActivity`、`VerificationCodeActivity`、`WxLoginActivity`、`WxLoginBindPhoneActivity`、`GestureActivity`、`FingerprintLoginActivity` |
| 首页 | `MainActivity`、`FragmentFront`、`FragmentFrontLandHeat`、`FindFarmActivity`、`FarmDetailActivity`、`FindJobActivity`、`JobDetailActivity`、`AddTaskActivity`、`UpdateTaskActivity`、`FreeAgriculturalMachineryActivity`、`DemandFarmWorkActivity`、`ScanCodeActivity`、`ScanAfterActivity` |
| 测量/地块 | `FragmentMeasure`、`MeasureActivity`、`MeasureInfoActivity`、`PlotInfoActivity`、`ItemPlotDetail` |
| 轨迹 | `FragmentTrajectory`、`TrajectoryActivity`、`WorkTrajectoryActivity`、`ItemActivity`、`TrajectoryReuploadActivity` |
| 消息 | `ReceiveSysMessageActivity`、`SysMsgDetailActivity` |
| 个人 | `PersonInfoActivity`、`EditNameActivity`、`SecurityActivity`、`PhoneChangeActivity`、`AuthCodeActivity`、`FeedbackActivity`、`ShowFeedbackActivity`、`CommunicationActivity`、`GpsActivity`、`BackInterfaceActivity`、`ProtocolActivity` |

### 4.3 当前登录实现（已核实）

- 登录页只有手机号输入框，`POST /login?phoneNumber=...`，**不传 captcha、不传密码**。
- 后端 `sms.login.skip-captcha` 默认 `true`，因此直接通过。
- 成功后保存 `token`、`rsa_public_key`，再 `GET /getInfo`。
- 详见 `LOGIN_API_ANALYSIS.md`。

### 4.4 明确缺失（代码中不存在）

| 能力 | 证据 |
|---|---|
| Agent 对话 / 工具调用 / SSE | 全仓库无相关类与依赖 |
| 语音识别 / 语音播报 | 无语音 SDK、无录音权限调用 |
| 结算页面 | App 无 settlement Activity，仅后端有接口 |
| 任务认领 / 开始作业 / 结束作业 | 无按钮级业务流程，只有轨迹采集 |
| 离线业务队列 | 仅轨迹有本地缓存，业务请求无队列 |
| 消息未读数轮询 | 有列表页，无统一未读状态管理 |

---

## 五、GPS 接入服务现状

| 项 | 值 | 依据 |
|---|---|---|
| 启动类 | `org.yzh.web.Application` | `jtt808-server/.../Application.java` |
| HTTP 端口 | `8100` | `application-dev.yml` L31 |
| JT808 TCP/UDP 端口 | `7100`（TCP + UDP） | `application-dev.yml` L36-38 |
| 附件/报警端口 | `7200` | `application-dev.yml` L44-49 |
| TCP 服务构建 | `JTConfig` + `NettyConfig` | `JTConfig.java` L34-50 |
| 位置消息入口 | `JT808Listener` | 原始点写 ES，有效点写 PostgreSQL 暂存表 |
| 有效点暂存 | `effective_point_buffer` | `gps_refactor_ddl.sql` |
| 异步处理 | `AsyncQueueService` 线程池 | 异常仅记录日志 |
| 对外 HTTP | `/media/**` 静态资源 | `WebMvcConfig` L26-31 |

部署侧：`deploy/docker-compose.yml` L164-211，GPS 依赖 PostgreSQL、Elasticsearch、CMS，暴露 `7100/tcp+udp`、`8100`、`7200`。

---

## 六、数据库现状

### 6.1 核心表（真实 DDL）

| 域 | 表 |
|---|---|
| 空间/地块 | `land_plot`、`land_plot_exclude`、`boundary`、`address` |
| 字典 | `crop`、`work`、`crop_work`、`work_type` |
| 农机 | `agricultural_machinery`、`agricultural_machinery_type`、`agricultural_machinery_crop`、`agricultural_machinery_image`、`agricultural_machinery_location` |
| 农活招工 | `farm_work`、`farm_work_crop`、`farm_work_land_plot`、`farm_work_type` |
| 轨迹/作业单 | `trajectory_info`、`trajectory_points`、`trajectory_exclude`、`effective_point_buffer` |
| 结算 | `pricing_rule`、`settlement`、`settlement_item`、`review_request` |
| 反馈/消息 | `sys_feedback`、`sys_feedback_label`、`sys_notification`、`sys_change_log`、`sys_release_note` |
| 其他 | `app_permission`、`screen_statistics`、`sys_user_wechat` |
| 若依基础表 | `sys_user`、`sys_role`、`sys_menu`、`sys_dept`、`sys_user_role` 等（见 `register_business_menu*.sql`） |

### 6.2 关键关系

```
sys_user(phone=username) 1 ── n trajectory_info ── n settlement_item ── 1 settlement
land_plot 1 ── n trajectory_info(plot_id)
trajectory_info 1 ── 1 trajectory_points(trajectory_id)
settlement 1 ── n review_request
```

> `trajectory_info` 是事实上的「作业单」，PRD 任务流需围绕它建模或新增任务表（待产品/后端确认）。

---

## 七、与 PRD 的差距总览

| PRD 能力 | 现状 | 结论 |
|---|---|---|
| 手机号 + 短信验证码登录 | 后端完整支持，默认跳过校验 | 仅需配置，不改代码 |
| 今日任务 / 待接收 / 进行中 | 无任务表与接口（`farm_work` 是招工信息） | **待开发（后端）** |
| 任务认领 / 回执编号 | 无 | **待开发** |
| 开始作业 / 结束作业 / 作业单号 | 仅 `POST /trajectoryInfo` upsert；无开工/完工语义 | **需新增接口或约定** |
| 作业面积核算 | 后端已有轨迹→有效轨迹→结算链路 | 可复用，需核对触发方式 |
| 地块 / 地图 / 边界 | 后端已有，App 有高德实现 | 可复用/迁移 |
| 结算查询 / 明细 / 扣减原因 | 后端已有接口 | 可复用（App 需新建页面） |
| 异常上报 | 复用 `sys_feedback`（类型需扩展） | 可复用 + 字段扩展 |
| 消息通知 | 后端有接口，但存在权限配置风险 | 修复后可复用 |
| 文件上传 | `/common/upload` | 可复用 |
| AI Agent / 语音 / SSE | 完全不存在 | **从零建设** |
| 离线轨迹缓存与补传 | Android 已实现；uni-app 需原生插件 | 需插件化迁移 |

---

## 八、指令与代码的三处冲突（需确认）

1. **「不要假设后端支持短信验证码」 vs 代码**
   代码实际支持：`/captcha` + `/login?captcha=`，仅默认 `skip-captcha=true`。
   建议：UI 按验证码设计，联调时据开关决定是否传 `captcha`，详见 `LOGIN_API_ANALYSIS.md`。

2. **「当前使用 Mock 模拟登录」 vs 真实可用登录**
   真实登录可直接联调（手机号即账号，任意手机号自动注册）。建议 Mock 层保留，但默认走真实接口更贴近验收。

3. **「原 Android 已有任务/作业流程」 vs 代码**
   原 App 没有「任务认领/开工/完工」按钮级流程，只有轨迹采集与回传。PRD 的任务流属于新增业务，必须后端配合建表建接口。

---

## 九、待核实清单

1. `sys_notification` 的类级 `@PreAuthorize` 是否为笔误，机手角色能否访问通知接口。
2. 机手角色（`insertUserAuth(..., 105L)`）是否被授予 `trajectory:*`、`settlement:*` 等权限。
3. 作业单是否需要独立 `work_order` 表，还是继续复用 `trajectory_info`。
4. 有效轨迹生成（`/effectiveTrajectory/byPhoneNumber`）由谁触发：定时任务、结束作业、还是管理端手动。
5. 结算周期任务（`Agri-CMS-quartz`）的实际执行时间与幂等策略。
6. 地图 Key（高德）是否可复用到 uni-app 新包名。
7. 业务库是否允许新增表/字段（任务、异常工单、Agent 会话）。
8. Agent 服务由谁建设、接口契约与部署形态。

---

## 十、结论

- **可复用**：全部 REST 业务接口、登录认证、地块/轨迹/结算数据模型、JTT808/Netty 采集链路、高德地图能力、GPS 服务。
- **需新建（前端）**：uni-app 全部页面、Mock 层、状态管理、Agent 对话 UI。
- **需新建（后端）**：任务/作业单流程接口、异常工单（或扩展 feedback）、Agent/SSE 服务、通知权限修复。
- **最大技术风险**：后台持续定位与离线轨迹可靠性；其次是后端任务流与 Agent 能力缺失。

> 本阶段仅完成分析，未修改任何代码、接口、数据库或配置。
