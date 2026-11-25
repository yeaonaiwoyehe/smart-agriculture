# 田界智航 AgriNav 农机作业管控平台 · 统一接口清单（前后端契约）

> 本清单由 springdoc OpenAPI 自动生成，是 Web 前端（C）与 Android（D）对接后端的统一契约。
> 在线可交互文档：`/swagger-ui.html`；机器可读 OpenAPI JSON：`/v3/api-docs`。

## 一、通用约定

| 项目 | 说明 |
| --- | --- |
| 后端服务地址（本地） | `http://localhost:8088` |
| 后端服务地址（云端） | `http://82.156.224.217`（经 Nginx，前缀 `/prod-api`） |
| 认证方式 | 请求头 `Authorization: Bearer {token}`，登录接口返回 token |
| 数据格式 | 请求/响应均为 `application/json;charset=UTF-8` |
| 统一返回 | `{ "code": 200, "msg": "操作成功", "data": ... }` |
| 分页返回 | `{ "code":200, "rows":[...], "total": 总数 }`，分页参数 `pageNum`、`pageSize` |
| 常用状态码 | 200 成功；401 未登录/登录过期；403 无权限；500 服务异常（HTTP 状态统一为 200，以 body.code 为准） |

## 二、接口分组清单

### 01-认证与登录（14 个接口）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | /captcha | 给指定手机号发送验证码，有效时长3min |
| GET | /getInfo | 获取当前用户信息 |
| GET | /getRouters | 获取路由信息 |
| POST | /login | 手机号登录注册 |
| GET | /loginOAuth | loginOAuth |
| POST | /loginQR | loginByQRCode |
| GET | /loginWechatMobile | loginWechatMobile |
| GET | /loginWithoutSecret | 免密登录 |
| DELETE | /render | render_5 |
| GET | /render | render |
| PATCH | /render | render_4 |
| POST | /render | render_2 |
| PUT | /render | render_3 |
| DELETE | /signOut/{username} | 注销用户 |

### 02-农机管理（9 个接口）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| PUT | /agriculturalMachinery | 更新农机找活信息 |
| DELETE | /agriculturalMachinery/{id} | 下架农机找活信息 |
| POST | /agriculturalMachinery/add | 新增农机找活信息 |
| POST | /agriculturalMachinery/list | 查询农机找活信息 |
| DELETE | /agriculturalMachinery/signOut/{username} | 注销该用户的农机找活信息 |
| PUT | /agriculturalMachineryType | 更新农机类型 |
| DELETE | /agriculturalMachineryType/{id} | 删除农机类型 |
| POST | /agriculturalMachineryType/add | 新增农机类型 |
| GET | /agriculturalMachineryType/list | 查询农机类型列表 |

### 03-农活招工（7 个接口）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| PUT | /farmWork | 更新寻找农机 |
| DELETE | /farmWork/{id} | 下架寻找农机 |
| POST | /farmWork/add | 新增寻找农机 |
| POST | /farmWork/list | 查询寻找农机 |
| DELETE | /farmWork/signOut/{username} | 注销该用户的地块 |
| GET | /workingLandPlot | queryWorkingLandPlot |
| POST | /workingLandPlot/workingTrajectory | queryWorkingTrajectoryOfLandPlot |

