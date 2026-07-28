package org.ruoyi.manager.skill;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.domain.entity.skill.UserSkill;
import org.ruoyi.mapper.skill.UserSkillMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * 技能加载管理器
 * 负责在聊天时自动加载用户启用的技能
 *
 * @author ruoyi
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class SkillLoaderManager {

    private final UserSkillMapper userSkillMapper;

    private static final String SKILLS_BASE_PATH = "E:\\working\\ruoyi-ai\\skills";

    /**
     * 缓存用户技能：userId -> List<UserSkill>
     */
    private final Map<Long, List<LoadedSkill>> userSkillCache = new ConcurrentHashMap<>();

    /**
     * 加载用户的所有启用技能
     *
     * @param userId 用户 ID
     * @return 技能列表
     */
    public List<LoadedSkill> loadUserSkills(Long userId) {
        log.info("开始加载用户技能：userId={}", userId);

        // 从数据库查询启用的技能
        List<UserSkill> skillList = userSkillMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserSkill>()
                .eq(UserSkill::getUserId, userId)
                .eq(UserSkill::getIsEnabled, "Y")
        );

        List<LoadedSkill> loadedSkills = new ArrayList<>();
        
        for (UserSkill skill : skillList) {
            LoadedSkill loadedSkill = loadSkill(skill);
            if (loadedSkill != null) {
                loadedSkills.add(loadedSkill);
            }
        }

        // 更新缓存
        userSkillCache.put(userId, loadedSkills);

        log.info("用户技能加载完成：userId={}, count={}", userId, loadedSkills.size());

        return loadedSkills;
    }

    /**
     * 加载单个技能
     */
    private LoadedSkill loadSkill(UserSkill userSkill) {
        LoadedSkill loadedSkill = new LoadedSkill();
        loadedSkill.setId(userSkill.getId());
        loadedSkill.setSkillName(userSkill.getSkillName());
        loadedSkill.setSkillCode(userSkill.getSkillCode());
        loadedSkill.setSkillType(userSkill.getSkillType());
        loadedSkill.setDescription(userSkill.getDescription());
        loadedSkill.setFilePath(userSkill.getFilePath());

        // 读取技能代码内容
        try {
            String codeContent = readSkillFile(userSkill.getFilePath());
            loadedSkill.setCodeContent(codeContent);
            
            // 根据技能类型加载
            if ("LOCAL".equals(userSkill.getSkillType())) {
                loadedSkill.setExecutable(true);
            } else if ("MCP".equals(userSkill.getSkillType())) {
                // 解析 MCP 配置
                loadedSkill.setMcpConfig(parseMcpConfig(codeContent));
            }
            
        } catch (Exception e) {
            log.error("加载技能失败：skillId={}, filePath={}", userSkill.getId(), userSkill.getFilePath(), e);
            loadedSkill.setLoadError(e.getMessage());
        }

        return loadedSkill;
    }

    /**
     * 读取技能文件内容
     */
    private String readSkillFile(String filePath) throws IOException {
        if (StringUtils.isBlank(filePath)) {
            return null;
        }
        
        Path path = Paths.get(filePath);
        if (Files.exists(path)) {
            return Files.readString(path, StandardCharsets.UTF_8);
        }
        return null;
    }

    /**
     * 解析 MCP 配置
     */
    private Map<String, Object> parseMcpConfig(String jsonContent) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(jsonContent);
            
            Map<String, Object> config = new HashMap<>();
            config.put("name", rootNode.path("name").asText());
            config.put("description", rootNode.path("description").asText());
            config.put("inputSchema", rootNode.path("inputSchema").toString());
            
            return config;
        } catch (Exception e) {
            log.error("解析 MCP 配置失败：{}", e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * 从缓存获取用户技能
     */
    public List<LoadedSkill> getCachedSkills(Long userId) {
        return userSkillCache.getOrDefault(userId, Collections.emptyList());
    }

    /**
     * 刷新用户技能缓存
     */
    public void refreshUserSkills(Long userId) {
        userSkillCache.remove(userId);
        loadUserSkills(userId);
    }

    /**
     * 清除用户技能缓存
     */
    public void clearUserSkills(Long userId) {
        userSkillCache.remove(userId);
    }

    /**
     * 清除所有缓存
     */
    public void clearAllCache() {
        userSkillCache.clear();
    }

    /**
     * 扫描文件系统，更新技能状态
     */
    public void scanUserSkills(Long userId) {
        Path userSkillDir = Paths.get(SKILLS_BASE_PATH, String.valueOf(userId));
        
        if (!Files.exists(userSkillDir)) {
            log.info("用户技能目录不存在：userId={}", userId);
            return;
        }

        try (Stream<Path> paths = Files.walk(userSkillDir)) {
            paths.filter(Files::isRegularFile)
                 .filter(path -> {
                     String fileName = path.getFileName().toString();
                     return fileName.endsWith(".py") || 
                            fileName.endsWith(".js") || 
                            fileName.endsWith(".json");
                 })
                 .forEach(path -> {
                     String fileName = path.getFileName().toString();
                     String skillCode = fileName.substring(0, fileName.lastIndexOf('.'));
                     
                     // 检查数据库中是否存在该技能
                     UserSkill skill = userSkillMapper.selectBySkillCode(skillCode, null);
                     
                     if (skill == null) {
                         log.warn("发现未注册的技能文件：userId={}, filePath={}", userId, path);
                         // TODO: 可以选择自动注册或发出警告
                     }
                 });
                 
        } catch (IOException e) {
            log.error("扫描用户技能目录失败：userId={}", userId, e);
        }
    }

    /**
     * 加载后的技能信息
     */
    public static class LoadedSkill {
        private Long id;
        private String skillName;
        private String skillCode;
        private String skillType;
        private String description;
        private String filePath;
        private String codeContent;
        private boolean executable;
        private Map<String, Object> mcpConfig;
        private String loadError;

        // Getters and Setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getSkillName() {
            return skillName;
        }

        public void setSkillName(String skillName) {
            this.skillName = skillName;
        }

        public String getSkillCode() {
            return skillCode;
        }

        public void setSkillCode(String skillCode) {
            this.skillCode = skillCode;
        }

        public String getSkillType() {
            return skillType;
        }

        public void setSkillType(String skillType) {
            this.skillType = skillType;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getFilePath() {
            return filePath;
        }

        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }

        public String getCodeContent() {
            return codeContent;
        }

        public void setCodeContent(String codeContent) {
            this.codeContent = codeContent;
        }

        public boolean isExecutable() {
            return executable;
        }

        public void setExecutable(boolean executable) {
            this.executable = executable;
        }

        public Map<String, Object> getMcpConfig() {
            return mcpConfig;
        }

        public void setMcpConfig(Map<String, Object> mcpConfig) {
            this.mcpConfig = mcpConfig;
        }

        public String getLoadError() {
            return loadError;
        }

        public void setLoadError(String loadError) {
            this.loadError = loadError;
        }
    }
}
