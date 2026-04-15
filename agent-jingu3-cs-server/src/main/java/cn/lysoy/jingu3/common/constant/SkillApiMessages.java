package cn.lysoy.jingu3.common.constant;

/**
 * 技能 REST 与订阅 API 的用户可见错误/校验文案（与 {@link cn.lysoy.jingu3.service.skill.DefaultSkillService} 对齐）。
 */
public final class SkillApiMessages {

    private SkillApiMessages() {
    }

    public static final String SLUG_REQUIRED = "slug 不能为空";
    public static final String SKILL_NOT_FOUND_OR_PRIVATE = "技能不存在或未公开";
    public static final String USER_ID_REQUIRED = "userId 不能为空";
    public static final String SKILL_ID_REQUIRED = "skillId 不能为空";
    public static final String USER_NOT_FOUND = "用户不存在";
    public static final String SKILL_NOT_FOUND = "技能不存在";
    public static final String SKILL_NOT_PUBLIC_OR_INACTIVE = "技能未公开或不可用";
    public static final String SUBSCRIPTION_NOT_FOUND = "未找到订阅";
}
