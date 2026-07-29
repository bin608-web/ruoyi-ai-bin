package org.ruoyi.mapper.skill;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.ruoyi.domain.entity.skill.SkillSubscription;

/**
 * 技能订阅 Mapper
 *
 * @author ruoyi
 */
@Mapper
public interface SkillSubscriptionMapper extends BaseMapper<SkillSubscription> {

    /**
     * 检查用户是否已订阅某个技能
     */
    SkillSubscription selectBySkillAndUser(@Param("skillId") Long skillId, @Param("userId") Long userId);
}