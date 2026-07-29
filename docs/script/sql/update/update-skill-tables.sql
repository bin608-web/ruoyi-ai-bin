-- ============================================================
-- 技能系统完整 SQL 脚本
-- 包含：技能分享记录表、技能订阅表、技能评分表、测试数据
-- 执行时间：2026-07-03
-- ============================================================

-- ----------------------------
-- 0. 前置修复：确保 user_skill 表 id 字段为 AUTO_INCREMENT
-- ----------------------------
ALTER TABLE `user_skill` MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `skill_market` MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `skill_share` MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键';
ALTER TABLE `skill_usage_log` MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键';

-- ----------------------------
-- 1. 技能分享记录表（用于管理后台 /skills/share/* 接口）
-- ----------------------------
DROP TABLE IF EXISTS `skill_share_record`;
CREATE TABLE `skill_share_record` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `skill_id` bigint NOT NULL COMMENT '技能 ID（关联 user_skill.id）',
    `skill_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '技能名称（冗余）',
    `from_user_id` bigint NOT NULL COMMENT '分享者用户 ID',
    `from_user_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '分享者名称',
    `to_user_id` bigint NOT NULL COMMENT '接收者用户 ID',
    `to_user_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '接收者名称',
    `share_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'PRIVATE' COMMENT '分享类型：PUBLIC-公开，PRIVATE-私有',
    `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING-待处理，ACCEPTED-已接受，REJECTED-已拒绝，REVOKED-已撤销',
    `message` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '分享消息',
    `create_time` datetime NULL DEFAULT NULL COMMENT '分享时间',
    `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_skill_id` (`skill_id`) USING BTREE,
    KEY `idx_from_user_id` (`from_user_id`) USING BTREE,
    KEY `idx_to_user_id` (`to_user_id`) USING BTREE,
    KEY `idx_status` (`status`) USING BTREE,
    KEY `idx_tenant_id` (`tenant_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '技能分享记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 2. 技能订阅表（用于 /skills/market/subscribe 接口）
-- ----------------------------
DROP TABLE IF EXISTS `skill_subscription`;
CREATE TABLE `skill_subscription` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `skill_id` bigint NOT NULL COMMENT '技能 ID（关联 user_skill.id）',
    `user_id` bigint NOT NULL COMMENT '订阅用户 ID',
    `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE-已订阅，CANCELLED-已取消',
    `create_time` datetime NULL DEFAULT NULL COMMENT '订阅时间',
    `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_skill_user` (`skill_id`, `user_id`) USING BTREE COMMENT '技能+用户唯一索引',
    KEY `idx_user_id` (`user_id`) USING BTREE,
    KEY `idx_tenant_id` (`tenant_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '技能订阅表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 3. 技能评分表（用于 /skills/market/rate 接口）
-- ----------------------------
DROP TABLE IF EXISTS `skill_rating`;
CREATE TABLE `skill_rating` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `skill_id` bigint NOT NULL COMMENT '技能 ID（关联 user_skill.id）',
    `user_id` bigint NOT NULL COMMENT '评分用户 ID',
    `user_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '评分用户名称',
    `rating` tinyint NOT NULL COMMENT '评分（1-5）',
    `comment` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '评价内容',
    `create_time` datetime NULL DEFAULT NULL COMMENT '评分时间',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_skill_user_rating` (`skill_id`, `user_id`) USING BTREE COMMENT '技能+用户唯一索引（同一用户只能评分一次）',
    KEY `idx_skill_id` (`skill_id`) USING BTREE,
    KEY `idx_user_id` (`user_id`) USING BTREE,
    KEY `idx_tenant_id` (`tenant_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '技能评分表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 4. 测试数据
-- ----------------------------

-- 4.1 测试用户技能数据
INSERT INTO `user_skill` (`id`, `user_id`, `skill_name`, `skill_code`, `skill_type`, `description`, `skill_config`, `skill_code_content`, `file_path`, `is_enabled`, `is_public`, `create_time`, `update_time`, `tenant_id`, `remark`) VALUES
(10001, 1, '文本摘要生成器', 'text_summary', 'LOCAL', '自动提取文本关键信息并生成摘要。支持中英文文本，可调整摘要长度。', '{"language": "python", "version": "1.0.0"}', '#!/usr/bin/env python3\n# -*- coding: utf-8 -*-\n"""文本摘要生成器 - 自动提取文本关键信息并生成摘要"""\nimport sys\nimport json\n\ndef main(input_data):\n    """主函数：接收文本输入，返回摘要结果"""\n    if isinstance(input_data, str):\n        try:\n            data = json.loads(input_data)\n        except:\n            data = {"text": input_data}\n    else:\n        data = input_data\n    \n    text = data.get("text", "")\n    max_length = data.get("max_length", 200)\n    \n    if not text:\n        return json.dumps({"success": False, "error": "输入文本为空"})\n    \n    # 简单摘要逻辑：提取前N个句子\n    sentences = text.replace("!", ".").replace("?", ".").replace("。", ".").split(".")\n    sentences = [s.strip() for s in sentences if s.strip()]\n    \n    summary = ""\n    for sentence in sentences:\n        if len(summary) + len(sentence) <= max_length:\n            summary += sentence + "。 "\n        else:\n            break\n    \n    return json.dumps({\n        "success": True,\n        "summary": summary.strip(),\n        "original_length": len(text),\n        "summary_length": len(summary),\n        "compression_ratio": f"{len(summary)/max(len(text),1)*100:.1f}%"\n    }, ensure_ascii=False)\n\nif __name__ == "__main__":\n    if len(sys.argv) > 1:\n        input_text = sys.argv[1]\n    else:\n        input_text = ""\n    print(main(input_text))', 'E:\\working\\ruoyi-ai\\skills\\1\\text_summary.py', 'Y', 'Y', NOW(), NOW(), 0, '文本处理类技能'),

(10002, 1, 'JSON 格式化工具', 'json_formatter', 'LOCAL', '格式化、验证和美化 JSON 数据。支持 JSON 压缩、转义、不同缩进风格。', '{"language": "python", "version": "1.0.0"}', '#!/usr/bin/env python3\n# -*- coding: utf-8 -*-\n"""JSON 格式化工具 - 格式化、验证和美化 JSON 数据"""\nimport sys\nimport json\n\ndef main(input_data):\n    """主函数：接收 JSON 字符串，返回格式化结果"""\n    if isinstance(input_data, str):\n        try:\n            data = json.loads(input_data)\n        except:\n            data = {"input": input_data}\n    else:\n        data = input_data\n    \n    json_str = data.get("input", "")\n    action = data.get("action", "format")\n    indent = data.get("indent", 2)\n    \n    if not json_str:\n        return json.dumps({"success": False, "error": "输入为空"})\n    \n    try:\n        parsed = json.loads(json_str)\n        if action == "compress":\n            result = json.dumps(parsed, ensure_ascii=False, separators=(",", ":"))\n        elif action == "validate":\n            result = json.dumps({"valid": True, "keys_count": len(parsed) if isinstance(parsed, dict) else len(parsed)})\n        else:\n            result = json.dumps(parsed, ensure_ascii=False, indent=indent)\n        \n        return json.dumps({"success": True, "output": result}, ensure_ascii=False)\n    except json.JSONDecodeError as e:\n        return json.dumps({"success": False, "error": f"JSON 格式错误: {str(e)}"})\n\nif __name__ == "__main__":\n    if len(sys.argv) > 1:\n        input_text = sys.argv[1]\n    else:\n        input_text = ""\n    print(main(input_text))', 'E:\\working\\ruoyi-ai\\skills\\1\\json_formatter.py', 'Y', 'Y', NOW(), NOW(), 0, '数据处理类技能'),

(10003, 1, '天气查询助手', 'weather_query', 'MCP', '通过 MCP 工具查询指定城市的实时天气信息，包括温度、湿度、天气状况等。', '{"mcp_server": "weather-service", "tool_name": "get_weather"}', '{\n  "mcp_server": "weather-service",\n  "tool_name": "get_weather",\n  "parameters": {\n    "city": "string",\n    "units": "metric"\n  }\n}', 'E:\\working\\ruoyi-ai\\skills\\1\\weather_query.json', 'Y', 'Y', NOW(), NOW(), 0, 'MCP 工具类技能'),

(10004, 1, '代码片段生成器', 'code_snippet', 'CUSTOM', '根据自然语言描述生成代码片段，支持多种编程语言。', '{"language": "javascript", "version": "1.0.0"}', '// 代码片段生成器 - 根据自然语言描述生成代码片段\nfunction main(input) {\n  const data = typeof input === "string" ? JSON.parse(input) : input;\n  const { description, language, framework } = data;\n  \n  // 根据描述和语言生成代码模板\n  const templates = {\n    python: `# ${description}\\ndef solution():\n    # TODO: 实现 ${description}\n    pass`,\n    javascript: `// ${description}\\nfunction solution() {\\n  // TODO: 实现 ${description}\\n}`,\n    java: `// ${description}\\npublic class Solution {\\n    public static void main(String[] args) {\\n        // TODO: 实现 ${description}\\n    }\\n}`\n  };\n  \n  const code = templates[language] || templates.javascript;\n  \n  return JSON.stringify({\n    success: true,\n    code: code,\n    language: language || "javascript",\n    description: description\n  });\n}\n\n// 导出函数\nif (typeof module !== "undefined") {\n  module.exports = { main };\n}', 'E:\\working\\ruoyi-ai\\skills\\1\\code_snippet.js', 'Y', 'N', NOW(), NOW(), 0, '代码生成类技能'),

(10005, 2, '数据清洗工具', 'data_cleaner', 'LOCAL', '清洗和预处理 CSV/JSON 数据，包括去重、缺失值处理、格式标准化等。', '{"language": "python", "version": "1.0.0"}', '#!/usr/bin/env python3\n# -*- coding: utf-8 -*-\n"""数据清洗工具 - 清洗和预处理数据"""\nimport sys\nimport json\n\ndef main(input_data):\n    """主函数：接收数据输入，返回清洗后的结果"""\n    if isinstance(input_data, str):\n        try:\n            data = json.loads(input_data)\n        except:\n            data = {"data": input_data}\n    else:\n        data = input_data\n    \n    raw_data = data.get("data", [])\n    remove_duplicates = data.get("remove_duplicates", True)\n    fill_missing = data.get("fill_missing", True)\n    \n    if not raw_data:\n        return json.dumps({"success": False, "error": "输入数据为空"})\n    \n    # 去重\n    if remove_duplicates and isinstance(raw_data, list):\n        seen = set()\n        unique = []\n        for item in raw_data:\n            key = json.dumps(item, sort_keys=True)\n            if key not in seen:\n                seen.add(key)\n                unique.append(item)\n        raw_data = unique\n    \n    return json.dumps({\n        "success": True,\n        "cleaned_data": raw_data,\n        "original_count": len(raw_data),\n        "cleaned_count": len(raw_data)\n    }, ensure_ascii=False)\n\nif __name__ == "__main__":\n    if len(sys.argv) > 1:\n        input_text = sys.argv[1]\n    else:\n        input_text = ""\n    print(main(input_text))', 'E:\\working\\ruoyi-ai\\skills\\2\\data_cleaner.py', 'Y', 'Y', NOW(), NOW(), 0, '数据处理类技能');

-- 4.2 测试技能分享记录
INSERT INTO `skill_share_record` (`id`, `skill_id`, `skill_name`, `from_user_id`, `from_user_name`, `to_user_id`, `to_user_name`, `share_type`, `status`, `message`, `create_time`, `update_time`, `tenant_id`) VALUES
(20001, 10001, '文本摘要生成器', 1, '管理员', 2, '测试用户', 'PRIVATE', 'PENDING', '分享一个文本摘要工具给你，希望对你有用！', NOW(), NOW(), 0),
(20002, 10002, 'JSON 格式化工具', 1, '管理员', 2, '测试用户', 'PRIVATE', 'ACCEPTED', 'JSON 格式化工具，日常工作必备', NOW(), NOW(), 0),
(20003, 10005, '数据清洗工具', 2, '测试用户', 1, '管理员', 'PRIVATE', 'PENDING', '我写的数据清洗工具，请帮忙看看', NOW(), NOW(), 0);

-- 4.3 测试技能订阅
INSERT INTO `skill_subscription` (`id`, `skill_id`, `user_id`, `status`, `create_time`, `update_time`, `tenant_id`) VALUES
(30001, 10001, 2, 'ACTIVE', NOW(), NOW(), 0),
(30002, 10002, 2, 'ACTIVE', NOW(), NOW(), 0),
(30003, 10005, 1, 'ACTIVE', NOW(), NOW(), 0);

-- 4.4 测试技能评分
INSERT INTO `skill_rating` (`id`, `skill_id`, `user_id`, `user_name`, `rating`, `comment`, `create_time`, `tenant_id`) VALUES
(40001, 10001, 2, '测试用户', 5, '非常好用的文本摘要工具，准确率高，速度快！', NOW(), 0),
(40002, 10002, 2, '测试用户', 4, 'JSON 格式化很好用，希望能支持更多格式选项', NOW(), 0);

-- 4.5 更新 user_skill 表中的评分和下载统计数据
UPDATE `user_skill` SET `test_result` = '{"success": true, "summary": "这是一个测试摘要结果。", "compression_ratio": "45.2%"}', `test_time` = NOW() WHERE `id` = 10001;
UPDATE `user_skill` SET `test_result` = '{"success": true, "output": "{\\"formatted\\": true}"}', `test_time` = NOW() WHERE `id` = 10002;

-- 4.6 测试 MCP 用户配置数据
INSERT INTO `user_mcp_config` (`id`, `user_id`, `tool_id`, `tool_name`, `config_name`, `description`, `config_json`, `status`, `priority`, `create_time`, `update_time`, `del_flag`) VALUES
(50001, 1, 1, 'bing-search', 'Bing 搜索配置', '用于联网搜索的 Bing MCP 配置', '{"serverUrl": "http://localhost:8080/mcp/bing", "authType": "none", "timeout": 30000}', 'ENABLED', 100, NOW(), NOW(), '0'),
(50002, 1, 2, 'filesystem', '文件系统管理', '本地文件系统读写 MCP 配置', '{"serverUrl": "http://localhost:8080/mcp/filesystem", "authType": "none", "timeout": 60000}', 'ENABLED', 90, NOW(), NOW(), '0'),
(50003, 2, 1, 'bing-search', '搜索工具-测试', '测试用户的环境搜索配置', '{"serverUrl": "http://localhost:8080/mcp/bing", "authType": "bearer", "authToken": "test-token-123", "timeout": 30000}', 'ENABLED', 100, NOW(), NOW(), '0');

-- 4.7 更新技能市场统计（skill_market 表）
INSERT INTO `skill_market` (`id`, `skill_id`, `skill_name`, `skill_code`, `author_id`, `author_name`, `skill_type`, `description`, `usage_guide`, `tags`, `download_count`, `rating`, `rating_count`, `is_approved`, `create_time`, `update_time`, `tenant_id`) VALUES
(60001, 10001, '文本摘要生成器', 'text_summary', 1, '管理员', 'LOCAL', '自动提取文本关键信息并生成摘要', '1. 在聊天中输入 /skill 10001 调用\n2. 输入需要摘要的文本\n3. 系统会自动返回摘要结果', '文本处理,AI,摘要', 156, 4.80, 25, 'A', NOW(), NOW(), 0),
(60002, 10002, 'JSON 格式化工具', 'json_formatter', 1, '管理员', 'LOCAL', '格式化、验证和美化 JSON 数据', '1. 在聊天中输入 /skill 10002\n2. 输入 JSON 字符串\n3. 选择格式化或压缩操作', 'JSON,数据处理,格式化', 89, 4.50, 12, 'A', NOW(), NOW(), 0),
(60003, 10005, '数据清洗工具', 'data_cleaner', 2, '测试用户', 'LOCAL', '清洗和预处理 CSV/JSON 数据', '1. 在聊天中输入 /skill 10005\n2. 输入待清洗的数据\n3. 选择清洗选项', '数据处理,ETL,清洗', 42, 4.20, 8, 'A', NOW(), NOW(), 0);

-- ============================================================
-- 说明
-- ============================================================
-- 1. skill_share_record: 替代原有的 skill_share 表，用于管理后台 /skills/share/* 接口
-- 2. skill_subscription: 用于技能订阅功能，支持 /skills/market/subscribe 和 /unsubscribe
-- 3. skill_rating: 用于技能评分功能，支持 /skills/market/rate
-- 4. 测试数据包含 2 个用户（用户ID 1 和 2），5 个技能，3 个分享记录