package org.ruoyi.domain.vo.skill;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.ruoyi.domain.entity.skill.UserSkill;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户技能 VO
 *
 * @author ruoyi
 */
@Data
public class UserSkillVo {

    /**
     * 转换实体为 VO
     */
    public static UserSkillVo convert(UserSkill entity) {
        if (entity == null) {
            return null;
        }
        UserSkillVo vo = new UserSkillVo();
        vo.setId(entity.getId());
        vo.setUserId(entity.getUserId());
        vo.setSkillName(entity.getSkillName());
        vo.setSkillCode(entity.getSkillCode());
        vo.setSkillType(entity.getSkillType());
        vo.setDescription(entity.getDescription());
        vo.setSkillConfig(entity.getSkillConfig());
        vo.setFilePath(entity.getFilePath());
        vo.setIsEnabled(entity.getIsEnabled());
        vo.setIsPublic(entity.getIsPublic());
        vo.setTestResult(entity.getTestResult());
        vo.setTestTime(entity.getTestTime());
        vo.setCreateTime(entity.getCreateTime());
        vo.setCreateBy(entity.getCreateBy());
        return vo;
    }

    /**
     * 转换实体列表为 VO 列表
     */
    public static List<UserSkillVo> convertList(List<UserSkill> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream().map(UserSkillVo::convert).collect(Collectors.toList());
    }

    /**
     * 主键 ID
     */
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

    /**
     * 最近测试时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime testTime;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /**
     * 创建者
     */
    private Long createBy;

}
