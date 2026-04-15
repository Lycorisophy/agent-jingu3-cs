package cn.lysoy.jingu3.skill.constant;

/**
 * {@code skill} / {@code user_skill} 等业务状态字符串（与 Flyway 注释及种子数据一致）。
 */
public final class SkillStatuses {

    private SkillStatuses() {
    }

    /** 技能或订阅处于可用状态（与 DB 约束及业务校验一致）。 */
    public static final String ACTIVE = "ACTIVE";
}
