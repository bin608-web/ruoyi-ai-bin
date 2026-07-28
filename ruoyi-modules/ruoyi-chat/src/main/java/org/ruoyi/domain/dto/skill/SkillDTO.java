package org.ruoyi.domain.dto.skill;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object for Skill
 */
public class SkillDTO {
    
    private Long id;
    private String name;
    private String description;
    private String code;
    private String language;
    private String category;
    private String tags;
    private Long ownerId;
    private String ownerName;
    private Boolean isShared;
    private Boolean isPublic;
    private Long sharedToUserId;
    private String sharedToUserName;
    private LocalDateTime sharedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer testCount;
    private Integer subscriptionCount;
    
    // Constructors
    public SkillDTO() {}
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    
    public Boolean getIsShared() { return isShared; }
    public void setIsShared(Boolean isShared) { this.isShared = isShared; }
    
    public Boolean getIsPublic() { return isPublic; }
    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }
    
    public Long getSharedToUserId() { return sharedToUserId; }
    public void setSharedToUserId(Long sharedToUserId) { this.sharedToUserId = sharedToUserId; }
    
    public String getSharedToUserName() { return sharedToUserName; }
    public void setSharedToUserName(String sharedToUserName) { this.sharedToUserName = sharedToUserName; }
    
    public LocalDateTime getSharedAt() { return sharedAt; }
    public void setSharedAt(LocalDateTime sharedAt) { this.sharedAt = sharedAt; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public Integer getTestCount() { return testCount; }
    public void setTestCount(Integer testCount) { this.testCount = testCount; }
    
    public Integer getSubscriptionCount() { return subscriptionCount; }
    public void setSubscriptionCount(Integer subscriptionCount) { this.subscriptionCount = subscriptionCount; }
}
