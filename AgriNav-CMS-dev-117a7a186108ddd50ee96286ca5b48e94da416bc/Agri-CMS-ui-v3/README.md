## 本地开发

本目录是当前使用的 Vue 3 前端，默认地址为 http://localhost:81。
需要先启动 CMS 后端（8088 端口），前端会将 `/dev-api` 请求代理到该后端。

1. 将 `.env.development.example` 复制为 `.env.development`。
2. 填入 `VITE_APP_AMAP_KEY` 和 `VITE_APP_AMAP_SECURITY_CODE`。旧版的 `VUE_APP_` 前缀在 Vite 中不生效。本地环境文件已被 Git 忽略，请勿提交真实密钥。
3. 在本目录执行：

```bash
npm install
npm run dev
```

Windows 也可以使用仓库根目录的 `.tools/start-ui.cmd`，该脚本会调用新版前端启动入口。

## 构建与测试

```bash
npm run build
node --test tests/*.test.mjs
```

构建产物默认输出到 `dist/`。地图 Key 应按部署环境另行配置。

## 地块功能验收

进入「农业作业管理 → 地块管理」：

- 「导入地块文件」支持 GeoJSON 和 Shapefile ZIP，示例预检不会保存数据；正式导入需要再次确认。
- 「地块分组」支持行政村和片区两个独立类别，列表分别标注归属，可筛选、创建分组和批量归组。
- 「查看地块边界」展示面积来源、平方米换算、周长估算和地图，支持卫星影像、测距及全屏。导入的 WGS84 边界转换后再显示在高德地图上。

示例边界用于验证流程，面积尚未经过实测核验。正式导入、创建分组和归组会修改共享数据库。
