-- 修复轨迹作业类型脏数据
-- 现象：trajectory_info.work_type 存的是 '1'，但 work 表主键从 4001 开始，
--       后端按 id=1 查农活得到 null，调用 getWorkName() 抛空指针，轨迹列表接口 500。
-- 处理：将历史测试轨迹的 work_type 修正为合法的农活 id（4001 = 耕地）。
-- 可重复执行：仅当 work_type 仍为非法值 '1' 时才更新。
UPDATE trajectory_info
SET work_type = '4001'
WHERE work_type = '1';
