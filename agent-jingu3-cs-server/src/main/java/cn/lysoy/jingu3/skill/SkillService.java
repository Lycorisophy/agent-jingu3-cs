package cn.lysoy.jingu3.skill;

import cn.lysoy.jingu3.common.vo.SkillListItemVo;

import java.util.List;

/**
 * 技能元数据查询（v0.7 市场只读）。
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
}
