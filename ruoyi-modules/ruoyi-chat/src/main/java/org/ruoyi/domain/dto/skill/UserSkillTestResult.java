package org.ruoyi.domain.dto.skill;

import lombok.Data;

/**
 * 技能测试结果
 *
 * @author ruoyi
 */
@Data
public class UserSkillTestResult {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 测试结果描述
     */
    private String message;

    /**
     * 输出内容
     */
    private String output;

    /**
     * 错误信息
     */
    private String error;

    /**
     * 执行时间（毫秒）
     */
    private Long executionTime;

    /**
     * 原始输出（JSON 格式）
     */
    private String rawOutput;

}
