-- 会话管理菜单 SQL
-- 在聊天消息之前插入会话管理菜单

-- 1. 插入会话管理菜单（父菜单：对话管理 menu_id=2000209300188356609）
-- order_num=0 会排在聊天消息(1)之前
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2000210914123456789, '会话管理', 2000209300188356609, 0, 'session', 'chat/session/index', NULL, 1, 0, 'C', '0', '0', 'system:session:list', 'lucide:message-circle', 103, 1, NOW(), NULL, NOW(), '');

-- 2. 插入按钮权限（F 类型）
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2000210914123456790, '会话管理查询', 2000210914123456789, 1, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:session:query', '#', 103, 1, NOW(), NULL, NOW(), '');

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2000210914123456791, '会话管理新增', 2000210914123456789, 2, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:session:add', '#', 103, 1, NOW(), NULL, NOW(), '');

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2000210914123456792, '会话管理修改', 2000210914123456789, 3, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:session:edit', '#', 103, 1, NOW(), NULL, NOW(), '');

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2000210914123456793, '会话管理删除', 2000210914123456789, 4, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:session:remove', '#', 103, 1, NOW(), NULL, NOW(), '');

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2000210914123456794, '会话管理导出', 2000210914123456789, 5, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:session:export', '#', 103, 1, NOW(), NULL, NOW(), '');

-- 3. 更新聊天消息的 order_num 为 1，保持排在会话管理后面
UPDATE `sys_menu` SET `order_num` = 1 WHERE `menu_id` = 2000210914680823809;