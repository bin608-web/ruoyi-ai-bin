package org.ruoyi.controller.chat;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.log.annotation.Log;
import org.ruoyi.common.log.enums.BusinessType;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.domain.dto.skill.*;
import org.ruoyi.service.skill.impl.SkillChatServiceImpl;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API Controller for Chat-Skill integration.
 * Provides endpoints for skill invocation, generation, and testing via chat.
 * 
 * This controller adapts the original ChatController to work with RuoYi-AI architecture.
 */
@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/chat/skill")
public class SkillChatController extends BaseController {

    private final SkillChatServiceImpl skillChatService;
    
    /**
     * Process a chat message that may involve skill operations.
     * POST /chat/skill/message
     */
    @Log(title = "技能聊天", businessType = BusinessType.OTHER)
    @PostMapping("/message")
    public R<SkillChatResponse> processChatMessage(@RequestBody SkillChatRequest chatRequest) {
        Long currentUserId = LoginHelper.getUserId();
        String currentUserName = LoginHelper.getUsername();
        
        log.info("Processing chat message from user {} (ID: {})", currentUserName, currentUserId);
        
        SkillChatResponse response = skillChatService.processChatMessage(chatRequest);
        return R.ok(response);
    }
    
    /**
     * Invoke a specific skill.
     * POST /chat/skill/invoke
     */
    @Log(title = "技能调用", businessType = BusinessType.OTHER)
    @PostMapping("/invoke")
    public R<SkillChatResponse> invokeSkill(@RequestBody SkillChatRequest chatRequest) {
        Long currentUserId = LoginHelper.getUserId();
        String currentUserName = LoginHelper.getUsername();
        
        if (chatRequest.getSkillId() == null) {
            return R.fail("skillId is required");
        }
        
        SkillChatRequest request = new SkillChatRequest();
        request.setSkillId(chatRequest.getSkillId());
        request.setInput(chatRequest.getInput());
        request.setParameters(chatRequest.getParameters());
        
        // Create a message for logging
        request.setMessage("/skill " + chatRequest.getSkillId());
        
        SkillChatResponse response = skillChatService.processChatMessage(request);
        return R.ok(response);
    }
    
    /**
     * Generate a new skill from description.
     * POST /chat/skill/generate
     */
    @SaCheckPermission("skill:my:add")
    @Log(title = "技能生成", businessType = BusinessType.INSERT)
    @PostMapping("/generate")
    public R<SkillGenerationResponse> generateSkill(@RequestBody SkillGenerationRequest generationRequest) {
        Long currentUserId = LoginHelper.getUserId();
        String currentUserName = LoginHelper.getUsername();
        
        log.info("Generating skill from description for user {} (ID: {}): {}", 
            currentUserName, currentUserId, generationRequest.getDescription());
        
        // Create a chat request for generation
        SkillChatRequest chatRequest = new SkillChatRequest();
        chatRequest.setMessage("/生成 " + generationRequest.getDescription());
        chatRequest.setInput(generationRequest.getInput() != null ? generationRequest.getInput() : "");
        
        SkillChatResponse chatResponse = skillChatService.processChatMessage(chatRequest);
        
        if (chatResponse.isSuccess()) {
            SkillGenerationResponse response = new SkillGenerationResponse(true, chatResponse.getMessage());
            if (chatResponse.getSkillId() != null) {
                SkillDTO skill = new SkillDTO();
                skill.setId(chatResponse.getSkillId());
                skill.setName(chatResponse.getSkillName());
                response.setSkill(skill);
            }
            return R.ok(response);
        } else {
            return R.fail(chatResponse.getMessage());
        }
    }
    
