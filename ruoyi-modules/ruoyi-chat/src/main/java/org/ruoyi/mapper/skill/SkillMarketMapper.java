package org.ruoyi.mapper.skill;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.ruoyi.domain.entity.skill.SkillMarket;

import java.util.List;

/**
 * 技能市场 Mapper 接口
 *
 * @author ruoyi
 */
@Mapper
public interface SkillMarketMapper extends BaseMapper<SkillMarket> {

    /**
     * 查询技能市场列表
     *
     * @param skillType 技能类型
     * @param isApproved 审核状态
     * @param keyword 关键词
     * @param tags 标签
     * @return 技能列表
     */
    List<SkillMarket> selectSkillMarketList(@Param("skillType") String skillType,
                                            @Param("isApproved") String isApproved,
                                            @Param("keyword") String keyword,
                                            @Param("tags") String tags);

    /**
     * 增加下载次数
     *
     * @param id 技能 ID
     * @return 结果
     */
    int incrementDownloadCount(@Param("id") Long id);

    /**
     * 更新评分
     *
     * @param id 技能 ID
     * @param rating 新评分
     * @param ratingCount 评分次数
     * @return 结果
     */
    int updateRating(@Param("id") Long id, @Param("rating") java.math.BigDecimal rating, @Param("ratingCount") Integer ratingCount);

    /**
     * 更新审核状态
     *
     * @param id 技能 ID
     * @param isApproved 审核状态
     * @return 结果
     */
    int updateApprovalStatus(@Param("id") Long id, @Param("isApproved") String isApproved);
}
