-- ============================================================
-- 业务闭环菜单（在 2000 农业作业管理目录下新增 2012-2014）
-- 可重复执行：先按固定 menu_id 删除再插入
-- 说明：component 为相对 src/views 的路径（不带 .vue）
-- ============================================================

DELETE FROM sys_menu WHERE menu_id IN (2012, 2013, 2014);

-- 2012 单价规则
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query,
    is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2012, '单价规则', 2000, 12, 'pricingrule', 'closure/pricingRule/index', NULL,
    1, 0, 'C', '0', '0', 'agri:pricingRule:list', 'coin', 'admin', NOW(), '作业单价规则菜单');

-- 2013 复核管理
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query,
    is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2013, '复核管理', 2000, 13, 'review', 'closure/reviewRequest/index', NULL,
    1, 0, 'C', '0', '0', 'agri:reviewRequest:list', 'validated', 'admin', NOW(), '复核管理菜单');

-- 2014 结算管理
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query,
    is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2014, '结算管理', 2000, 14, 'settlement', 'closure/settlement/index', NULL,
    1, 0, 'C', '0', '0', 'agri:settlement:list', 'money', 'admin', NOW(), '结算管理菜单');

-- 验证
SELECT menu_id, menu_name, parent_id, path, component FROM sys_menu
WHERE menu_id BETWEEN 2012 AND 2014 ORDER BY menu_id;
