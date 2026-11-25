# NATIVE_PLUGIN_PLAN.md — 原生插件规划（第一阶段）

> 背景：uni-app 的 JS 层无法可靠完成后台持续定位、TCP 长连接、批量本地写入与高负载地图渲染。
> 原则：优先复用原 Android 项目的 Java 代码；不删除原工程；插件与业务 API 通过事件/接口解耦。
> 目标平台：Android（minSdk 26 / targetSdk 34，与原 App 一致）

---

## 一、能力矩阵

| 能力 | uni-app 原生能力 | 结论 | 插件 |
|---|---|---|---|
| 前台/后台持续定位 | `uni.getLocation` 仅前台、易中断 | ❌ 不可用 | `agri-location` |
| JTT808 报文编解码 | 无 | ❌ | `agri-jtt808` |
| TCP 长连接（Netty） | 无 | ❌ | `agri-jtt808` |
| 本地轨迹批量存储 | `plus.sqlite` 性能弱 | 🟡 可用但不稳 | `agri-storage` |
| 网络恢复自动补传 | `uni.onNetworkStatusChange` 可触发 | ✅ 可配合插件 | `agri-jtt808` + JS 状态机 |
| 地图基础展示/折线/多边形 | `<map>` 组件 | ✅ | 无 |
| 热力图/大规模轨迹渲染 | `<map>` 能力有限 | 🟡 视效果 | `agri-map`（可选） |
| 语音识别 ASR | 无 | ❌ | `agri-voice` |
| 语音播报 TTS | 无（可用系统 TTS） | 🟡 | `agri-voice` |
| RSA/AES 传输加密 | JS 可实现，但密钥安全性下降 | 🟡 当前后端已关闭 | 暂不做插件 |
| 文件上传 | `uni.uploadFile` | ✅ | 无 |
| 消息推送 | 需厂商推送 | ❌ 未规划 | 待定 |

---

## 二、插件拆分

### 2.1 `agri-location`（P0）

职责：后台持续定位、权限引导、状态事件、作业会话绑定。

对外 API（JS 侧）：

```ts
interface AgriLocation {
  requestPermission(): Promise<{ granted: boolean; background: boolean }>
  start(options: {
    interval: number        // 秒，建议 5–10
    minDistance: number     // 米
    mode: 'high' | 'balanced'
    sessionId?: string      // 作业单号
  }): Promise<void>
  stop(): Promise<void>
  getStatus(): Promise<{
    running: boolean
    gpsEnabled: boolean
    permission: 'granted' | 'denied' | 'foregroundOnly'
    lastPoint?: { lat: number; lng: number; time: number; speed: number }
  }>
  onPoint(cb: (p: Point) => void): void
  onError(cb: (e: { code: string; message: string }) => void): void
}
```

实现要点：
- 前台服务 + 常驻通知「作业中」；`FOREGROUND_SERVICE_LOCATION`。
- 高德定位 SDK（复用原 App 依赖与 Key）。
- 定位点先交 `agri-storage` 落库，再交 `agri-jtt808` 发送。
- 处理 Android 12+ 后台启动限制、厂商省电白名单引导。
- 作业开始/结束由 JS 调用 `start/stop`，与作业状态机一致。

复用代码：
- `trajectory/service/JTService.java`（调度逻辑）
- 原 App 定位初始化与权限处理
- `FragmentTrajectory` 中的定位客户端配置

### 2.2 `agri-jtt808`（P0）

职责：JTT808 组包/解包、TCP 连接、心跳、注册、位置汇报、断线重连、批量补传。

对外 API：

```ts
interface AgriJtt808 {
  connect(options: { host: string; port: number; terminalId: string }): Promise<void>
  disconnect(): Promise<void>
  sendPositions(points: Point[]): Promise<{ accepted: number }>
  flushQueue(): Promise<{ pending: number; uploaded: number }>
  getStatus(): Promise<{
    connected: boolean
    registered: boolean
    pending: number
    lastHeartbeat?: number
  }>
  onStatus(cb: (s: Status) => void): void
}
```

