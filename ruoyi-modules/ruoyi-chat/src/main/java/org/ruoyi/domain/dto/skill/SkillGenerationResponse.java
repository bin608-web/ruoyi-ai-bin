package org.ruoyi.domain.dto.skill;

import java.time.LocalDateTime;

/**
 * Response DTO for skill generation.
 */
public class SkillGenerationResponse {
    
    private boolean success;
    private String message;
    private SkillDTO skill;
    private String generatedCode;
    private String generationNotes;
    private LocalDateTime timestamp;
    
    // Constructors
    public SkillGenerationResponse() {}
    
    public SkillGenerationResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }
    
    public SkillGenerationResponse(boolean success, String message, SkillDTO skill) {
        this.success = success;
        this.message = message;
        this.skill = skill;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public SkillDTO getSkill() { return skill; }
    public void setSkill(SkillDTO skill) { this.skill = skill; }
    
    public String getGeneratedCode() { return generatedCode; }
    public void setGeneratedCode(String generatedCode) { this.generatedCode = generatedCode; }
    
    public String getGenerationNotes() { return generationNotes; }
    public void setGenerationNotes(String generationNotes) { this.generationNotes = generationNotes; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
