# AgriNav（田界智航）Git 分支策略与协作规范

> 适用范围：AgriNav 单仓库（Monorepo，内含 APP、CMS、GPS 三个子工程目录），四人项目组统一遵守。
> 本项目采用**单主干 + 功能分支**的轻量模型：只保留一个长期分支 `develop`，所有功能在临时分支上开发、通过 Pull Request 合并。

## 一、分支模型

| 分支          | 生命周期 | 用途                                   | 谁能直接推送          |
| ----------- | ---- | ------------------------------------ | --------------- |
| `develop`   | 长期   | 唯一主干，随时可构建、可演示，通过 Pull Request 接收功能合并 | 禁止直接推送，只接受 PR 合并 |
| `feature/*` | 临时   | 具体功能开发，合并后删除                         | 功能负责人           |
| `fix/*`     | 临时   | 缺陷修复（含演示前紧急修复），合并后删除                 | 修复人             |

> 说明：本项目不再单独保留 `main` 稳定分支和 `hotfix` 紧急分支——`develop` 既当开发主干也当演示基线，紧急修复直接用 `fix/*` 走 PR 即可。

## 二、分支命名规范



* 功能分支：`feature/模块-功能`，例如 `feature/ag01-task-assistant`、`feature/mp01-plot-management`

* 修复分支：`fix/问题简述`，例如 `fix/login-token-expire`

* 全部使用小写英文与连字符，禁止中文、空格、个人姓名拼音

## 三、标准工作流（以开发一个功能为例）

核心原则：**`develop` 不直接推送，所有改动都在自己的功能分支上做，完成后通过 Pull Request（PR）合并。**

### 3.1 本地操作

```bash
# 1. 先切到 develop 并拉到最新（每次开工必做）
git checkout develop
git pull

# 2. 从最新 develop 切出功能分支
git checkout -b feature/xxx

# 3. 在功能分支上开发，小步提交（可多次 commit）
git add -A
git commit -m "feat: 实现地块边界绘制接口"

# 4. 第一次推送功能分支（-u 建立跟踪，之后直接 git push 即可）
git push -u origin feature/xxx
```

### 3.2 在 GitHub 上发起 Pull Request（图文步骤）

1. 推送后打开仓库页面，顶部会出现黄色提示条，点 **"Compare & pull request"（比较并拉取请求）**；
   （没看到提示条，就点 **Pull requests（拉取请求）** → **New pull request**，手动选分支）
2. 确认两个分支：**base（目标）选 `develop`**，compare（你的）选 `feature/xxx`；
3. 填写标题和说明（说清楚这个功能做了什么、怎么验证），点 **"Create pull request"（创建拉取请求）**；
4. 至少一名组员在 PR 页面查看改动、评论评审，通过后点 **"Merge pull request"（合并拉取请求）** → 确认；
5. 合并成功后点 **"Delete branch"（删除分支）** 删掉已合并的功能分支。

### 3.3 合并后同步回本地

```bash
git checkout develop
git pull                   # 把刚合并的内容拉下来
git branch -d feature/xxx  # 删除本地功能分支（可选）
```

### 3.4 功能分支要定期同步 develop

功能做了好几天、别人已经往 develop 合并了新东西，要定期把 develop 的更新并进来，减少最后冲突：

```bash
git checkout feature/xxx
git merge develop      # 有冲突就解决后 commit；搞不定找组长
```

## 四、本期功能分支命名实例（对照开发任务）

| 任务 | 建议分支名 |
| --- | --- |
| AG-01 任务助手 | `feature/ag01-task-assistant` |
| AG-02 作业助手 | `feature/ag02-work-assistant` |
| AG-03 结算助手 | `feature/ag03-settlement-assistant` |
| MA-01 调度建议 | `feature/ma01-dispatch-suggestion` |
| MP-01 地块补齐 | `feature/mp01-plot-management` |
| MP-05 统计报表 | `feature/mp05-statistics-report` |
| 修复登录问题 | `fix/login-token-expire` |

> 一个功能一个分支，不要一个分支里塞多个不相关的功能；分支名按上表风格，全小写英文 + 连字符。

## 五、提交信息规范（Conventional Commits）

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

## 六、安全红线



1. 任何 `.env`、`endpoint.properties`、含真实密码 / 密钥的文件一律禁止入库，已由 `.gitignore` 拦截，提交前必须检查。

2. 新增第三方凭据时，代码中只写 `${ENV_VAR}` 占位，并在 `.env.example` 中补充变量名。

3. 合并前自查：`git diff --cached` 中不得出现真实 IP 账号密码、token、云密钥。

4. 禁止使用 `--no-verify`、`--force` 推送共享分支。

## 七、当前仓库状态

三个子工程已合并为一个单仓库（Monorepo），并托管到 GitHub：

* 远程地址：`https://github.com/yeaonaiwoyehe/smart-agriculture.git`
* 仓库根目录：`E:\smart argiculture`，其下为 APP、CMS、GPS 三个子工程目录及 PRD、分工文档
* 提交身份仅在本仓库内设置为 GitHub 账号（`耶奥奈沃耶赫`，使用 GitHub 隐私邮箱），未改动机器全局配置
* 现仅保留 `develop` 一个长期分支（已设为 GitHub 默认分支），`main` 已删除；功能开发走 `feature/*`、`fix/*` 临时分支 + PR
* 全部真实密码、密钥、云凭据已从代码中移除，改由环境变量注入，根目录与各子工程均有 `.gitignore` 兜底

新成员克隆仓库（默认就在 develop，无需再切分支）：

```
git clone https://github.com/yeaonaiwoyehe/smart-agriculture.git
cd smart-agriculture
```

功能分支推送时使用（第一次加 `-u origin 分支名`，之后直接 `git push`）：

```
git push -u origin feature/xxx
```