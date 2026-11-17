-- ============================================================================
-- AgriNav（田界智航）PostgreSQL/PostGIS 演示种子数据
-- 目标库：AgriNav（PostgreSQL 17 + PostGIS 3.5），默认 schema：public
-- 场景：河北石家庄藁城/正定平原的农机找活、农活招工、地块量测与热力图
--
-- 硬约束（务必遵守）：
--   1. 所有 create_by / update_by 必须是 MySQL ry-vue.sys_user 中真实存在的用户 ID：
--      100=测试号(18888888888)  1=admin  2=ry，否则后端 VO 构建 selectUserById 会空指针。
--   2. 农机/农活 status：0=可用(首页默认) 1=已下架 2=已删除(查询排除)，本脚本不使用 2。
--   3. 地块 is_deleted=0，boundary/center_point 均为 SRID=4326；
--      land_area 单位为“亩”，由 ST_Area(geography)/666.6667 自动计算，与边界严格一致。
--   4. 关联表的外键列均与主表 ID 对应（无数据库外键，靠应用维护）。
--   5. 图片为 MinIO 风格 URL，本地未起 MinIO 时图片加载失败，但不影响文字与数据。
--
-- 可重复执行：开头先按固定 ID 清理本脚本写入的演示数据。
-- 执行：PGPASSWORD=agrinav psql -h 127.0.0.1 -U postgres -d AgriNav -f agrinav_demo_data.sql
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 0. 清理旧演示数据（可重复执行）
-- ----------------------------------------------------------------------------
DELETE FROM public.agricultural_machinery_crop    WHERE agricultural_machinery_id IN ('M001','M002','M003','M004','M005','M006','M007','M008');
DELETE FROM public.agricultural_machinery_image   WHERE agricultural_machinery_id IN ('M001','M002','M003','M004','M005','M006','M007','M008');
DELETE FROM public.agricultural_machinery_location WHERE agriculture_machinery_id IN ('M001','M002','M003','M004','M005','M006','M007','M008');
DELETE FROM public.agricultural_machinery          WHERE id IN ('M001','M002','M003','M004','M005','M006','M007','M008');

DELETE FROM public.farm_work_crop      WHERE farm_work_id IN ('F001','F002','F003','F004','F005','F006','F007','F008');
DELETE FROM public.farm_work_type      WHERE farm_work_id IN ('F001','F002','F003','F004','F005','F006','F007','F008');
DELETE FROM public.farm_work_land_plot WHERE farm_work_id IN ('F001','F002','F003','F004','F005','F006','F007','F008');
DELETE FROM public.farm_work           WHERE id IN ('F001','F002','F003','F004','F005','F006','F007','F008');

DELETE FROM public.land_plot WHERE id BETWEEN 3001 AND 3010;
DELETE FROM public.land_plot_exclude WHERE id BETWEEN 5001 AND 5002;
DELETE FROM public.crop_work WHERE crop_id BETWEEN 1001 AND 1006;
DELETE FROM public.crop      WHERE id BETWEEN 1001 AND 1006;
DELETE FROM public.work      WHERE id BETWEEN 4001 AND 4006;
DELETE FROM public.work_type WHERE id BETWEEN 2001 AND 2008;
DELETE FROM public.agricultural_machinery_type WHERE id IN ('T01','T02','T03','T04','T05','T06');

-- ============================================================================
-- 一、字典数据
-- ============================================================================

-- 1. 作物 crop -------------------------------------------------------------
INSERT INTO public.crop (id, crop_name, is_deleted, create_time, update_time) VALUES
 (1001, '冬小麦', FALSE, TIMESTAMP '2026-11-01 08:00:00', TIMESTAMP '2026-11-01 08:00:00'),
 (1002, '夏玉米', FALSE, TIMESTAMP '2026-11-01 08:00:00', TIMESTAMP '2026-11-01 08:00:00'),
 (1003, '棉花',   FALSE, TIMESTAMP '2026-11-01 08:00:00', TIMESTAMP '2026-11-01 08:00:00'),
 (1004, '大豆',   FALSE, TIMESTAMP '2026-11-01 08:00:00', TIMESTAMP '2026-11-01 08:00:00'),
 (1005, '花生',   FALSE, TIMESTAMP '2026-11-01 08:00:00', TIMESTAMP '2026-11-01 08:00:00'),
 (1006, '谷子',   FALSE, TIMESTAMP '2026-11-01 08:00:00', TIMESTAMP '2026-11-01 08:00:00');

