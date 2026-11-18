-- ============================================================
-- 照片替换：将失效的 MinIO(:9000) 图片 URL 改为本地 /profile 静态资源
-- 可重复执行：WHERE 精确匹配旧值，已是新值时不会重复更新
-- 基础地址 http://127.0.0.1:8088/profile/agri
-- ============================================================

-- 1. 农机图片（9 条，按旧文件名匹配）
UPDATE agricultural_machinery_image
SET image = 'http://127.0.0.1:8088/profile/agri/machinery/M001_1.jpg'
WHERE image LIKE '%/m001_1.jpg';

UPDATE agricultural_machinery_image
SET image = 'http://127.0.0.1:8088/profile/agri/machinery/M001_2.jpg'
WHERE image LIKE '%/m001_2.jpg';

UPDATE agricultural_machinery_image
SET image = 'http://127.0.0.1:8088/profile/agri/machinery/M002_1.jpg'
WHERE image LIKE '%/m002_1.jpg';

UPDATE agricultural_machinery_image
SET image = 'http://127.0.0.1:8088/profile/agri/machinery/M003_1.jpg'
WHERE image LIKE '%/m003_1.jpg';

UPDATE agricultural_machinery_image
SET image = 'http://127.0.0.1:8088/profile/agri/machinery/M004_1.jpg'
WHERE image LIKE '%/m004_1.jpg';

UPDATE agricultural_machinery_image
SET image = 'http://127.0.0.1:8088/profile/agri/machinery/M005_1.jpg'
WHERE image LIKE '%/m005_1.jpg';

UPDATE agricultural_machinery_image
SET image = 'http://127.0.0.1:8088/profile/agri/machinery/M006_1.jpg'
WHERE image LIKE '%/m006_1.jpg';

UPDATE agricultural_machinery_image
SET image = 'http://127.0.0.1:8088/profile/agri/machinery/M007_1.jpg'
WHERE image LIKE '%/m007_1.jpg';

UPDATE agricultural_machinery_image
SET image = 'http://127.0.0.1:8088/profile/agri/machinery/M008_1.jpg'
WHERE image LIKE '%/m008_1.jpg';

-- 2. 地块图片（10 条，按主键匹配）
UPDATE land_plot SET image = 'http://127.0.0.1:8088/profile/agri/landplot/3001.jpg' WHERE id = 3001;
UPDATE land_plot SET image = 'http://127.0.0.1:8088/profile/agri/landplot/3002.jpg' WHERE id = 3002;
UPDATE land_plot SET image = 'http://127.0.0.1:8088/profile/agri/landplot/3003.jpg' WHERE id = 3003;
UPDATE land_plot SET image = 'http://127.0.0.1:8088/profile/agri/landplot/3004.jpg' WHERE id = 3004;
UPDATE land_plot SET image = 'http://127.0.0.1:8088/profile/agri/landplot/3005.jpg' WHERE id = 3005;
UPDATE land_plot SET image = 'http://127.0.0.1:8088/profile/agri/landplot/3006.jpg' WHERE id = 3006;
UPDATE land_plot SET image = 'http://127.0.0.1:8088/profile/agri/landplot/3007.jpg' WHERE id = 3007;
UPDATE land_plot SET image = 'http://127.0.0.1:8088/profile/agri/landplot/3008.jpg' WHERE id = 3008;
UPDATE land_plot SET image = 'http://127.0.0.1:8088/profile/agri/landplot/3009.jpg' WHERE id = 3009;
UPDATE land_plot SET image = 'http://127.0.0.1:8088/profile/agri/landplot/3010.jpg' WHERE id = 3010;

-- 3. 轨迹缩略图（2 条，按轨迹主键匹配）
UPDATE trajectory_info
SET trajectory_img = 'http://127.0.0.1:8088/profile/agri/trajectory/traj_1.jpg'
WHERE trajectory_info_id = '0cfd4cb7-440b-4f9a-a73f-9db257ba02a7';

UPDATE trajectory_info
SET trajectory_img = 'http://127.0.0.1:8088/profile/agri/trajectory/traj_2.jpg'
WHERE trajectory_info_id = '7816b10c-d28f-4552-a2ab-de42f0f7a1ad';
