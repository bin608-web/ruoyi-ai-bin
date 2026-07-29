package org.ruoyi.mapper.skill;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.ruoyi.domain.entity.skill.SkillShareRecord;

import java.util.List;

/**
 * 技能分享记录 Mapper
 *
 * @author ruoyi
 */
@Mapper
public interface SkillShareRecordMapper extends BaseMapper<SkillShareRecord> {

    /**
     * 查询待处理的分享记录
     */
    List<SkillShareRecord> selectPendingByUserId(@Param("userId") Long userId);

    /**
     * 查询用户发起的分享记录
     */
    List<SkillShareRecord> selectByFromUserId(@Param("userId") Long userId);
}