-- 2. 作业类型 work（大屏/轨迹用字典） --------------------------------------
INSERT INTO public.work (id, work_name, is_deleted, create_time, update_time) VALUES
 (4001, '耕地',     FALSE, TIMESTAMP '2026-11-01 08:00:00', TIMESTAMP '2026-11-01 08:00:00'),
 (4002, '旋耕',     FALSE, TIMESTAMP '2026-11-01 08:00:00', TIMESTAMP '2026-11-01 08:00:00'),
 (4003, '播种',     FALSE, TIMESTAMP '2026-11-01 08:00:00', TIMESTAMP '2026-11-01 08:00:00'),
 (4004, '植保',     FALSE, TIMESTAMP '2026-11-01 08:00:00', TIMESTAMP '2026-11-01 08:00:00'),
 (4005, '收割',     FALSE, TIMESTAMP '2026-11-01 08:00:00', TIMESTAMP '2026-11-01 08:00:00'),
 (4006, '秸秆还田', FALSE, TIMESTAMP '2026-11-01 08:00:00', TIMESTAMP '2026-11-01 08:00:00');

-- 3. 工作类型 work_type（农活/招工实际引用） -------------------------------
INSERT INTO public.work_type (id, work_name, create_time, update_time, delete) VALUES
 (2001, '深翻耕地',   TIMESTAMP '2026-11-01 08:00:00', TIMESTAMP '2026-11-01 08:00:00', FALSE),
 (2002, '旋耕整地',   TIMESTAMP '2026-11-01 08:00:00', TIMESTAMP '2026-11-01 08:00:00', FALSE),
 (2003, '精量播种',   TIMESTAMP '2026-11-01 08:00:00', TIMESTAMP '2026-11-01 08:00:00', FALSE),
 (2004, '植保打药',   TIMESTAMP '2026-11-01 08:00:00', TIMESTAMP '2026-11-01 08:00:00', FALSE),
 (2005, '联合收割',   TIMESTAMP '2026-11-01 08:00:00', TIMESTAMP '2026-11-01 08:00:00', FALSE),
 (2006, '秸秆还田',   TIMESTAMP '2026-11-01 08:00:00', TIMESTAMP '2026-11-01 08:00:00', FALSE),
 (2007, '无人机飞防', TIMESTAMP '2026-11-01 08:00:00', TIMESTAMP '2026-11-01 08:00:00', FALSE),
 (2008, '粮食转运',   TIMESTAMP '2026-11-01 08:00:00', TIMESTAMP '2026-11-01 08:00:00', FALSE);

-- 4. 作物-作业类型关联 crop_work（部分常见组合） ---------------------------
INSERT INTO public.crop_work (crop_id, work_id, is_available) VALUES
 (1001, 4001, TRUE), (1001, 4002, TRUE), (1001, 4003, TRUE), (1001, 4004, TRUE), (1001, 4005, TRUE), (1001, 4006, TRUE),
 (1002, 4002, TRUE), (1002, 4003, TRUE), (1002, 4004, TRUE), (1002, 4005, TRUE), (1002, 4006, TRUE),
 (1003, 4004, TRUE), (1003, 4005, TRUE),
 (1004, 4001, TRUE), (1004, 4003, TRUE), (1004, 4005, TRUE),
 (1005, 4001, TRUE), (1005, 4003, TRUE), (1005, 4005, TRUE);

-- 5. 农机类型 agricultural_machinery_type ----------------------------------
INSERT INTO public.agricultural_machinery_type (id, name, status, create_time, create_by, update_time, update_by) VALUES
 ('T01', '轮式拖拉机',   0, TIMESTAMP '2026-11-01 08:30:00', '1', TIMESTAMP '2026-11-01 08:30:00', '1'),
 ('T02', '履带式拖拉机', 0, TIMESTAMP '2026-11-01 08:30:00', '1', TIMESTAMP '2026-11-01 08:30:00', '1'),
 ('T03', '联合收割机',   0, TIMESTAMP '2026-11-01 08:30:00', '1', TIMESTAMP '2026-11-01 08:30:00', '1'),
 ('T04', '播种机',       0, TIMESTAMP '2026-11-01 08:30:00', '1', TIMESTAMP '2026-11-01 08:30:00', '1'),
 ('T05', '植保无人机',   0, TIMESTAMP '2026-11-01 08:30:00', '1', TIMESTAMP '2026-11-01 08:30:00', '1'),
 ('T06', '粮食运输车',   0, TIMESTAMP '2026-11-01 08:30:00', '1', TIMESTAMP '2026-11-01 08:30:00', '1');

