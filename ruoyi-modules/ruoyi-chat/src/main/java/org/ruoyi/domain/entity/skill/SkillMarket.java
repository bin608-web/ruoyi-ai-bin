package org.ruoyi.domain.entity.skill;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.tenant.core.TenantEntity;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 技能市场实体
 *
 * @author ruoyi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("skill_market")
public class SkillMarket extends TenantEntity {

    /**
     * 主键 ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联的用户技能 ID
     */
    private Long skillId;

    /**
     * 技能名称
     */
    private String skillName;

    /**
     * 技能编码
     */
    private String skillCode;

    /**
     * 作者用户 ID
     */
    private Long authorId;

    /**
     * 作者名称
     */
    private String authorName;

    /**
     * 技能类型
     */
    private String skillType;

    /**
     * 技能描述
     */
    private String description;

    /**
     * 使用指南
     */
    private String usageGuide;

    /**
     * 标签（逗号分隔）
     */
    private String tags;

    /**
     * 下载次数
     */
    private Integer downloadCount;

    /**
     * 评分（0-5）
     */
    private BigDecimal rating;

    /**
     * 评分次数
     */
    private Integer ratingCount;

    /**
     * 审核状态（P-待审核，A-已通过，R-已拒绝）
     */
    private String isApproved;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

}
