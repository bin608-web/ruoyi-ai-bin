package org.ruoyi.domain.bo.skill;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 用户技能 BO
 *
 * @author ruoyi
 */
@Data
public class UserSkillBo {

    /**
     * 主键 ID
     */
    private Long id;

    /**
     * 技能名称
     */
    @NotBlank(message = "技能名称不能为空")
    private String skillName;

    /**
     * 技能编码（唯一标识）
     */
    @NotBlank(message = "技能编码不能为空")
    private String skillCode;

    /**
     * 技能类型：LOCAL-本地脚本，MCP- MCP 工具，CUSTOM-自定义
     */
    @NotBlank(message = "技能类型不能为空")
    private String skillType;

    /**
     * 技能描述
     */
    private String description;

    /**
     * 技能配置（JSON 格式）
     */
    private String skillConfig;

    /**
     * 技能代码内容
     */
    private String skillCodeContent;

    /**
     * 技能文件路径
     */
    private String filePath;

    /**
     * 是否启用（Y/N）
     */
    private String isEnabled;

    /**
     * 是否公开（Y/N）
     */
    private String isPublic;

    /**
     * 测试输入（用于测试）
     */
    private String testInput;

    /**
     * 关键词（用于搜索）
     */
    private String keyword;

}
