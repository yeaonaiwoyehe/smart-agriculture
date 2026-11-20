# AgriNav（田界智航）Git 分支策略与协作规范

> 适用范围：AgriNav 单仓库（Monorepo，内含 APP、CMS、GPS 三个子工程目录），四人项目组统一遵守。
> 本项目采用**主干 + 个人分支**模型：一个主干 `develop`，四个人各用一个长期分支独立开发，再合并到主干。

## 一、分支模型

| 分支 | 类型 | 用途 | 谁用 |
| --- | --- | --- | --- |
| `develop` | 主干 | 最终合并目标，保持可构建、可演示 | 不直接在上面开发，只接收合并 |
| `dev-a` | 个人分支 | A 同学的独立开发线 | A |
| `dev-b` | 个人分支 | B 同学的独立开发线 | B |
| `dev-c` | 个人分支 | C 同学的独立开发线 | C |
| `dev-d` | 个人分支 | D 同学的独立开发线 | D |

> 四人分工对应：A 后端架构与智能体、B 数据库与中间件、C Web 前端、D Android 与端到端。
> 每个人**只在自己的分支上写代码**，不要去动别人的分支。

## 二、第一次：下载代码并切到自己的分支

```bash
# 1. 克隆仓库
git clone https://github.com/yeaonaiwoyehe/smart-agriculture.git
cd smart-agriculture

# 2. 切到自己的分支（自己是哪个就切哪个，例如 A 同学）
git checkout dev-a

# 3. 建立推送跟踪（只需第一次）
git push -u origin dev-a
```

## 三、日常开发（在自己分支上）

```bash
# 确认当前在自己的分支上（带 * 号）
git branch

# 写代码，然后小步提交、推送
git add .
git commit -m "feat: 用一句话说明改了什么"
git push
```

## 四、把个人分支合并到主干 develop

### 方式一：在 GitHub 上发 Pull Request（推荐，操作直观）

1. 打开仓库 → **Pull requests → New pull request**；
2. **base 选 `develop`**，compare 选自己的分支（如 `dev-a`）；
3. 点 **Create pull request**，填写说明，再点 **Merge pull request** 合并；
4. 四个人依次对自己的分支各发一次 PR。

### 方式二：本地命令合并（组长操作）

```bash
git checkout develop
git pull
git merge dev-a
git merge dev-b
git merge dev-c
git merge dev-d
git push origin develop
```

合并后各分支保留，可继续在上面开发下一轮。

## 五、重要：不要憋到最后才合并

各写各的、最后一次性合并，听起来省事，但如果多人改了**同一个文件**（公共 pom、路由、菜单、配置类），最后会冒出大量冲突，非常难解决。建议：

1. **每隔 2～3 天就往 develop 合并一次**，让冲突小步暴露、当场解决，别攒着；
2. 每次合并完，各人把主干更新拉回自己分支，保持同步：
   ```bash
   git checkout dev-a
   git merge develop
   ```
3. 改公共文件（pom、前端路由、菜单、共享配置）前，先在群里说一声，避免多人同时改。

## 六、提交信息规范

格式：`类型: 中文描述`，说清楚做了什么，不写"改了一下"这种空话。

| 类型 | 含义 |
| --- | --- |
| `feat` | 新功能 |
| `fix` | 修复缺陷 |
| `refactor` | 重构，不改变外部行为 |
| `docs` | 文档 |
| `chore` | 构建、依赖、配置等杂项 |

## 七、安全红线

1. `.env`、`dev-local.env`、`endpoint.properties` 等含真实密码/密钥的文件一律禁止入库，已由 `.gitignore` 拦截；
2. 代码里只写 `${ENV_VAR}` 占位，真实密码通过本地环境变量注入；
3. 提交前自查，不得把真实 IP、密码、token、云密钥推上去；
4. 禁止 `--force` 推送共享分支。
