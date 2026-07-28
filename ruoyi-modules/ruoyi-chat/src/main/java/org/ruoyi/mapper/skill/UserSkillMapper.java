package org.ruoyi.mapper.skill;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.ruoyi.domain.entity.skill.UserSkill;

import java.util.List;

/**
 * 用户技能 Mapper 接口
 *
 * @author ruoyi
 */
@Mapper
public interface UserSkillMapper extends BaseMapper<UserSkill> {

    /**
     * 查询用户技能列表
     *
     * @param userId 用户 ID
     * @param skillType 技能类型
     * @param isEnabled 是否启用
     * @param keyword 关键词
     * @return 技能列表
     */
    List<UserSkill> selectUserSkillList(@Param("userId") Long userId,
                                        @Param("skillType") String skillType,
                                        @Param("isEnabled") String isEnabled,
                                        @Param("keyword") String keyword);

    /**
     * 根据技能编码查询
     *
     * @param skillCode 技能编码
     * @param tenantId 租户 ID
     * @return 技能信息
     */
    UserSkill selectBySkillCode(@Param("skillCode") String skillCode, @Param("tenantId") Long tenantId);

    /**
     * 更新技能测试结果
     *
     * @param id 技能 ID
     * @param testResult 测试结果
     * @param testTime 测试时间
     * @return 结果
     */
    int updateTestResult(@Param("id") Long id, @Param("testResult") String testResult, @Param("testTime") java.time.LocalDateTime testTime);

    /**
     * 批量启用/禁用技能
     *
     * @param ids 技能 ID 列表
     * @param isEnabled 是否启用
     * @return 结果
     */
    int batchUpdateEnabled(@Param("ids") List<Long> ids, @Param("isEnabled") String isEnabled);

    /**
     * 根据用户 ID 查询启用的技能列表
     *
     * @param userId 用户 ID
     * @return 技能列表
     */
    List<UserSkill> selectEnabledByUserId(@Param("userId") Long userId);

    /**
     * 根据用户 ID 和技能 ID 列表查询技能
     *
     * @param userId 用户 ID
     * @param ids 技能 ID 列表
     * @return 技能列表
     */
    List<UserSkill> selectListByUserAndIds(@Param("userId") Long userId, @Param("ids") List<Long> ids);
}
