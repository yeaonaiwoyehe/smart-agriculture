# 农机手 App 登录接口分析（只读核实）

> 分析对象：`AgriNav-CMS-dev-*`（Spring Boot 3.2.6 后端）与 `AgriNav-APP-master-*`（现有 Android App）
> 分析方式：纯静态代码阅读，**未修改任何代码、接口、数据库或配置文件**
> 目的：为 uni-app + Vue 3 重建 App 提供与现有后端 100% 兼容的登录对接依据

---

## 0. 结论速览（六个必答问题）

| # | 问题 | 结论 |
|---|---|---|
| 1 | `/login` 支持什么登录方式？ | **手机号登录（登录即注册）**。只要求 `phoneNumber`；`captcha` 为可选的短信验证码。密码不需要前端传，由后端取手机号**后 8 位**自动生成并校验。 |
| 2 | `captcha` 是图片验证码、短信验证码还是其他？ | **短信验证码**（6 位数字，存 Redis，有效期 3 分钟）。**不是**图形/图片验证码。 |
| 3 | 短信验证码的发送接口是什么？ | `GET /captcha?phoneNumber=xxx`；单独校验用 `GET /validate?phoneNumber=xxx&captcha=xxx`。 |
| 4 | 现有 App 如何完成登录？ | 只提交 `phoneNumber` 一个参数（`POST /login?phoneNumber=...`），**不传 captcha**。因为后端 `sms.login.skip-captcha` 默认 `true`，登录时直接跳过验证码校验。 |
| 5 | uni-app 应该如何调用？ | `uni.request` 以 `POST` 调用 `/login`，参数放 **query string 或 `application/x-www-form-urlencoded` 表单**（**不能放 JSON body**），从响应**顶层**读取 `token` 与 `rsa_public_key`；后续请求带 `Authorization: <token>`（也兼容 `Bearer <token>`）。 |
| 6 | 是否需要改后端才能实现 PRD 的手机号验证码登录？ | **不需要改代码**。功能已完整具备，只需改配置：关闭 `sms.login.skip-captcha`（设为 `false`）并配置腾讯云短信凭据、关闭短信 mock。 |

> 一句话总结：**后端早已实现"手机号 + 短信验证码"登录，只是当前默认开关把验证码校验关掉了，现有 App 走的是"仅手机号"的演示路径。**

---

## 1. 环境与基础约定

### 1.1 服务地址

| 项 | 值 | 依据 |
|---|---|---|
| 端口 | `8088` | `Agri-CMS-admin/src/main/resources/application.yml` L20；`application-pro.yml` L3 |
| context-path | `/` | `application.yml` L23 |
| 基础地址 | `http://<后端IP>:8088` | 现有 App 使用 `http://10.218.37.215:8088`（`AgriNav-APP-master-*/app/endpoint.properties` L4） |

### 1.2 统一响应结构

后端所有接口返回 `AjaxResult`（继承 `HashMap<String, Object>`）：

```json
{
  "code": 200,
  "msg": "操作成功",
  "token": "eyJhbGciOiJIUzUxMiJ9....",
  "rsa_public_key": "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8A..."
}
```

要点：`AjaxResult` 是 Map，**`token`、`rsa_public_key` 等字段平铺在响应顶层**，没有 `data` 包装层。

- `code = 200` 成功（`Constants.CODE_SUCCESS = 200`）
- `code = 500` 业务失败，`msg` 为错误提示

### 1.3 请求头约定

| Header | 值 | 说明 |
|---|---|---|
| `Authorization` | `<token>` 或 `Bearer <token>` | 后端 `TokenService.getToken()` 会剥离 `Bearer ` 前缀，两种都可用 |
| `source` | `App` | 现有 App 的 `OkHttpInterceptor` 对所有请求统一添加；**当前后端未强制校验**（仅用于加密场景区分来源） |
| `Content-Type` | `application/x-www-form-urlencoded` | 登录接口用表单参数，不是 JSON |

### 1.4 传输加密现状（重要）

- 后端：`Agri-CMS-common/.../config/EncryptConfig.java` 中
  `ENCRYPT_PATHS = ["/**"]` 且 `EXCLUDE_PATHS = ["/**"]`，排除项优先级更高 → `isEncryptPath()` **任何路径都返回 false**，即**全局关闭传输加密**。
