package org.ruoyi.domain.entity.skill;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.ruoyi.common.tenant.core.TenantEntity;

import java.time.LocalDateTime;

/**
 * 技能分享记录实体
 *
 * @author ruoyi
 */
@Data
@TableName("skill_share_record")
public class SkillShareRecord extends TenantEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 技能 ID */
    private Long skillId;

    /** 技能名称（冗余） */
    private String skillName;

    /** 分享者用户 ID */
    private Long fromUserId;

    /** 分享者名称 */
    private String fromUserName;

    /** 接收者用户 ID */
    private Long toUserId;

    /** 接收者名称 */
    private String toUserName;

    /** 分享类型：PUBLIC/PRIVATE */
    private String shareType;

    /** 状态：PENDING/ACCEPTED/REJECTED/REVOKED */
    private String status;

    /** 分享消息 */
    private String message;
}