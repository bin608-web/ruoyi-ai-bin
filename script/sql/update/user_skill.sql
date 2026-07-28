-- ----------------------------
-- 用户技能表
-- ----------------------------
DROP TABLE IF EXISTS `user_skill`;
CREATE TABLE `user_skill` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户 ID',
  `skill_name` varchar(100) NOT NULL COMMENT '技能名称',
  `skill_code` varchar(100) NOT NULL COMMENT '技能编码（唯一标识）',
  `skill_type` varchar(20) NOT NULL COMMENT '技能类型：LOCAL-本地脚本，MCP-MCP 工具，CUSTOM-自定义',
  `description` varchar(500) DEFAULT NULL COMMENT '技能描述',
  `skill_config` text DEFAULT NULL COMMENT '技能配置（JSON 格式）',
  `skill_code_content` longtext DEFAULT NULL COMMENT '技能代码内容',
  `file_path` varchar(500) DEFAULT NULL COMMENT '技能文件路径',
  `is_enabled` char(1) DEFAULT 'Y' COMMENT '是否启用（Y/N）',
  `is_public` char(1) DEFAULT 'N' COMMENT '是否公开（Y/N）',
  `test_result` text DEFAULT NULL COMMENT '最近测试结果',
  `test_time` datetime DEFAULT NULL COMMENT '最近测试时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint(20) DEFAULT NULL COMMENT '租户 ID',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_skill_code_tenant` (`skill_code`, `tenant_id`) USING BTREE COMMENT '技能编码唯一索引',
  KEY `idx_user_id` (`user_id`) USING BTREE COMMENT '用户 ID 索引',
  KEY `idx_skill_type` (`skill_type`) USING BTREE COMMENT '技能类型索引',
  KEY `idx_is_enabled` (`is_enabled`) USING BTREE COMMENT '启用状态索引',
  KEY `idx_tenant_id` (`tenant_id`) USING BTREE COMMENT '租户 ID 索引'
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户技能表';

-- ----------------------------
-- 技能市场表（用于公开分享的技能）
-- ----------------------------
DROP TABLE IF EXISTS `skill_market`;
CREATE TABLE `skill_market` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `skill_id` bigint(20) NOT NULL COMMENT '关联的用户技能 ID',
  `user_id` bigint(20) NOT NULL COMMENT '发布者用户 ID',
  `skill_name` varchar(100) NOT NULL COMMENT '技能名称',
  `skill_code` varchar(100) NOT NULL COMMENT '技能编码',
  `skill_type` varchar(20) NOT NULL COMMENT '技能类型',
  `description` varchar(500) DEFAULT NULL COMMENT '技能描述',
  `usage_example` text DEFAULT NULL COMMENT '使用示例',
  `download_count` int(11) DEFAULT '0' COMMENT '下载次数',
  `rating` decimal(3,2) DEFAULT '0.00' COMMENT '评分',
  `is_approved` char(1) DEFAULT 'N' COMMENT '是否审核通过（Y/N）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint(20) DEFAULT NULL COMMENT '租户 ID',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`) USING BTREE COMMENT '用户 ID 索引',
  KEY `idx_skill_type` (`skill_type`) USING BTREE COMMENT '技能类型索引',
  KEY `idx_is_approved` (`is_approved`) USING BTREE COMMENT '审核状态索引',
  KEY `idx_tenant_id` (`tenant_id`) USING BTREE COMMENT '租户 ID 索引'
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='技能市场表';

-- ----------------------------
-- 技能分享记录表
-- ----------------------------
DROP TABLE IF EXISTS `skill_share`;
CREATE TABLE `skill_share` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `skill_id` bigint(20) NOT NULL COMMENT '技能 ID',
  `from_user_id` bigint(20) NOT NULL COMMENT '分享者用户 ID',
  `to_user_id` bigint(20) DEFAULT NULL COMMENT '接收者用户 ID（NULL 表示公开）',
  `share_type` varchar(20) NOT NULL COMMENT '分享类型：PUBLIC-公开，PRIVATE-私聊',
  `message` varchar(500) DEFAULT NULL COMMENT '分享消息',
  `is_read` char(1) DEFAULT 'N' COMMENT '是否已读（Y/N）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint(20) DEFAULT NULL COMMENT '租户 ID',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_from_user_id` (`from_user_id`) USING BTREE COMMENT '分享者用户 ID 索引',
  KEY `idx_to_user_id` (`to_user_id`) USING BTREE COMMENT '接收者用户 ID 索引',
  KEY `idx_skill_id` (`skill_id`) USING BTREE COMMENT '技能 ID 索引',
  KEY `idx_tenant_id` (`tenant_id`) USING BTREE COMMENT '租户 ID 索引'
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='技能分享记录表';