- App：`AgriNav-APP-master-*/.../utils/encryption/EncryptConfig.java` 的 `checkPath()` 直接 `return false`，注释写明"后端当前全局关闭传输加密，APP 端同步关闭"。
- 结论：**uni-app 只需明文请求/解析明文响应**，不需要实现 RSA/AES 握手。

> 若日后后端恢复加密，`/login` 属于登录白名单路径，仍需按 `EncryptConfig.LOGIN_PATH` 的约定先做密钥交换，再加密参数。当前版本无需处理。

---

## 2. 登录相关接口清单

| 方法 | 路径 | 鉴权 | 用途 | 代码位置 |
|---|---|---|---|---|
| POST | `/login` | 匿名 | 手机号登录/自动注册 | `SysLoginController` L85-103 |
| GET | `/captcha` | 匿名 | 发送短信验证码 | `CaptchaController` L38-42 |
| GET | `/validate` | 匿名 | 单独校验短信验证码 | `CaptchaController` L44-50 |
| GET | `/captchaQR` | 匿名 | 生成扫码登录二维码（base64 + uuid） | `CaptchaController` L62-69 |
| GET | `/validateQR?uuid=` | 匿名 | 扫码轮询，成功返回 token | `CaptchaController` L53-57 |
| POST | `/loginQR` | 匿名 | 二维码确认登录 | `SysLoginController` L105-108 |
| GET | `/loginWithoutSecret` | 需 token | 免密续登，返回 token + 公钥 | `SysLoginController` L127-137 |
| GET | `/loginOAuth` | 匿名 | 微信 OAuth 登录（PC） | `SysLoginController` L115-119 |
| GET | `/loginWechatMobile` | 匿名 | 微信移动端登录 | `SysLoginController` L121-125 |
| GET | `/getInfo` | 需 token | 获取当前用户信息 | `SysLoginController` L145-158 |
| GET | `/getRouters` | 需 token | 获取菜单路由 | `SysLoginController` L166 |
| DELETE | `/signOut/{username}` | 需 token | 注销（**物理删除用户及关联数据**，App 勿调） | `SysLoginController` |

匿名白名单依据：`Agri-CMS-framework/.../config/SecurityConfig.java` L108-118。

---

## 3. `/login` 详解

### 3.1 接口定义

```java
@PostMapping("/login")
@Operation(summary = "手机号登录注册")
public AjaxResult loginOrRegisterByMobile(
        @RequestParam(value = "phoneNumber") @NotEmpty(message = "手机号不得为空") String phoneNumber,
        @RequestParam(value = "captcha", required = false) String captcha) throws Exception {
    if (!PhoneUtil.isPhone(phoneNumber)) {
        throw new Exception("手机号格式不正确");
    }
    AjaxResult ajax = AjaxResult.success();
    SysUser user = new SysUser();
    user.setPhonenumber(phoneNumber);
    sysUserService.preLoginInsertUser(user);
    AjaxResult login = loginService.login(user.getPhonenumber(),
            user.getPhonenumber().substring(
                    user.getPhonenumber().length() - Constants.PASSWORD_INTERCEPTION), captcha);
    ajax.putAll(login);
    return ajax;
}
```

- 文件：`AgriNav-CMS-dev-*/Agri-CMS-admin/src/main/java/com/agri/cms/web/controller/system/SysLoginController.java` L85-103

### 3.2 参数表

| 参数 | 位置 | 必填 | 类型 | 说明 |
|---|---|---|---|---|
| `phoneNumber` | query / form | 是 | String | 11 位手机号，`PhoneUtil.isPhone` 校验 |
| `captcha` | query / form | 否 | String | 短信验证码。仅当后端 `sms.login.skip-captcha=false` 时才会校验 |
| `password` | — | **禁止传** | — | 不存在该参数；密码由后端派生 |
| `username` | — | **禁止传** | — | 不存在该参数；用户名即手机号 |

> **关键**：因为使用 `@RequestParam`，**只能通过 URL query 或 `application/x-www-form-urlencoded` 表单传参**。
> 传 `Content-Type: application/json` + JSON body **会直接 400/参数缺失**。

### 3.3 后端处理流程

