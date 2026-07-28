package org.ruoyi.domain.dto.skill;

/**
 * Request DTO for testing a skill via chat.
 */
public class SkillTestChatRequest {
    
    private Long skillId;
    private String testName;
    private String input;
    private String parameters;
    private String expectedOutput;
    
    // Constructors
    public SkillTestChatRequest() {}
    
    public SkillTestChatRequest(Long skillId, String input) {
        this.skillId = skillId;
        this.input = input;
    }
    
    public SkillTestChatRequest(Long skillId, String testName, String input, String parameters) {
        this.skillId = skillId;
        this.testName = testName;
        this.input = input;
        this.parameters = parameters;
    }
    
    // Getters and Setters
    public Long getSkillId() { return skillId; }
    public void setSkillId(Long skillId) { this.skillId = skillId; }
    
    public String getTestName() { return testName; }
    public void setTestName(String testName) { this.testName = testName; }
    
    public String getInput() { return input; }
    public void setInput(String input) { this.input = input; }
    
    public String getParameters() { return parameters; }
    public void setParameters(String parameters) { this.parameters = parameters; }
    
    public String getExpectedOutput() { return expectedOutput; }
    public void setExpectedOutput(String expectedOutput) { this.expectedOutput = expectedOutput; }
}