### 04-地块管理（32 个接口）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | /landAddress/getCity | 获取城市列表 |
| POST | /landAddress/getDistrict | 获取区县列表 |
| POST | /landAddress/getHamlet | 获取村镇列表 |
| GET | /landAddress/getProvince | 获取省份列表 |
| POST | /landAddress/getVillage | 获取乡镇列表 |
| GET | /landPlot | 查询当前用户或部门下所有地块信息 |
| POST | /landPlot | 新增地块 |
| PUT | /landPlot | 修改地块信息 |
| DELETE | /landPlot/{id} | 删除地块 |
| GET | /landPlot/{id} | 根据地块id查询地块信息 |
| PUT | /landPlot/{id} | restoreLandPlot |
| GET | /landPlot/area | 返回总体面积 |
| GET | /landPlot/areaByAddress | 根据地址返回该地址内地块的面积之和 |
| GET | /landPlot/byCoordinate | 根据经纬度查询地块信息 |
| GET | /landPlot/boundary/check | B-09：点在界内／边界／界外／排除区判断，边界不可用时明确返回未知 |
| POST | /landPlot/boundary/clip | B-09：只读轨迹线段裁剪与米制长度，详见 database/B09边界裁剪接口说明.md |
| GET | /landPlot/groups | 团队分组字典及本人有效地块的分组统计 |
| POST | /landPlot/groups | 创建行政村或片区字典项，相同编号不得使用不同名称 |
| PUT | /landPlot/groups/assignment | 整批归组，groupCode为null时移出该类分组；只允许修改本人有效地块 |
| GET | /landPlot/groups/assignments | 查询指定本人地块的行政村与片区 |
| GET | /landPlot/heatmap | 获取地块热力图 |
| GET | /landPlot/history | findHistoryLandPlots |
| POST | /landPlot/import/geojson | GeoJSON 地块导入或文件预检（默认仅预检） |
| POST | /landPlot/import/shapefile | Shapefile ZIP 预检或导入（默认仅预检） |
| GET | /landPlot/query | 根据条件查询地块信息 |
| GET | /landPlot/queryByAddress | 根据位置模糊查询地块信息 |
| GET | /landPlot/queryByAddressAll | 根据地址查询所有地块 |
| GET | /landPlot/remeasure | 查询当前指定地块的复测历史信息 |
| DELETE | /landPlot/signOut/{username} | deleteByUsername |
| POST | /landPlotExclude/addLandPlotExclude | 新增无用区域 |
| POST | /landPlotExclude/batchQuery | 批量查询无用区域 |
| GET | /landPlotExclude/queryByLandPlotId | 通过地块id查询无用区域 |

### 05-作业类型与作物（10 个接口）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | /crop/addCrop | 添加新的作物 |
| DELETE | /crop/deleteCrop | 删除作物 |
| GET | /crop/getCrop | 查看数据库中可用的作物列表 |
| POST | /crop/updateCrop | 修改作物信息 |
| POST | /fieldWork/addWork | 添加新的工作类型 |
| DELETE | /fieldWork/deleteWork | 删除工作类型 |
| GET | /fieldWork/getWork | 查看数据库中可用的工作类型列表 |
| GET | /fieldWork/list | 获取全部可用工作类型 |
| GET | /fieldWork/listUnique | 查询所有唯一的工作类型 |
| POST | /fieldWork/updateWork | 修改工作类型 |

### 06-轨迹管理（21 个接口）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | /effectiveTrajectory | 查询当前用户的轨迹 |
| GET | /effectiveTrajectory/byPhoneNumber | 生成轨迹 |
| POST | /effectiveTrajectory/conditions | 通过条件查询轨迹信息 |
| DELETE | /effectiveTrajectory/signOut/{username} | 注销该用户所有轨迹 |
| DELETE | /originalTrajectory/deleteByUserName | 删除用户轨迹 |
| DELETE | /originalTrajectory/queryOriginalTrajectory | 查询原始轨迹 |
| GET | /originalTrajectory/queryOriginalTrajectory | 查询原始轨迹 |
| PATCH | /originalTrajectory/queryOriginalTrajectory | 查询原始轨迹 |
| POST | /originalTrajectory/queryOriginalTrajectory | 查询原始轨迹 |
| PUT | /originalTrajectory/queryOriginalTrajectory | 查询原始轨迹 |
| DELETE | /originalTrajectory/queryOriginalTrajectoryByPhoneAndTime | 根据手机号时间查询原始轨迹 |
| GET | /originalTrajectory/queryOriginalTrajectoryByPhoneAndTime | 根据手机号时间查询原始轨迹 |
| PATCH | /originalTrajectory/queryOriginalTrajectoryByPhoneAndTime | 根据手机号时间查询原始轨迹 |
| POST | /originalTrajectory/queryOriginalTrajectoryByPhoneAndTime | 根据手机号时间查询原始轨迹 |
| PUT | /originalTrajectory/queryOriginalTrajectoryByPhoneAndTime | 根据手机号时间查询原始轨迹 |
| POST | /trajectoryInfo | 上传轨迹信息 |
| GET | /trajectoryInfo/create | 生成轨迹id |
| GET | /trajectoryInfo/picture | getTrajectoryPictures |
| GET | /trajectory/playback | B-08：分别查询原始或已归档有效轨迹，按作业单或终端时间段回放；详见 database/B08轨迹回放接口说明.md |
| POST | /trajectory/accounting/preview | B-10：按地块、厘米幅宽及分段轨迹只读试算，返回覆盖去重及边界扣减明细；详见 database/B10幅宽核算接口说明.md |
| GET | /trajectory/accounting | B-10：按作业单归档与记录幅宽重算，返回历史面积差值，不回填结算 |

