# 后台管理平台 Vue 3 迁移分析报告

> 分析对象：`AgriNav-CMS-dev-117a7a186108ddd50ee96286ca5b48e94da416bc/Agri-CMS-ui`
> 分析阶段：第一阶段（只分析，未修改任何代码）
> 迁移目标：Vue 2 + Element UI + Vuex + Vue CLI → Vue 3 + Element Plus + Pinia + Vite，页面、业务、接口全部保持不变

---

## 一、原项目技术栈

| 分类 | 现状 | 版本 |
| --- | --- | --- |
| 基础框架 | Vue 2 | 2.6.12 |
| 脚手架/构建 | Vue CLI | 4.4.6 |
| 路由 | Vue Router | 3.4.9 |
| 状态管理 | Vuex | 3.6.0 |
| UI 组件库 | Element UI | 2.15.14 |
| 请求库 | Axios | 0.24.0 |
| 图表 | ECharts | 5.4.x |
| 地图 | @amap/amap-jsapi-loader | 1.0.1 |
| 样式 | Sass（dart-sass） | 1.32.13 |
| 图标 | svg-sprite-loader + 85 个 svg | 5.1.1 |
| 开发端口 | 80（代理到后端 8088） | — |
| 生产构建 | webpack + gzip + splitChunks | — |

项目基于若依（RuoYi-Vue）3.8.8 定制，并在其上叠加了自有业务模块和一套自研 RSA+AES 请求加密体系。

## 二、代码资产盘点

| 目录 | 数量 | 说明 |
| --- | --- | --- |
| `src/views` | 66 个 .vue | 业务页面 + 若依系统/监控/工具页面 |
| `src/components` | 31 个 .vue | 公共组件（分页、字典标签、上传、编辑器等） |
| `src/layout` | 13 个 .vue | 整体布局（侧边栏、导航、页签、设置） |
| `src/api` | 36 个 .js | 接口封装，需原样保留 URL 与参数 |
| `src/utils` | 24 个 .js | 工具函数、request 封装、加密、权限 |
| `src/store` | 6 个模块 | app / dict / user / permission / settings / tagsView |
| `src/directive` | 6 个 .js | v-hasPermi、v-hasRole、拖拽、剪贴板 |
| `src/assets/styles` | 9 个 scss | 全局样式与 Element 变量覆盖 |
| `src/assets/icons/svg` | 85 个 | 图标雪碧图 |

业务页面（非若依自带）约 20 个：地块（area/square/work/heatmap）、农机、作业、轨迹（list/map）、结算（settlement/pricingRule/reviewRequest）、反馈、变更日志、App 组件、大屏、首页 dashboard。

## 三、依赖替换方案

| 原依赖 | 迁移后 | 处理方式 |
| --- | --- | --- |
| vue 2.6 | vue 3.4+ | 全量组件语法迁移 |
| vue-router 3 | vue-router 4 | `addRoutes` → 循环 `addRoute`；删除 `Router.prototype.push` 全局改写 |
| vuex 3 | pinia 2 | 6 个 module 逐个改写 |
| element-ui 2.15 | element-plus 2.x | 组件名/事件/图标类名适配 + 全局样式覆盖 |
| @vue/cli-service | vite 5 | 新建 `vite.config.ts`，环境变量 `VUE_APP_*` → `VITE_*` |
| svg-sprite-loader | vite-plugin-svg-icons | `SvgIcon` 组件改用 `virtual:svg-icons-register` |
| vuedraggable 2 | vuedraggable@4 | 用于拖拽排序页面 |
| vue-count-to | vue3-count-to | 首页数字滚动 |
| vue-cropper 0.5 | vue-cropper@2（Vue3 版） | 头像裁剪 |
| vue-treeselect 0.4 | vue3-treeselect 或 el-tree-select | 部门/菜单树选择 |
| quill 1.3 + 自封装 Editor | quill 2 + 自封装（保留原 API） | 富文本组件 |
| vue-meta | 移除，改用路由守卫设置 `document.title` | 若依只用于标题 |
| echarts 5.4 | 不变 | 仅调整实例挂载/销毁时机 |
| @amap/amap-jsapi-loader | 不变 | 纯 JS 库，Vue 3 直接可用 |
| js-cookie / jsencrypt / crypto-js / file-saver / screenfull / sortablejs / nprogress / fuse.js / highlight.js / clipboard | 全部不变 | 与框架无关 |