-- ============================================================================
-- 二、地块 land_plot（含 SRID 4326 边界与中心点，面积自动按亩计算）
-- 每条用 CTE 让几何只写一次：land_area=ST_Area(geography)/666.6667，center_point=ST_Centroid
-- ============================================================================

-- 3001 藁城区廉州镇 -------------------------------------------------------
WITH g AS (SELECT ST_SetSRID(ST_GeomFromText(
  'POLYGON((114.820 38.025, 114.825 38.025, 114.825 38.020, 114.820 38.020, 114.820 38.025))'),4326) AS geom)
INSERT INTO public.land_plot
 (id, land_name, land_type, land_area, is_deleted, address, create_by, create_time, update_by, update_time,
  image, measurement_method, boundary, center_point, owner_name, owner_phone)
SELECT 3001, '廉州镇东一号田', '新增',
 ROUND((ST_Area(g.geom::geography)/666.6667)::numeric,2)::real, 0,
 '河北省石家庄市藁城区廉州镇北街村', '100', TIMESTAMP '2026-11-02 09:10:00', '100', TIMESTAMP '2026-11-02 09:10:00',
 'http://127.0.0.1:9000/bkt-media/demo/plot3001.jpg', 'GPS测量',
 g.geom, ST_Centroid(g.geom), '张建国', '13931102001'
FROM g;

-- 3002 藁城区廉州镇 -------------------------------------------------------
WITH g AS (SELECT ST_SetSRID(ST_GeomFromText(
  'POLYGON((114.840 38.025, 114.846 38.025, 114.846 38.019, 114.840 38.019, 114.840 38.025))'),4326) AS geom)
INSERT INTO public.land_plot
 (id, land_name, land_type, land_area, is_deleted, address, create_by, create_time, update_by, update_time,
  image, measurement_method, boundary, center_point, owner_name, owner_phone)
SELECT 3002, '廉州镇东二号田', '复测',
 ROUND((ST_Area(g.geom::geography)/666.6667)::numeric,2)::real, 0,
 '河北省石家庄市藁城区廉州镇郭庄村', '100', TIMESTAMP '2026-11-03 10:20:00', '100', TIMESTAMP '2026-11-05 15:00:00',
 'http://127.0.0.1:9000/bkt-media/demo/plot3002.jpg', '无人机测绘',
 g.geom, ST_Centroid(g.geom), '张建国', '13931102001'
FROM g;

-- 3003 藁城区兴安镇（不规则四边形） ---------------------------------------
WITH g AS (SELECT ST_SetSRID(ST_GeomFromText(
  'POLYGON((114.820 38.012, 114.826 38.013, 114.825 38.007, 114.819 38.008, 114.820 38.012))'),4326) AS geom)
INSERT INTO public.land_plot
 (id, land_name, land_type, land_area, is_deleted, address, create_by, create_time, update_by, update_time,
  image, measurement_method, boundary, center_point, owner_name, owner_phone)
SELECT 3003, '兴安镇合作农田A', '新增',
 ROUND((ST_Area(g.geom::geography)/666.6667)::numeric,2)::real, 0,
 '河北省石家庄市藁城区兴安镇贾村', '1', TIMESTAMP '2026-11-04 08:40:00', '1', TIMESTAMP '2026-11-04 08:40:00',
 'http://127.0.0.1:9000/bkt-media/demo/plot3003.jpg', 'GPS测量',
 g.geom, ST_Centroid(g.geom), '李茂才', '13731103002'
FROM g;

-- 3004 藁城区兴安镇 -------------------------------------------------------
WITH g AS (SELECT ST_SetSRID(ST_GeomFromText(
  'POLYGON((114.840 38.012, 114.845 38.012, 114.845 38.007, 114.840 38.007, 114.840 38.012))'),4326) AS geom)
