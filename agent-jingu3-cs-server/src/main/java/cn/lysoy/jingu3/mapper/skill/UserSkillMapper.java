package cn.lysoy.jingu3.mapper.skill;

import cn.lysoy.jingu3.common.vo.SkillSubscriptionItemVo;
import cn.lysoy.jingu3.skill.entity.UserSkillEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户技能关联查询（{@code user_skill} JOIN {@code skill}）。
 */
public interface UserSkillMapper extends BaseMapper<UserSkillEntity> {

    @Select({
        "SELECT us.id AS subscriptionId, us.user_id AS userId, us.status AS subscriptionStatus,",
        "us.local_version AS localVersion, us.server_version AS serverVersion,",
        "us.is_external AS externalSkill, us.external_path AS externalPath,",
        "s.id AS skillId, s.name AS name, s.slug AS slug, s.description AS description, s.version AS version,",
        "s.category AS category, s.tags AS tags, s.trigger_words AS triggerWords, s.icon_url AS iconUrl,",
        "s.is_official AS official, s.status AS skillStatus,",
        "us.last_sync_at AS lastSyncAt",
        "FROM user_skill us INNER JOIN skill s ON s.id = us.skill_id",
        "WHERE us.user_id = #{userId} AND us.status = 'ACTIVE' AND s.status = 'ACTIVE'",
        "ORDER BY us.updated_at DESC LIMIT #{limit}"
    })
    List<SkillSubscriptionItemVo> selectActiveSubscriptionsWithSkill(
            @Param("userId") String userId, @Param("limit") int limit);
}