## 四、必须重写的部分

1. **入口与全局注册**：`main.js` 的 `Vue.prototype`、`Vue.component`、`Vue.use` → `app.config.globalProperties`、`app.component`、`app.use`。
2. **路由**：`src/router/index.js`（含 `Router.prototype.push` hack）、`src/store/modules/permission.js`（路由生成）、`src/permission.js`（守卫）。
3. **状态管理**：6 个 Vuex 模块 → Pinia store，`getters` 合并入 store 或独立 computed。
4. **布局**：`layout` 下 13 个组件，涉及 `$listeners`、`$children`、`slot-scope`、`beforeDestroy`、`v-model` 修饰符。
5. **权限指令**：`v-hasPermi`/`v-hasRole` 的 `inserted` → `mounted`。
6. **公共组件**：`Pagination`、`RightToolbar`、`DictTag`、`DictData`、`Editor`、`FileUpload`、`ImageUpload`、`ImagePreview`、`SvgIcon`、`ThemePicker`、`Screenfull`、`TreeSelect` 包装等。
7. **66 个业务页面**：统一处理 `data/methods/computed/watch/mixins/filters` → Composition API。
8. **样式体系**：`element-variables.scss`（Element UI 变量）→ Element Plus CSS 变量；`/deep/`、`::v-deep` 适配。
9. **加密体系**：`src/utils/encrypt/*` 逻辑保留，但 `request.js` 中对 `store` 的同步引用需改为惰性获取，避免 Pinia 循环依赖。

## 五、可直接复用的部分

- `src/api/**` 36 个接口文件：URL、method、参数、返回结构完全不变，仅 `import request from '@/utils/request'` 路径保持。
- `src/utils/ruoyi.js`、`auth.js`、`errorCode.js`、`validate.js`、`permission.js`、`dict.js` 等纯函数工具，改造成本极低。
- `src/assets/icons/svg` 85 个图标资源原样复用。
- `src/assets/styles/*.scss` 绝大部分规则可复用，仅 Element 相关变量需替换。
- 所有 ECharts 配置项、高德地图绘制逻辑、业务表结构定义可原样保留。

## 六、兼容性风险清单（按严重程度）

| 级别 | 风险点 | 影响 | 应对 |
| --- | --- | --- | --- |
| 高 | Element UI → Element Plus 视觉差异 | 表格行高、按钮尺寸、表单间距、图标类名（`el-icon-*`）、弹窗圆角均会变化 | 锁定统一 `size`，编写 `element-plus-override.scss` 逐项对齐；图标改用 `@element-plus/icons-vue` 或保留 svg |
| 高 | 动态路由 `addRoutes` | 登录后菜单/页面 404 | 改为遍历 `addRoute`，并处理 `next({...to, replace:true})` 时序 |
| 高 | Vuex → Pinia 的模块依赖 | 守卫、request 拦截器、组件多处依赖 `store.getters.roles` 等 | 统一封装 `useUserStore()` 等，非组件环境用 `setActivePinia` |
| 高 | 自研请求加密 | 加密路径白名单、公钥刷新、加解密时机 | 加密逻辑原样搬迁，只在拦截器中改为懒加载 store |
| 中 | `$set` / `$delete` / `beforeDestroy` / `slot-scope` / `$listeners` / `$children` 共 144 处 | 编译报错或行为异常 | 逐文件替换为 Vue 3 等价写法 |
| 中 | `filters` 语法 | Vue 3 已移除 | 项目仅 `DictTag` 1 处，改为方法/computed |
| 中 | Sass 版本与新语法 | `/deep/` 废弃、除法告警 | 统一 `:deep()`，锁 sass 版本 |
| 中 | 第三方组件库 Vue2 版本 | 拖拽、数字滚动、裁剪、树选择不可用 | 按第三节替换为 Vue 3 版本 |
| 低 | `vue-meta` | 仅控制标题 | 路由守卫内设置 `document.title` |
| 低 | `Vue.observable` / EventBus | 局部使用 | `reactive` / `mitt` |
| 低 | 环境变量名 | `VUE_APP_BASE_API` 等读取失效 | 改为 `VITE_*` 并同步 `.env` |

