package org.ruoyi.domain.dto.skill;

import lombok.Data;

/**
 * 用户技能 DTO（用于返回生成的技能信息）
 *
 * @author ruoyi
 */
@Data
public class UserSkillDto {

    /**
     * 技能 ID
     */
    private Long id;

    /**
     * 技能名称
     */
    private String skillName;

    /**
     * 技能编码
     */
    private String skillCode;

    /**
     * 技能类型
     */
    private String skillType;

    /**
     * 技能描述
     */
    private String description;

    /**
     * 技能代码内容
     */
    private String skillCodeContent;

    /**
     * 技能文件路径
     */
    private String filePath;

    /**
     * 是否生成成功
     */
    private boolean success;

    /**
     * 错误信息
     */
    private String errorMessage;

}
