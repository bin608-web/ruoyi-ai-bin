package org.ruoyi.service.mcp;

import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.domain.bo.mcp.UserMcpConfigBo;
import org.ruoyi.domain.dto.mcp.McpToolListResult;
import org.ruoyi.domain.vo.mcp.UserMcpConfigVo;

import java.util.List;

/**
 * 用户 MCP 配置服务接口
 *
 * @author ruoyi team
 */
public interface IUserMcpConfigService {

    /**
     * 分页查询用户 MCP 配置列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 配置分页列表
     */
    TableDataInfo<UserMcpConfigVo> selectPageList(UserMcpConfigBo bo, PageQuery pageQuery);

    /**
     * 查询用户 MCP 配置列表（不分页）
     *
     * @param bo 查询条件
     * @return 配置列表
     */
    List<UserMcpConfigVo> queryList(UserMcpConfigBo bo);

    /**
     * 根据 ID 查询配置
     *
     * @param id 配置 ID
     * @return 配置信息
     */
    UserMcpConfigVo selectById(Long id);

    /**
     * 根据用户 ID 查询配置列表
     *
     * @param userId 用户 ID
     * @return 配置列表
     */
    List<UserMcpConfigVo> selectByUserId(Long userId);

    /**
     * 根据用户 ID 和工具 ID 查询配置
     *
     * @param userId  用户 ID
     * @param toolId  工具 ID
     * @return 配置信息
     */
    UserMcpConfigVo selectByUserIdAndToolId(Long userId, Long toolId);

    /**
     * 获取用户当前可用的 MCP 工具列表（根据用户配置过滤）
     *
     * @param userId 用户 ID
     * @return MCP 工具列表
     */
    McpToolListResult getUserAvailableTools(Long userId);

    /**
     * 新增用户 MCP 配置
     *
     * @param bo 配置信息
     * @return 结果
     */
    String insert(UserMcpConfigBo bo);

    /**
     * 批量新增用户 MCP 配置
     *
     * @param bo 配置信息
     * @return 结果
     */
    String insertBatch(UserMcpConfigBo bo);

    /**
     * 更新用户 MCP 配置
     *
     * @param bo 配置信息
     * @return 结果
     */
    String update(UserMcpConfigBo bo);

    /**
     * 删除用户 MCP 配置
     *
     * @param ids 配置 ID 列表
     */
    void deleteByIds(List<Long> ids);

    /**
     * 删除用户的所有配置
     *
     * @param userId 用户 ID
     */
    void deleteByUserId(Long userId);

    /**
     * 更新配置状态
     *
     * @param id     配置 ID
     * @param status 状态
     */
    void updateStatus(Long id, String status);

    /**
     * 根据工具 ID 删除配置
     *
     * @param toolId 工具 ID
     */
    void deleteByToolId(Long toolId);

}
