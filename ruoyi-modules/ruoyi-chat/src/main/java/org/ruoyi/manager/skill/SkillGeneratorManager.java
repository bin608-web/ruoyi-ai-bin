package org.ruoyi.manager.skill;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.domain.dto.skill.UserSkillDto;
import org.ruoyi.domain.entity.skill.UserSkill;
import org.ruoyi.mapper.skill.UserSkillMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

/**
 * 技能生成管理器
 * 负责根据用户描述动态生成技能代码
 *
 * @author ruoyi
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class SkillGeneratorManager {

    private final UserSkillMapper userSkillMapper;

    private static final String SKILLS_BASE_PATH = "E:\\working\\ruoyi-ai\\skills";

    /**
     * 根据用户描述生成技能
     *
     * @param userId 用户 ID
     * @param description 技能描述
     * @param skillType 技能类型
     * @return 生成的技能 DTO
     */
    public UserSkillDto generateSkill(Long userId, String description, String skillType) {
        UserSkillDto dto = new UserSkillDto();

        try {
            log.info("开始生成技能：userId={}, description={}, skillType={}", userId, description, skillType);

            // 1. 调用 LLM 生成技能代码
            String generatedCode = generateCodeWithLLM(description, skillType);

            if (StrUtil.isBlank(generatedCode)) {
                dto.setSuccess(false);
                dto.setErrorMessage("生成技能代码失败");
                return dto;
            }

            // 2. 解析生成的技能信息
            SkillInfo skillInfo = parseSkillInfo(generatedCode, description);

            // 3. 创建技能实体
            UserSkill userSkill = new UserSkill();
            userSkill.setUserId(userId);
            userSkill.setSkillName(skillInfo.getName());
            userSkill.setSkillCode(skillInfo.getCode());
            userSkill.setSkillType(skillType);
            userSkill.setDescription(description);
            userSkill.setSkillCodeContent(generatedCode);
            userSkill.setIsEnabled("Y");
            userSkill.setIsPublic("N");

            // 4. 保存到数据库和文件系统
            saveSkill(userSkill);

            // 5. 填充 DTO
            dto.setId(userSkill.getId());
            dto.setSkillName(userSkill.getSkillName());
            dto.setSkillCode(userSkill.getSkillCode());
            dto.setSkillType(userSkill.getSkillType());
            dto.setDescription(userSkill.getDescription());
            dto.setSkillCodeContent(generatedCode);
            dto.setFilePath(userSkill.getFilePath());
            dto.setSuccess(true);

            log.info("成功生成技能：userId={}, skillId={}, skillCode={}", userId, userSkill.getId(), userSkill.getSkillCode());

        } catch (Exception e) {
            log.error("生成技能失败：userId={}, description={}", userId, description, e);
            dto.setSuccess(false);
            dto.setErrorMessage("生成技能失败：" + e.getMessage());
        }

        return dto;
    }

    /**
     * 调用 LLM 生成技能代码
     */
    private String generateCodeWithLLM(String description, String skillType) {
        // 构建提示词
        String systemPrompt = buildSystemPrompt(skillType);
        String userPrompt = buildUserPrompt(description, skillType);

        log.debug("系统提示词：{}", systemPrompt);
        log.debug("用户提示词：{}", userPrompt);

        // TODO: 调用实际的 LLM 服务
        // 这里返回一个模板代码作为示例
        return generateTemplateCode(description, skillType);
    }

    /**
     * 构建系统提示词
     */
    private String buildSystemPrompt(String skillType) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个专业的技能生成助手。你的任务是根据用户的描述生成可执行的技能代码。\n\n");
        prompt.append("技能类型：").append(skillType).append("\n\n");

        if ("LOCAL".equals(skillType)) {
            prompt.append("请生成 Python 脚本，要求：\n");
            prompt.append("1. 代码必须完整可执行\n");
            prompt.append("2. 包含详细的注释说明\n");
            prompt.append("3. 定义 main 函数处理输入参数\n");
            prompt.append("4. 处理异常情况\n");
            prompt.append("5. 返回清晰的结果\n\n");
        } else if ("MCP".equals(skillType)) {
            prompt.append("请生成 MCP 工具配置，要求：\n");
            prompt.append("1. 符合 MCP 协议规范\n");
            prompt.append("2. 包含工具名称、描述、参数定义\n");
            prompt.append("3. 定义工具执行逻辑\n\n");
        } else {
            prompt.append("请生成 JavaScript 代码，要求：\n");
            prompt.append("1. 代码必须完整可执行\n");
            prompt.append("2. 包含详细的注释说明\n");
            prompt.append("3. 定义主函数处理输入参数\n\n");
        }

        prompt.append("只返回代码，不要包含其他说明文字。");

        return prompt.toString();
    }

    /**
     * 构建用户提示词
     */
    private String buildUserPrompt(String description, String skillType) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请生成一个").append(skillType).append("类型的技能。\n\n");
        prompt.append("功能描述：").append(description).append("\n\n");
        prompt.append("输入：用户输入的文本或参数\n");
        prompt.append("输出：处理后的结果\n\n");
        prompt.append("请返回完整的可执行代码。");

        return prompt.toString();
    }

    /**
     * 生成模板代码（示例实现）
     */
    private String generateTemplateCode(String description, String skillType) {
        LocalDateTime now = LocalDateTime.now();

        if ("LOCAL".equals(skillType)) {
            return String.format(
                "#!/usr/bin/env python3\n" +
                "# -*- coding: utf-8 -*-\n" +
                "\"\"\"\n" +
                "技能描述：%s\n" +
                "生成时间：%s\n" +
                "\"\"\"\n\n" +
                "import sys\n" +
                "import json\n\n" +
                "def main(input_data):\n" +
                "    \"\"\"\n" +
                "    技能主函数\n" +
                "    \n" +
                "    Args:\n" +
                "        input_data: 输入数据（字符串或 JSON 格式）\n" +
                "    \n" +
                "    Returns:\n" +
                "        处理结果（字符串）\n" +
                "    \"\"\"\n" +
                "    try:\n" +
                "        # 解析输入\n" +
                "        if isinstance(input_data, str):\n" +
                "            try:\n" +
                "                input_json = json.loads(input_data)\n" +
                "            except json.JSONDecodeError:\n" +
                "                input_json = {\"text\": input_data}\n" +
                "        else:\n" +
                "            input_json = input_data\n" +
                "        \n" +
                "        # TODO: 根据技能描述实现具体功能\n" +
                "        # 这里是一个示例处理逻辑\n" +
                "        text = input_json.get(\"text\", \"\")\n" +
                "        \n" +
                "        # 示例：简单的文本处理\n" +
                "        result = {\n" +
                "            \"status\": \"success\",\n" +
                "            \"message\": \"技能执行成功\",\n" +
                "            \"input\": text,\n" +
                "            \"output\": f\"处理结果：{text}\"\n" +
                "        }\n" +
                "        \n" +
                "        return json.dumps(result, ensure_ascii=False)\n" +
                "        \n" +
                "    except Exception as e:\n" +
                "        error_result = {\n" +
                "            \"status\": \"error\",\n" +
                "            \"message\": str(e)\n" +
                "        }\n" +
                "        return json.dumps(error_result, ensure_ascii=False)\n\n" +
                "if __name__ == \"__main__\":\n" +
                "    # 从命令行参数获取输入\n" +
                "    if len(sys.argv) > 1:\n" +
                "        input_text = \" \".join(sys.argv[1:])\n" +
                "    else:\n" +
                "        input_text = \"\"\n" +
                "    \n" +
                "    result = main(input_text)\n" +
                "    print(result)",
                description, now
            );
        } else if ("MCP".equals(skillType)) {
            return String.format(
                "{\n" +
                "  \"name\": \"custom_tool_%s\",\n" +
                "  \"description\": \"%s\",\n" +
                "  \"generated_at\": \"%s\",\n" +
                "  \"inputSchema\": {\n" +
                "    \"type\": \"object\",\n" +
                "    \"properties\": {\n" +
                "      \"text\": {\n" +
                "        \"type\": \"string\",\n" +
                "        \"description\": \"输入文本\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"required\": [\"text\"]\n" +
                "  },\n" +
                "  \"handler\": \"async def handler(input):\\n    # TODO: 实现具体逻辑\\n    return {\\n        'status': 'success',\\n        'result': f'处理结果：{input}'\\n    }\"\n" +
                "}",
                System.currentTimeMillis(), description, now
            );
        } else {
            return String.format(
                "/**\n" +
                " * 技能描述：%s\n" +
                " * 生成时间：%s\n" +
                " */\n\n" +
                "async function main(inputData) {\n" +
                "    try {\n" +
                "        // TODO: 根据技能描述实现具体功能\n" +
                "        const text = typeof inputData === 'string' ? inputData : JSON.stringify(inputData);\n" +
                "        \n" +
                "        // 示例：简单的文本处理\n" +
                "        const result = {\n" +
                "            status: 'success',\n" +
                "            message: '技能执行成功',\n" +
                "            input: text,\n" +
                "            output: `处理结果：${text}`\n" +
                "        };\n" +
                "        \n" +
                "        return result;\n" +
                "    } catch (error) {\n" +
                "        return {\n" +
                "            status: 'error',\n" +
                "            message: error.message\n" +
                "        };\n" +
                "    }\n" +
                "}\n\n" +
                "// 导出主函数\n" +
                "module.exports = { main };",
                description, now
            );
        }
    }

    /**
     * 解析技能信息
     */
    private SkillInfo parseSkillInfo(String code, String description) {
        SkillInfo info = new SkillInfo();

        // 从描述中提取技能名称（前 20 个字符）
        String name = description.length() > 20 ? description.substring(0, 20) : description;
        info.setName(name.trim());

        // 生成技能编码
        String codeStr = name.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "")
                            .replaceAll("\\u4e00-\\u9fa5", "")
                            .toLowerCase();
        info.setCode(codeStr + "_" + System.currentTimeMillis());

        return info;
    }

    /**
     * 保存技能到数据库和文件系统
     */
    private void saveSkill(UserSkill userSkill) {
        try {
            Long userId = userSkill.getUserId();
            String skillCode = userSkill.getSkillCode();
            String skillContent = userSkill.getSkillCodeContent();

            if (userId == null || StrUtil.isBlank(skillCode)) {
                throw new RuntimeException("用户 ID 或技能编码为空");
            }

            // 创建用户技能目录
            Path userSkillDir = Paths.get(SKILLS_BASE_PATH, String.valueOf(userId));
            Files.createDirectories(userSkillDir);

            // 确定文件扩展名
            String extension = getFileExtension(userSkill.getSkillType());
            String fileName = skillCode + extension;
            Path filePath = userSkillDir.resolve(fileName);

            // 写入文件
            Files.writeString(filePath, skillContent);

            // 更新文件路径
            userSkill.setFilePath(filePath.toString());

            // 保存到数据库
            userSkillMapper.insert(userSkill);

            log.info("保存技能成功：userId={}, skillCode={}, filePath={}", userId, skillCode, filePath);

        } catch (IOException e) {
            log.error("保存技能失败：userId={}, skillCode={}", userSkill.getUserId(), userSkill.getSkillCode(), e);
            throw new RuntimeException("保存技能失败：" + e.getMessage());
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String skillType) {
        if ("LOCAL".equals(skillType)) {
            return ".py";
        } else if ("MCP".equals(skillType)) {
            return ".json";
        } else {
            return ".js";
        }
    }

    /**
     * 技能信息内部类
     */
    private static class SkillInfo {
        private String name;
        private String code;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }
    }
}
