package org.ruoyi.common.utils;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Optional;

/**
 * Helper utility to get current user information from request headers.
 * In production, this would integrate with your authentication system (JWT, OAuth, etc.)
 * 
 * Note: This is a simplified version for skill chat integration.
 * In the RuoYi-AI project, prefer using org.ruoyi.common.satoken.utils.LoginHelper
 */
@Component
public class SkillLoginHelper {
    
    private static final String USER_ID_HEADER = "X-User-ID";
    private static final String USER_NAME_HEADER = "X-User-Name";
    private static final Long DEFAULT_USER_ID = 1L;
    private static final String DEFAULT_USER_NAME = "Test User";
    
    /**
     * Get current user ID from request headers.
     * Falls back to default user ID if not present.
     * 
     * @return Current user ID
     */
    public Long getCurrentUserId() {
        ServletRequestAttributes attributes = 
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String userIdStr = request.getHeader(USER_ID_HEADER);
            if (userIdStr != null && !userIdStr.isEmpty()) {
                try {
                    return Long.parseLong(userIdStr);
                } catch (NumberFormatException e) {
                    // Log warning and fall back to default
                    System.err.println("Invalid user ID format: " + userIdStr);
                }
            }
        }
        
        return DEFAULT_USER_ID;
    }
    
    /**
     * Get current user ID from request headers with explicit default.
     * 
     * @param defaultUserId Default user ID to use if header is not present
     * @return Current user ID
     */
    public Long getCurrentUserId(Long defaultUserId) {
        ServletRequestAttributes attributes = 
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String userIdStr = request.getHeader(USER_ID_HEADER);
            if (userIdStr != null && !userIdStr.isEmpty()) {
                try {
                    return Long.parseLong(userIdStr);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid user ID format: " + userIdStr);
                }
            }
        }
        
        return defaultUserId != null ? defaultUserId : DEFAULT_USER_ID;
    }
    
    /**
     * Get current user name from request headers.
     * Falls back to default user name if not present.
     * 
     * @return Current user name
     */
    public String getCurrentUserName() {
        ServletRequestAttributes attributes = 
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String userName = request.getHeader(USER_NAME_HEADER);
            if (userName != null && !userName.isEmpty()) {
                return userName;
            }
        }
        
        return DEFAULT_USER_NAME;
    }
    
    /**
     * Get current user name from request headers with explicit default.
     * 
     * @param defaultUserName Default user name to use if header is not present
     * @return Current user name
     */
    public String getCurrentUserName(String defaultUserName) {
        ServletRequestAttributes attributes = 
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String userName = request.getHeader(USER_NAME_HEADER);
            if (userName != null && !userName.isEmpty()) {
                return userName;
            }
        }
        
        return defaultUserName != null ? defaultUserName : DEFAULT_USER_NAME;
    }
    
    /**
     * Get current user information as a map.
     * 
     * @return Map containing userId and userName
     */
    public Map<String, Object> getCurrentUserInfo() {
        return Map.of(
            "userId", getCurrentUserId(),
            "userName", getCurrentUserName()
        );
    }
    
    /**
     * Check if current request has valid user authentication.
     * 
     * @return true if user ID is present and valid
     */
    public boolean isAuthenticated() {
        Long userId = getCurrentUserId();
        return userId != null && !userId.equals(DEFAULT_USER_ID);
    }
}
