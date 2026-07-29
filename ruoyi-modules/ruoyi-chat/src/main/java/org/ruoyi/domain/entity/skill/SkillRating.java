package org.ruoyi.domain.entity.skill;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.ruoyi.common.tenant.core.TenantEntity;

/**
 * 技能评分实体
 *
 * @author ruoyi
 */
@Data
@TableName("skill_rating")
public class SkillRating extends TenantEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 技能 ID */
    private Long skillId;

    /** 评分用户 ID */
    private Long userId;

    /** 评分用户名称 */
    private String userName;

    /** 评分（1-5） */
    private Integer rating;

    /** 评价内容 */
    private String comment;
}