package org.ruoyi.skills.service.core;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.skills.FileSystemSkillLoader;
import dev.langchain4j.skills.FileSystemSkill;
import dev.langchain4j.skills.shell.ShellSkills;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.domain.entity.skill.UserSkill;
import org.ruoyi.mapper.skill.UserSkillMapper;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Skills 工具提供者服务
 * 直接从本地 skills 目录加载用户技能，不创建临时文件
 *
 * @author ruoyi team
 */
@Slf4j
@Service
public class SkillsToolProviderService {

    private final UserSkillMapper userSkillMapper;

    private static final String SKILLS_BASE_PATH = "E:\\working\\ruoyi-ai\\skills";

    private final ConcurrentHashMap<String, ToolProvider> toolProviderCache = new ConcurrentHashMap<>();

    public SkillsToolProviderService(UserSkillMapper userSkillMapper) {
        this.userSkillMapper = userSkillMapper;
    }

    private static ToolProvider emptyToolProvider() {
        return McpToolProvider.builder().mcpClients(Collections.emptyList()).build();
    }

    /**
     * 根据用户 ID 和 Skills ID 列表获取 ToolProvider
     */
    public ToolProvider getToolProvider(Long userId, List<Long> skillIds) {
        if (userId == null) {
            return emptyToolProvider();
        }

        List<UserSkill> skills;
        if (skillIds != null && !skillIds.isEmpty()) {
            skills = userSkillMapper.selectListByUserAndIds(userId, skillIds);
        } else {
            skills = userSkillMapper.selectEnabledByUserId(userId);
        }

        if (skills == null || skills.isEmpty()) {
            log.info("用户 {} 没有可用的 Skills", userId);
            return emptyToolProvider();
        }

        String cacheKey = buildCacheKey(userId, skills);
        ToolProvider cached = toolProviderCache.get(cacheKey);
        if (cached != null) {
            log.info("从缓存返回用户 {} 的 ToolProvider", userId);
            return cached;
        }

        log.info("为用户 {} 加载 {} 个 Skills（从本地目录）", userId, skills.size());

        try {
            // 直接从本地 skills 目录加载
            Path userSkillDir = Paths.get(SKILLS_BASE_PATH, String.valueOf(userId));
            if (!Files.exists(userSkillDir)) {
                log.warn("用户 {} 的技能目录不存在: {}", userId, userSkillDir);
                return emptyToolProvider();
            }

            List<FileSystemSkill> skillsList = FileSystemSkillLoader.loadSkills(userSkillDir);
            if (skillsList.isEmpty()) {
                log.warn("用户 {} 没有成功加载任何 Skills", userId);
                return emptyToolProvider();
            }

            ShellSkills shellSkills = ShellSkills.from(skillsList);
            ToolProvider toolProvider = shellSkills.toolProvider();

            toolProviderCache.put(cacheKey, toolProvider);
            return toolProvider;

        } catch (Exception e) {
            log.error("Failed to load skills for user {}: {}", userId, e.getMessage(), e);
            return emptyToolProvider();
        }
    }

    public void clearCache(Long userId) {
        String prefix = userId + ":";
        toolProviderCache.keySet().removeIf(key -> key.startsWith(prefix));
    }

    private String buildCacheKey(Long userId, List<UserSkill> skills) {
        String skillIds = skills.stream()
            .map(s -> String.valueOf(s.getId()))
            .sorted()
            .collect(Collectors.joining(","));
        return userId + ":" + skillIds;
    }
}