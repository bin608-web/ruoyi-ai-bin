package org.ruoyi.mapper.mcp;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.ruoyi.common.mybatis.core.mapper.BaseMapperPlus;
import org.ruoyi.domain.entity.mcp.UserMcpConfig;
import org.ruoyi.domain.vo.mcp.UserMcpConfigVo;

import java.util.List;

/**
 * 用户 MCP 配置 Mapper
 *
 * @author ruoyi team
 */
@Mapper
public interface UserMcpConfigMapper extends BaseMapperPlus<UserMcpConfig, UserMcpConfigVo> {

    /**
     * 根据用户 ID 查询启用的配置列表
     *
     * @param userId 用户 ID
     * @return 配置列表
     */
    List<UserMcpConfigVo> selectEnabledByUserId(@Param("userId") Long userId);

    /**
     * 根据用户 ID 和工具 ID 查询配置
     *
     * @param userId  用户 ID
     * @param toolId  工具 ID
     * @return 配置信息
     */
    UserMcpConfigVo selectByUserIdAndToolId(@Param("userId") Long userId, @Param("toolId") Long toolId);

    /**
     * 根据用户 ID 和配置 ID 列表查询启用的配置
     *
     * @param userId 用户 ID
     * @param configIds 配置 ID 列表
     * @return 配置列表
     */
    List<UserMcpConfigVo> selectByIdsAndUserId(@Param("userId") Long userId, @Param("configIds") List<Long> configIds);

    /**
     * 删除用户的配置
     *
     * @param userId 用户 ID
     */
    int deleteByUserId(@Param("userId") Long userId);

    /**
     * 根据工具 ID 删除配置
     *
     * @param toolId 工具 ID
     */
    int deleteByToolId(@Param("toolId") Long toolId);

}