    /**
     * Test a specific skill.
     * POST /chat/skill/test
     */
    @Log(title = "技能测试", businessType = BusinessType.OTHER)
    @PostMapping("/test")
    public R<SkillTestChatResponse> testSkill(@RequestBody SkillTestChatRequest testRequest) {
        Long currentUserId = LoginHelper.getUserId();
        String currentUserName = LoginHelper.getUsername();
        
        if (testRequest.getSkillId() == null) {
            return R.fail("skillId is required");
        }
        
        try {
            // Create a chat request for testing
            SkillChatRequest chatRequest = new SkillChatRequest();
            chatRequest.setMessage("/测试 " + testRequest.getSkillId());
            chatRequest.setInput(testRequest.getInput());
            chatRequest.setParameters(testRequest.getParameters());
            
            SkillChatResponse chatResponse = skillChatService.processChatMessage(chatRequest);
            
            // Convert to test response
            SkillTestChatResponse response = new SkillTestChatResponse(
                chatResponse.isSuccess(), 
                chatResponse.getMessage());
            response.setSkillId(testRequest.getSkillId());
            response.setSkillName(chatResponse.getSkillName());
            response.setActualOutput(chatResponse.getOutput());
            response.setExecutionTimeMs(chatResponse.getExecutionTimeMs());
            response.setErrorMessage(chatResponse.getErrorMessage());
            
            return R.ok(response);
            
        } catch (Exception e) {
            log.error("Error testing skill", e);
            return R.fail("Error testing skill: " + e.getMessage());
        }
    }
    
    /**
     * List user's available skills.
     * GET /chat/skill/skills
     */
    @GetMapping("/skills")
    public R<List<SkillDTO>> getUserSkills() {
        Long currentUserId = LoginHelper.getUserId();
        
        List<SkillDTO> skills = skillChatService.getUserAvailableSkills(currentUserId);
        return R.ok(skills);
    }
    
    /**
     * Get skill suggestions based on message context.
     * GET /chat/skill/suggestions
     */
    @GetMapping("/suggestions")
    public R<Map<String, Object>> getSkillSuggestions(@RequestParam String message) {
        Long currentUserId = LoginHelper.getUserId();
        
        List<String> suggestions = skillChatService.getSkillSuggestions(message);
        
        Map<String, Object> response = Map.of(
            "userId", currentUserId,
            "message", message,
            "suggestions", suggestions
        );
        
        return R.ok(response);
    }
    
    /**
     * Quick skill execution shortcut.
     * POST /chat/skill/quick
     */
    @Log(title = "技能快速执行", businessType = BusinessType.OTHER)
    @PostMapping("/quick")
    public R<SkillChatResponse> quickExecute(@RequestBody Map<String, String> requestBody) {
        String command = requestBody.get("command");
        String input = requestBody.get("input");
        String parameters = requestBody.get("parameters");
        
        if (command == null || command.trim().isEmpty()) {
            return R.fail("command is required");
        }
        
        SkillChatRequest chatRequest = new SkillChatRequest(command);
        chatRequest.setInput(input);
        chatRequest.setParameters(parameters);
        
        SkillChatResponse response = skillChatService.processChatMessage(chatRequest);
        return R.ok(response);
    }
    
    /**
     * Health check endpoint.
     * GET /chat/skill/health
     */
    @GetMapping("/health")
    public R<Map<String, String>> healthCheck() {
        Map<String, String> response = Map.of(
            "status", "UP",
            "service", "Chat-Skill Integration"
        );
        return R.ok(response);
    }
    
    /**
     * Get chat command help.
     * GET /chat/skill/help
     */
    @GetMapping("/help")
    public R<Map<String, Object>> getHelp() {
        Map<String, Object> help = Map.of(
            "commands", Map.of(
                "/skill <id>", "Invoke a skill by ID or name",
                "/use <id>", "Alias for /skill",
                "/生成 <description>", "Generate a new skill from description",
                "/create <description>", "Alias for /生成",
                "/测试 <id>", "Test a skill",
                "/test <id>", "Alias for /测试"
            ),
            "examples", List.of(
                "/skill 123 - Execute skill with ID 123",
                "/生成 一个查询天气的技能 - Create a weather query skill",
                "/测试 456 北京 - Test skill 456 with input '北京'"
            )
        );
        return R.ok(help);
    }
}
