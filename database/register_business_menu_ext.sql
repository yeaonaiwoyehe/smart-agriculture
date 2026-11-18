-- ============================================================
-- 补全剩余农业业务菜单（在 2000 农业作业管理目录下新增 2003-2011）
-- 可重复执行：先按固定 menu_id 删除再插入
-- 说明：component 为相对 src/views 的路径（不带 .vue），与 2001/2002 一致
-- ============================================================

DELETE FROM sys_menu WHERE menu_id IN (2003, 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011);

-- 2003 农活管理
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query,
    is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2003, '农活管理', 2000, 3, 'farmwork', 'farmWork/index', NULL,
    1, 0, 'C', '0', '0', 'agri:farmwork:list', 'edit', 'admin', NOW(), '农活管理菜单');

-- 2004 作业地块
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query,
    is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2004, '作业地块', 2000, 4, 'workingplot', 'field/work/index', NULL,
    1, 0, 'C', '0', '0', 'agri:workingplot:list', 'guide', 'admin', NOW(), '作业地块菜单');

-- 2005 轨迹列表
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query,
    is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2005, '轨迹列表', 2000, 5, 'trajlist', 'trajaction/list/index', NULL,
    1, 0, 'C', '0', '0', 'agri:trajlist:list', 'list', 'admin', NOW(), '轨迹列表菜单');

-- 2006 轨迹地图
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query,
    is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2006, '轨迹地图', 2000, 6, 'trajmap', 'trajaction/map/index', NULL,
    1, 0, 'C', '0', '0', 'agri:trajmap:list', 'monitor', 'admin', NOW(), '轨迹地图菜单');

-- 2007 作业热力图
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query,
    is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2007, '作业热力图', 2000, 7, 'heatmap', 'field/heatmap/index', NULL,
    1, 0, 'C', '0', '0', 'agri:heatmap:list', 'chart', 'admin', NOW(), '作业热力图菜单');

-- 2008 面积统计
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query,
    is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2008, '面积统计', 2000, 8, 'area', 'field/area/index', NULL,
    1, 0, 'C', '0', '0', 'agri:area:list', 'documentation', 'admin', NOW(), '面积统计菜单');

-- 2009 反馈管理
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query,
    is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2009, '反馈管理', 2000, 9, 'feedback', 'feedback/index', NULL,
    1, 0, 'C', '0', '0', 'agri:feedback:list', 'message', 'admin', NOW(), '反馈管理菜单');

-- 2010 数据仪表盘
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query,
    is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2010, '数据仪表盘', 2000, 10, 'dashboard', 'monitor/instrumentpanel/index', NULL,
    1, 0, 'C', '0', '0', 'agri:dashboard:list', 'dashboard', 'admin', NOW(), '数据仪表盘菜单');

-- 2011 APP组件管理
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query,
    is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2011, 'APP组件管理', 2000, 11, 'appcomponent', 'appComponent/index', NULL,
    1, 0, 'C', '0', '0', 'agri:appcomponent:list', 'component', 'admin', NOW(), 'APP组件管理菜单');

-- 验证
SELECT menu_id, menu_name, parent_id, path, component FROM sys_menu
WHERE menu_id BETWEEN 2000 AND 2011 ORDER BY menu_id;