1. `PhoneUtil.isPhone(phoneNumber)` 校验手机号格式，不合法抛"手机号格式不正确"。
2. `sysUserService.preLoginInsertUser(user)`：
   - 按手机号查用户；
   - **不存在则自动注册**：
     - `userName = 手机号`
     - `nickName = Constants.USERNAME + 手机号后 4 位`（`USERNAME_INTERCEPTION = 4`）
     - 密码 = `BCrypt(passwordEncoder, 手机号后 8 位)`（`PASSWORD_INTERCEPTION = 8`）
     - 插入 `sys_user`，并 `insertUserAuth(userId, {105L})` 绑定角色 id `105`（角色名称需查 `sys_role` 数据确认）
   - 依据：`Agri-CMS-system/.../service/impl/SysUserServiceImpl.java` L278-296
3. `loginService.login(phoneNumber, 手机号后8位, captcha)`：
   - 若 `!skipCaptcha` → `captchaService.validate(phoneNumber, captcha)`；默认 `skipCaptcha=true` 直接跳过；
   - 调用 `defaultLogin` → AuthenticationManager 认证 → `tokenService.createToken(loginUser)`；
   - 返回 `token` + `rsa_public_key`。
   - 依据：`Agri-CMS-framework/.../web/service/SysLoginService.java` L125-179
4. `ajax.putAll(login)` → 字段平铺到响应顶层。

### 3.4 请求示例

**curl（推荐，表单参数）**

```bash
curl -X POST "http://192.168.1.100:8088/login" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -H "source: App" \
  -d "phoneNumber=13800138000"
```

**带短信验证码（需后端关闭 skip-captcha）**

```bash
curl -X POST "http://192.168.1.100:8088/login" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "phoneNumber=13800138000&captcha=123456"
```

**query 方式（现有 App 采用的方式，同样可用）**

```bash
curl -X POST "http://192.168.1.100:8088/login?phoneNumber=13800138000"
```

**uni.request**

```js
uni.request({
  url: 'http://192.168.1.100:8088/login',
  method: 'POST',
  header: {
    'Content-Type': 'application/x-www-form-urlencoded',
    source: 'App'
  },
  data: { phoneNumber: '13800138000' }
})
```

### 3.5 返回示例

**成功（演示模式 / skip-captcha=true）**

```json
{
  "code": 200,
  "msg": "操作成功",
  "token": "eyJhbGciOiJIUzUxMiJ9.eyJsb2dpbl91c2VyX2tleSI6ImFiY2RlZjEyMzQ1NiJ9.xxxxx",
  "rsa_public_key": "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA..."
}
```

**成功（已开启验证码校验）**：结构相同，仅前置校验通过。

**失败示例**

```json
{ "code": 500, "msg": "手机号格式不正确" }
```

```json
{ "code": 500, "msg": "验证码已失效" }
```

```json
{ "code": 500, "msg": "验证码错误" }
```

```json
{ "code": 500, "msg": "密码输入错误5次，帐户锁定10分钟" }
```

### 3.6 一个容易误判的点

> 登录**不接收前端密码**，也不校验前端密码。后端把"手机号后 8 位"同时作为注册密码和登录密码，因此任何手机号都能"登录即注册"。
>
> **业务风险**（供产品决策，不影响对接）：这意味着只要知道手机号就能登录，安全性依赖短信验证码开关。上线前必须关闭 `skip-captcha` 并启用真实短信。

---

## 4. 验证码机制（`captcha`）

### 4.1 类型与生成

- 类型：**短信验证码**（纯数字），不是图片验证码。
- 生成：`ThreadLocalRandom.current().nextInt(100000, 999999)`，6 位。
- 存储：Redis，key 为 `CacheConstants.CAPTCHA_CODE_KEY + phoneNumber`，TTL `Constants.CAPTCHA_EXPIRATION = 3` **分钟**。
- 依据：`Agri-CMS-system/.../service/impl/CaptchaServiceImpl.java` L53-88；`Constants.java`

### 4.2 发送接口

```
GET /captcha?phoneNumber=13800138000
```

```json
// 真实短信模式
{ "code": 200, "msg": "发送成功" }

// mock 演示模式（sms.mock.enabled=true）
{ "code": 200, "msg": "演示模式，验证码已生成（未真实发送短信）", "mockCode": 486213 }
```