实现要点：
- 复用 `utils/jtt808/**`（T0002/T0003/T0100/T0102/T0200/T0801 等）。
- 复用 `utils/netty/**`（`NettyClient`、编解码器、重连）。
- 终端号与车牌/设备号映射：**待确认**（原 App 从配置读取）。
- 发送失败进入本地队列，指数退避重试；去重键 `clientTraceId`。
- 与 GPS 服务 `7100/tcp` 对接，协议不变。

### 2.3 `agri-storage`（P0/P1）

职责：轨迹点本地持久化、待上传队列、批量查询、过期清理。

对外 API：

```ts
interface AgriStorage {
  init(): Promise<void>
  appendPoints(sessionId: string, points: Point[]): Promise<void>
  queryPending(limit: number): Promise<Point[]>
  markUploaded(ids: string[]): Promise<void>
  markFailed(ids: string[], reason: string): Promise<void>
  stats(): Promise<{ total: number; pending: number; failed: number; bytes: number }>
  cleanup(days: number): Promise<{ removed: number }>
}
```

实现要点：
- 首选复用原 Room 数据库（`trajectory/dao/*`），插件内聚；备选 `plus.sqlite`。
- 表结构对齐 `trajectory_points`：`id/session_id/lat/lng/time/speed/accuracy/status/retry/client_trace_id`。
- 批量事务写入，避免逐点 IO。
- 默认保留 7 天（原 App 3 天清理，可配置）。

### 2.4 `agri-voice`（P1）

职责：语音识别（ASR）与播报（TTS）。

对外 API：

```ts
interface AgriVoice {
  startRecord(): Promise<void>
  stopRecord(): Promise<{ text: string; confidence: number }>
  cancelRecord(): Promise<void>
  speak(text: string): Promise<void>
  stopSpeak(): Promise<void>
  onState(cb: (s: 'idle' | 'recording' | 'recognizing' | 'speaking') => void): void
}
```

实现要点：
- ASR 供应商未定（讯飞/百度/系统），**待确认**。
- 当前阶段可先由 JS Mock 返回识别文本，插件接口保持稳定。
- TTS 可先用系统 `TextToSpeech`，减少依赖。

### 2.5 `agri-map`（P2，可选）

职责：热力图、大规模轨迹渲染、轨迹回放性能优化。

- 若 `<map>` 组件满足演示，则不开发。
- 若不满足，封装高德地图 View，复用原 App `PolygonUtils` 与绘制逻辑。
- 需评估与 uni-app 原生页面的混合渲染成本。

---

## 三、插件与 JS 的边界

| 能力 | 放在插件 | 放在 JS |
|---|---|---|
| 定位采集 | ✅ | — |
| JTT808 组包/发送 | ✅ | — |
| 本地轨迹库 | ✅ | — |
| 上传时机/重试策略 | — | ✅（状态机） |
| 作业开始/结束业务 | — | ✅（API 调用） |
| 网络状态监听 | 原生回调 | ✅（`uni.onNetworkStatusChange`） |
| 权限引导文案 | — | ✅ |
| 轨迹渲染 | 可选 | ✅（`<map>`） |
| 业务数据请求 | — | ✅（`uni.request`） |

---

## 四、权限与合规

| 权限 | 用途 | 处理 |
|---|---|---|
| `ACCESS_FINE_LOCATION` | 定位 | 运行时申请 |
| `ACCESS_COARSE_LOCATION` | 辅助定位 | 运行时申请 |
| `ACCESS_BACKGROUND_LOCATION` | 后台定位 | Android 10+ 二次引导 |
| `FOREGROUND_SERVICE` | 前台服务 | 清单声明 |
| `FOREGROUND_SERVICE_LOCATION` | 定位前台服务 | 清单声明 |
| `POST_NOTIFICATIONS` | 常驻通知（Android 13+） | 运行时申请 |
| `RECORD_AUDIO` | 语音输入 | 运行时申请 |
| `INTERNET` / `ACCESS_NETWORK_STATE` | 网络 | 清单声明 |
| `WAKE_LOCK` | 保持唤醒 | 清单声明（谨慎） |

