-- ============================================================
-- A-12 修正历史作业单状态
-- 历史演示数据已带 end_time，但 status 仍为 0（进行中），
-- 会被 AG-02 的"进行中作业单"检测误判。统一修正为已完成（status=1）。
-- 仅更新"有结束时间且仍标记进行中"的历史单，幂等，可重复执行
-- ============================================================

UPDATE trajectory_info
SET status = 1
WHERE status = 0
  AND end_time IS NOT NULL;

-- 验证
SELECT trajectory_info_id, created_by, work_type, status, start_time, end_time
FROM trajectory_info
ORDER BY created_time;