INSERT INTO public.land_plot
 (id, land_name, land_type, land_area, is_deleted, address, create_by, create_time, update_by, update_time,
  image, measurement_method, boundary, center_point, owner_name, owner_phone)
SELECT 3004, '兴安镇合作农田B', '插花地',
 ROUND((ST_Area(g.geom::geography)/666.6667)::numeric,2)::real, 0,
 '河北省石家庄市藁城区兴安镇武家庄', '2', TIMESTAMP '2026-11-05 11:00:00', '2', TIMESTAMP '2026-11-05 11:00:00',
 'http://127.0.0.1:9000/bkt-media/demo/plot3004.jpg', '人工测绘',
 g.geom, ST_Centroid(g.geom), '王秀兰', '13531104003'
FROM g;

-- 3005 正定县新城铺镇 -----------------------------------------------------
WITH g AS (SELECT ST_SetSRID(ST_GeomFromText(
  'POLYGON((114.820 38.040, 114.826 38.040, 114.826 38.035, 114.820 38.035, 114.820 38.040))'),4326) AS geom)
INSERT INTO public.land_plot
 (id, land_name, land_type, land_area, is_deleted, address, create_by, create_time, update_by, update_time,
  image, measurement_method, boundary, center_point, owner_name, owner_phone)
SELECT 3005, '新城铺镇北田', '新增',
 ROUND((ST_Area(g.geom::geography)/666.6667)::numeric,2)::real, 0,
 '河北省石家庄市正定县新城铺镇北王庄村', '100', TIMESTAMP '2026-11-06 09:00:00', '100', TIMESTAMP '2026-11-06 09:00:00',
 'http://127.0.0.1:9000/bkt-media/demo/plot3005.jpg', 'GPS测量',
 g.geom, ST_Centroid(g.geom), '赵满仓', '13831105004'
FROM g;

-- 3006 正定县新城铺镇（不规则） -------------------------------------------
WITH g AS (SELECT ST_SetSRID(ST_GeomFromText(
  'POLYGON((114.840 38.041, 114.847 38.040, 114.846 38.034, 114.841 38.035, 114.840 38.041))'),4326) AS geom)
INSERT INTO public.land_plot
 (id, land_name, land_type, land_area, is_deleted, address, create_by, create_time, update_by, update_time,
  image, measurement_method, boundary, center_point, owner_name, owner_phone)
SELECT 3006, '新城铺镇大田', '复测',
 ROUND((ST_Area(g.geom::geography)/666.6667)::numeric,2)::real, 0,
 '河北省石家庄市正定县新城铺镇东平乐村', '1', TIMESTAMP '2026-11-07 14:30:00', '1', TIMESTAMP '2026-11-09 10:10:00',
 'http://127.0.0.1:9000/bkt-media/demo/plot3006.jpg', '无人机测绘',
 g.geom, ST_Centroid(g.geom), '赵满仓', '13831105004'
FROM g;

-- 3007 藁城区岗上镇 -------------------------------------------------------
WITH g AS (SELECT ST_SetSRID(ST_GeomFromText(
  'POLYGON((114.820 38.000, 114.824 38.000, 114.824 37.996, 114.820 37.996, 114.820 38.000))'),4326) AS geom)
INSERT INTO public.land_plot
 (id, land_name, land_type, land_area, is_deleted, address, create_by, create_time, update_by, update_time,
  image, measurement_method, boundary, center_point, owner_name, owner_phone)
SELECT 3007, '岗上镇南洼地', '新增',
 ROUND((ST_Area(g.geom::geography)/666.6667)::numeric,2)::real, 0,
 '河北省石家庄市藁城区岗上镇故献村', '100', TIMESTAMP '2026-11-08 08:20:00', '100', TIMESTAMP '2026-11-08 08:20:00',
 'http://127.0.0.1:9000/bkt-media/demo/plot3007.jpg', 'GPS测量',
 g.geom, ST_Centroid(g.geom), '孙有德', '13631106005'
FROM g;

-- 3008 藁城区岗上镇 -------------------------------------------------------
WITH g AS (SELECT ST_SetSRID(ST_GeomFromText(
  'POLYGON((114.840 38.000, 114.846 38.000, 114.846 37.995, 114.840 37.995, 114.840 38.000))'),4326) AS geom)
INSERT INTO public.land_plot
 (id, land_name, land_type, land_area, is_deleted, address, create_by, create_time, update_by, update_time,
  image, measurement_method, boundary, center_point, owner_name, owner_phone)
