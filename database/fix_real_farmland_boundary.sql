-- ============================================================
-- 给真实耕地 3011/3012 补 boundary 包围盒记录
-- 这两个地块在 gps_refactor_ddl 之后才新增，boundary 表缺少对应记录，
-- 会导致 checkPoint 粗筛无法命中、作业点不写入 effective_point_buffer。
-- 可重复执行：先删除再插入；坐标*1e6，最小值用 floor、最大值用 ceil 保证点在盒内
-- ============================================================

DELETE FROM boundary WHERE land_plot_id IN (3011, 3012);

INSERT INTO boundary
    (id, left_down_longitude, left_down_latitude,
     right_top_longitude, right_top_latitude, land_plot_id, status)
SELECT
    id,
    FLOOR(ST_XMin(boundary) * 1000000),
    FLOOR(ST_YMin(boundary) * 1000000),
    CEIL(ST_XMax(boundary) * 1000000),
    CEIL(ST_YMax(boundary) * 1000000),
    id,
    0
FROM land_plot
WHERE id IN (3011, 3012);
