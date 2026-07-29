-- 添加思考步骤字段到聊天消息表
ALTER TABLE `chat_message` ADD COLUMN `thinking_steps` longtext NULL COMMENT 'ReAct 思考步骤（JSON）' AFTER `content`;