SELECT 3008, '岗上镇规整田', '新增',
 ROUND((ST_Area(g.geom::geography)/666.6667)::numeric,2)::real, 0,
 '河北省石家庄市藁城区岗上镇塔元庄村', '2', TIMESTAMP '2026-11-09 09:40:00', '2', TIMESTAMP '2026-11-09 09:40:00',
 'http://127.0.0.1:9000/bkt-media/demo/plot3008.jpg', 'GPS测量',
 g.geom, ST_Centroid(g.geom), '孙有德', '13631106005'
FROM g;

-- 3009 藁城区常安镇（较大连片田） -----------------------------------------
WITH g AS (SELECT ST_SetSRID(ST_GeomFromText(
  'POLYGON((114.860 38.025, 114.868 38.025, 114.868 38.017, 114.860 38.017, 114.860 38.025))'),4326) AS geom)
INSERT INTO public.land_plot
 (id, land_name, land_type, land_area, is_deleted, address, create_by, create_time, update_by, update_time,
  image, measurement_method, boundary, center_point, owner_name, owner_phone)
SELECT 3009, '常安镇连片田', '新增',
 ROUND((ST_Area(g.geom::geography)/666.6667)::numeric,2)::real, 0,
 '河北省石家庄市藁城区常安镇北楼村', '100', TIMESTAMP '2026-11-10 10:00:00', '100', TIMESTAMP '2026-11-10 10:00:00',
 'http://127.0.0.1:9000/bkt-media/demo/plot3009.jpg', '无人机测绘',
 g.geom, ST_Centroid(g.geom), '周大田', '13331107006'
FROM g;

-- 3010 藁城区常安镇（不规则） ---------------------------------------------
WITH g AS (SELECT ST_SetSRID(ST_GeomFromText(
  'POLYGON((114.860 38.042, 114.867 38.043, 114.868 38.036, 114.861 38.035, 114.860 38.042))'),4326) AS geom)
INSERT INTO public.land_plot
 (id, land_name, land_type, land_area, is_deleted, address, create_by, create_time, update_by, update_time,
  image, measurement_method, boundary, center_point, owner_name, owner_phone)
SELECT 3010, '常安镇北畈田', '插花地',
 ROUND((ST_Area(g.geom::geography)/666.6667)::numeric,2)::real, 0,
 '河北省石家庄市藁城区常安镇东辛庄村', '1', TIMESTAMP '2026-11-11 11:20:00', '1', TIMESTAMP '2026-11-11 11:20:00',
 'http://127.0.0.1:9000/bkt-media/demo/plot3010.jpg', '人工测绘',
 g.geom, ST_Centroid(g.geom), '周大田', '13331107006'
FROM g;

-- ============================================================================
-- 三、农机 agricultural_machinery 及关联（找活）
-- ============================================================================
INSERT INTO public.agricultural_machinery
 (id, type_id, phone_number, status, create_by, create_time, update_by, update_time,
  having_grain_carrier, end_time, name, address_prefix) VALUES
 ('M001','T01','13932100001',0,'100',TIMESTAMP '2026-11-08 09:00:00','100',TIMESTAMP '2026-11-08 09:00:00',FALSE,TIMESTAMP '2026-12-31 18:00:00','120马力轮式拖拉机带犁','石家庄藁城区及周边乡镇'),
 ('M002','T03','13732100002',0,'1',  TIMESTAMP '2026-11-09 10:30:00','1',  TIMESTAMP '2026-11-09 10:30:00',FALSE,TIMESTAMP '2026-12-31 18:00:00','大型小麦联合收割机','石家庄正定县、藁城区'),
 ('M003','T04','13532100003',0,'2',  TIMESTAMP '2026-11-10 08:10:00','2',  TIMESTAMP '2026-11-10 08:10:00',FALSE,TIMESTAMP '2026-12-31 18:00:00','六行精量玉米播种机','藁城区兴安镇附近'),
 ('M004','T05','13832100004',0,'100',TIMESTAMP '2026-11-11 14:00:00','100',TIMESTAMP '2026-11-11 14:00:00',FALSE,TIMESTAMP '2026-12-31 18:00:00','植保无人机飞防作业队','全市范围可上门'),
 ('M005','T02','13632100005',0,'1',  TIMESTAMP '2026-11-12 09:20:00','1',  TIMESTAMP '2026-11-12 09:20:00',FALSE,TIMESTAMP '2026-12-31 18:00:00','大马力履带式拖拉机','藁城、晋州一带'),
 ('M006','T06','13332100006',0,'100',TIMESTAMP '2026-11-12 15:40:00','100',TIMESTAMP '2026-11-12 15:40:00',TRUE, TIMESTAMP '2026-12-31 18:00:00','自卸运粮车带粮食转运','藁城区及周边粮站'),
 ('M007','T01','13232100007',0,'2',  TIMESTAMP '2026-11-13 08:50:00','2',  TIMESTAMP '2026-11-13 08:50:00',FALSE,TIMESTAMP '2026-12-31 18:00:00','160马力大马力拖拉机','正定新城铺周边'),
 ('M008','T03','13132100008',1,'100',TIMESTAMP '2026-10-20 10:00:00','100',TIMESTAMP '2026-10-25 16:00:00',FALSE,TIMESTAMP '2026-11-05 18:00:00','老款收割机（已下架）','藁城区');

