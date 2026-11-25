# AgriNav（田界智航）云端部署手册

单台 4核4G 轻量服务器，Docker Compose 一键编排。`deploy/` 目录已做成**自包含**（JAR、前端 dist、SQL 脚本都在里面），服务器只需上传这一个目录即可。

## 一、架构与容器拓扑

```
                 公网
                  │
        ┌─────────┴──────────┐
        │ nginx (80)         │  前端静态托管 + 反向代理
        └──┬────────────┬────┘
           │ /prod-api  │ /profile      /media
           ▼            ▼               ▼
        ┌──────┐   ┌────────────────────────┐
        │ cms  │   │ gps (8100/7100/7200)   │  7100 TCP/UDP 直接对公网
        │8088  │   └───────────┬────────────┘
        └──┬───┘               │
   ┌───────┼─────────┬─────────┼────────┐
   ▼       ▼         ▼         ▼        ▼
 mysql  postgres   redis      es   （全部仅内网，不暴露公网）
 3306    5432       6379      9200
```

内部网络 `agrinav`，容器间用服务名互访；数据库端口不映射公网，唯一公网入口：nginx 的 80、gps 的 7100(TCP/UDP)。

## 二、端口与安全组

| 端口 | 协议 | 对公网 | 用途 |
|---|---|---|---|
| 22 | TCP | 是 | SSH |
| 80 | TCP | 是 | Web 前端 + API |
| 7100 | TCP+UDP | 是 | JT808 车载终端接入 |
| 8100/7200 | TCP | 默认否 | GPS HTTP/报警，需要时再开 |
| 3306/5432/6379/9200 | - | 否 | 中间件，仅内网 |

两层都要配：服务器内 `ufw`（01 脚本已配）+ 腾讯云轻量控制台「防火墙」。

## 三、本地准备（Windows）

一键方式（推荐，已验证可用）：

```powershell
cd "E:\smart argiculture"
powershell -ExecutionPolicy Bypass -File deploy\build-all.ps1
```

该脚本自动完成：打包两个后端 → 构建前端 → 把 JAR、dist 归集到 deploy。

手动方式与脚本内部一致，关键注意点：
- 前端构建前需有 `Agri-CMS-ui-v3\.env.production.local`（gitignored），写入真实高德 key：
  `VITE_APP_AMAP_KEY` 与 `VITE_APP_AMAP_SECURITY_CODE`；
- 前端用便携 Node22，执行 `npm run build`（Vite）。

构建完成后，`deploy/` 内关键产物：
- `cms/app.jar`、`gps/app.jar`
- `frontend/dist/`（含 index.html）
- `sql/mysql/`（6 个）、`sql/pg/`（11 个，按编号顺序执行）

## 四、上传到服务器

只需上传自包含的 `deploy` 目录：

```powershell
scp -r "E:\smart argiculture\deploy" root@82.156.224.217:/root/deploy
```

也可用 WinSCP 图形化上传。

## 五、服务器初始化

```bash
ssh root@82.156.224.217
cd /root/deploy/scripts
bash 01-server-init.sh
```

## 六、配置环境变量

```bash
cd /root/deploy
cp .env.example .env
vi .env
```

必改项：
- `PUBLIC_BASE_URL=http://82.156.224.217`（带 http:// 前缀、结尾不带斜杠；有域名就填 https://域名）
- `MYSQL_ROOT_PASSWORD` / `PG_PASSWORD` / `REDIS_PASSWORD`：强密码
- `TOKEN_SECRET`：随机长串（≥32 位）

## 七、预检、启动、初始化

```bash
cd /root/deploy
bash scripts/prepare.sh               # 预检 JAR/dist/.env/SQL 是否就位
docker compose up -d --build          # 构建并启动全部容器
docker compose ps                     # 等待全部 healthy
bash scripts/02-db-init.sh            # 初始化数据库 + 重写图片 URL
```

## 八、验证清单

浏览器访问 `http://82.156.224.217`：

- [ ] 用手机号 15888888888 可直接登录（无需验证码）
- [ ] 地块列表 12 块，含 3011/3012 两块真实耕地
- [ ] 农机、地块图片正常加载（无裂图、无 127.0.0.1 残留）
- [ ] 轨迹、热力图、面积统计、仪表盘正常
- [ ] 单价/复核/结算闭环页面可操作
- [ ] 手机浏览器或 APP 改指向公网 IP 后可登录

命令行核对：

```bash
docker compose logs -f cms
docker exec agrinav-postgres psql -U postgres -d AgriNav \
  -c "SELECT count(*) FROM land_plot; SELECT count(*) FROM boundary;"
```

## 九、日常运维

```bash
docker compose ps                    # 状态
docker compose logs -f cms           # 跟踪日志
docker compose restart cms           # 重启单个服务
docker compose down                  # 停止（数据在卷里不丢）
docker compose up -d                 # 重新启动
```

更新代码：本地重跑 `build-all.ps1` → 上传对应产物 → `docker compose up -d --build`。

### 备份

```bash
bash scripts/backup.sh               # 手动备份到 /data/agrinav-backup
# 每天凌晨 3 点定时备份
(crontab -l ; echo "0 3 * * * /root/deploy/scripts/backup.sh") | crontab -
```

## 十、四人协作纪律

1. 云端维护一套稳定演示数据，禁止手动删改共享数据；
2. 各自测试用自己造的数据（自己的手机号/加前缀），互不干扰；
3. 表结构变更写 SQL 脚本，群里通知后再部署，不直接在共享库手改；
4. 合并走 Git 流程（见根目录《Git分支策略与协作规范.md》），develop 验证通过再部署云端。

## 十一、常见问题

- **容器一直 unhealthy**：`docker compose logs 服务名`；compose 已配健康检查依赖，重跑 `docker compose up -d`。
- **图片裂图**：确认 .env 的 PUBLIC_BASE_URL 正确，重跑 02 脚本（图片 URL 重写段）。
- **7100 设备连不上**：确认云控制台防火墙和 ufw 都放行了 7100 的 TCP 和 UDP。
- **内存不足**：已配 2G swap；仍紧张可调小 .env 的 ES_JAVA_OPTS 或临时停 ES。
- **脚本报 \r 错误**：脚本已统一为 LF；若 Windows 编辑后被改，可 `sed -i 's/\r$//' scripts/*.sh`。
