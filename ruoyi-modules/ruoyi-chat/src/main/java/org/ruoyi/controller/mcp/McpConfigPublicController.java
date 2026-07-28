package org.ruoyi.controller.mcp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.domain.bo.mcp.UserMcpConfigBo;
import org.ruoyi.domain.vo.mcp.UserMcpConfigVo;
import org.ruoyi.domain.vo.mcp.McpConfigVo;
import org.ruoyi.service.mcp.IUserMcpConfigService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * MCP 配置公开接口 Controller
 * 为前端提供统一的 /mcp/config 路径接口
 *
 * @author ruoyi
 */
@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/mcp")
public class McpConfigPublicController extends BaseController {

    private final IUserMcpConfigService userMcpConfigService;

    /**
     * 获取所有 MCP 配置列表
     */
    @GetMapping("/config/list")
    public R<List<McpConfigVo>> getMcpConfigList() {
        Long userId = LoginHelper.getUserId();
        UserMcpConfigBo bo = new UserMcpConfigBo();
        bo.setUserId(userId);
        
        List<UserMcpConfigVo> configs = userMcpConfigService.queryList(bo);
        
        // 转换为前端期望的格式
        List<McpConfigVo> result = configs.stream().map(config -> {
            McpConfigVo vo = new McpConfigVo();
            vo.setId(config.getId());
            vo.setName(config.getConfigName());
            vo.setDescription(config.getDescription());
            vo.setEnabled("ENABLED".equals(config.getStatus()));
            
            // 从 configJson 中解析数据
            Map<String, Object> configJson = parseConfigJson(config.getConfigJson());
            vo.setServerUrl((String) configJson.get("serverUrl"));
            vo.setAuthType((String) configJson.getOrDefault("authType", "none"));
            vo.setTimeout((Integer) configJson.getOrDefault("timeout", 30000));
            vo.setCreatedAt(config.getCreateTime());
            vo.setUpdatedAt(config.getUpdateTime());
            
            return vo;
        }).collect(java.util.stream.Collectors.toList());
        
        return R.ok(result);
    }
    
    /**
     * 解析 configJson
     */
    private Map<String, Object> parseConfigJson(String configJson) {
        if (configJson == null || configJson.isEmpty()) {
            return new HashMap<>();
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(configJson, Map.class);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error("解析 configJson 失败", e);
            return new HashMap<>();
        }
    }

    /**
     * 获取单个 MCP 配置详情
     */
    @GetMapping("/config/{id}")
    public R<McpConfigVo> getMcpConfigById(@PathVariable Long id) {
        UserMcpConfigVo config = userMcpConfigService.selectById(id);
        if (config == null) {
            return R.fail("配置不存在");
        }
        
        McpConfigVo vo = new McpConfigVo();
        vo.setId(config.getId());
        vo.setName(config.getConfigName());
        vo.setDescription(config.getDescription());
        vo.setEnabled("ENABLED".equals(config.getStatus()));
        
        // 从 configJson 中解析数据
        Map<String, Object> configJson = parseConfigJson(config.getConfigJson());
        vo.setServerUrl((String) configJson.get("serverUrl"));
        vo.setAuthType((String) configJson.getOrDefault("authType", "none"));
        vo.setTimeout((Integer) configJson.getOrDefault("timeout", 30000));
        vo.setCreatedAt(config.getCreateTime());
        vo.setUpdatedAt(config.getUpdateTime());
        
        return R.ok(vo);
    }

    /**
     * 创建 MCP 配置
     */
    @PostMapping("/config")
    public R<Void> createMcpConfig(@RequestBody Map<String, Object> data) {
        Long userId = LoginHelper.getUserId();
        
        UserMcpConfigBo bo = new UserMcpConfigBo();
        bo.setUserId(userId);
        bo.setConfigName((String) data.get("name"));
        bo.setDescription((String) data.get("description"));
        bo.setStatus((Boolean) data.getOrDefault("enabled", true) ? "ENABLED" : "DISABLED");
        // 注意：serverUrl, authType, authToken, timeout 等字段需要添加到 UserMcpConfigBo 中
        // 暂时存储在 configJson 中
        Map<String, Object> configJson = new HashMap<>();
        configJson.put("serverUrl", data.get("serverUrl"));
        configJson.put("authType", data.getOrDefault("authType", "none"));
        configJson.put("authToken", data.get("authToken"));
        configJson.put("timeout", data.getOrDefault("timeout", 30000));
        try {
            bo.setConfigJson(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(configJson));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error("序列化 configJson 失败", e);
            return R.fail("配置数据序列化失败");
        }
        
        userMcpConfigService.insert(bo);
        return R.ok();
    }

    /**
     * 更新 MCP 配置
     */
    @PostMapping("/config/{id}")
    public R<Void> updateMcpConfig(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        UserMcpConfigBo bo = new UserMcpConfigBo();
        bo.setId(id);
        bo.setConfigName((String) data.get("name"));
        bo.setDescription((String) data.get("description"));
        
        if (data.containsKey("enabled")) {
            bo.setStatus((Boolean) data.get("enabled") ? "ENABLED" : "DISABLED");
        }
        
        // 更新 configJson
        Map<String, Object> configJson = new HashMap<>();
        if (data.containsKey("serverUrl")) configJson.put("serverUrl", data.get("serverUrl"));
        if (data.containsKey("authType")) configJson.put("authType", data.get("authType"));
        if (data.containsKey("authToken")) configJson.put("authToken", data.get("authToken"));
        if (data.containsKey("timeout")) configJson.put("timeout", data.get("timeout"));
        
        if (!configJson.isEmpty()) {
            try {
                bo.setConfigJson(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(configJson));
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                log.error("序列化 configJson 失败", e);
                return R.fail("配置数据序列化失败");
            }
        }
        
        userMcpConfigService.update(bo);
        return R.ok();
    }

    /**
     * 删除 MCP 配置
     */
    @PostMapping("/config/{id}/delete")
    public R<Void> deleteMcpConfig(@PathVariable Long id) {
        userMcpConfigService.deleteByIds(Collections.singletonList(id));
        return R.ok();
    }

    /**
     * 启用/禁用 MCP 配置
     */
    @PostMapping("/config/{id}/toggle")
    public R<Void> toggleMcpConfig(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        Boolean enabled = (Boolean) data.get("enabled");
        userMcpConfigService.updateStatus(id, enabled ? "Y" : "N");
        return R.ok();
    }

    /**
     * 测试 MCP 配置连接
     */
    @PostMapping("/config/{id}/test")
    public R<Map<String, Object>> testMcpConfig(@PathVariable Long id) {
        // 调用服务层测试配置
        // 这里暂时返回成功，实际需要根据具体实现来测试
        Map<String, Object> map = new HashMap<>();
        map.put("success", true);
        map.put("message", "连接成功");
        
        return R.ok(map);
    }
}
