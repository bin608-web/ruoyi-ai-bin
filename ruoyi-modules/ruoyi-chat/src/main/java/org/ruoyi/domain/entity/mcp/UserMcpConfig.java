package org.ruoyi.domain.entity.mcp;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;

/**
 * 用户 MCP 配置实体
 *
 * @author ruoyi team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_mcp_config")
public class UserMcpConfig extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 配置 ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 工具 ID（关联 mcp_tool_info.id）
     */
    private Long toolId;

    /**
     * 工具名称（冗余字段，便于查询）
     */
    private String toolName;

    /**
     * 配置名称
     */
    private String configName;

    /**
     * 配置描述
     */
    private String description;

    /**
     * 覆盖配置信息（JSON 格式）
     * 用户可覆盖工具的默认配置
     * LOCAL: {"command": "npx", "args": ["-y", "@example/mcp-server"], "env": {...}}
     * REMOTE: {"baseUrl": "http://localhost:8080/mcp", "headers": {...}}
     */
    private String configJson;

    /**
     * 状态：ENABLED-启用，DISABLED-禁用
     */
    private String status;

    /**
     * 优先级（数字越小优先级越高）
     */
    private Integer priority;

}