-- 农机-作物关联 agricultural_machinery_crop -------------------------------
INSERT INTO public.agricultural_machinery_crop (agricultural_machinery_id, crop_id) VALUES
 ('M001',1001),('M001',1004),
 ('M002',1001),('M002',1002),
 ('M003',1002),
 ('M004',1001),('M004',1003),('M004',1002),
 ('M005',1001),('M005',1004),
 ('M006',1002),('M006',1005),
 ('M007',1001),('M007',1006),
 ('M008',1002);

-- 农机位置 agricultural_machinery_location（列名沿用 agriculture_machinery_id）
INSERT INTO public.agricultural_machinery_location (agriculture_machinery_id, location) VALUES
 ('M001','河北省石家庄市藁城区廉州镇'),
 ('M002','河北省石家庄市正定县新城铺镇'),
 ('M003','河北省石家庄市藁城区兴安镇'),
 ('M004','河北省石家庄市藁城区'),
 ('M005','河北省石家庄市藁城区常安镇'),
 ('M006','河北省石家庄市藁城区岗上镇'),
 ('M007','河北省石家庄市正定县新城铺镇'),
 ('M008','河北省石家庄市藁城区');

-- 农机图片 agricultural_machinery_image -----------------------------------
INSERT INTO public.agricultural_machinery_image (agricultural_machinery_id, image) VALUES
 ('M001','http://127.0.0.1:9000/bkt-media/demo/m001_1.jpg'),
 ('M001','http://127.0.0.1:9000/bkt-media/demo/m001_2.jpg'),
 ('M002','http://127.0.0.1:9000/bkt-media/demo/m002_1.jpg'),
 ('M003','http://127.0.0.1:9000/bkt-media/demo/m003_1.jpg'),
 ('M004','http://127.0.0.1:9000/bkt-media/demo/m004_1.jpg'),
 ('M005','http://127.0.0.1:9000/bkt-media/demo/m005_1.jpg'),
 ('M006','http://127.0.0.1:9000/bkt-media/demo/m006_1.jpg'),
 ('M007','http://127.0.0.1:9000/bkt-media/demo/m007_1.jpg'),
 ('M008','http://127.0.0.1:9000/bkt-media/demo/m008_1.jpg');