- 依据：`Agri-CMS-admin/.../web/controller/common/CaptchaController.java` L38-42
- 发送实现：`SmsUtils.sendSms()` 调用**腾讯云 SMS**（`sms.tencentcloudapi.com`，地域 `ap-beijing`），使用 `sms.sdk-app-id`、`sms.sign-name`、`sms.template-id`，模板参数为验证码。
- 依据：`Agri-CMS-common/.../utils/SmsUtils.java` L44-80

### 4.3 重复发送限制

- 若 Redis 中已存在该手机号的验证码 key，直接抛 `CaptchaRepeatException`（即 **3 分钟内不能重复发送**）。
- 依据：`CaptchaServiceImpl.getCode()` L58-60。

### 4.4 校验方式

**方式 A：登录时校验（推荐）**

`POST /login` 带 `captcha`，由 `SysLoginService.login()` → `CaptchaService.validate()` 完成。

```java
// SysLoginService.java L127-143
if (!skipCaptcha) {
    captchaService.validate(username, code); // 过期为 CaptchaExpireException，不匹配为 CaptchaException
}
```

**方式 B：独立校验接口**

```
GET /validate?phoneNumber=13800138000&captcha=123456
```

```json
{ "code": 200, "msg": "操作成功" }
```

依据：`CaptchaController.java` L44-50 → `CaptchaServiceImpl.validate(String phoneNumber, String captcha)` L91-104。

### 4.5 校验规则细节

```java
public void validate(String phoneNumber, String captcha) {
    String verifyKey = CacheConstants.CAPTCHA_CODE_KEY + phoneNumber;
    Integer captchaCode = redisCache.getCacheObject(verifyKey);
    if (captchaCode == null) throw new CaptchaExpireException();          // 已过期/未发送
    if (!captcha.equalsIgnoreCase(String.valueOf(captchaCode))) throw new CaptchaException(); // 不匹配
    if (!phoneNumber.equals(Constants.TEST_ACCOUNT)) {                    // 测试账号不删，可复用
        redisCache.deleteObject(verifyKey);                               // 一次性使用
    }
}
```

- 有效期：**3 分钟**
- 使用后立即失效（一次性）
- 特殊白名单：`Constants.TEST_ACCOUNT = "18888888888"`，该号码验证码**校验后不删除**，可重复使用（演示用）

### 4.6 验证码是否会真的发送？

| 配置 | 行为 |
|---|---|
| `sms.mock.enabled = true`（dev/pro 默认值 `${SMS_MOCK_ENABLED:true}`） | **不调用腾讯云**，验证码只写 Redis + 日志，并在 `/captcha` 响应中返回 `mockCode` |
| `sms.mock.enabled = false` | 调用腾讯云真实发送；未配置 `secret-id/secret-key/sdk-app-id/sign-name/template-id` 会发送失败 |

依据：`application-dev.yml` L110-113；`application-pro.yml` L97-99。

---

## 5. Token 与 RSA 公钥

### 5.1 Token

| 项 | 值 | 依据 |
|---|---|---|
| 字段名 | `token`（响应顶层） | `Constants.TOKEN = "token"` |
| 格式 | JWT（HS512） | `TokenService.createToken()` |
| 载荷 | `login_user_key` = UUID | `Constants.LOGIN_USER_KEY` |
| 服务端会话 | Redis 存 `LoginUser`，key 前缀 `login_user_key:` | `TokenService.refreshToken()` |
| 有效期 | `2592000000` ms = **30 天**（滑动续期） | `application.yml` L76；`TokenService.verifyToken()` |
| 请求头 | `Authorization` | `application.yml` L72 |
| 前缀 | `Bearer `（解析时可选，裸 token 同样接受） | `Constants.TOKEN_PREFIX`；`TokenService.getToken()` L215-218 |

### 5.2 RSA 公钥

| 项 | 值 |
|---|---|
| 字段名 | `rsa_public_key`（响应顶层） |
| 来源 | `encryptService.getRSAPublicKey()` |
| 用途 | 前端用 RSA 加密 AES 密钥，用于**传输加密握手** |
| 当前是否需要 | **不需要**。后端已全局关闭传输加密，App 端也已关闭 |

- 依据：`SysLoginService.java` L174-177；`EncryptConstants.RSA_PUBLIC_KEY_NAME = "rsa_public_key"`
- 建议：uni-app 仍**保存**该字段（存 storage），便于后端未来恢复加密时平滑升级。

---

## 6. `getInfo` 用户信息接口