### 07-作业调度（9 个接口）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | /dispatch | 单选派单 |
| GET | /dispatch/{id} | 派单详情 |
| PUT | /dispatch/{id}/accept | 机手接单 |
| PUT | /dispatch/{id}/cancel | 取消派单 |
| PUT | /dispatch/{id}/complete | 完成派单 |
| POST | /dispatch/batch | 批量派单 |
| GET | /dispatch/list | 派单列表 |
| GET | /dispatch/stats | 完成率统计 |
| POST | /dispatch/timeout | 手动刷新超时标记 |

### 08-内部结算（16 个接口）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | /pricingRule | 新增单价规则 |
| PUT | /pricingRule | 修改单价规则 |
| GET | /pricingRule/{id} | 查询单价规则详情 |
| DELETE | /pricingRule/{ids} | 删除单价规则 |
| GET | /pricingRule/list | 查询单价规则列表 |
| POST | /reviewRequest | 提交复核申请 |
| GET | /reviewRequest/{id} | 查询复核申请详情 |
| PUT | /reviewRequest/final | 管理员终审 |
| PUT | /reviewRequest/initial | 队长初核 |
| GET | /reviewRequest/list | 查询复核申请列表 |
| GET | /settlement/{id} | 查询结算单详情 |
| POST | /settlement/export/{id} | 导出结算明细Excel |
| POST | /settlement/generate | 生成结算单 |
| GET | /settlement/items/{id} | 查询结算单明细 |
| GET | /settlement/list | 查询结算单列表 |
| PUT | /settlement/status | 更新结算单状态 |

### 09-智能体（10 个接口）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | /agent/audit/{id} | 查询审计记录详情 |
| GET | /agent/audit/list | 分页查询审计记录 |
| GET | /agent/conversation/{id} | 查询会话详情（含全部消息） |
| GET | /agent/conversation/list | 分页查询会话列表 |
| GET | /llm/agent | agent |
| GET | /llm/chat | chat |
| GET | /llm/intent | intent |
| GET | /llm/route | route |
| GET | /llm/tool | tool |
| GET | /llm/tool/confirm | toolConfirm |

