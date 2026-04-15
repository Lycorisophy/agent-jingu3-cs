package cn.lysoy.jingu3.skill;

import cn.lysoy.jingu3.common.vo.SkillListItemVo;
import cn.lysoy.jingu3.common.vo.SkillSubscriptionItemVo;

import java.util.List;

/**
 * 技能市场元数据与订阅（v0.7）。
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
