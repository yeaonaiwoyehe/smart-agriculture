-- ============================================================================
-- 两块真实耕地数据：边界在高德卫星影像上沿真实田埂描点取得
-- 面积由 ST_Area(geography)/666.6667 自动计算，与边界严格一致
-- 可重复执行：先删除 3011/3012 再插入
-- ============================================================================

DELETE FROM public.land_plot WHERE id IN (3011, 3012);

-- 3011 真实耕地一（兴安镇，规则长方形，已翻耕） -----------------------------
WITH g AS (SELECT ST_SetSRID(ST_GeomFromText(
  'POLYGON((114.811664 38.007774,114.815312 38.007817,114.815258 38.005914,114.811611 38.005872,114.811664 38.007774))'),4326) AS geom)
INSERT INTO public.land_plot
 (id, land_name, land_type, land_area, is_deleted, address, create_by, create_time, update_by, update_time,
  image, measurement_method, boundary, center_point, owner_name, owner_phone)
SELECT 3011, '兴安镇真实耕地一号', '新增',
 ROUND((ST_Area(g.geom::geography)/666.6667)::numeric,2)::real, 0,
 '河北省石家庄市藁城区兴安镇', '1', TIMESTAMP '2026-11-18 10:00:00', '1', TIMESTAMP '2026-11-18 10:00:00',
 'http://127.0.0.1:8088/profile/agri/landplot/3003.jpg', '卫星图描点',
 g.geom, ST_Centroid(g.geom), '周大田', '13331107006'
FROM g;

-- 3012 真实耕地二（兴安镇，规则长方形，已翻耕） -----------------------------
WITH g AS (SELECT ST_SetSRID(ST_GeomFromText(
  'POLYGON((114.806542 38.008156,114.808366 38.008156,114.808312 38.006397,114.806595 38.006423,114.806542 38.008156))'),4326) AS geom)
INSERT INTO public.land_plot
 (id, land_name, land_type, land_area, is_deleted, address, create_by, create_time, update_by, update_time,
  image, measurement_method, boundary, center_point, owner_name, owner_phone)
SELECT 3012, '兴安镇真实耕地二号', '新增',
 ROUND((ST_Area(g.geom::geography)/666.6667)::numeric,2)::real, 0,
 '河北省石家庄市藁城区兴安镇', '1', TIMESTAMP '2026-11-18 10:10:00', '1', TIMESTAMP '2026-11-18 10:10:00',
 'http://127.0.0.1:8088/profile/agri/landplot/3005.jpg', '卫星图描点',
 g.geom, ST_Centroid(g.geom), '周大田', '13331107006'
FROM g;

-- 验证
SELECT id, land_name, land_area AS 面积亩,
       ROUND((ST_Area(boundary::geography)/666.6667)::numeric,3) AS 复算亩,
       measurement_method AS 测量方式
FROM public.land_plot WHERE id IN (3011, 3012) ORDER BY id;
