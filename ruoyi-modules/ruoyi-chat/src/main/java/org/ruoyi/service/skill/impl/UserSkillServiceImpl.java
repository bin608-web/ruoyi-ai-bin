package org.ruoyi.service.skill.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.domain.dto.skill.UserSkillDto;
import org.ruoyi.domain.dto.skill.UserSkillTestResult;
import org.ruoyi.domain.entity.skill.UserSkill;
import org.ruoyi.domain.bo.skill.UserSkillBo;
import org.ruoyi.domain.vo.skill.UserSkillVo;
import org.ruoyi.mapper.skill.UserSkillMapper;
import org.ruoyi.service.skill.IUserSkillService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户技能 Service 业务层处理
 *
 * @author ruoyi
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class UserSkillServiceImpl implements IUserSkillService {

    private final UserSkillMapper userSkillMapper;
    private final IChatModelService chatModelService;

    private static final String SKILLS_BASE_PATH = "E:\\working\\ruoyi-ai\\skills";

    // 默认用于生成技能的模型名称（可配置）
    private static final String SKILL_GENERATOR_MODEL = "DeepSeek-V4-Pro";

    @Override
    public TableDataInfo<UserSkillVo> selectPageList(UserSkillBo bo, PageQuery pageQuery) {
        Long userId = LoginHelper.getUserId();
        if (userId == null) {
            return TableDataInfo.build();
        }

        LambdaQueryWrapper<UserSkill> wrapper = buildQueryWrapper(bo, userId);
        Page<UserSkill> page = userSkillMapper.selectPage(
            pageQuery.build(), wrapper);
        List<UserSkill> records = page.getRecords();
        List<UserSkillVo> userSkillVos = BeanUtil.copyToList(records, UserSkillVo.class);
        Page<UserSkillVo> pages = new Page<>(page.getCurrent(), page.getSize());
        pages.setRecords(userSkillVos);
        return TableDataInfo.build(pages);
    }

    @Override
    public List<UserSkillVo> queryList(UserSkillBo bo) {
        Long userId = LoginHelper.getUserId();
        if (userId == null) {
            return new ArrayList<>();
        }

        LambdaQueryWrapper<UserSkill> wrapper = buildQueryWrapper(bo, userId);
        List<UserSkill> list = userSkillMapper.selectList(wrapper);
        return list.stream().map(UserSkillVo::convert).collect(Collectors.toList());
    }

    @Override
    public UserSkillVo selectById(Long id) {
        UserSkill userSkill = userSkillMapper.selectById(id);
        return UserSkillVo.convert(userSkill);
    }

    @Override
    public UserSkillVo selectBySkillCode(String skillCode) {
        Long userId = LoginHelper.getUserId();
        if (userId == null) {
            return null;
        }

        UserSkill userSkill = userSkillMapper.selectBySkillCode(skillCode, userId);
        return UserSkillVo.convert(userSkill);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insert(UserSkillBo bo) {
        Long userId = LoginHelper.getUserId();
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }

        UserSkill userSkill = new UserSkill();
        BeanUtils.copyProperties(bo, userSkill);
        userSkill.setUserId(userId);

        if (StringUtils.isBlank(userSkill.getSkillCode())) {
            userSkill.setSkillCode(generateSkillCode(bo.getSkillName()));
        }

        if (StringUtils.isBlank(userSkill.getIsEnabled())) {
            userSkill.setIsEnabled("Y");
        }

        if (StringUtils.isBlank(userSkill.getIsPublic())) {
            userSkill.setIsPublic("N");
        }

        // 保存技能代码到文件
        saveSkillToFile(userSkill);

        userSkillMapper.insert(userSkill);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(UserSkillBo bo) {
        UserSkill userSkill = new UserSkill();
        BeanUtils.copyProperties(bo, userSkill);

        // 更新技能代码到文件
        saveSkillToFile(userSkill);

        userSkillMapper.updateById(userSkill);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByIds(List<Long> ids) {
        for (Long id : ids) {
            UserSkill userSkill = userSkillMapper.selectById(id);
            if (userSkill != null) {
                // 删除技能文件
                deleteSkillFile(userSkill.getFilePath());
                userSkillMapper.deleteById(id);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, String isEnabled) {
        UserSkill userSkill = new UserSkill();
        userSkill.setId(id);
        userSkill.setIsEnabled(isEnabled);
        userSkillMapper.updateById(userSkill);
    }

    @Override
    public UserSkillTestResult testSkill(Long id, String testInput) {
        UserSkill userSkill = userSkillMapper.selectById(id);
        if (userSkill == null) {
            return createTestResult(false, "技能不存在", null, "技能不存在", null);
        }

        Long userId = LoginHelper.getUserId();
        if (!userId.equals(userSkill.getUserId())) {
            return createTestResult(false, "无权测试他人技能", null, "无权测试他人技能", null);
        }

        UserSkillTestResult result = new UserSkillTestResult();
        try {
            String output;
            long startTime = System.currentTimeMillis();

            if ("LOCAL".equals(userSkill.getSkillType())) {
                output = executeLocalScript(userSkill, testInput);
            } else if ("MCP".equals(userSkill.getSkillType())) {
                output = executeMcpTool(userSkill, testInput);
            } else {
                output = executeCustomSkill(userSkill, testInput);
            }

            long executionTime = System.currentTimeMillis() - startTime;
            result.setSuccess(true);
            result.setMessage("测试成功");
            result.setOutput(output);
            result.setRawOutput(output);
            result.setExecutionTime(executionTime);

            userSkill.setTestResult(output);
            userSkill.setTestTime(LocalDateTime.now());
            userSkillMapper.updateById(userSkill);

        } catch (Exception e) {
            log.error("测试技能失败：id={}", id, e);
            result.setSuccess(false);
            result.setMessage("测试失败：" + e.getMessage());
            result.setError(e.getMessage());
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserSkillDto generateSkill(Long userId, String prompt, String skillType) {
        UserSkillDto dto = new UserSkillDto();

        try {
            log.info("开始生成技能：userId={}, prompt={}, skillType={}", userId, prompt, skillType);

            // 1. 获取 LLM 模型配置
            ChatModelVo chatModelVo = chatModelService.selectModelByName(SKILL_GENERATOR_MODEL);
            if (chatModelVo == null) {
                // 尝试获取第一个可用模型
                log.warn("未找到技能生成模型 {}，尝试使用默认模型", SKILL_GENERATOR_MODEL);
                chatModelVo = getDefaultModel();
            }
            if (chatModelVo == null) {
                dto.setSuccess(false);
                dto.setErrorMessage("未找到可用的 LLM 模型配置，请先配置模型");
                return dto;
            }

            // 2. 使用 LLM 生成技能元信息（名称、编码、描述）
            SkillMeta meta = generateSkillMetaWithLLM(chatModelVo, prompt, skillType);
            String skillName = meta.getName();
            String skillCode = meta.getCode();
            String description = meta.getDescription();

            // 3. 调用 LLM 生成 SKILL.md 内容和技能代码
            String skillMdContent = generateSkillMdWithLLM(chatModelVo, prompt, skillName, skillType);
            String skillScriptContent = generateSkillScriptWithLLM(chatModelVo, prompt, skillName, skillType);

            if (StrUtil.isBlank(skillMdContent) && StrUtil.isBlank(skillScriptContent)) {
                dto.setSuccess(false);
                dto.setErrorMessage("LLM 生成技能代码失败，返回为空");
                return dto;
            }

            // 4. 提取纯代码（去除 ---PARAMS---/---CODE--- 标记）
            String cleanScript = stripParamsBlock(skillScriptContent);

            // 5. 拼接完整的技能内容（SKILL.md + 纯代码）
            StringBuilder fullContent = new StringBuilder();
            if (StrUtil.isNotBlank(skillMdContent)) {
                fullContent.append(skillMdContent);
            }
            if (StrUtil.isNotBlank(cleanScript)) {
                if (fullContent.length() > 0) {
                    fullContent.append("\n\n---\n\n");
                }
                fullContent.append(cleanScript);
            }

            // 6. 从生成的代码中解析输入参数定义（优先从 ---PARAMS--- 块）
            String inputParams = parseInputParams(skillScriptContent, skillMdContent);

            // 7. 创建技能实体
            UserSkill userSkill = new UserSkill();
            userSkill.setUserId(userId);
            userSkill.setSkillName(skillName);
            userSkill.setSkillCode(skillCode);
            userSkill.setSkillType(skillType);
            userSkill.setDescription(description);
            userSkill.setSkillCodeContent(fullContent.toString());
            userSkill.setSkillConfig(inputParams);  // 存储参数定义
            userSkill.setIsEnabled("Y");
            userSkill.setIsPublic("N");

            // 6. 保存到数据库和文件系统（创建标准 skill 目录结构）
            saveSkillToFile(userSkill);

            userSkillMapper.insert(userSkill);

            // 7. 填充 DTO
            dto.setId(userSkill.getId());
            dto.setSkillName(skillName);
            dto.setSkillCode(skillCode);
            dto.setSkillType(skillType);
            dto.setDescription(description);
            dto.setSkillCodeContent(fullContent.toString());
            dto.setFilePath(userSkill.getFilePath());
            dto.setSuccess(true);

            log.info("成功生成技能：userId={}, skillId={}, skillCode={}", userId, userSkill.getId(), skillCode);

        } catch (Exception e) {
            log.error("生成技能失败：userId={}, prompt={}", userId, prompt, e);
            dto.setSuccess(false);
            dto.setErrorMessage("生成技能失败：" + e.getMessage());
        }

        return dto;
    }

    /**
     * 使用 LLM 生成 SKILL.md 内容
     * 严格遵循 DB-GPT skill-creator 标准格式
     */
    private String generateSkillMdWithLLM(ChatModelVo chatModelVo, String prompt, String skillName, String skillType) {
        try {
            ChatModel chatModel = buildChatModel(chatModelVo);

            String systemPrompt = buildSkillMdSystemPrompt(skillType);
            String userPrompt = buildSkillMdUserPrompt(prompt, skillName, skillType);

            dev.langchain4j.data.message.ChatMessage systemMsg =
                dev.langchain4j.data.message.SystemMessage.from(systemPrompt);
            dev.langchain4j.data.message.ChatMessage userMsg =
                dev.langchain4j.data.message.UserMessage.from(userPrompt);

            dev.langchain4j.model.chat.response.ChatResponse response =
                chatModel.chat(systemMsg, userMsg);

            String content = response.aiMessage().text();
            log.info("LLM 生成 SKILL.md 成功，长度：{}", content != null ? content.length() : 0);
            return content;

        } catch (Exception e) {
            log.error("LLM 生成 SKILL.md 失败：{}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 使用 LLM 生成技能脚本代码
     */
    private String generateSkillScriptWithLLM(ChatModelVo chatModelVo, String prompt, String skillName, String skillType) {
        try {
            ChatModel chatModel = buildChatModel(chatModelVo);

            String systemPrompt = buildScriptSystemPrompt(skillType);
            String userPrompt = buildScriptUserPrompt(prompt, skillName, skillType);

            dev.langchain4j.data.message.ChatMessage systemMsg =
                dev.langchain4j.data.message.SystemMessage.from(systemPrompt);
            dev.langchain4j.data.message.ChatMessage userMsg =
                dev.langchain4j.data.message.UserMessage.from(userPrompt);

            dev.langchain4j.model.chat.response.ChatResponse response =
                chatModel.chat(systemMsg, userMsg);

            String content = response.aiMessage().text();
            log.info("LLM 生成技能脚本成功，长度：{}", content != null ? content.length() : 0);
            return content;

        } catch (Exception e) {
            log.error("LLM 生成技能脚本失败：{}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 构建 SKILL.md 生成的系统提示词
     * 严格模仿 DB-GPT skill-creator/SKILL.md 中的规范
     */
    private String buildSkillMdSystemPrompt(String skillType) {
        return """
            你是一个专业的技能（Skill）创建助手。你的任务是根据用户的描述，生成符合标准的 SKILL.md 文件。

            ## SKILL.md 格式要求

            你必须输出一个完整的 SKILL.md 文件，格式如下：

            ---
            name: <技能名称，使用小写英文和连字符>
            description: <详细描述技能功能和使用场景，这是触发技能的关键信息>
            ---

            # <技能标题>

            ## Overview

            [1-2句话说明这个技能能做什么]

            ## 使用场景

            [列出典型的使用场景]

            ## 工作流程

            [清晰的步骤说明]

            ## 输入/输出

            - 输入：[描述输入参数]
            - 输出：[描述输出结果]

            ## 脚本说明

            [如果技能包含脚本，说明脚本的用途和使用方法]

            ## 注意事项

            [重要的注意事项和限制]

            ## 编写 SKILL.md 的原则：
            1. **简洁是关键**：只包含 AI 不知道的信息，不解释显而易见的概念
            2. **使用命令式/不定式语气**：用"执行"、"创建"而非"你应该执行"
            3. **description 字段最重要**：它是触发技能的主要机制，必须包含"何时使用"的信息
            4. **name 字段**：必须使用小写英文和连字符，如 'data-analyzer'
            5. **不要包含**：README.md、INSTALLATION_GUIDE.md 等辅助文件的内容

            只返回 SKILL.md 的完整内容，不要包含任何其他说明。
            """;
    }

    /**
     * 构建 SKILL.md 生成的用户提示词
     */
    private String buildSkillMdUserPrompt(String prompt, String skillName, String skillType) {
        String fileExt = ".py";
        if ("LOCAL".equals(skillType)) {
            fileExt = ".py";
        } else if ("MCP".equals(skillType)) {
            fileExt = ".json";
        } else {
            fileExt = ".js";
        }

        return String.format("""
            请为以下技能生成一个完整的 SKILL.md 文件：

            技能名称：%s
            技能类型：%s（脚本文件扩展名：%s）
            功能描述：%s

            该技能包含一个可执行的脚本文件 `scripts/main%s`。

            请生成一个实用的、生产就绪的 SKILL.md，清晰描述技能的功能、使用场景和工作流程。
            严格遵循 SKILL.md 标准格式，YAML frontmatter 中的 name 字段使用英文。
            """,
            skillName, skillType, fileExt, prompt, fileExt);
    }

    /**
     * 构建脚本生成的系统提示词
     */
    private String buildScriptSystemPrompt(String skillType) {
        String language;
        String requirements;
        String cliComment;

        if ("LOCAL".equals(skillType)) {
            language = "Python 3";
            requirements = """
                1. 代码必须完整可执行，使用 Python 3 语法
                2. 包含详细的 docstring 和注释
                3. 使用 argparse 解析命令行参数（--参数名 值），不要用 sys.argv 直接取
                4. 处理所有可能的异常情况
                5. 返回 JSON 格式的结果（包含 status, message, output/output_file 字段）
                6. 支持命令行直接运行（if __name__ == "__main__"）
                7. 包含错误处理和参数验证
                8. 使用标准库为主，避免过多第三方依赖
                """;
            cliComment = "#";
        } else if ("MCP".equals(skillType)) {
            language = "JSON + Python";
            requirements = """
                1. 符合 MCP (Model Context Protocol) 规范
                2. 包含 name, description, inputSchema 字段
                3. inputSchema 使用 JSON Schema 格式定义
                4. 包含 handler 函数的实现代码
                5. 返回 JSON 格式的结果
                """;
            cliComment = "//";
        } else {
            language = "JavaScript (Node.js)";
            requirements = """
                1. 代码必须完整可执行，使用现代 JavaScript 语法
                2. 包含详细的 JSDoc 注释
                3. 使用命令行参数（process.argv 解析 --key value 格式）
                4. 处理所有可能的异常情况
                5. 返回 JSON 格式的结果
                6. 支持 module.exports 导出
                """;
            cliComment = "//";
        }

        return String.format("""
            你是一个专业的代码生成助手。你的任务是根据技能描述生成可执行的技能脚本代码。

            ## 语言：%s

            ## 代码要求：
            %s

            ## 通用要求：
            - 代码必须生产就绪，不只是示例
            - 输出格式统一为 JSON：{"status": "success"|"error", "message": "...", "output": ...}
            - 添加适当的日志输出
            - 考虑边界情况

            ## 输出格式要求：
            你必须按照以下三段式格式输出：

            ---PARAMS---
            [
              {"name": "参数1", "type": "string", "label": "显示名", "required": true, "description": "参数说明", "defaultValue": null}
            ]
            ---CODE---
            %s 实际的代码内容
            # 注意：单独使用 argparse，不要定义 main 函数（除非代码逻辑确实需要）

            参数定义要求：
            - type 取值为 string、number、boolean、file、json
            - required 为 true 或 false
            - defaultValue 可选，不需要时设为 null
            - 参数名使用英文小写+下划线
            - CLI 参数格式统一为 --参数名 值（如 --url "https://..." --method "GET"）

            只返回上述格式的内容，不要包含额外的 markdown 标记或说明。
            """, language, requirements, cliComment);
    }

    /**
     * 构建脚本生成的用户提示词
     */
    private String buildScriptUserPrompt(String prompt, String skillName, String skillType) {
        String language = "LOCAL".equals(skillType) ? "Python" : "MCP".equals(skillType) ? "MCP配置" : "JavaScript";

        return String.format("""
            请为以下技能生成完整的可执行代码：

            技能名称：%s
            功能描述：%s
            实现语言：%s

            请生成实现上述功能的完整代码，确保代码可以实际运行。
            """, skillName, prompt, language);
    }

    /**
     * 获取默认模型
     */
    private ChatModelVo getDefaultModel() {
        try {
            // 获取分页列表中的第一个模型
            org.ruoyi.common.chat.domain.bo.chat.ChatModelBo bo =
                new org.ruoyi.common.chat.domain.bo.chat.ChatModelBo();
            PageQuery pageQuery = new PageQuery(1,1);
            TableDataInfo<ChatModelVo> result = chatModelService.queryPageList(bo, pageQuery);
            if (result != null && result.getRows() != null && !result.getRows().isEmpty()) {
                return result.getRows().get(0);
            }
        } catch (Exception e) {
            log.warn("获取默认模型失败：{}", e.getMessage());
        }
        return null;
    }

    /**
     * 构建同步聊天模型
     */
    private ChatModel buildChatModel(ChatModelVo chatModelVo) {
        return OpenAiChatModel.builder()
            .baseUrl(chatModelVo.getApiHost())
            .apiKey(chatModelVo.getApiKey())
            .modelName(chatModelVo.getModelName())
            .timeout(Duration.ofSeconds(120))
            .temperature(0.7)
            .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void shareSkill(Long skillId, Long toUserId, String shareType, String message) {
        UserSkill userSkill = userSkillMapper.selectById(skillId);
        if (userSkill == null) {
            throw new RuntimeException("技能不存在");
        }

        Long userId = LoginHelper.getUserId();
        if (!userId.equals(userSkill.getUserId())) {
            throw new RuntimeException("无权分享他人技能");
        }

        if ("PUBLIC".equals(shareType)) {
            // 公开分享
            userSkill.setIsPublic("Y");
            userSkillMapper.updateById(userSkill);
        } else if ("PRIVATE".equals(shareType) && toUserId != null) {
            // 私聊分享 - 复制技能给目标用户
            UserSkill newSkill = new UserSkill();
            BeanUtils.copyProperties(userSkill, newSkill);
            newSkill.setId(null);
            newSkill.setUserId(toUserId);
            newSkill.setIsPublic("N");
            newSkill.setIsEnabled("Y");
            userSkillMapper.insert(newSkill);
        }
    }

    @Override
    public List<UserSkillVo> getAvailableSkills(Long userId) {
        LambdaQueryWrapper<UserSkill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserSkill::getUserId, userId)
               .eq(UserSkill::getIsEnabled, "Y");

        List<UserSkill> list = userSkillMapper.selectList(wrapper);
        return list.stream().map(UserSkillVo::convert).collect(Collectors.toList());
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 构建查询条件
     */
    private LambdaQueryWrapper<UserSkill> buildQueryWrapper(UserSkillBo bo, Long userId) {
        LambdaQueryWrapper<UserSkill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserSkill::getUserId, userId);

        if (StrUtil.isNotBlank(bo.getSkillType())) {
            wrapper.eq(UserSkill::getSkillType, bo.getSkillType());
        }
        if (StrUtil.isNotBlank(bo.getIsEnabled())) {
            wrapper.eq(UserSkill::getIsEnabled, bo.getIsEnabled());
        }
        if (StrUtil.isNotBlank(bo.getKeyword())) {
            wrapper.and(w -> w.like(UserSkill::getSkillName, bo.getKeyword())
                             .or()
                             .like(UserSkill::getDescription, bo.getKeyword()));
        }
        wrapper.orderByDesc(UserSkill::getCreateTime);

        return wrapper;
    }

    /**
     * 保存技能到文件（创建标准 skill 目录结构）
     * 模仿 DB-GPT skill-creator 格式：
     * skill-code/
     *   ├── SKILL.md
     *   └── scripts/
     *       └── main.<ext>
     */
    private void saveSkillToFile(UserSkill userSkill) {
        try {
            Long userId = userSkill.getUserId();
            String skillCode = userSkill.getSkillCode();
            String skillContent = userSkill.getSkillCodeContent();

            if (userId == null || StrUtil.isBlank(skillCode)) {
                return;
            }

            // 创建标准 skill 目录结构：skills/<userId>/<skillCode>/
            Path skillDir = Paths.get(SKILLS_BASE_PATH, String.valueOf(userId), skillCode);
            Files.createDirectories(skillDir);

            // 创建 scripts 子目录
            Path scriptsDir = skillDir.resolve("scripts");
            Files.createDirectories(scriptsDir);

            // 解析 SKILL.md 和脚本代码
            String skillMdContent = "";
            String scriptContent = "";

            if (skillContent != null) {
                String separator = "\n\n---\n\n";
                int separatorIndex = skillContent.indexOf(separator);
                if (separatorIndex > 0) {
                    skillMdContent = skillContent.substring(0, separatorIndex);
                    scriptContent = skillContent.substring(separatorIndex + separator.length());
                } else {
                    // 如果内容以 "---" 开头（YAML frontmatter），则是 SKILL.md
                    if (skillContent.trim().startsWith("---")) {
                        skillMdContent = skillContent;
                    } else {
                        scriptContent = skillContent;
                    }
                }
            }

            // 确定文件扩展名
            String extension = getFileExtension(userSkill.getSkillType());

            // 写入 SKILL.md
            Path skillMdPath = skillDir.resolve("SKILL.md");
            if (StrUtil.isNotBlank(skillMdContent)) {
                Files.writeString(skillMdPath, skillMdContent, StandardCharsets.UTF_8);
            } else {
                // 生成默认 SKILL.md
                String defaultSkillMd = generateDefaultSkillMd(userSkill);
                Files.writeString(skillMdPath, defaultSkillMd, StandardCharsets.UTF_8);
            }

            // 写入脚本文件
            String scriptFileName = "main" + extension;
            Path scriptPath = scriptsDir.resolve(scriptFileName);
            if (StrUtil.isNotBlank(scriptContent)) {
                Files.writeString(scriptPath, scriptContent, StandardCharsets.UTF_8);
            }

            // 更新文件路径（指向 SKILL.md）
            userSkill.setFilePath(skillDir.toString());

            log.info("保存技能文件：userId={}, skillCode={}, dir={}", userId, skillCode, skillDir);

        } catch (IOException e) {
            log.error("保存技能文件失败：userId={}, skillCode={}",
                userSkill.getUserId(), userSkill.getSkillCode(), e);
            throw new RuntimeException("保存技能文件失败：" + e.getMessage());
        }
    }

    /**
     * 生成默认 SKILL.md（当 LLM 未生成时使用）
     */
    private String generateDefaultSkillMd(UserSkill userSkill) {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return String.format("""
            ---
            name: %s
            description: %s
            ---

            # %s

            ## Overview

            %s

            ## 使用场景

            ## 工作流程

            1. 调用 scripts/main%s 脚本
            2. 传入输入参数
            3. 获取处理结果

            ## 脚本说明

            脚本位于 `scripts/main%s`，执行命令：
            ```
            python scripts/main%s
            ```

            ## 生成信息

            - 生成时间：%s
            - 技能类型：%s
            """,
            userSkill.getSkillCode(),
            userSkill.getDescription(),
            userSkill.getSkillName(),
            userSkill.getDescription(),
            getFileExtension(userSkill.getSkillType()),
            getFileExtension(userSkill.getSkillType()),
            getFileExtension(userSkill.getSkillType()),
            now,
            userSkill.getSkillType()
        );
    }

    /**
     * 删除技能文件
     */
    private void deleteSkillFile(String filePath) {
        if (StrUtil.isBlank(filePath)) {
            return;
        }

        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                // 递归删除整个技能目录
                try (var walk = Files.walk(path)) {
                    walk.sorted(java.util.Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                Files.delete(p);
                            } catch (IOException e) {
                                log.warn("删除文件失败：{}", p, e);
                            }
                        });
                }
                log.info("删除技能目录：filePath={}", filePath);
            }
        } catch (IOException e) {
            log.error("删除技能文件失败：filePath={}", filePath, e);
        }
    }

    /**
     * 生成技能编码
     */
    private String generateSkillCode(String skillName) {
        String code = StrUtil.toUnderlineCase(skillName).toLowerCase();
        code = code.replaceAll("[^a-z0-9_]", "_");
        return code + "_" + System.currentTimeMillis();
    }

    /**
     * 使用 LLM 生成技能元信息（名称、编码、描述）
     */
    private SkillMeta generateSkillMetaWithLLM(ChatModelVo chatModelVo, String prompt, String skillType) {
        try {
            ChatModel chatModel = buildChatModel(chatModelVo);

            String systemPrompt = """
                你是一个技能元信息生成助手。根据用户的提示词，生成技能的名称、编码和描述。

                要求：
                1. name：技能名称，使用中文，简洁明了（不超过20字）
                2. code：技能编码，使用英文小写+连字符，如 'data-analyzer'、'text-summarizer'（不超过30字符）
                3. description：技能描述，一句话概括功能（不超过100字）

                请严格按照以下 JSON 格式返回，不要包含其他任何内容：
                {"name": "技能名称", "code": "skill-code", "description": "技能描述"}
                """;

            String userPrompt = String.format("""
                根据以下提示词，生成技能元信息：

                提示词：%s
                技能类型：%s
                """, prompt, skillType);

            dev.langchain4j.data.message.ChatMessage systemMsg =
                dev.langchain4j.data.message.SystemMessage.from(systemPrompt);
            dev.langchain4j.data.message.ChatMessage userMsg =
                dev.langchain4j.data.message.UserMessage.from(userPrompt);

            dev.langchain4j.model.chat.response.ChatResponse response =
                chatModel.chat(systemMsg, userMsg);

            String content = response.aiMessage().text();
            log.info("LLM 生成技能元信息：{}", content);

            // 解析 JSON 响应
            return parseSkillMetaResponse(content, prompt);

        } catch (Exception e) {
            log.error("LLM 生成技能元信息失败：{}", e.getMessage(), e);
            // Fallback：使用规则生成
            return fallbackGenerateMeta(prompt);
        }
    }

    /**
     * 解析 LLM 返回的 JSON 元信息
     */
    private SkillMeta parseSkillMetaResponse(String content, String prompt) {
        SkillMeta meta = new SkillMeta();
        try {
            // 尝试提取 JSON 对象
            String jsonStr = content;
            int start = content.indexOf('{');
            int end = content.lastIndexOf('}');
            if (start >= 0 && end > start) {
                jsonStr = content.substring(start, end + 1);
            }

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(jsonStr);

            String name = node.has("name") ? node.get("name").asText().trim() : "";
            String code = node.has("code") ? node.get("code").asText().trim() : "";
            String description = node.has("description") ? node.get("description").asText().trim() : "";

            // 验证和清理
            if (name.isEmpty() || name.length() > 50) {
                name = fallbackGenerateName(prompt);
            }
            if (code.isEmpty() || code.length() > 40) {
                code = fallbackGenerateCode(name);
            }
            // 清理 code：只保留小写字母、数字、连字符
            code = code.toLowerCase().replaceAll("[^a-z0-9-]", "-").replaceAll("-{2,}", "-");
            if (code.startsWith("-")) code = code.substring(1);
            if (code.endsWith("-")) code = code.substring(0, code.length() - 1);
            if (code.isEmpty()) {
                code = fallbackGenerateCode(name);
            }
            // 加上时间戳避免重复
            code = code + "_" + System.currentTimeMillis();

            if (description.isEmpty()) {
                description = prompt.length() > 100 ? prompt.substring(0, 100) : prompt;
            }

            meta.setName(name);
            meta.setCode(code);
            meta.setDescription(description);

        } catch (Exception e) {
            log.warn("解析技能元信息 JSON 失败：{}，使用 fallback", e.getMessage());
            return fallbackGenerateMeta(prompt);
        }

        return meta;
    }

    /**
     * Fallback：根据提示词生成技能名称
     */
    private String fallbackGenerateName(String prompt) {
        String cleaned = prompt.trim();
        if (cleaned.length() > 20) {
            int lastSpace = cleaned.lastIndexOf(' ', 19);
            int lastComma = cleaned.lastIndexOf('，', 19);
            int lastPeriod = cleaned.lastIndexOf('。', 19);
            int cutoff = Math.max(lastSpace, Math.max(lastComma, lastPeriod));
            if (cutoff > 0) {
                cleaned = cleaned.substring(0, cutoff);
            } else {
                cleaned = cleaned.substring(0, 20);
            }
        }
        return cleaned;
    }

    /**
     * Fallback：根据名称生成技能编码
     */
    private String fallbackGenerateCode(String name) {
        String code = StrUtil.toUnderlineCase(name).toLowerCase();
        code = code.replaceAll("[^a-z0-9_]", "_");
        return code + "_" + System.currentTimeMillis();
    }

    /**
     * Fallback：生成完整元信息
     */
    private SkillMeta fallbackGenerateMeta(String prompt) {
        SkillMeta meta = new SkillMeta();
        meta.setName(fallbackGenerateName(prompt));
        meta.setCode(fallbackGenerateCode(meta.getName()));
        meta.setDescription(prompt.trim());
        return meta;
    }

    /**
     * 剥离 LLM 生成的 ---PARAMS--- ... ---CODE--- 标记，
     * 只保留 ---CODE--- 之后的纯代码（写入脚本文件）。
     */
    private String stripParamsBlock(String generatedContent) {
        if (generatedContent == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern
            .compile("---CODE---\\s*\\n?", java.util.regex.Pattern.DOTALL)
            .matcher(generatedContent);
        if (m.find()) {
            return generatedContent.substring(m.end()).trim();
        }
        // 没有 ---CODE--- 标记，返回原文
        return generatedContent;
    }

    /**
     * 从生成的代码中解析输入参数定义。
     * <p>
     * 优先解析 LLM 直接输出的结构化参数规格（---PARAMS--- ... ---CODE--- 格式），
     * 降级时使用正则从代码中推断参数。
     */
    private String parseInputParams(String code, String skillMd) {
        List<Map<String, Object>> params = new ArrayList<>();
        String content = (code != null ? code : "") + "\n" + (skillMd != null ? skillMd : "");

        try {
            // 1. 优先解析 LLM 结构化输出：---PARAMS--- JSON ---CODE---
            java.util.regex.Matcher paramsMatcher = java.util.regex.Pattern
                .compile("---PARAMS---\\s*\\n(.*?)(?:\n|---CODE---)", java.util.regex.Pattern.DOTALL)
                .matcher(content);
            if (paramsMatcher.find()) {
                String paramsJson = paramsMatcher.group(1).trim();
                // 提取 JSON 数组
                int start = paramsJson.indexOf('[');
                int end = paramsJson.lastIndexOf(']');
                if (start >= 0 && end > start) {
                    paramsJson = paramsJson.substring(start, end + 1);
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    com.fasterxml.jackson.databind.JsonNode arr = mapper.readTree(paramsJson);
                    if (arr.isArray()) {
                        for (com.fasterxml.jackson.databind.JsonNode node : arr) {
                            Map<String, Object> param = new LinkedHashMap<>();
                            param.put("name", node.has("name") ? node.get("name").asText() : "");
                            param.put("type", node.has("type") ? node.get("type").asText() : "string");
                            param.put("label", node.has("label") ? node.get("label").asText() : param.get("name"));
                            param.put("required", node.has("required") ? node.get("required").asBoolean() : false);
                            param.put("description", node.has("description") ? node.get("description").asText() : "");
                            if (node.has("defaultValue") && !node.get("defaultValue").isNull()) {
                                param.put("defaultValue", node.get("defaultValue").asText());
                            }
                            params.add(param);
                        }
                        if (!params.isEmpty()) {
                            log.info("从 LLM 结构化输出解析到 {} 个参数", params.size());
                            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(params);
                        }
                    }
                }
            }

            // 2. 降级：正则解析 argparse 参数
            java.util.regex.Matcher argMatcher = java.util.regex.Pattern
                .compile("add_argument\\s*\\(\\s*['\"]--?(\\w+)['\"]"
                    + "(?:\\s*,\\s*['\"]-?(\\w+)['\"])?"
                    + "(?:\\s*,\\s*type\\s*=\\s*(\\w+))?"
                    + "(?:\\s*,\\s*help\\s*=\\s*['\"]([^'\"]+)['\"])?"
                    + "(?:\\s*,\\s*default\\s*=\\s*([^,)]+))?"
                    + "(?:\\s*,\\s*required\\s*=\\s*(True|False))?")
                .matcher(content);
            while (argMatcher.find()) {
                String name = argMatcher.group(1);
                String type = argMatcher.group(3) != null ? argMatcher.group(3) : "str";
                String help = argMatcher.group(4) != null ? argMatcher.group(4) : "";
                String defaultVal = argMatcher.group(5) != null ? argMatcher.group(5).trim() : null;
                boolean required = "True".equalsIgnoreCase(argMatcher.group(6));
                if (params.stream().noneMatch(p -> name.equals(p.get("name")))) {
                    Map<String, Object> param = new LinkedHashMap<>();
                    param.put("name", name);
                    param.put("type", mapType(type));
                    param.put("label", help != null && !help.isEmpty() ? help : name);
                    param.put("required", required);
                    if (defaultVal != null && !"None".equals(defaultVal)) {
                        param.put("defaultValue", defaultVal.replaceAll("^['\"]|['\"]$", ""));
                    }
                    param.put("description", help);
                    params.add(param);
                }
            }

            // 3. 降级：解析 def main(...) 函数参数（旧格式）
            if (params.isEmpty()) {
                java.util.regex.Matcher mainMatcher = java.util.regex.Pattern
                    .compile("def\\s+main\\s*\\(([^)]*)\\)")
                    .matcher(content);
                if (mainMatcher.find()) {
                    String args = mainMatcher.group(1).replaceAll("self,?", "").trim();
                    if (!args.isEmpty()) {
                        for (String arg : args.split(",")) {
                            String cleaned = arg.replaceAll("[:=].*$", "").trim();
                            if (!cleaned.isEmpty()) {
                                Map<String, Object> param = new LinkedHashMap<>();
                                param.put("name", cleaned);
                                param.put("type", "string");
                                param.put("label", cleaned);
                                param.put("required", true);
                                param.put("description", "参数: " + cleaned);
                                params.add(param);
                            }
                        }
                    }
                }
            }

            // 4. 最终降级：通用 text 参数
            if (params.isEmpty()) {
                Map<String, Object> param = new LinkedHashMap<>();
                param.put("name", "text");
                param.put("type", "string");
                param.put("label", "输入文本");
                param.put("required", true);
                param.put("description", "需要处理的文本内容");
                params.add(param);
            }

        } catch (Exception e) {
            log.warn("解析输入参数失败：{}，使用默认参数", e.getMessage());
            Map<String, Object> param = new LinkedHashMap<>();
            param.put("name", "text");
            param.put("type", "string");
            param.put("label", "输入文本");
            param.put("required", true);
            param.put("description", "需要处理的文本内容");
            params.add(param);
        }

        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(params);
        } catch (Exception e) {
            return "[]";
        }
    }

    private String mapType(String pythonType) {
        switch (pythonType.toLowerCase()) {
            case "int": case "integer": return "number";
            case "float": case "double": return "number";
            case "bool": case "boolean": return "boolean";
            case "file": return "file";
            case "str": default: return "string";
        }
    }

    /**
     * 技能元信息
     */
    private static class SkillMeta {
        private String name;
        private String code;
        private String description;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
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
     * 生成技能编码（用于手动创建技能时的 fallback）
     */

    /**
     * 执行本地脚本
     * <p>
     * 参数传递策略：
     * <ul>
     *   <li>如果 input 是 JSON 对象格式，将每个键值对作为 --key value 展开传递</li>
     *   <li>如果 input 是普通字符串，直接作为单个位置参数传递</li>
     *   <li>如果 input 为 null 或空，不传参数</li>
     * </ul>
     */
    private String executeLocalScript(UserSkill userSkill, String input) throws IOException, InterruptedException {
        String filePath = userSkill.getFilePath();
        if (StrUtil.isBlank(filePath)) {
            throw new RuntimeException("技能文件路径不存在");
        }

        // 找到脚本文件
        Path skillDir = Paths.get(filePath);
        Path scriptPath = skillDir.resolve("scripts").resolve("main.py");
        if (!Files.exists(scriptPath)) {
            // 尝试旧格式
            scriptPath = Paths.get(filePath);
            if (!Files.exists(scriptPath)) {
                throw new RuntimeException("技能脚本文件不存在：" + scriptPath);
            }
        }

        List<String> cmd = new ArrayList<>();
        cmd.add("D:\\python\\python.exe");
        cmd.add(scriptPath.toString());

        // 展开参数：JSON Map → --key value 逐个传入
        if (input != null && !input.isBlank()) {
            Map<String, Object> params = parseInputToMap(input);
            if (params != null && !params.isEmpty()) {
                for (Map.Entry<String, Object> entry : params.entrySet()) {
                    cmd.add("--" + entry.getKey());
                    cmd.add(String.valueOf(entry.getValue()));
                }
            } else {
                cmd.add(input);
            }
        }

        ProcessBuilder processBuilder = new ProcessBuilder(cmd);
        processBuilder.redirectErrorStream(true);
        log.info("执行技能脚本: {}", String.join(" ", cmd));
        Process process = processBuilder.start();

        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        process.waitFor();

        return output;
    }

    /**
     * 将输入字符串解析为参数 Map
     * 优先尝试 JSON 对象格式，失败则返回 null
     */
    private Map<String, Object> parseInputToMap(String input) {
        String trimmed = input.trim();
        if (trimmed.startsWith("{")) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(trimmed, Map.class);
                // 如果内层还有 params 包装，展开它
                if (map.containsKey("params") && map.get("params") instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> inner = (Map<String, Object>) map.get("params");
                    return inner;
                }
                return map;
            } catch (Exception e) {
                log.debug("输入不是合法 JSON Map，按纯文本处理: {}", e.getMessage());
            }
        }
        return null;
    }

    /**
     * 执行 MCP 工具
     */
    private String executeMcpTool(UserSkill userSkill, String input) {
        // TODO: 实现 MCP 工具调用
        return "MCP 工具执行结果：" + input;
    }

    /**
     * 执行自定义技能
     */
    private String executeCustomSkill(UserSkill userSkill, String input) {
        // TODO: 实现自定义技能执行逻辑
        return "自定义技能执行结果：" + input;
    }

    /**
     * 创建测试结果
     */
    private UserSkillTestResult createTestResult(boolean success, String message,
                                                  String output, String error, Long executionTime) {
        UserSkillTestResult result = new UserSkillTestResult();
        result.setSuccess(success);
        result.setMessage(message);
        result.setOutput(output);
        result.setError(error);
        result.setExecutionTime(executionTime);
        return result;
    }
}
