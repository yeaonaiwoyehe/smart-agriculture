-- ============================================================
-- 智能体指令审计菜单（在 2000 农业作业管理目录下新增 2015）
-- 可重复执行：先按固定 menu_id 删除再插入
-- 说明：component 为相对 src/views 的路径（不带 .vue）
-- ============================================================

DELETE FROM sys_menu WHERE menu_id = 2015;

-- 2015 指令审计
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query,
    is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2015, '指令审计', 2000, 15, 'agent-audit', 'ai/audit/index', NULL,
    1, 0, 'C', '0', '0', 'agri:audit:list', 'log', 'admin', NOW(), '智能体指令审计菜单');

-- 验证
SELECT menu_id, menu_name, parent_id, path, component FROM sys_menu
WHERE menu_id = 2015;
