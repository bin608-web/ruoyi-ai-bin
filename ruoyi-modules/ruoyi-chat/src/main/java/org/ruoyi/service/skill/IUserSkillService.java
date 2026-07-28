package org.ruoyi.service.skill;

import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.domain.bo.skill.UserSkillBo;
import org.ruoyi.domain.dto.skill.UserSkillDto;
import org.ruoyi.domain.dto.skill.UserSkillTestResult;
import org.ruoyi.domain.vo.skill.UserSkillVo;

import java.util.List;

/**
 * 用户技能 Service 接口
 *
 * @author ruoyi
 */
public interface IUserSkillService {

    /**
     * 查询用户技能列表（分页）
     *
     * @param bo 查询条件
     * @param pageQuery 分页参数
     * @return 技能列表
     */
    TableDataInfo<UserSkillVo> selectPageList(UserSkillBo bo, PageQuery pageQuery);

    /**
     * 查询用户技能列表（不分页）
     *
     * @param bo 查询条件
     * @return 技能列表
     */
    List<UserSkillVo> queryList(UserSkillBo bo);

    /**
     * 根据 ID 查询技能
     *
     * @param id 技能 ID
     * @return 技能信息
     */
    UserSkillVo selectById(Long id);

    /**
     * 根据技能编码查询
     *
     * @param skillCode 技能编码
     * @return 技能信息
     */
    UserSkillVo selectBySkillCode(String skillCode);

    /**
     * 新增技能
     *
     * @param bo 技能信息
     * @return 结果
     */
    void insert(UserSkillBo bo);

    /**
     * 更新技能
     *
     * @param bo 技能信息
     * @return 结果
     */
    void update(UserSkillBo bo);

    /**
     * 删除技能
     *
     * @param ids 技能 ID 列表
     * @return 结果
     */
    void deleteByIds(List<Long> ids);

    /**
     * 更新技能状态
     *
     * @param id 技能 ID
     * @param isEnabled 是否启用
     * @return 结果
     */
    void updateStatus(Long id, String isEnabled);

    /**
     * 测试技能
     *
     * @param id 技能 ID
     * @param testInput 测试输入
     * @return 测试结果
     */
    UserSkillTestResult testSkill(Long id, String testInput);

    /**
     * 生成技能（基于对话内容）
     *
     * @param userId 用户 ID
     * @param prompt 提示词
     * @param skillType 技能类型
     * @return 生成的技能信息
     */
    UserSkillDto generateSkill(Long userId, String prompt, String skillType);

    /**
     * 分享技能
     *
     * @param skillId 技能 ID
     * @param toUserId 接收者用户 ID（NULL 表示公开）
     * @param shareType 分享类型
     * @param message 分享消息
     * @return 结果
     */
    void shareSkill(Long skillId, Long toUserId, String shareType, String message);

    /**
     * 获取用户可用的技能列表（用于聊天时选择）
     *
     * @param userId 用户 ID
     * @return 技能列表
     */
    List<UserSkillVo> getAvailableSkills(Long userId);
}
