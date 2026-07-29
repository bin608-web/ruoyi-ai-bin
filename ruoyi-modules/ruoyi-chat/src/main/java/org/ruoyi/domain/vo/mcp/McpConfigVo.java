package org.ruoyi.domain.vo.mcp;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * MCP 配置 VO（用于前端 API）
 *
 * @author ruoyi
 */
@Data
public class McpConfigVo {

    /**
     * 配置 ID
     */
    private Long id;

    /**
     * 配置名称
     */
    private String name;

    /**
     * 配置描述
     */
    private String description;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * MCP 服务器地址
     */
    private String serverUrl;

    /**
     * 认证方式
     */
    private String authType;

    /**
     * 认证 token
     */
    private String authToken;

    /**
     * 超时时间 (ms)
     */
    private Integer timeout;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updatedAt;

}
