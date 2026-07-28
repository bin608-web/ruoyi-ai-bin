package org.ruoyi.domain.vo.mcp;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.domain.entity.mcp.UserMcpConfig;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户 MCP 配置视图对象
 *
 * @author ruoyi team
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = UserMcpConfig.class)
public class UserMcpConfigVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 配置 ID
     */
    @ExcelProperty(value = "配置 ID")
    private Long id;

    /**
     * 用户 ID
     */
    @ExcelProperty(value = "用户 ID")
    private Long userId;

    /**
     * 工具 ID
     */
    @ExcelProperty(value = "工具 ID")
    private Long toolId;

    /**
     * 工具名称
     */
    @ExcelProperty(value = "工具名称")
    private String toolName;

    /**
     * 配置名称
     */
    @ExcelProperty(value = "配置名称")
    private String configName;

    /**
     * 配置描述
     */
    @ExcelProperty(value = "配置描述")
    private String description;

    /**
     * 配置信息
     */
    @ExcelProperty(value = "配置信息")
    private String configJson;

    /**
     * 状态
     */
    @ExcelProperty(value = "状态")
    private String status;

    /**
     * 优先级
     */
    @ExcelProperty(value = "优先级")
    private Integer priority;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;

    /**
     * 更新时间
     */
    @ExcelProperty(value = "更新时间")
    private Date updateTime;

}