## 七、迁移实施顺序

1. **环境搭建**：新建 Vue 3 + Vite 工程骨架，配置别名 `@`、代理 `/dev-api` → `http://localhost:8088`、svg 插件、全局 scss。
2. **基础设施**：`utils/request.js`（含加密）、`utils/auth.js`、`utils/ruoyi.js`、`directive`、`plugins`、`errorCode`。
3. **状态与路由**：Pinia store（user/permission/app/dict/settings/tagsView）+ 路由表 + 守卫。
4. **布局层**：`layout` 全部组件（侧边栏、导航、面包屑、页签、设置、AppMain）。
5. **公共组件**：分页、字典、上传、编辑器、图标、树选择、图表封装。
6. **样式体系**：全局样式迁移 + Element Plus 覆盖层，先保证登录页与首页视觉对齐。
7. **业务页面**（按依赖顺序）：
   - 登录 / 绑定手机号 / 首页 / 个人中心
   - 地块（area、square、work、heatmap）
   - 农机、作业、轨迹（list、map）
   - 结算（settlement、pricingRule、reviewRequest）
   - 反馈、变更日志、App 组件、大屏
   - 若依系统管理（用户/角色/菜单/部门/字典/参数/通知/岗位）
   - 监控与工具（在线用户、日志、定时任务、代码生成）
8. **逐模块联调**：登录 → 菜单 → 列表增删改查 → 导入导出 → 地图/图表 → 权限指令。
9. **构建验收**：`npm run dev`、`npm run build`、控制台零报错、接口零异常。

## 八、工作量评估（供排期参考）

| 阶段 | 内容 | 相对工作量 |
| --- | --- | --- |
| 环境 + 基础设施 | 骨架、request、路由、store | 中 |
| 布局 + 公共组件 | 44 个组件 | 大 |
| 业务页面 | 约 20 个核心页 | 大 |
| 若依系统页 | 约 30 个标准页 | 中（模式重复，可批量处理） |
| 样式对齐 | 全站视觉回归 | 大（最易被低估） |
| 联调与修复 | 接口、权限、地图、图表 | 中 |

## 九、迁移实施结果

### 已确认的决策
1. **迁移方式**：新建并行目录 `Agri-CMS-ui-v3`，原 `Agri-CMS-ui` 完全未改动，可随时回退。
2. **语言**：纯 JavaScript，未引入 TypeScript。
3. **迁移范围**：全部 66 个页面（业务页 + 若依系统管理 + 监控 + 代码生成）。

### 完成情况

| 项目 | 结果 |
| --- | --- |
| 技术栈 | Vue 3.4 + Vite 5 + Pinia + Vue Router 4 + Element Plus 2.7 |
| 页面 | 66 个全部迁移为 `<script setup>`，模板结构与样式未改 |
| 布局 | 13 个布局组件全部迁移 |
| 公共组件 | 31 个全部迁移（含 Editor、Pagination、DictTag、上传、图标等） |
| 接口 | 36 个 api 文件零改动，URL/参数/请求头/分页格式保持原样 |
| 加密 | 自研 RSA + AES 请求加密体系完整保留 |
| 权限 | `v-hasPermi` / `v-hasRole`、动态路由、路由守卫、Token 刷新逻辑保留 |
| 样式 | 保留原 SCSS，新增 `element-plus-override.scss` 对齐 Element UI 的尺寸、间距、表格、弹窗、配色 |
| 构建 | `npm run build` 通过，产物输出到 `dist/` |
| 运行 | `npm run dev` 正常，登录页渲染正常、无控制台报错 |

