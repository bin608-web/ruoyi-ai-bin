package org.ruoyi.domain.entity.skill;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.ruoyi.common.tenant.core.TenantEntity;

/**
 * 技能订阅实体
 *
 * @author ruoyi
 */
@Data
@TableName("skill_subscription")
public class SkillSubscription extends TenantEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 技能 ID */
    private Long skillId;

    /** 订阅用户 ID */
    private Long userId;

    /** 状态：ACTIVE/CANCELLED */
    private String status;
}