package org.ruoyi.mapper.skill;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.ruoyi.domain.entity.skill.SkillShare;

import java.util.List;

/**
 * 技能分享 Mapper 接口
 *
 * @author ruoyi
 */
@Mapper
public interface SkillShareMapper extends BaseMapper<SkillShare> {

    /**
     * 查询待接受的分享列表
     *
     * @param toUserId 接收者用户 ID
     * @return 分享列表
     */
    List<SkillShare> selectPendingShares(@Param("toUserId") Long toUserId);

    /**
     * 查询我分享的列表
     *
     * @param fromUserId 分享者用户 ID
     * @return 分享列表
     */
    List<SkillShare> selectMyShares(@Param("fromUserId") Long fromUserId);

    /**
     * 更新分享状态
     *
     * @param id 分享 ID
     * @param status 状态
     * @return 结果
     */
    int updateShareStatus(@Param("id") Long id, @Param("status") String status);

    /**
     * 根据技能 ID 和接收者 ID 查询分享
     *
     * @param skillId 技能 ID
     * @param toUserId 接收者用户 ID
     * @return 分享信息
     */
    SkillShare selectBySkillAndUser(@Param("skillId") Long skillId, @Param("toUserId") Long toUserId);
}
