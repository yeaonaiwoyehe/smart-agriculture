-- ============================================================
-- 修复 trajectory_info.status 为 NULL 导致轨迹列表查不到的问题
-- 历史轨迹默认视为未删除（status=0）。可重复执行
-- ============================================================

UPDATE trajectory_info SET status = 0 WHERE status IS NULL;

-- 验证
SELECT trajectory_info_id, phone_number, work_type, area, status
FROM trajectory_info ORDER BY created_time;
