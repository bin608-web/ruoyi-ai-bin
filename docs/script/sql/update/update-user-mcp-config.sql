-- ----------------------------
-- Table structure for user_mcp_config
-- 用户 MCP 配置表 - 允许不同用户配置不同的 MCP 服务
-- ----------------------------
DROP TABLE IF EXISTS `user_mcp_config`;
CREATE TABLE `user_mcp_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置 ID',
  `user_id` bigint NOT NULL COMMENT '用户 ID',
  `tool_id` bigint NOT NULL COMMENT '工具 ID（关联 mcp_tool_info.id）',
  `tool_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '工具名称（冗余字段，便于查询）',
  `config_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '配置名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '配置描述',
  `config_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '覆盖配置信息（JSON 格式）',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED-启用，DISABLED-禁用',
  `priority` int NOT NULL DEFAULT '100' COMMENT '优先级（数字越小优先级越高）',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '删除标志（0 代表存在 2 代表删除）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_user_tool` (`user_id`,`tool_id`) USING BTREE COMMENT '用户 + 工具唯一索引',
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_tool_id` (`tool_id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_priority` (`priority`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户 MCP 配置表';

-- ----------------------------
-- 说明文档
-- ----------------------------
-- 
-- 字段说明:
-- 1. user_id: 用户 ID，用于区分不同用户的配置
-- 2. tool_id: 关联 mcp_tool_info 表的工具 ID
-- 3. tool_name: 冗余工具名称，避免联表查询
-- 4. config_name: 配置名称，如"开发环境配置"、"生产环境配置"
-- 5. description: 配置描述
-- 6. config_json: 用户自定义的配置，覆盖工具默认配置
--    - LOCAL 类型：{"command": "npx", "args": ["-y", "@example/mcp-server"], "env": {"KEY": "value"}}
--    - REMOTE 类型：{"baseUrl": "http://localhost:8080/mcp", "headers": {"Authorization": "Bearer xxx"}}
-- 7. status: ENABLED-启用，DISABLED-禁用
-- 8. priority: 优先级，数字越小优先级越高，用于多个配置冲突时选择
--
-- 使用场景:
-- 1. 不同用户可以使用不同的 MCP 服务器地址
-- 2. 用户可以自定义工具的环境变量
-- 3. 用户可以启用/禁用某些工具
-- 4. 支持多配置管理（如开发、测试、生产环境）
--
-- 权限说明:
-- - mcp:user-config:list: 查询配置列表权限
-- - mcp:user-config:query: 查询配置详情权限
-- - mcp:user-config:add: 新增配置权限
-- - mcp:user-config:edit: 修改配置权限
-- - mcp:user-config:remove: 删除配置权限
-- - mcp:user-config:export: 导出配置权限
--
