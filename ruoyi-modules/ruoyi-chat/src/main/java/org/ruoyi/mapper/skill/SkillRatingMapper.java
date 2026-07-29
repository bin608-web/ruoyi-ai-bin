package org.ruoyi.mapper.skill;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.ruoyi.domain.entity.skill.SkillRating;

/**
 * 技能评分 Mapper
 *
 * @author ruoyi
 */
@Mapper
public interface SkillRatingMapper extends BaseMapper<SkillRating> {

    /**
     * 查询技能的平均评分
     */
    Double selectAverageRating(@Param("skillId") Long skillId);

    /**
     * 查询技能的评分数量
     */
    Long selectRatingCount(@Param("skillId") Long skillId);
}