合规要求：
- 首次进入作业页前展示定位用途说明。
- 拒绝后台定位时降级为「仅前台采集」并明确提示影响计费。
- 不上传与业务无关的隐私数据。

---

## 五、原生工程集成方式

1. 新建 uni-app 项目，`manifest.json` 配置包名（建议沿用 `cn.agrinav` 需确认签名冲突）。
2. 使用 HBuilderX 本地打包或离线打包（Android Studio）。
3. 插件以 `uni_modules/agri-*` 形式引入，内部为 Android Library。
4. 原 Android 代码以「复制 + 包名调整」方式进入插件，不直接依赖原 APK。
5. 地图/定位 Key 绑定新包名与签名，需在高德控制台更新。

---

## 六、复用代码映射

| 原代码 | 去向 | 改造点 |
|---|---|---|
| `utils/jtt808/**` | `agri-jtt808` | 包名、日志、线程模型 |
| `utils/netty/**` | `agri-jtt808` | 连接参数改为 JS 注入 |
| `trajectory/service/JTService.java` | `agri-location` / `agri-jtt808` | 去掉 Activity 依赖，改 Service/前台服务 |
| `trajectory/dao/*`、`database/*` | `agri-storage` | 暴露批量 API |
| `utils/encryption/**` | 暂不迁移 | 后端加密关闭；恢复后再评估 |
| 高德定位/地图初始化 | `agri-location` / `agri-map` | Key、权限、生命周期 |
| `trajectory/util/DataUtil` 轨迹解析 | JS 重写 | 只保留数据转换 |

---

## 七、实施步骤

1. 建插件骨架，跑通「JS 调用 → 原生 Toast/回调」。
2. `agri-location`：前台服务 + 定时回调，真机锁屏验证 30 分钟。
3. `agri-storage`：批量写入 + 查询 + 清理，验证 10 万点性能。
4. `agri-jtt808`：连接 GPS 服务、注册、心跳、位置上报，抓包验证。
5. 串联：定位 → 落库 → 上报 → 标记；断网 → 恢复 → 补传。
6. `agri-voice`：先 Mock，后接供应商。
7. `agri-map`：按演示效果决定是否开发。

---

## 八、验收标准

| 项 | 标准 |
|---|---|
| 后台定位 | 锁屏/切后台 30 分钟持续采点，间隔误差 < 20% |
| 断网缓存 | 断网 4 小时不丢点，恢复后自动补传 |
| 去重 | 同一 `clientTraceId` 不重复入库/上传 |
| 电量 | 连续作业 4 小时耗电可接受（对比原 App） |
| 兼容 | 覆盖 Android 10/12/14 各至少 1 台真机 |
| 权限 | 拒绝后台定位时有明确降级提示 |
| 协议 | 与 GPS 服务 7100 端口联通，轨迹可在后端查询 |

---

## 九、风险与对策

| 风险 | 级别 | 对策 |
|---|---|---|
| 厂商省电杀进程 | P0 | 前台服务 + 引导白名单 + 心跳自检 + 恢复重启 |
| Android 12+ 前台服务限制 | P0 | 按官方规范声明类型与启动时机 |
| 定位漂移影响面积 | P0 | 精度过滤 + 后端有效轨迹算法（已有） |
| TCP 断线丢点 | P0 | 本地先落库，发送成功才标记 |
| 重复上传/重复建单 | P1 | `clientTraceId` + 后端唯一约束（待配合） |
| 语音 SDK 授权与噪声 | P1 | 供应商评估 + 文本兜底 |
| 地图 Key/包名变更 | P2 | 提前在高德控制台配置新签名 |
| 插件与原 App 代码分叉 | P2 | 以插件为主，原工程仅作参考，不做双向同步 |

> 本阶段只做规划，不创建插件工程、不修改原 Android 代码。