### 10-系统管理（113 个接口）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | /permission/add | 添加组件 |
| GET | /permission/check | 返回当前的所有组件 |
| DELETE | /permission/delete | 删除组件 |
| GET | /permission/get/{id} | 根据id获取组件 |
| GET | /permission/list | 返回所有的组件 |
| POST | /permission/update | 更新组件是否可见 |
| POST | /system/changelog | add_10 |
| PUT | /system/changelog | edit_10 |
| DELETE | /system/changelog/{id} | remove_10 |
| GET | /system/changelog/{id} | getInfo_11 |
| GET | /system/changelog/list/{releaseNoteId} | list_10 |
| POST | /system/config | add_9 |
| PUT | /system/config | edit_9 |
| GET | /system/config/{configId} | getInfo_10 |
| DELETE | /system/config/{configIds} | remove_9 |
| GET | /system/config/configKey/{configKey} | getConfigKey |
| POST | /system/config/export | export_5 |
| GET | /system/config/list | list_9 |
| DELETE | /system/config/refreshCache | refreshCache_1 |
| POST | /system/dept | add_8 |
| PUT | /system/dept | edit_8 |
| DELETE | /system/dept/{deptId} | remove_8 |
| GET | /system/dept/{deptId} | getInfo_9 |
| GET | /system/dept/list | list_8 |
| GET | /system/dept/list/exclude/{deptId} | excludeChild |
| POST | /system/dict/data | add_7 |
| PUT | /system/dict/data | edit_7 |
| GET | /system/dict/data/{dictCode} | getInfo_8 |
| DELETE | /system/dict/data/{dictCodes} | remove_7 |
| POST | /system/dict/data/export | export_4 |
| GET | /system/dict/data/list | list_7 |
| GET | /system/dict/data/type/{dictType} | dictType |
| POST | /system/dict/type | add_6 |
| PUT | /system/dict/type | edit_6 |
| GET | /system/dict/type/{dictId} | getInfo_7 |
| DELETE | /system/dict/type/{dictIds} | remove_6 |
| POST | /system/dict/type/export | export_3 |
| GET | /system/dict/type/list | list_6 |
| GET | /system/dict/type/optionselect | optionselect_2 |
| DELETE | /system/dict/type/refreshCache | refreshCache |
| POST | /system/feedback | 提交反馈信息 |
| POST | /system/feedback/check | 确认反馈 |
| GET | /system/feedback/labels | getLabels |
| POST | /system/feedback/list | 获取反馈列表 |
| GET | /system/feedback/myFeedback | 获取我的反馈 |
| POST | /system/key/exchange | exchangeKey |
| GET | /system/key/public | getPublicKey |
| POST | /system/menu | add_5 |
| PUT | /system/menu | edit_5 |
| DELETE | /system/menu/{menuId} | remove_5 |
| GET | /system/menu/{menuId} | getInfo_6 |
| GET | /system/menu/list | list_5 |
| GET | /system/menu/roleMenuTreeselect/{roleId} | roleMenuTreeselect |
| GET | /system/menu/treeselect | treeselect |
| POST | /system/notice | add_4 |
| PUT | /system/notice | edit_4 |
| GET | /system/notice/{noticeId} | getInfo_5 |
| DELETE | /system/notice/{noticeIds} | remove_4 |
| GET | /system/notice/list | list_4 |
| DELETE | /system/notification | clearNotification |
| GET | /system/notification | getNotificationList |
| PUT | /system/notification | readAllNotification |
| DELETE | /system/notification/{id} | deleteNotification |
| PUT | /system/notification/{id} | readNotification |
| GET | /system/notification/unread | getUnreadNotificationList |
| POST | /system/post | add_3 |
| PUT | /system/post | edit_3 |
| GET | /system/post/{postId} | getInfo_4 |
| DELETE | /system/post/{postIds} | remove_3 |
| POST | /system/post/export | export_2 |
| GET | /system/post/list | list_3 |
| GET | /system/post/optionselect | optionselect_1 |
| POST | /system/releaseNote | add_2 |
| PUT | /system/releaseNote | edit_2 |
| DELETE | /system/releaseNote/{id} | remove_2 |
| GET | /system/releaseNote/{id} | getInfo_3 |
| GET | /system/releaseNote/list | list_2 |
| POST | /system/role | add_1 |
| PUT | /system/role | edit_1 |
| GET | /system/role/{roleId} | getInfo_2 |
| DELETE | /system/role/{roleIds} | remove_1 |
| GET | /system/role/authUser/allocatedList | allocatedList |
| PUT | /system/role/authUser/cancel | cancelAuthUser |
| PUT | /system/role/authUser/cancelAll | cancelAuthUserAll |
| PUT | /system/role/authUser/selectAll | selectAuthUserAll |
| GET | /system/role/authUser/unallocatedList | unallocatedList |
| PUT | /system/role/changeStatus | changeStatus_1 |
| PUT | /system/role/dataScope | dataScope |
| GET | /system/role/deptTree/{roleId} | deptTree_1 |
| POST | /system/role/export | export_1 |
| GET | /system/role/list | list_1 |
| GET | /system/role/optionselect | optionselect |
| POST | /system/user | add |
| PUT | /system/user | edit |
| GET | /system/user/ | getInfo_1 |
| GET | /system/user/{userId} | getInfo |
| DELETE | /system/user/{userIds} | remove |
| PUT | /system/user/authRole | insertAuthRole |
| GET | /system/user/authRole/{userId} | authRole |
| POST | /system/user/bind | bindingUser |
| PUT | /system/user/changeStatus | changeStatus |
| GET | /system/user/check/{phoneNumber} | checkUser |
| GET | /system/user/deptTree | deptTree |
| POST | /system/user/export | export |
| POST | /system/user/importData | importData |
| POST | /system/user/importTemplate | importTemplate |
| GET | /system/user/list | list |
| GET | /system/user/profile | 获取个人信息 |
| PUT | /system/user/profile | 修改用户 |
| POST | /system/user/profile/avatar | 更新头像 |
| PUT | /system/user/profile/phoneNumber | 修改手机号 |
| PUT | /system/user/profile/updatePwd | 重置密码 |
| PUT | /system/user/resetPwd | resetPwd |