### 6.1 定义

```java
@Operation(summary = "获取当前用户信息")
@GetMapping("getInfo")          // 注意：无前导斜杠，最终路径仍为 /getInfo
public AjaxResult getInfo() {
    SysUser user = SecurityUtils.getLoginUser().getUser();
    Set<String> roles = permissionService.getRolePermission(user);
    Set<String> permissions = permissionService.getMenuPermission(user);
    AjaxResult ajax = AjaxResult.success();
    ajax.put("user", user);
    ajax.put("roles", roles);
    ajax.put("permissions", permissions);
    ajax.put("departmentNames", userService.selectDeptNameById(user.getDeptId()));
    return ajax;
}
```

- 路径：`GET /getInfo`
- 鉴权：必须带 `Authorization`（在 `anyRequest().authenticated()` 范围内，不在匿名白名单）
- 依据：`SysLoginController.java` L145-158；`SecurityConfig.java` L108-118

### 6.2 返回示例

```json
{
  "code": 200,
  "msg": "操作成功",
  "user": {
    "userId": 1001,
    "deptId": null,
    "userName": "13800138000",
    "nickName": "农机手8000",
    "phonenumber": "13800138000",
    "sex": "0",
    "avatar": "",
    "status": "0",
    "delFlag": "0",
    "loginIp": "192.168.1.20",
    "loginDate": "2026-11-24 10:00:00",
    "createTime": "2026-11-24 09:59:58"
  },
  "roles": ["machinery_operator"],
  "permissions": ["..."],
  "departmentNames": null
}
```

### 6.3 请求示例

```bash
curl -X GET "http://192.168.1.100:8088/getInfo" \
  -H "Authorization: eyJhbGciOiJIUzUxMiJ9...." \
  -H "source: App"
```

---

## 7. 安全与限制规则

| 规则 | 值 | 依据 |
|---|---|---|
| 密码最大错误次数 | `5` | `application.yml` L44-45 |
| 锁定时间 | `10` 分钟 | `application.yml` L46-47 |
| 锁 key | `pwd_err_cnt:` + 用户名（手机号） | `SysPasswordService.getCacheKey()` |
| 超限异常 | `UserPasswordRetryLimitExceedException` → "密码输入错误5次，帐户锁定10分钟" | `SysPasswordService.validate()` L44-57 |
| 验证码有效期 | 3 分钟 | `Constants.CAPTCHA_EXPIRATION = 3` |
| 验证码一次性 | 校验通过后删除（测试账号除外） | `CaptchaServiceImpl.validate()` |
| 重复发送限制 | 有效期内拒绝再次发送 | `CaptchaServiceImpl.getCode()` |
| IP 黑名单 | `sys.login.blackIPList` | `SysLoginService.loginPreCheck()` |
| 用户名/密码长度 | `user.password.minLength/maxLength`、`user.name.maxLength` | `loginPreCheck()` |
| Token 有效期 | 30 天滑动续期 | `application.yml` L76 |

> 关于"失败次数锁定"的实际影响：因为密码由后端用手机号后 8 位派生，**正常路径下前端无法输错密码**。该机制主要防爆破/防止历史数据密码不一致，App 侧无需感知。

---

## 8. 现有 Android App 的真实登录实现

### 8.1 只传手机号

```java
// LoginActivity.java L119-141
HashMap<String, String> params = new HashMap<>(1);
params.put(getString(R.string.verification_code_request_param_key_login_phone), userPhone); // phoneNumber
Request.Builder builder = okHttpUtils.createBuilder(
        getString(R.string.server_url),                          // http://IP:8088
        getString(R.string.verification_code_router_login),      // /login
        params);
builder.setMethod$okhttp("POST");
```

`createBuilder(ip, path, params)` 的实现是把参数拼成 **URL query**（`?phoneNumber=...`），依据 `OkHttpUtils.java` L257-270。

### 8.2 保存 token 与 RSA 公钥

```java
// LoginActivity.java L97-103
editor.putString(getString(R.string.user_pref_key_phone), edtPhone.getText().toString());
editor.putString(getString(R.string.user_pref_key_token), body.getToken());
editor.putString(getString(R.string.user_pref_key_rsa_public), body.getRsa_public_key());
editor.commit();
getUserInfo(body.getToken());
```

