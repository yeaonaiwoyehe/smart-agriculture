# AgriNav（田界智航）Git 分支策略与协作规范

> 适用范围：AgriNav-APP、AgriNav-CMS、AgriNav-GPS 三个独立仓库，四人项目组统一遵守。

## 一、分支模型



| 分支          | 生命周期 | 用途                                   | 谁能直接推送          |
| ----------- | ---- | ------------------------------------ | --------------- |
| `main`      | 长期   | 随时可构建、可演示的稳定版本，对应答辩演示环境              | 禁止直接推送，只接受合并    |
| `develop`   | 长期   | 集成分支，各功能完成后合并到这里联调                   | 禁止直接推送，通过合并请求合入 |
| `feature/*` | 临时   | 具体功能开发，完成后删除                         | 功能负责人           |
| `fix/*`     | 临时   | 缺陷修复，完成后删除                           | 修复人             |
| `hotfix/*`  | 临时   | 演示前紧急修复 main，完成后同时回合并 main 与 develop | 指定人             |

## 二、分支命名规范



* 功能分支：`feature/模块-功能`，例如 `feature/agent-crop-planning`、`feature/web-vue3-migration`

* 修复分支：`fix/问题简述`，例如 `fix/login-token-expire`

* 紧急修复：`hotfix/问题简述`

* 全部使用小写英文与连字符，禁止中文、空格、个人姓名拼音

## 三、标准工作流（以开发一个功能为例）



```
# 1. 从最新 develop 切出功能分支
git checkout develop
git pull
git checkout -b feature/xxx

# 2. 在功能分支上开发，小步提交
git add -A
git commit -m "feat: 实现地块边界绘制接口"

# 3. 推送并在托管平台（Gitee/GitHub）发起合并请求 → develop
git push -u origin feature/xxx

# 4. 至少一名组员评审通过后合并，删除功能分支
```

## 四、提交信息规范（Conventional Commits）

格式：`类型: 中文描述`，描述说清楚 "做了什么"，不写 "改了一下" 这类空话。



| 类型         | 含义          |
| ---------- | ----------- |
| `feat`     | 新功能         |
| `fix`      | 修复缺陷        |
| `refactor` | 重构，不改变外部行为  |
| `style`    | 格式调整，不影响逻辑  |
| `docs`     | 文档          |
| `test`     | 测试相关        |
| `chore`    | 构建、依赖、配置等杂项 |

## 五、安全红线



1. 任何 `.env`、`endpoint.properties`、含真实密码 / 密钥的文件一律禁止入库，已由 `.gitignore` 拦截，提交前必须检查。

2. 新增第三方凭据时，代码中只写 `${ENV_VAR}` 占位，并在 `.env.example` 中补充变量名。

3. 合并前自查：`git diff --cached` 中不得出现真实 IP 账号密码、token、云密钥。

4. 禁止使用 `--no-verify`、`--force` 推送共享分支。

## 六、当前仓库状态（阶段 0 基线）

三个仓库均已完成首次提交并建立 `main`、`develop` 两个分支：



* APP：首次提交 582 个文件，端点地址改由 Gradle 生成

* CMS：首次提交 931 个文件，全部配置改为环境变量注入

* GPS：首次提交 243 个文件，全部配置改为环境变量注入

后续在远程托管平台建仓后，通过以下命令关联并推送：



```
git remote add origin <远程仓库地址>
git push -u origin main
git push -u origin develop
```