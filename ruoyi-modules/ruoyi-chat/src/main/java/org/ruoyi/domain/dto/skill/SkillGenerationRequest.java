package org.ruoyi.domain.dto.skill;

/**
 * Request DTO for skill generation from user description.
 */
public class SkillGenerationRequest {
    
    private String description;
    private String category;
    private String language;
    private String examples;
    private String constraints;
    private String input;
    
    // Constructors
    public SkillGenerationRequest() {}
    
    public SkillGenerationRequest(String description) {
        this.description = description;
    }
    
    public SkillGenerationRequest(String description, String category, String language) {
        this.description = description;
        this.category = category;
        this.language = language;
    }
    
    // Getters and Setters
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    
    public String getExamples() { return examples; }
    public void setExamples(String examples) { this.examples = examples; }
    
    public String getConstraints() { return constraints; }
    public void setConstraints(String constraints) { this.constraints = constraints; }
    
    public String getInput() { return input; }
    public void setInput(String input) { this.input = input; }
}