`RegisterAndLoginBodyEntity` 直接从顶层解析 `token`、`rsa_public_key`。

### 8.3 拉取用户信息

```java
// LoginActivity.java L146-162
Request request = okHttpUtils
        .createBuilder(getString(R.string.server_url),
                getString(R.string.verification_code_router_getInfo))   // /getInfo
        .addHeader(getString(R.string.request_header_key_token), token) // Authorization
        .build();
```

### 8.4 全局请求头

```java
// OkHttpUtils.java L71
request = request.newBuilder().addHeader("source", "App").build();
```

### 8.5 类注释直接说明

```java
/**
 * 登录页：仅输入手机号即可直接登录（测试/演示模式，后端免验证码）
 */
public class LoginActivity extends AppCompatActivity {
```

→ 现有 App **没有短信验证码 UI**，登录页只有手机号输入框 + 登录按钮 + 注册入口。

---

## 9. uni-app + Vue 3 对接方案

### 9.1 环境配置

```bash
# .env.development
VITE_API_BASE_URL=http://192.168.1.100:8088
```

### 9.2 请求封装（关键：form-urlencoded）

```js
// src/utils/request.js
const BASE_URL = import.meta.env.VITE_API_BASE_URL

function request({ url, method = 'GET', data = {}, header = {}, isForm = false }) {
  return new Promise((resolve, reject) => {
    uni.request({
      url: BASE_URL + url,
      method,
      header: {
        source: 'App',
        ...(isForm ? { 'Content-Type': 'application/x-www-form-urlencoded' } : {}),
        ...header
      },
      data,
      success: (res) => {
        const body = res.data || {}
        if (body.code === 401 || res.statusCode === 401) {
          // token 失效：清理并回登录页
          uni.removeStorageSync('token')
          uni.reLaunch({ url: '/pages/login/login' })
          return reject(new Error('登录已失效'))
        }
        if (body.code !== 200) {
          return reject(new Error(body.msg || '请求失败'))
        }
        resolve(body)
      },
      fail: reject
    })
  })
}

export default request
```

> 说明：`uni.request` 在 `Content-Type: application/x-www-form-urlencoded` 时会把 `data` 序列化为 `a=1&b=2` 表单体，正好匹配后端 `@RequestParam`。若不想依赖该行为，也可把参数直接拼到 URL query 上（与现有 App 完全一致）。

### 9.3 登录 API

```js
// src/api/login.js
import request from '@/utils/request'

// 发送短信验证码（仅当后端开启验证码校验时需要）
export const sendSmsCode = (phoneNumber) =>
  request({ url: '/captcha', method: 'GET', data: { phoneNumber } })

// 登录（phoneNumber 必传；captcha 视后端开关而定）
export const login = (phoneNumber, captcha) =>
  request({
    url: '/login',
    method: 'POST',
    isForm: true,
    data: captcha ? { phoneNumber, captcha } : { phoneNumber }
  })

// 用户信息
export const getInfo = () =>
  request({
    url: '/getInfo',
    method: 'GET',
    header: { Authorization: uni.getStorageSync('token') || '' }
  })
```

### 9.4 登录流程

```js
// 推荐流程（同时兼容"仅手机号"和"手机号+验证码"两种后端配置）
async function doLogin(phoneNumber, captcha) {
  const res = await login(phoneNumber, captcha)
  uni.setStorageSync('token', res.token)
  uni.setStorageSync('rsa_public_key', res.rsa_public_key || '')
  const info = await getInfo()
  uni.setStorageSync('userInfo', info.user)
  uni.setStorageSync('roles', info.roles)
  uni.setStorageSync('permissions', info.permissions)
  uni.reLaunch({ url: '/pages/home/index' })
}
```

### 9.5 uni-app 对接注意事项

