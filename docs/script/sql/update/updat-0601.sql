-- 更新脚本：添加用户技能管理和 MCP 用户配置功能
-- 执行时间：2026-06-01

-- ----------------------------
-- 1. 扩展 mcp_tool_info 表，添加用户配置字段
-- ----------------------------
ALTER TABLE `mcp_tool_info` 
ADD COLUMN `user_id` bigint NULL DEFAULT NULL COMMENT '用户 ID（NULL 表示全局工具）' AFTER `tenant_id`,
ADD COLUMN `is_shared` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'N' COMMENT '是否共享给其他用户（Y/N）' AFTER `user_id`,
ADD COLUMN `shared_scope` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '共享范围（用户 ID 列表，逗号分隔）' AFTER `is_shared`;

-- ----------------------------
-- 2. 创建用户技能表
-- ----------------------------
DROP TABLE IF EXISTS `user_skill`;
CREATE TABLE `user_skill` (
    `id` bigint NOT NULL COMMENT '主键',
    `user_id` bigint NOT NULL COMMENT '用户 ID',
    `skill_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '技能名称',
    `skill_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '技能编码（唯一标识）',
    `skill_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '技能类型：LOCAL-本地脚本, MCP- MCP 工具, CUSTOM-自定义',
    `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '技能描述',
    `skill_config` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '技能配置（JSON 格式）',
    `skill_code_content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '技能代码内容',
    `file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '技能文件路径',
    `is_enabled` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'Y' COMMENT '是否启用（Y/N）',
    `is_public` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'N' COMMENT '是否公开（Y/N）',
    `test_result` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '最近测试结果',
    `test_time` datetime NULL DEFAULT NULL COMMENT '最近测试时间',
    `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
    `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
    `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
    `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
    `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
    `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_skill_code` (`skill_code`, `tenant_id`) USING BTREE COMMENT '技能编码唯一索引',
    KEY `idx_user_id` (`user_id`) USING BTREE,
    KEY `idx_skill_type` (`skill_type`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户技能表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 3. 创建技能市场表
-- ----------------------------
DROP TABLE IF EXISTS `skill_market`;
CREATE TABLE `skill_market` (
    `id` bigint NOT NULL COMMENT '主键',
    `skill_id` bigint NOT NULL COMMENT '关联的用户技能 ID',
    `skill_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '技能名称',
    `skill_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '技能编码',
    `author_id` bigint NOT NULL COMMENT '作者用户 ID',
    `author_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '作者名称',
    `skill_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '技能类型',
    `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '技能描述',
    `usage_guide` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '使用指南',
    `tags` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '标签（逗号分隔）',
    `download_count` int NULL DEFAULT 0 COMMENT '下载次数',
    `rating` decimal(3,2) NULL DEFAULT 0.00 COMMENT '评分（0-5）',
    `rating_count` int NULL DEFAULT 0 COMMENT '评分次数',
    `is_approved` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'P' COMMENT '审核状态（P-待审核，A-已通过，R-已拒绝）',
    `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
    `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_market_skill_code` (`skill_code`, `tenant_id`) USING BTREE,
    KEY `idx_author_id` (`author_id`) USING BTREE,
    KEY `idx_skill_type` (`skill_type`) USING BTREE,
    KEY `idx_is_approved` (`is_approved`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '技能市场表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 4. 创建技能分享表
-- ----------------------------
DROP TABLE IF EXISTS `skill_share`;
CREATE TABLE `skill_share` (
    `id` bigint NOT NULL COMMENT '主键',
    `skill_id` bigint NOT NULL COMMENT '用户技能 ID',
    `from_user_id` bigint NOT NULL COMMENT '分享者用户 ID',
    `to_user_id` bigint NULL DEFAULT NULL COMMENT '接收者用户 ID（NULL 表示公开）',
    `to_user_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '接收者名称',
    `share_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '分享类型：PUBLIC-公开，PRIVATE-私有，GROUP-群组',
    `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'PENDING' COMMENT '分享状态（PENDING-待接受，ACCEPTED-已接受，REJECTED-已拒绝，CANCELLED-已取消）',
    `message` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '分享消息',
    `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
    `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_skill_id` (`skill_id`) USING BTREE,
    KEY `idx_from_user_id` (`from_user_id`) USING BTREE,
    KEY `idx_to_user_id` (`to_user_id`) USING BTREE,
    KEY `idx_status` (`status`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '技能分享表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 5. 创建技能使用记录表
-- ----------------------------
DROP TABLE IF EXISTS `skill_usage_log`;
CREATE TABLE `skill_usage_log` (
    `id` bigint NOT NULL COMMENT '主键',
    `skill_id` bigint NOT NULL COMMENT '用户技能 ID',
    `session_id` bigint NULL DEFAULT NULL COMMENT '会话 ID',
    `user_id` bigint NOT NULL COMMENT '使用用户 ID',
    `usage_context` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '使用上下文',
    `result` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '使用结果',
    `success` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'Y' COMMENT '是否成功（Y/N）',
    `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_skill_id` (`skill_id`) USING BTREE,
    KEY `idx_user_id` (`user_id`) USING BTREE,
    KEY `idx_session_id` (`session_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '技能使用记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 6. 添加菜单权限
-- ----------------------------
-- 技能管理主菜单
INSERT INTO `sys_menu` VALUES (3000, '技能管理', 0, 3, 'skill', '', '', 1, 0, 'M', '0', '0', '', 'mdi:star-circle', 103, 1, NOW(), 1, NOW(), '技能管理模块');

-- 我的技能
INSERT INTO `sys_menu` VALUES (3001, '我的技能', 3000, 1, 'my', 'skill/my/index', '', 1, 0, 'C', '0', '0', 'skill:my:list', 'mdi:star', 103, 1, NOW(), 1, NOW(), '我的技能管理');
INSERT INTO `sys_menu` VALUES (3002, '我的技能查询', 3001, 1, '#', '', '', 1, 0, 'F', '0', '0', 'skill:my:query', '#', 103, 1, NOW(), NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (3003, '我的技能新增', 3001, 2, '#', '', '', 1, 0, 'F', '0', '0', 'skill:my:add', '#', 103, 1, NOW(), NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (3004, '我的技能修改', 3001, 3, '#', '', '', 1, 0, 'F', '0', '0', 'skill:my:edit', '#', 103, 1, NOW(), NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (3005, '我的技能删除', 3001, 4, '#', '', '', 1, 0, 'F', '0', '0', 'skill:my:remove', '#', 103, 1, NOW(), NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (3006, '我的技能测试', 3001, 5, '#', '', '', 1, 0, 'F', '0', '0', 'skill:my:test', '#', 103, 1, NOW(), NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (3007, '我的技能分享', 3001, 6, '#', '', '', 1, 0, 'F', '0', '0', 'skill:my:share', '#', 103, 1, NOW(), NULL, NULL, '');

-- 技能市场
INSERT INTO `sys_menu` VALUES (3010, '技能市场', 3000, 2, 'market', 'skill/market/index', '', 1, 0, 'C', '0', '0', 'skill:market:list', 'mdi:storefront', 103, 1, NOW(), 1, NOW(), '技能市场');
INSERT INTO `sys_menu` VALUES (3011, '技能市场查询', 3010, 1, '#', '', '', 1, 0, 'F', '0', '0', 'skill:market:query', '#', 103, 1, NOW(), NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (3012, '技能市场下载', 3010, 2, '#', '', '', 1, 0, 'F', '0', '0', 'skill:market:download', '#', 103, 1, NOW(), NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (3013, '技能市场审核', 3010, 3, '#', '', '', 1, 0, 'F', '0', '0', 'skill:market:audit', '#', 103, 1, NOW(), NULL, NULL, '');

-- 分享管理
INSERT INTO `sys_menu` VALUES (3020, '分享管理', 3000, 3, 'share', 'skill/share/index', '', 1, 0, 'C', '0', '0', 'skill:share:list', 'mdi:share-variant', 103, 1, NOW(), 1, NOW(), '分享管理');
INSERT INTO `sys_menu` VALUES (3021, '分享查询', 3020, 1, '#', '', '', 1, 0, 'F', '0', '0', 'skill:share:query', '#', 103, 1, NOW(), NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (3022, '分享接受', 3020, 2, '#', '', '', 1, 0, 'F', '0', '0', 'skill:share:accept', '#', 103, 1, NOW(), NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (3023, '分享拒绝', 3020, 3, '#', '', '', 1, 0, 'F', '0', '0', 'skill:share:reject', '#', 103, 1, NOW(), NULL, NULL, '');

-- ----------------------------
-- 7. 更新 MCP 工具表权限（允许用户级配置）
-- ----------------------------
-- 添加用户配置 MCP 工具的权限
INSERT INTO `sys_menu` VALUES (2020, 'MCP 用户配置', 2000, 3, 'user-config', 'mcp/user-config/index', '', 1, 0, 'C', '0', '0', 'mcp:user-config:list', 'mdi:account-cog', 103, 1, NOW(), 1, NOW(), 'MCP 用户级配置');
INSERT INTO `sys_menu` VALUES (2021, 'MCP 用户配置查询', 2020, 1, '#', '', '', 1, 0, 'F', '0', '0', 'mcp:user-config:query', '#', 103, 1, NOW(), NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (2022, 'MCP 用户配置新增', 2020, 2, '#', '', '', 1, 0, 'F', '0', '0', 'mcp:user-config:add', '#', 103, 1, NOW(), NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (2023, 'MCP 用户配置修改', 2020, 3, '#', '', '', 1, 0, 'F', '0', '0', 'mcp:user-config:edit', '#', 103, 1, NOW(), NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (2024, 'MCP 用户配置删除', 2020, 4, '#', '', '', 1, 0, 'F', '0', '0', 'mcp:user-config:remove', '#', 103, 1, NOW(), NULL, NULL, '');
