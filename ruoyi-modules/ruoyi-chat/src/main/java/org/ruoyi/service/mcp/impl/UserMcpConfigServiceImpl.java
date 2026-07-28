package org.ruoyi.service.mcp.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.domain.bo.mcp.UserMcpConfigBo;
import org.ruoyi.domain.dto.mcp.McpToolListResult;
import org.ruoyi.domain.entity.mcp.McpTool;
import org.ruoyi.domain.entity.mcp.UserMcpConfig;
import org.ruoyi.domain.vo.mcp.UserMcpConfigVo;
import org.ruoyi.enums.McpToolStatus;
import org.ruoyi.mapper.mcp.McpToolMapper;
import org.ruoyi.mapper.mcp.UserMcpConfigMapper;
import org.ruoyi.service.mcp.IUserMcpConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户 MCP 配置服务实现
 *
 * @author ruoyi team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserMcpConfigServiceImpl implements IUserMcpConfigService {

    private final UserMcpConfigMapper baseMapper;
    private final McpToolMapper mcpToolMapper;

    @Override
    public TableDataInfo<UserMcpConfigVo> selectPageList(UserMcpConfigBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<UserMcpConfig> wrapper = buildQueryWrapper(bo);
        Page<UserMcpConfigVo> page = baseMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public List<UserMcpConfigVo> queryList(UserMcpConfigBo bo) {
        LambdaQueryWrapper<UserMcpConfig> wrapper = buildQueryWrapper(bo);
        return baseMapper.selectVoList(wrapper);
    }

    @Override
    public UserMcpConfigVo selectById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public List<UserMcpConfigVo> selectByUserId(Long userId) {
        return baseMapper.selectEnabledByUserId(userId);
    }

    @Override
    public UserMcpConfigVo selectByUserIdAndToolId(Long userId, Long toolId) {
        return baseMapper.selectByUserIdAndToolId(userId, toolId);
    }

    @Override
    public McpToolListResult getUserAvailableTools(Long userId) {
        // 获取用户启用的配置
        List<UserMcpConfigVo> userConfigs = baseMapper.selectEnabledByUserId(userId);
        
        if (userConfigs == null || userConfigs.isEmpty()) {
            // 用户没有配置，返回空列表
            return McpToolListResult.of(new ArrayList<>());
        }

        // 获取所有启用的工具
        LambdaQueryWrapper<McpTool> toolWrapper = new LambdaQueryWrapper<>();
        toolWrapper.eq(McpTool::getStatus, McpToolStatus.ENABLED.getValue());
        List<McpTool> allEnabledTools = mcpToolMapper.selectList(toolWrapper);

        // 根据用户配置过滤工具
        List<McpTool> availableTools = new ArrayList<>();
        for (UserMcpConfigVo config : userConfigs) {
            McpTool tool = allEnabledTools.stream()
                .filter(t -> t.getId().equals(config.getToolId()))
                .findFirst()
                .orElse(null);
            
            if (tool != null) {
                // 如果用户有自定义配置，使用用户配置
                if (StringUtils.hasText(config.getConfigJson())) {
                    // 这里可以合并用户配置和工具默认配置
                    // 暂时直接使用工具信息
                }
                availableTools.add(tool);
            }
        }

        return McpToolListResult.of(availableTools);
    }

    @Override
    @Transactional
    public String insert(UserMcpConfigBo bo) {
        // 检查是否已存在该用户的该工具配置
        UserMcpConfigVo existing = selectByUserIdAndToolId(bo.getUserId(), bo.getToolId());
        if (existing != null) {
            throw new ServiceException("该用户已存在此工具的配置");
        }

        // 验证工具是否存在
        McpTool tool = mcpToolMapper.selectById(bo.getToolId());
        if (tool == null) {
            throw new ServiceException("工具不存在");
        }

        UserMcpConfig config = MapstructUtils.convert(bo, UserMcpConfig.class);
        config.setToolName(tool.getName());
        
        if (config.getStatus() == null) {
            config.setStatus(McpToolStatus.ENABLED.getValue());
        }
        if (config.getPriority() == null) {
            config.setPriority(100); // 默认优先级
        }
        
        baseMapper.insert(config);
        return String.valueOf(config.getId());
    }

    @Override
    @Transactional
    public String insertBatch(UserMcpConfigBo bo) {
        if (bo.getToolIds() == null || bo.getToolIds().isEmpty()) {
            throw new ServiceException("工具 ID 列表不能为空");
        }

        List<String> resultIds = new ArrayList<>();
        for (Long toolId : bo.getToolIds()) {
            UserMcpConfigBo singleBo = new UserMcpConfigBo();
            singleBo.setUserId(bo.getUserId());
            singleBo.setToolId(toolId);
            singleBo.setConfigName(bo.getConfigName());
            singleBo.setDescription(bo.getDescription());
            singleBo.setConfigJson(bo.getConfigJson());
            singleBo.setStatus(bo.getStatus());
            singleBo.setPriority(bo.getPriority());
            
            try {
                String id = insert(singleBo);
                resultIds.add(id);
            } catch (Exception e) {
                log.warn("批量添加配置时跳过工具 {}: {}", toolId, e.getMessage());
            }
        }
        
        return String.format("成功添加 %d 个配置", resultIds.size());
    }

    @Override
    @Transactional
    public String update(UserMcpConfigBo bo) {
        UserMcpConfig existing = baseMapper.selectById(bo.getId());
        if (existing == null) {
            throw new ServiceException("配置不存在");
        }

        // 验证工具是否存在
        if (bo.getToolId() != null && !bo.getToolId().equals(existing.getToolId())) {
            McpTool tool = mcpToolMapper.selectById(bo.getToolId());
            if (tool == null) {
                throw new ServiceException("工具不存在");
            }
            existing.setToolName(tool.getName());
        }

        UserMcpConfig config = MapstructUtils.convert(bo, UserMcpConfig.class);
        baseMapper.updateById(config);

        return String.valueOf(config.getId());
    }

    @Override
    @Transactional
    public void deleteByIds(List<Long> ids) {
        baseMapper.deleteBatchIds(ids);
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        baseMapper.deleteByUserId(userId);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, String status) {
        UserMcpConfig config = new UserMcpConfig();
        config.setId(id);
        config.setStatus(status);
        baseMapper.updateById(config);
    }

    @Override
    @Transactional
    public void deleteByToolId(Long toolId) {
        baseMapper.deleteByToolId(toolId);
    }

    private LambdaQueryWrapper<UserMcpConfig> buildQueryWrapper(UserMcpConfigBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<UserMcpConfig> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(bo.getUserId() != null, UserMcpConfig::getUserId, bo.getUserId())
            .eq(bo.getToolId() != null, UserMcpConfig::getToolId, bo.getToolId())
            .eq(StringUtils.hasText(bo.getStatus()), UserMcpConfig::getStatus, bo.getStatus())
            .like(StringUtils.hasText(bo.getConfigName()), UserMcpConfig::getConfigName, bo.getConfigName())
            .like(StringUtils.hasText(bo.getDescription()), UserMcpConfig::getDescription, bo.getDescription());
        wrapper.orderByDesc(UserMcpConfig::getPriority, UserMcpConfig::getCreateTime);
        return wrapper;
    }
}