1. **不要用 JSON body 调 `/login`**：后端是 `@RequestParam`，JSON 会取不到参数。
2. **不要传 `password` / `username`**：后端不接收，密码由后端派生。
3. **响应字段在顶层**：`res.token`、`res.rsa_public_key`，不是 `res.data.token`。
4. **`Authorization` 带裸 token 即可**：后端兼容 `Bearer ` 前缀，建议统一存裸 token、发送时按需加前缀。
5. **401 统一处理**：后端 token 失效会返回 401，需清理本地存储并跳登录页。
6. **首次登录自动注册**：不需要单独的注册页；如需"注册"入口，实际也是调 `/login`。
7. **验证码倒计时**：`/captcha` 成功后本地做 60s 倒计时；后端真实限制是 3 分钟内不可重复发送，建议倒计时按 60s 展示、错误提示按后端 `msg` 展示。
8. **mock 模式联调**：后端 `sms.mock.enabled=true` 时 `/captcha` 会返回 `mockCode`，联调阶段可直接用它填验证码；上线前必须关闭。
9. **不要调用 `/signOut/{username}`**：该接口会**物理删除**用户及其微信绑定、农机、农活、轨迹、地块、ES 轨迹数据，App 退出登录只需清除本地 token。

---

## 10. 生产与开发环境配置差异

### 10.1 与登录相关的配置项

| 配置项 | dev 默认 | pro 默认 | 作用 |
|---|---|---|---|
| `sms.mock.enabled` | `${SMS_MOCK_ENABLED:true}` | `${SMS_MOCK_ENABLED:true}` | 是否 mock 短信（不真发） |
| `sms.login.skip-captcha` | **未在 yml 配置**，代码默认 `true` | **未在 yml 配置**，代码默认 `true` | 登录是否跳过验证码校验 |
| `token.expireTime` | `2592000000` | 同 | token 30 天 |
| `user.password.maxRetryCount` | `5` | 同 | 密码错误上限 |
| `user.password.lockTime` | `10` | 同 | 锁定时长（分钟） |

### 10.2 腾讯云短信配置

`application-dev.yml` L106-113 / `application-pro.yml` L89-99：

```yaml
tencent:
  cloud:
    secret-id: ${SMS_SECRET_ID:}
    secret-key: ${SMS_SECRET_KEY:}
sms:
  sdk-app-id: ${SMS_SDK_APP_ID:}
  sign-name: ${SMS_SIGN:田界智航APP}     # 生产默认签名
  template-id: ${SMS_TEMPLATE_ID:}
  mock:
    enabled: ${SMS_MOCK_ENABLED:true}
```

> dev 与 pro 的 `sign-name` 默认值不同（dev 为空、pro 为"田界智航APP"），`secret-id/secret-key/sdk-app-id/template-id` 默认均为空，必须通过环境变量注入。

### 10.3 要让"手机号 + 短信验证码"真正生效，需要

1. 注入腾讯云凭据：`SMS_SECRET_ID`、`SMS_SECRET_KEY`、`SMS_SDK_APP_ID`、`SMS_TEMPLATE_ID`、`SMS_SIGN`
2. 设置 `SMS_MOCK_ENABLED=false`
3. 设置 `SMS_LOGIN_SKIP_CAPTCHA=false`（Spring 宽松绑定映射到 `sms.login.skip-captcha`）

> 第 3 项**目前任何 yml 都没有**，只能在启动参数/环境变量/配置中心注入，或新增配置项（新增配置属于改配置，不属于改代码）。

---

## 11. 是否需要修改后端？

### 结论：**不需要修改代码**

`/login` + `/captcha` + `/validate` 已经构成完整的"手机号 + 短信验证码登录"闭环，`SysLoginService` 也保留了验证码校验分支，只是默认关闭。

### 11.1 三种上线策略

| 策略 | 需要做的事 | 适用场景 |
|---|---|---|
| A. 演示/内测（与现有 App 一致） | 什么都不改，`/login` 只传手机号 | 答辩、内测、本地联调 |
| B. 启用真实验证码（PRD 目标） | 仅改配置：注入腾讯云凭据 + `SMS_MOCK_ENABLED=false` + `SMS_LOGIN_SKIP_CAPTCHA=false` | 正式上线 |
| C. 增强安全（可选，需改代码） | 发送频率限制、图形验证码防刷、token 刷新机制、注销逻辑改为软删除 | 有更高安全要求时 |

### 11.2 PRD 差距分析

| PRD 期望 | 后端现状 | 差距 |
|---|---|---|
| 手机号输入 | 支持 | 无 |
| 发送短信验证码 | `GET /captcha` | 无（需配置真实短信） |
| 验证码登录 | `/login` 带 `captcha` | 无（需关闭 skip 开关） |
| 自动注册 | `preLoginInsertUser` 已实现，绑定角色 id `105` | 无 |
| 登录态保持 | JWT 30 天 + Redis | 无 |
| 用户信息 | `GET /getInfo` | 无 |

