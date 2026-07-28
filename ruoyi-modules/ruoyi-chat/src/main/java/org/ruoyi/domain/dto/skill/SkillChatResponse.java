package org.ruoyi.domain.dto.skill;

import java.time.LocalDateTime;

/**
 * Response DTO for skill invocation from chat.
 */
public class SkillChatResponse {
    
    private boolean success;
    private String message;
    private String output;
    private Long skillId;
    private String skillName;
    private Long executionTimeMs;
    private String errorMessage;
    private LocalDateTime timestamp;
    
    // Constructors
    public SkillChatResponse() {}
    
    public SkillChatResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }
    
    public SkillChatResponse(boolean success, String message, String output) {
        this.success = success;
        this.message = message;
        this.output = output;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }
    
    public Long getSkillId() { return skillId; }
    public void setSkillId(Long skillId) { this.skillId = skillId; }
    
    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }
    
    public Long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(Long executionTimeMs) { this.executionTimeMs = executionTimeMs; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
