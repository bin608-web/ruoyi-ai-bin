package org.ruoyi.domain.bo.mcp;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import org.ruoyi.domain.entity.mcp.UserMcpConfig;

import java.io.Serial;
import java.util.List;

/**
 * 用户 MCP 配置业务对象
 *
 * @author ruoyi team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = UserMcpConfig.class, reverseConvertGenerate = false)
public class UserMcpConfigBo extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 配置 ID
     */
    private Long id;

    /**
     * 用户 ID
     */
    @NotNull(message = "用户 ID 不能为空")
    private Long userId;

    /**
     * 工具 ID
     */
    @NotNull(message = "工具 ID 不能为空")
    private Long toolId;

    /**
     * 工具名称
     */
    private String toolName;

    /**
     * 配置名称
     */
    @NotBlank(message = "配置名称不能为空")
    @Size(min = 1, max = 100, message = "配置名称不能超过{max}个字符")
    private String configName;

    /**
     * 配置描述
     */
    @Size(max = 500, message = "配置描述不能超过{max}个字符")
    private String description;

    /**
     * 覆盖配置信息（JSON 格式）
     */
    private String configJson;

    /**
     * 状态：ENABLED-启用，DISABLED-禁用
     */
    private String status;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 工具 ID 列表（用于批量操作）
     */
    private List<Long> toolIds;

}
