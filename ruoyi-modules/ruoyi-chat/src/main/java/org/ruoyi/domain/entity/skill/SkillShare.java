package org.ruoyi.domain.entity.skill;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.tenant.core.TenantEntity;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * 技能分享实体
 *
 * @author ruoyi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("skill_share")
public class SkillShare extends TenantEntity {

    /**
     * 主键 ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户技能 ID
     */
    private Long skillId;

    /**
     * 分享者用户 ID
     */
    private Long fromUserId;

    /**
     * 接收者用户 ID（NULL 表示公开）
     */
    private Long toUserId;

    /**
     * 接收者名称
     */
    private String toUserName;

    /**
     * 分享类型：PUBLIC-公开，PRIVATE-私有，GROUP-群组
     */
    private String shareType;

    /**
     * 分享状态（PENDING-待接受，ACCEPTED-已接受，REJECTED-已拒绝，CANCELLED-已取消）
     */
    private String status;

    /**
     * 分享消息
     */
    private String message;

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
