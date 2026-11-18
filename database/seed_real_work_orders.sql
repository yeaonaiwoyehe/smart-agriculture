-- ============================================================
-- 为两块真实耕地 3011/3012 补正式作业单（"结束与核算"环节产物）
-- 面积直接取地块 land_area，作业时间 2026-11-18，纳入 11 月结算周期
-- 可重复执行：先按固定 UUID 删除再插入
-- ============================================================

DELETE FROM trajectory_info WHERE trajectory_info_id IN
    ('a1000000-0000-0000-0000-000000003011',
     'a1000000-0000-0000-0000-000000003012');

INSERT INTO trajectory_info
    (trajectory_info_id, work_type, phone_number, width, created_by, created_time,
     status, start_time, end_time, trajectory_img, plot_id, area)
SELECT 'a1000000-0000-0000-0000-000000003011', '4001', '15888888888', 200, '1', now(),
       0, '2026-11-18 09:00:00', '2026-11-18 10:30:00',
       'http://127.0.0.1:8088/profile/agri/trajectory/traj_1.jpg', id, land_area
FROM land_plot WHERE id = 3011;

INSERT INTO trajectory_info
    (trajectory_info_id, work_type, phone_number, width, created_by, created_time,
     status, start_time, end_time, trajectory_img, plot_id, area)
SELECT 'a1000000-0000-0000-0000-000000003012', '4001', '15888888888', 200, '1', now(),
       0, '2026-11-18 11:00:00', '2026-11-18 11:50:00',
       'http://127.0.0.1:8088/profile/agri/trajectory/traj_1.jpg', id, land_area
FROM land_plot WHERE id = 3012;

-- 验证
SELECT trajectory_info_id, plot_id, area, start_time FROM trajectory_info
WHERE plot_id IN (3011, 3012) ORDER BY start_time;
