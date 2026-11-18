-- ============================================================
-- APP 组件管理种子数据（app_permission）
-- 对应 AgriNav 安卓 APP 的真实页面/功能，按 parent_id 组成树形结构
-- 可重复执行：先清空再插入，并重置自增序列
-- is_visible: 0 可见 / 1 隐藏；is_deleted: 0 未删除 / 1 已删除
-- ============================================================

-- 1. 清空旧数据（可重复执行）
DELETE FROM app_permission;

-- 2. 一级功能模块（parent_id = 0）
INSERT INTO app_permission (component_id, permission_name, parent_id, component_str, is_visible, is_deleted) VALUES
 (1, '首页',     0, 'cn.agrinav.MainActivity',                                        0, 0),
 (2, '找农活',   0, 'cn.agrinav.front.activity.findjob.FindJobActivity',             0, 0),
 (3, '找农田',   0, 'cn.agrinav.front.activity.findfarm.FindFarmActivity',           0, 0),
 (4, '测亩',     0, 'cn.agrinav.measure.activity.MeasureActivity',                   0, 0),
 (5, '作业轨迹', 0, 'cn.agrinav.trajectory.activity.TrajectoryActivity',             0, 0),
 (6, '消息通知', 0, 'cn.agrinav.front.activity.sysmessage.ReceiveSysMessageActivity',0, 0),
 (7, '个人中心', 0, 'cn.agrinav.personal.activity.info.PersonInfoActivity',          0, 0),
 (8, '扫码',     0, 'cn.agrinav.front.activity.ScanCodeActivity',                    0, 0);

-- 3. 找农活子页面（parent_id = 2）
INSERT INTO app_permission (component_id, permission_name, parent_id, component_str, is_visible, is_deleted) VALUES
 (11, '农活列表', 2, 'cn.agrinav.front.activity.findjob.FindJobActivity',  0, 0),
 (12, '农活详情', 2, 'cn.agrinav.front.activity.findjob.JobDetailActivity',0, 0),
 (13, '发布农活', 2, 'cn.agrinav.front.activity.findjob.AddTaskActivity',  0, 0);

-- 4. 找农田子页面（parent_id = 3）
INSERT INTO app_permission (component_id, permission_name, parent_id, component_str, is_visible, is_deleted) VALUES
 (21, '农田列表',   3, 'cn.agrinav.front.activity.findfarm.FindFarmActivity',      0, 0),
 (22, '农田详情',   3, 'cn.agrinav.front.activity.findfarm.FarmDetailActivity',    0, 0),
 (23, '农田项详情', 3, 'cn.agrinav.front.activity.findfarm.FarmItemDetailActivity',0, 0);

-- 5. 测亩子页面（parent_id = 4）
INSERT INTO app_permission (component_id, permission_name, parent_id, component_str, is_visible, is_deleted) VALUES
 (31, '测亩首页', 4, 'cn.agrinav.measure.activity.MeasureActivity',      0, 0),
 (32, '测亩信息', 4, 'cn.agrinav.measure.activity.MeasureInfoActivity',  0, 0),
 (33, '地块信息', 4, 'cn.agrinav.measure.activity.PlotInfoActivity',     0, 0);

-- 6. 作业轨迹子页面（parent_id = 5）
INSERT INTO app_permission (component_id, permission_name, parent_id, component_str, is_visible, is_deleted) VALUES
 (41, '轨迹列表', 5, 'cn.agrinav.trajectory.activity.TrajectoryActivity',         0, 0),
 (42, '作业轨迹', 5, 'cn.agrinav.trajectory.activity.WorkTrajectoryActivity',     0, 0),
 (43, '轨迹重传', 5, 'cn.agrinav.trajectory.activity.TrajectoryReuploadActivity', 0, 0);

-- 7. 消息通知子页面（parent_id = 6）
INSERT INTO app_permission (component_id, permission_name, parent_id, component_str, is_visible, is_deleted) VALUES
 (51, '消息列表', 6, 'cn.agrinav.front.activity.sysmessage.ReceiveSysMessageActivity', 0, 0),
 (52, '消息详情', 6, 'cn.agrinav.front.activity.sysmessage.SysMsgDetailActivity',     0, 0);

-- 8. 个人中心子页面（parent_id = 7）
INSERT INTO app_permission (component_id, permission_name, parent_id, component_str, is_visible, is_deleted) VALUES
 (61, '个人信息', 7, 'cn.agrinav.personal.activity.info.PersonInfoActivity',                 0, 0),
 (62, '意见反馈', 7, 'cn.agrinav.personal.activity.feedback.FeedbackActivity',              0, 0),
 (63, '账号安全', 7, 'cn.agrinav.personal.activity.security.SecurityActivity',              0, 0),
 (64, '联系客服', 7, 'cn.agrinav.personal.activity.communication.CommunicationActivity',    0, 0);

-- 9. 扫码子页面（parent_id = 8）
INSERT INTO app_permission (component_id, permission_name, parent_id, component_str, is_visible, is_deleted) VALUES
 (71, '扫码结果', 8, 'cn.agrinav.front.activity.scan.ScanAfterActivity', 0, 0);

-- 10. 重置自增序列，避免后续新增组件主键冲突
SELECT setval(pg_get_serial_sequence('app_permission', 'component_id'),
              (SELECT MAX(component_id) FROM app_permission));
