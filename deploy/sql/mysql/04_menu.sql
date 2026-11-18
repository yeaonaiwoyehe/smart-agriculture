-- ============================================================
-- 注册农业业务菜单（补齐业务页面入口缺失）
-- 可重复执行：先按固定 menu_id 删除再插入
-- ============================================================

DELETE FROM sys_menu WHERE menu_id IN (2000, 2001, 2002);

-- 业务目录
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query,
    is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2000, '农业作业管理', 0, 5, 'agri', NULL, NULL,
    1, 0, 'M', '0', '0', NULL, 'guide', 'admin', NOW(), '农业业务目录');

-- 农机管理
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query,
    is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2001, '农机管理', 2000, 1, 'machinery', 'agriculturalMachinery/index', NULL,
    1, 0, 'C', '0', '0', 'agri:machinery:list', 'component', 'admin', NOW(), '农机管理菜单');

-- 地块管理
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query,
    is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2002, '地块管理', 2000, 2, 'landplot', 'field/square/index', NULL,
    1, 0, 'C', '0', '0', 'agri:landplot:list', 'tree', 'admin', NOW(), '地块管理菜单');