**唯一需要产品确认的点**：是否需要"密码登录"或"微信登录"。当前后端：
- 密码登录：`/login` **不支持**前端传密码（密码被后端强制派生），需改代码才能支持；
- 微信登录：已有 `/loginOAuth`、`/loginWechatMobile`，但涉及 JustAuth 配置与绑定流程，App 端未使用。

---

## 12. 代码依据索引

| 主题 | 文件 | 行号 |
|---|---|---|
| `/login` 定义 | `Agri-CMS-admin/.../controller/system/SysLoginController.java` | 85-103 |
| `getInfo` 定义 | 同上 | 145-158 |
| `/loginWithoutSecret` | 同上 | 127-137 |
| `/loginQR`、`/render`、微信登录 | 同上 | 105-125 |
| 登录服务与验证码分支 | `Agri-CMS-framework/.../web/service/SysLoginService.java` | 125-145 |
| `defaultLogin`（token + 公钥） | 同上 | 147-179 |
| `skip-captcha` 开关 | 同上 | 73-78 |
| 登录前置校验 | 同上 | 188+ |
| 验证码发送/校验 | `Agri-CMS-system/.../service/impl/CaptchaServiceImpl.java` | 53-104 |
| 验证码/扫码接口 | `Agri-CMS-admin/.../controller/common/CaptchaController.java` | 38-69 |
| 腾讯云短信 | `Agri-CMS-common/.../utils/SmsUtils.java` | 44-80 |
| 自动注册 + 角色 105 | `Agri-CMS-system/.../service/impl/SysUserServiceImpl.java` | 278-296 |
| 匿名白名单 | `Agri-CMS-framework/.../config/SecurityConfig.java` | 108-118 |
| 密码错误锁定 | `Agri-CMS-framework/.../web/service/SysPasswordService.java` | 30-65 |
| token 生成/解析 | `Agri-CMS-framework/.../web/service/TokenService.java` | 114-150, 215-218 |
| 后端加密开关（全局关闭） | `Agri-CMS-common/.../config/EncryptConfig.java` | 19-50, 66-85 |
| 常量定义 | `Agri-CMS-common/.../constant/Constants.java` | `TOKEN`、`TOKEN_PREFIX`、`PASSWORD_INTERCEPTION=8`、`USERNAME_INTERCEPTION=4`、`CAPTCHA_EXPIRATION=3`、`TEST_ACCOUNT` |
| 服务端口/context-path | `Agri-CMS-admin/src/main/resources/application.yml` | 19-23 |
| token 配置 | 同上 | 69-76 |
| 密码策略 | 同上 | 42-47 |
| dev 短信配置 | `application-dev.yml` | 106-113 |
| pro 短信配置 | `application-pro.yml` | 89-99 |
| 现有 App 登录 | `AgriNav-APP-master-*/app/.../user/login/LoginActivity.java` | 36-38, 96-104, 119-141, 146-162 |
| App 接口路径常量 | `AgriNav-APP-master-*/app/src/main/res/values/rooter.xml` | 4-8 |
| App 全局 source 头 | `AgriNav-APP-master-*/app/.../utils/OkHttpUtils.java` | 71 |
| App 加密开关（关闭） | `AgriNav-APP-master-*/app/.../utils/encryption/EncryptConfig.java` | `checkPath()` |
| App 服务地址 | `AgriNav-APP-master-*/app/endpoint.properties` | 4 |

---

## 13. 给 uni-app 团队的最小可行结论

1. 登录页做"手机号 + 获取验证码 + 登录"，**但代码要同时兼容 `captcha` 可选**：后端关闭校验时传了也无害，未关闭时必须传。
2. `POST /login`，`Content-Type: application/x-www-form-urlencoded`，参数 `phoneNumber`、`captcha`。
3. 成功后读顶层 `token`、`rsa_public_key`，存本地。
4. 后续所有请求带 `Authorization: <token>`，可选带 `source: App`。
5. `GET /getInfo` 拿用户信息与权限。
6. 不要自行设计 `/api/auth/login` 之类的路径，也不要设计 JSON 登录体。
7. 上线前与后端确认：`SMS_LOGIN_SKIP_CAPTCHA=false`、`SMS_MOCK_ENABLED=false`、腾讯云短信凭据已注入。