### 迁移中的关键兼容处理
1. **Element UI 图标**：保留 `element-ui` 图标字体（`element-icons.css` + 字体文件），并把 280 个 `el-icon-*` 名称注册为全局组件，使 `<i class="el-icon-plus">` 和 `prefix-icon="el-icon-search"` 等旧写法继续生效。
2. **第三方组件替换**：`vuedraggable@4`、`vue3-count-to`、`vue-cropper@1`；`@riophae/vue-treeselect` 用本地封装 `components/Treeselect`（底层 `el-tree-select`）替代。
3. **Vuex → Pinia**：6 个模块改写为 `app/user/settings/tagsView/dict/permission`，全局方法通过 `globalProperties` 提供，页面侧调用方式基本不变。
4. **`addRoutes` → `addRoute`**：动态路由改为遍历添加，并保留 `next({ ...to, replace: true })` 时序。
5. **Vue 2 专有写法**：`$set/$delete`、`slot-scope`、`$listeners`、`.sync`、`beforeDestroy`、filters、`require()` 全部清理。

### 运行方式

```bash
cd AgriNav-CMS-dev-xxxx/Agri-CMS-ui-v3
npm install
npm run dev     # 默认 http://localhost:81，/dev-api 代理到 http://localhost:8088
npm run build   # 产物在 dist/
```

### 仍需人工回归验证
1. 登录后的动态菜单与权限路由（需要后端 8088 正常启动）。
2. 各地图页（地块、轨迹、热力图）的高德 Key 与覆盖物渲染。
3. 富文本编辑器 Editor 的选图上传。
4. 代码生成页 `tool/gen` 的代码预览与高亮复制。
5. Element Plus 与 Element UI 的细微视觉差异（表格行高、按钮间距）已做了全局覆盖，仍需逐页比对。

## 十、2026-11-24 合并与补充验收

当前 Git 中的前端入口为 `Agri-CMS-ui-v3`，旧目录已由迁移提交移除，不能依赖旧目录继续开发。上文的“原目录完全未改动”描述的是迁移实施时的状态。

本次同步保留了 B-08 轨迹回放、B-09 边界判断及轨迹裁剪的后端和作业助手接口，并将地块 GeoJSON / Shapefile 导入、行政村 / 片区分组、地块详情接回 Vue 3 页面。表格分别标注行政村和片区，避免将其中一类未分组误认为全部未分组。

登录后联调还修复了以下迁移兼容问题：

- 后端外链菜单保留在侧边栏，但不作为站内路由注册；路由加载失败会向调用方传递错误。
- 布局中的侧栏状态变量、标签栏中的滚动引用与组件重名，导致菜单空白和递归更新，现已分开命名。
- 侧边菜单在计算单个可见子菜单时不再修改渲染依赖；首页链接与固定标签使用绝对路径。
- 补齐 Element Plus 弹窗、插槽、单选按钮和组件卸载时的地图清理。

本次验收范围：

| 检查 | 结果 |
| --- | --- |
| 后端 Maven 测试与打包 | 154 项测试通过，构建成功 |
| 前端坐标与菜单路由测试 | 11 项通过 |
| 真实网页与后端 | 登录、动态菜单、地块列表、两种格式示例预检通过，未执行正式导入 |
| 地图详情 | 坐标转换、卫星图切换、测距、全屏、关闭重开及移动布局通过 |
| 分组交互 | 模拟创建、两类归组、筛选、移出当前类别通过；延迟的旧请求不会覆盖重置结果 |

分组交互的四次写请求由浏览器测试模拟应答，没有写入共享地块或分组。其他业务页面、上传和代码生成等仍按上文列出的范围另行回归。

本地开发需要在新版 `.env.development` 中配置 `VITE_APP_AMAP_KEY`、`VITE_APP_AMAP_SECURITY_CODE`，默认访问端口为 81。真实密码和地图密钥只保存在被 Git 忽略的本地环境文件中。