### 11-监控运维（32 个接口）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | /monitor/cache | getInfo_3 |
| DELETE | /monitor/cache/clearCacheAll | clearCacheAll |
| DELETE | /monitor/cache/clearCacheKey/{cacheKey} | clearCacheKey |
| DELETE | /monitor/cache/clearCacheName/{cacheName} | clearCacheName |
| GET | /monitor/cache/getKeys/{cacheName} | getCacheKeys |
| GET | /monitor/cache/getNames | cache |
| GET | /monitor/cache/getValue/{cacheName}/{cacheKey} | getCacheValue |
| POST | /monitor/job | add |
| PUT | /monitor/job | edit |
| GET | /monitor/job/{jobId} | getInfo_2 |
| DELETE | /monitor/job/{jobIds} | remove_3 |
| PUT | /monitor/job/changeStatus | changeStatus |
| POST | /monitor/job/export | export_3 |
| GET | /monitor/job/list | list_4 |
| PUT | /monitor/job/run | run |
| GET | /monitor/jobLog/{jobLogId} | getInfo_1 |
| DELETE | /monitor/jobLog/{jobLogIds} | remove_2 |
| DELETE | /monitor/jobLog/clean | clean_2 |
| POST | /monitor/jobLog/export | export_2 |
| GET | /monitor/jobLog/list | list_3 |
| DELETE | /monitor/logininfor/{infoIds} | remove_1 |
| DELETE | /monitor/logininfor/clean | clean_1 |
| POST | /monitor/logininfor/export | export_1 |
| GET | /monitor/logininfor/list | list_2 |
| GET | /monitor/logininfor/unlock/{userName} | unlock |
| DELETE | /monitor/online/{tokenId} | forceLogout |
| GET | /monitor/online/list | list_1 |
| DELETE | /monitor/operlog/{operIds} | remove |
| DELETE | /monitor/operlog/clean | clean |
| POST | /monitor/operlog/export | export |
| GET | /monitor/operlog/list | list |
| GET | /monitor/server | getInfo |

### 12-统计分析（29 个接口）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | /analysis/trajectory/overview | getTrajectoryAnalysisOverview |
| GET | /analysis/trajectory/quality/{plotId} | getTrajectoryQuality |
| GET | /analysis/trajectory/time-range | getTrajectoryAnalysisByTimeRange |
| GET | /analysis/user/authenticity | analyzeOneUserAuthenticity |
| GET | /analysis/user/authenticity/all | analyzeAllUsersAuthenticity |
| GET | /analysis/user/high-risk | getHighRiskUsers |
| GET | /analysis/user/one | analyzeUserActivity |
| GET | /analysis/user/overview | getOverview |
| GET | /analysis/user/top-active | getTopActiveUsers |
| GET | /screen/getActiveUserPercentage | getActiveUserPercentage |
| GET | /screen/getBiggestFieldWorkType | getBiggestFieldWorkType |
| GET | /screen/getCropSituation | getCropSituation |
| GET | /screen/getFarmWorkInfo | getFarmWorkInfo |
| GET | /screen/getFinishedFarmWorkPercentage | getFinishedFarmWorkPercentage |
| GET | /screen/getLandPlotCountByProvice | getLandPlotCountInProvince |
| GET | /screen/getLandPlotCountInDetailedProvince | getLandPlotCountInDetailedProvince |
| GET | /screen/getLandPlotNumByMonth | getLandPlotNumByMonth |
| GET | /screen/getLandPlotsTotalArea | getLandPlotsTotalArea |
| GET | /screen/getLandPlotsWithinProvince | getLandPlotsWithinProvince |
| GET | /screen/getMostCrop | getMostCrop |
| GET | /screen/getMostFarmWorkProvince | getMostFarmWorkProvince |
| GET | /screen/getMostFieldWorkType | getMostFieldWorkType |
| GET | /screen/getScreenStatistics | getScreenStatistics |
| GET | /screen/getTrajectoryNumByMonth | getTrajectoryNumByMonth |
| GET | /screen/refresh | refreshScreenStatistics |
| GET | /static/user/dau | getUserStatistics |
| GET | /static/user/lc | getUserLoginCount |
| GET | /static/user/location | getUserLocation |
| GET | /static/user/rc | getUserRegisterCount |

### 13-通用与测试（11 个接口）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | /common/download | fileDownload |
| GET | /common/download/resource | resourceDownload |
| POST | /common/upload | uploadFile |
| POST | /common/uploads | uploadFiles |
| DELETE | /test/user/{userId} | 删除用户信息 |
| GET | /test/user/{userId} | 获取用户详细 |
| GET | /test/user/list | 获取用户列表 |
| POST | /test/user/save | 新增用户 |
| PUT | /test/user/update | 更新用户 |
| GET | /version | getVersionCode |
| POST | /version | updateVersionCode |

## 三、分组统计

| 分组 | 接口数 |
| --- | --- |
| 01-认证与登录 | 14 |
| 02-农机管理 | 9 |
| 03-农活招工 | 7 |
| 04-地块管理 | 30 |
| 05-作业类型与作物 | 10 |
| 06-轨迹管理 | 21 |
| 07-作业调度 | 9 |
| 08-内部结算 | 16 |
| 09-智能体 | 10 |
| 10-系统管理 | 113 |
| 11-监控运维 | 32 |
| 12-统计分析 | 29 |
| 13-通用与测试 | 11 |
| **合计** | **311** |

