package org.ruoyi.domain.dto.skill;

import java.time.LocalDateTime;

/**
 * Response DTO for skill test via chat.
 */
public class SkillTestChatResponse {
    
    private boolean success;
    private String message;
    private Long skillId;
    private String skillName;
    private String testName;
    private String input;
    private String actualOutput;
    private String expectedOutput;
    private boolean outputMatches;
    private Long executionTimeMs;
    private String errorMessage;
    private LocalDateTime timestamp;
    
    // Constructors
    public SkillTestChatResponse() {}
    
    public SkillTestChatResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public Long getSkillId() { return skillId; }
    public void setSkillId(Long skillId) { this.skillId = skillId; }
    
    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }
    
    public String getTestName() { return testName; }
    public void setTestName(String testName) { this.testName = testName; }
    
    public String getInput() { return input; }
    public void setInput(String input) { this.input = input; }
    
    public String getActualOutput() { return actualOutput; }
    public void setActualOutput(String actualOutput) { this.actualOutput = actualOutput; }
    
    public String getExpectedOutput() { return expectedOutput; }
    public void setExpectedOutput(String expectedOutput) { this.expectedOutput = expectedOutput; }
    
    public boolean isOutputMatches() { return outputMatches; }
    public void setOutputMatches(boolean outputMatches) { this.outputMatches = outputMatches; }
    
    public Long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(Long executionTimeMs) { this.executionTimeMs = executionTimeMs; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