-- ============================================================================
-- 四、农活 farm_work 及关联（招工）
-- ============================================================================
INSERT INTO public.farm_work
 (id, name, area, end_time, phone_number, create_by, create_time, status, update_by, update_time) VALUES
 ('F001','廉州镇冬小麦深翻耕地招工',120.00,TIMESTAMP '2026-11-25 18:00:00','15032100001','100',TIMESTAMP '2026-11-12 09:00:00',0,'100',TIMESTAMP '2026-11-12 09:00:00'),
 ('F002','夏玉米联合收割招机手',    150.00,TIMESTAMP '2026-11-28 18:00:00','15132100002','1',  TIMESTAMP '2026-11-12 10:30:00',0,'1',  TIMESTAMP '2026-11-12 10:30:00'),
 ('F003','小麦病虫害植保打药作业',    80.00,TIMESTAMP '2026-11-26 18:00:00','15232100003','100',TIMESTAMP '2026-11-13 08:40:00',0,'100',TIMESTAMP '2026-11-13 08:40:00'),
 ('F004','棉田无人机飞防喷药',        200.00,TIMESTAMP '2026-12-02 18:00:00','15332100004','1',  TIMESTAMP '2026-11-13 14:00:00',0,'1',  TIMESTAMP '2026-11-13 14:00:00'),
 ('F005','大豆地旋耕整地作业',        60.00,TIMESTAMP '2026-11-27 18:00:00','15432100005','2',  TIMESTAMP '2026-11-14 09:10:00',0,'2',  TIMESTAMP '2026-11-14 09:10:00'),
 ('F006','玉米精量播种找播种机',      95.00,TIMESTAMP '2026-12-05 18:00:00','15532100006','2',  TIMESTAMP '2026-11-14 11:00:00',0,'2',  TIMESTAMP '2026-11-14 11:00:00'),
 ('F007','小麦秸秆还田作业',          70.00,TIMESTAMP '2026-11-30 18:00:00','15632100007','100',TIMESTAMP '2026-11-15 08:30:00',0,'100',TIMESTAMP '2026-11-15 08:30:00'),
 ('F008','秋粮转运找运粮车（已结束）',110.00,TIMESTAMP '2026-11-06 18:00:00','15732100008','100',TIMESTAMP '2026-10-22 10:00:00',1,'100',TIMESTAMP '2026-10-28 16:00:00');

-- 农活-作物关联 farm_work_crop --------------------------------------------
INSERT INTO public.farm_work_crop (farm_work_id, crop_id) VALUES
 ('F001',1001),('F002',1002),('F003',1001),('F004',1003),
 ('F005',1004),('F006',1002),('F007',1001),('F008',1002);

-- 农活-工作类型关联 farm_work_type ----------------------------------------
INSERT INTO public.farm_work_type (farm_work_id, work_type_id) VALUES
 ('F001',2001),('F002',2005),('F003',2004),('F004',2007),
 ('F005',2002),('F006',2003),('F007',2006),('F008',2008);

-- 农活-地块关联 farm_work_land_plot ---------------------------------------
INSERT INTO public.farm_work_land_plot (farm_work_id, land_plot_id) VALUES
 ('F001',3001),('F002',3002),('F003',3005),('F004',3009),
 ('F005',3003),('F006',3004),('F007',3007),('F008',3006);

-- ============================================================================
-- 五、插花地无用（空洞）区域 land_plot_exclude
-- 语义：插花地内部不属于本户的小块地（如别家坟地、邻地），地图上用 PolygonHole 挖洞显示。
-- 几何取父地块中心点做缓冲圆，保证空洞严格落在父地块内部；area 同样自动算亩。
-- ============================================================================
INSERT INTO public.land_plot_exclude (id, land_plot_id, boundary, area, is_deleted)
SELECT 5001, 3004, hole.geom,
       ROUND((ST_Area(hole.geom::geography)/666.6667)::numeric, 2)::real, 0
FROM (
  SELECT ST_Buffer(ST_Centroid(boundary)::geography, 45)::geometry AS geom
  FROM public.land_plot WHERE id = 3004
) hole;

INSERT INTO public.land_plot_exclude (id, land_plot_id, boundary, area, is_deleted)
SELECT 5002, 3010, hole.geom,
       ROUND((ST_Area(hole.geom::geography)/666.6667)::numeric, 2)::real, 0
FROM (
  SELECT ST_Buffer(ST_Centroid(boundary)::geography, 50)::geometry AS geom
  FROM public.land_plot WHERE id = 3010
) hole;

-- ============================================================================
-- 验证查询（执行后可人工核对）
-- ============================================================================
-- 校验空洞完全包含在父地块内（inside 应为 true）：
-- SELECT p.id AS land_plot_id, e.id AS exclude_id,
--        ST_Contains(p.boundary, e.boundary) AS inside, e.area
-- FROM public.land_plot_exclude e
-- JOIN public.land_plot p ON p.id = e.land_plot_id;
-- 各地块亩数与中心点：
-- SELECT id, land_name, land_area, ST_AsText(center_point) FROM public.land_plot ORDER BY id;
-- 首页统计（空地址=全部）：
-- SELECT COUNT(*), SUM(land_area) FROM public.land_plot WHERE is_deleted=0;
