package org.ruoyi.domain.dto.skill;

/**
 * Request DTO for skill invocation from chat.
 */
public class SkillChatRequest {
    
    private String message;
    private Long skillId;
    private String input;
    private String parameters;
    
    // Constructors
    public SkillChatRequest() {}
    
    public SkillChatRequest(String message) {
        this.message = message;
    }
    
    public SkillChatRequest(Long skillId, String input, String parameters) {
        this.skillId = skillId;
        this.input = input;
        this.parameters = parameters;
    }
    
    // Getters and Setters
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public Long getSkillId() { return skillId; }
    public void setSkillId(Long skillId) { this.skillId = skillId; }
    
    public String getInput() { return input; }
    public void setInput(String input) { this.input = input; }
    
    public String getParameters() { return parameters; }
    public void setParameters(String parameters) { this.parameters = parameters; }
}
