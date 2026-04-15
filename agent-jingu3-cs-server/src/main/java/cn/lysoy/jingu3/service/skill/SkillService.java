package cn.lysoy.jingu3.service.skill;

import cn.lysoy.jingu3.common.vo.SkillListItemVo;
import cn.lysoy.jingu3.common.vo.SkillSubscriptionItemVo;

import java.util.List;

/**
 * <strong>技能系统（市场侧）</strong>（路线图 v0.7）：管理「公开技能目录」与「用户订阅关系」，与对话内
 * {@link cn.lysoy.jingu3.tool.Jingu3Tool} 内置工具体系<strong>解耦</strong>——本接口面向 REST 与元数据，
 * 不在此直接执行客户端下发技能逻辑（参见 {@code docs/workspace/skill-system-design.md}）。
 */
public interface SkillService {

    /**
     * 公开且状态为 ACTIVE 的技能列表，按 {@code updated_at} 倒序，条数受配置限制。
     */
    List<SkillListItemVo> listPublicCatalog();

    /**
     * 按 slug 查询公开且 ACTIVE 的技能；不存在或不可见时抛出 {@link cn.lysoy.jingu3.common.exception.ServiceException}（NOT_FOUND）。
     */
    SkillListItemVo getPublicBySlug(String slug);

    /**
     * 当前用户在 {@code user_skill} 中的有效订阅，并附带 {@code skill} 元数据。
     */
    List<SkillSubscriptionItemVo> listMySubscriptions(String userId);

    /**
     * 订阅公开且可用的技能；已 ACTIVE 则幂等；曾非 ACTIVE 则恢复为 ACTIVE。
     */
    void subscribe(String userId, String skillId);

    /** 取消订阅（删除 {@code user_skill} 行）。 */
    void unsubscribe(String userId, String skillId);
}
