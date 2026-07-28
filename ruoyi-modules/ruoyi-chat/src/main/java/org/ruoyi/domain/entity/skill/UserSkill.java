package org.ruoyi.domain.entity.skill;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.tenant.core.TenantEntity;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 用户技能实体
 *
 * @author ruoyi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_skill")
public class UserSkill extends TenantEntity {

    /**
     * 主键 ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 技能名称
     */
    private String skillName;

    /**
     * 技能编码（唯一标识）
     */
    private String skillCode;

    /**
     * 技能类型：LOCAL-本地脚本，MCP- MCP 工具，CUSTOM-自定义
     */
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
     * 最近测试结果
     */
    private String testResult;

    private String delFlag;

    /**
     * 最近测试时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime testTime;

}
