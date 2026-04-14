package cn.lysoy.jingu3.common.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * 技能市场列表项（不含对象存储路径等内部字段）。
 */
@Getter
@Setter
public class SkillListItemVo {

    private String id;

    private String name;

    private String slug;

    private String description;

    private String version;

    private String category;

    /** JSON 数组字符串，与库表 {@code tags} 一致 */
    private String tags;

    /** JSON 数组字符串，与库表 {@code trigger_words} 一致 */
    private String triggerWords;

    private String iconUrl;

    private boolean official;

    private String status;

    private String createdAt;

    private String updatedAt;
}
