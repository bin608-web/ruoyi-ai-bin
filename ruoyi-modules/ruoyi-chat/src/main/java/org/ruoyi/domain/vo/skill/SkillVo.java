package org.ruoyi.domain.vo.skill;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * Skill VO（用于前端 API）
 *
 * @author ruoyi
 */
@Data
public class SkillVo {

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updatedAt;

    /** Skill ID */
    private Long id;

    /** Skill 名称 */
    private String name;

    /** Skill 描述 */
    private String description;

    /** Skill 分类 */
    private String category;

    /** 是否启用 */
    private Boolean enabled;

    /** 是否公开分享 */
    private Boolean isPublic;

    /** 作者信息 */
    private AuthorInfo author;

    /** Skill 代码/配置 */
    private String code;

    /** 输入参数定义（JSON Schema 数组） */
    private java.util.List<InputParam> inputParams;

    /** 使用次数 */
    private Integer usageCount;

    /** 评分 */
    private Double rating;

    /** 标签列表 */
    private java.util.List<String> tags;

    /**
     * 输入参数定义
     */
    @Data
    public static class InputParam {
        /** 参数名 */
        private String name;
        /** 参数类型：string/number/boolean/file/json */
        private String type;
        /** 参数标签 */
        private String label;
        /** 是否必填 */
        private Boolean required;
        /** 默认值 */
        private Object defaultValue;
        /** 参数描述 */
        private String description;
    }

    /**
     * 作者信息
     */
    @Data
    public static class AuthorInfo {
        private Long id;
        private String name;
        private String avatar;
    }

}
