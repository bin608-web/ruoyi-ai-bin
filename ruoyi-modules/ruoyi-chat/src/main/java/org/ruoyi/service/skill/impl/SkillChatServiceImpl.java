package org.ruoyi.service.skill.impl;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.common.utils.SkillLoginHelper;
import org.ruoyi.domain.dto.skill.*;
import org.ruoyi.domain.vo.skill.UserSkillVo;
import org.ruoyi.manager.skill.SkillGeneratorManager;
import org.ruoyi.service.skill.IUserSkillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for integrating skills with chat functionality.
 * Handles skill invocation, generation, and testing from chat messages.
 * 
 * This service adapts the original SkillChatService to work with RuoYi-AI architecture.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SkillChatServiceImpl {
    
    @Autowired
    private IUserSkillService userSkillService;
    
    @Autowired
    private SkillGeneratorManager skillGeneratorManager;
    
    @Autowired
    private SkillLoginHelper skillLoginHelper;
    
    // Pattern to detect skill invocation: /skill <skill_id> or /use <skill_id>
    private static final Pattern SKILL_INVOCATION_PATTERN = 
        Pattern.compile("(?i)/(skill|use|执行 | 调用)\\s*(\\d+|[\\u4e00-\\u9fa5]+)");
    
    // Pattern to detect skill generation request
    private static final Pattern SKILL_GENERATION_PATTERN = 
        Pattern.compile("(?i)/(生成 | 创建 | 制作)\\s*(?:技能)?(.+)");
    
    // Pattern to detect skill testing request
    private static final Pattern SKILL_TEST_PATTERN = 
        Pattern.compile("(?i)/(测试 | 试验 | 运行)\\s*(\\d+|[\\u4e00-\\u9fa5]+)\\s*(.*)");
    
    /**
     * Process a chat message and determine if it involves skill operations.
     * 
     * @param chatRequest The chat request containing the message
     * @return Response with skill operation result or regular chat response
     */
    @Transactional(readOnly = true)
    public SkillChatResponse processChatMessage(SkillChatRequest chatRequest) {
        Long userId = LoginHelper.getUserId();
        if (userId == null) {
            userId = skillLoginHelper.getCurrentUserId();
        }
        String userName = LoginHelper.getUsername();
        if (StrUtil.isBlank(userName)) {
            userName = skillLoginHelper.getCurrentUserName();
        }
        
        log.info("Processing chat message from user {} (ID: {}): {}", userName, userId, chatRequest.getMessage());
        
        try {
            String message = chatRequest.getMessage();
            if (message == null || message.trim().isEmpty()) {
                return new SkillChatResponse(false, "Message cannot be empty");
            }
            
            message = message.trim();
            
            // Check for skill invocation
            Matcher invocationMatcher = SKILL_INVOCATION_PATTERN.matcher(message);
            if (invocationMatcher.find()) {
                String skillIdentifier = invocationMatcher.group(2);
                return invokeSkillById(skillIdentifier, chatRequest.getInput(), chatRequest.getParameters(), userId, userName);
            }
            
            // Check for skill generation request
            Matcher generationMatcher = SKILL_GENERATION_PATTERN.matcher(message);
            if (generationMatcher.find()) {
                String description = generationMatcher.group(2).trim();
                return generateSkillFromChat(description, userId, userName);
            }
            
            // Check for skill test request
            Matcher testMatcher = SKILL_TEST_PATTERN.matcher(message);
            if (testMatcher.find()) {
                String skillIdentifier = testMatcher.group(2);
                String testInput = testMatcher.group(3).isEmpty() ? chatRequest.getInput() : testMatcher.group(3);
                return testSkillFromChat(skillIdentifier, testInput, chatRequest.getParameters(), userId, userName);
            }
            
            // Check if message contains a skill ID directly (e.g., "使用技能 123")
            if (message.contains("技能") || message.contains("skill")) {
                // Try to extract skill ID from message
                Pattern idPattern = Pattern.compile("(\\d+)");
                Matcher idMatcher = idPattern.matcher(message);
                if (idMatcher.find()) {
                    try {
                        Long skillId = Long.parseLong(idMatcher.group(1));
                        return invokeSkillById(skillId.toString(), chatRequest.getInput(), chatRequest.getParameters(), userId, userName);
                    } catch (NumberFormatException e) {
                        // Not a valid skill ID, continue
                    }
                }
            }
            
            // No skill operation detected, return regular chat response
            return new SkillChatResponse(true, "Message received. Use /skill <id> to invoke a skill, /生成 <description> to create a skill, or /测试 <id> to test a skill.");
            
        } catch (Exception e) {
            log.error("Error processing chat message", e);
            return new SkillChatResponse(false, "Error processing message: " + e.getMessage());
        }
    }
    
    /**
     * Invoke a skill by ID or name.
     */
    private SkillChatResponse invokeSkillById(String skillIdentifier, String input, String parameters, Long userId, String userName) {
        try {
            // Try to find skill by ID first
            UserSkillVo skill = null;
            try {
                Long skillId = Long.parseLong(skillIdentifier);
                skill = userSkillService.selectById(skillId);
            } catch (NumberFormatException e) {
                // Not a number, try to search by name
            }
            
            // If not found by ID, search by name
            if (skill == null) {
                List<UserSkillVo> allSkills = userSkillService.getAvailableSkills(userId);
                for (UserSkillVo s : allSkills) {
                    if (s.getSkillName() != null && 
                        (s.getSkillName().contains(skillIdentifier) || s.getSkillCode().contains(skillIdentifier))) {
                        skill = s;
                        break;
                    }
                }
            }
            
            if (skill == null) {
                return new SkillChatResponse(false, "Skill not found: " + skillIdentifier);
            }
            
            log.info("Invoking skill {} (ID: {}) for user {}", skill.getSkillName(), skill.getId(), userName);
            
            long startTime = System.currentTimeMillis();
            
            // Test the skill using the existing service
            String testInput = StrUtil.blankToDefault(input != null ? input : "", parameters != null ? parameters : "");
            var testResult = userSkillService.testSkill(skill.getId(), testInput);
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            SkillChatResponse response = new SkillChatResponse(
                testResult.isSuccess(), 
                testResult.getMessage(),
                testResult.getOutput());
            response.setSkillId(skill.getId());
            response.setSkillName(skill.getSkillName());
            response.setExecutionTimeMs(executionTime);
            response.setErrorMessage(testResult.getError());
            
            return response;
            
        } catch (Exception e) {
            log.error("Error invoking skill", e);
            return new SkillChatResponse(false, "Error invoking skill: " + e.getMessage());
        }
    }
    
    /**
     * Generate a skill from chat description.
     */
    private SkillChatResponse generateSkillFromChat(String description, Long userId, String userName) {
        try {
            log.info("Generating skill from description for user {}: {}", userName, description);
            
            // Determine skill type from description
            String skillType = determineSkillType(description);
            
            // Use the existing generateSkill method
            var genResult = userSkillService.generateSkill(userId, description, skillType);
            
            if (genResult.isSuccess()) {
                String message = String.format("技能已创建成功！\n名称：%s\n描述：%s\nID: %d", 
                    genResult.getSkillName(), genResult.getDescription(), genResult.getId());
                
                SkillChatResponse response = new SkillChatResponse(true, message);
                response.setSkillId(genResult.getId());
                response.setSkillName(genResult.getSkillName());
                
                log.info("Skill generated successfully: {} (ID: {})", genResult.getSkillName(), genResult.getId());
                return response;
            } else {
                return new SkillChatResponse(false, "技能生成失败：" + genResult.getErrorMessage());
            }
            
        } catch (Exception e) {
            log.error("Error generating skill", e);
            return new SkillChatResponse(false, "Error generating skill: " + e.getMessage());
        }
    }
    
    /**
     * Test a skill from chat request.
     */
    private SkillChatResponse testSkillFromChat(String skillIdentifier, String input, String parameters, Long userId, String userName) {
        try {
            // Try to find skill by ID first
            UserSkillVo skill = null;
            try {
                Long skillId = Long.parseLong(skillIdentifier);
                skill = userSkillService.selectById(skillId);
            } catch (NumberFormatException e) {
                // Not a number, try to search by name
            }
            
            // If not found by ID, search by name
            if (skill == null) {
                List<UserSkillVo> allSkills = userSkillService.getAvailableSkills(userId);
                for (UserSkillVo s : allSkills) {
                    if (s.getSkillName() != null && 
                        (s.getSkillName().contains(skillIdentifier) || s.getSkillCode().contains(skillIdentifier))) {
                        skill = s;
                        break;
                    }
                }
            }
            
            if (skill == null) {
                return new SkillChatResponse(false, "Skill not found: " + skillIdentifier);
            }
            
            log.info("Testing skill {} (ID: {}) for user {}", skill.getSkillName(), skill.getId(), userName);
            
            String testInput = StrUtil.blankToDefault(input != null ? input : "", parameters != null ? parameters : "");
            var testResult = userSkillService.testSkill(skill.getId(), testInput);
            
            String message = String.format("技能测试完成！\n成功：%s\n执行时间：%dms", 
                testResult.isSuccess() ? "是" : "否", 
                testResult.getExecutionTime() != null ? testResult.getExecutionTime() : 0);
            
            if (testResult.getOutput() != null) {
                message += "\n输出：" + testResult.getOutput();
            }
            
            if (testResult.getError() != null) {
                message += "\n错误：" + testResult.getError();
            }
            
            SkillChatResponse response = new SkillChatResponse(testResult.isSuccess(), message);
            response.setSkillId(skill.getId());
            response.setSkillName(skill.getSkillName());
            response.setOutput(testResult.getOutput());
            response.setExecutionTimeMs(testResult.getExecutionTime());
            response.setErrorMessage(testResult.getError());
            
            return response;
            
        } catch (Exception e) {
            log.error("Error testing skill", e);
            return new SkillChatResponse(false, "Error testing skill: " + e.getMessage());
        }
    }
    
    /**
     * Determine skill type from description.
     */
    private String determineSkillType(String description) {
        String lowerDesc = description.toLowerCase();
        
        if (lowerDesc.contains("本地") || lowerDesc.contains("脚本") || lowerDesc.contains("python")) {
            return "LOCAL";
        }
        
        if (lowerDesc.contains("mcp") || lowerDesc.contains("工具")) {
            return "MCP";
        }
        
        return "LOCAL"; // Default to LOCAL
    }
    
    /**
     * List user's available skills for chat.
     */
    @Transactional(readOnly = true)
    public List<SkillDTO> getUserAvailableSkills(Long userId) {
        List<UserSkillVo> userSkills = userSkillService.getAvailableSkills(userId);
        
        List<SkillDTO> skills = new ArrayList<>();
        for (UserSkillVo vo : userSkills) {
            SkillDTO dto = new SkillDTO();
            dto.setId(vo.getId());
            dto.setName(vo.getSkillName());
            dto.setDescription(vo.getDescription());
            dto.setCode(vo.getSkillCode());
            dto.setLanguage("python"); // Default language
            dto.setCategory(vo.getSkillType());
            dto.setOwnerId(vo.getUserId());
            dto.setOwnerName(vo.getCreateBy() != null ? vo.getCreateBy().toString() : "unknown");
            dto.setIsPublic("Y".equals(vo.getIsPublic()));
            dto.setCreatedAt(vo.getCreateTime() != null ? vo.getCreateTime().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null);
            skills.add(dto);
        }
        
        return skills;
    }
    
    /**
     * Get skill usage suggestions based on message context.
     */
    @Transactional(readOnly = true)
    public List<String> getSkillSuggestions(String message) {
        List<String> suggestions = new ArrayList<>();
        
        if (message == null || message.trim().isEmpty()) {
            return suggestions;
        }
        
        String lowerMessage = message.toLowerCase();
        
        // Suggest relevant commands based on keywords
        if (lowerMessage.contains("天气") || lowerMessage.contains("weather")) {
            suggestions.add("/生成 一个查询天气的技能");
            suggestions.add("/skill 天气");
        }
        
        if (lowerMessage.contains("计算") || lowerMessage.contains("计算") || 
            lowerMessage.contains("+") || lowerMessage.contains("-")) {
            suggestions.add("/生成 一个计算器技能");
            suggestions.add("/skill 计算器");
        }
        
        if (lowerMessage.contains("时间") || lowerMessage.contains("time") || 
            lowerMessage.contains("日期") || lowerMessage.contains("date")) {
            suggestions.add("/生成 一个查询时间的技能");
            suggestions.add("/skill 时间");
        }
        
        if (lowerMessage.contains("测试") || lowerMessage.contains("test")) {
            suggestions.add("/测试 <skill_id>");
        }
        
        return suggestions;
    }
}
