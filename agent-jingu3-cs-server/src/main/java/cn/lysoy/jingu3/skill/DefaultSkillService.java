package cn.lysoy.jingu3.skill;

import cn.lysoy.jingu3.common.enums.ErrorCode;
import cn.lysoy.jingu3.common.exception.ServiceException;
import cn.lysoy.jingu3.common.vo.SkillListItemVo;
import cn.lysoy.jingu3.common.vo.SkillSubscriptionItemVo;
import cn.lysoy.jingu3.config.Jingu3Properties;
import cn.lysoy.jingu3.skill.constant.SkillStatuses;
import cn.lysoy.jingu3.skill.entity.SkillEntity;
import cn.lysoy.jingu3.skill.entity.UserSkillEntity;
import cn.lysoy.jingu3.skill.mapper.SkillMapper;
import cn.lysoy.jingu3.skill.mapper.UserSkillMapper;
import cn.lysoy.jingu3.user.mapper.UserMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 技能域默认实现：公开技能目录查询、按 slug 拉取元数据、用户订阅/退订（{@code skill} / {@code user_skill} 表）。
 * <p>与「技能与工具」史诗对齐：此处管<strong>技能包生命周期与订阅关系</strong>；对话中是否注入某技能正文由
 * {@link SkillService} 的消费方（编排 / 提示词装配）决定。可通过 {@code jingu3.skill.api-enabled=false} 关闭本 Bean。</p>
 */
@Service
@ConditionalOnProperty(prefix = "jingu3.skill", name = "api-enabled", havingValue = "true", matchIfMissing = true)
public class DefaultSkillService implements SkillService {

    private final SkillMapper skillMapper;

    private final UserSkillMapper userSkillMapper;

    private final UserMapper userMapper;

    private final Jingu3Properties properties;

    public DefaultSkillService(
            SkillMapper skillMapper,
            UserSkillMapper userSkillMapper,
            UserMapper userMapper,
            Jingu3Properties properties) {
        this.skillMapper = skillMapper;
        this.userSkillMapper = userSkillMapper;
        this.userMapper = userMapper;
        this.properties = properties;
    }

    @Override
    public List<SkillListItemVo> listPublicCatalog() {
        int limit = Math.max(1, properties.getSkill().getListMaxSize());
        List<SkillEntity> rows = skillMapper.selectPublicActive(limit);
        List<SkillListItemVo> out = new ArrayList<>(rows.size());
        for (SkillEntity e : rows) {
            out.add(toVo(e));
        }
        return out;
    }

    @Override
    public SkillListItemVo getPublicBySlug(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new ServiceException(ErrorCode.BAD_REQUEST, "slug 不能为空");
        }
        SkillEntity e = skillMapper.selectPublicActiveBySlug(slug.trim());
        if (e == null) {
            throw new ServiceException(ErrorCode.NOT_FOUND, "技能不存在或未公开");
        }
        return toVo(e);
    }

    @Override
    public List<SkillSubscriptionItemVo> listMySubscriptions(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ServiceException(ErrorCode.BAD_REQUEST, "userId 不能为空");
        }
        int limit = Math.max(1, properties.getSkill().getListMaxSize());
        return userSkillMapper.selectActiveSubscriptionsWithSkill(userId.trim(), limit);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void subscribe(String userId, String skillId) {
        if (skillId == null || skillId.isBlank()) {
            throw new ServiceException(ErrorCode.BAD_REQUEST, "skillId 不能为空");
        }
        String uid = userId == null ? "" : userId.trim();
        String sid = skillId.trim();
        if (uid.isEmpty()) {
            throw new ServiceException(ErrorCode.BAD_REQUEST, "userId 不能为空");
        }
        if (userMapper.countById(uid) == 0) {
            throw new ServiceException(ErrorCode.BAD_REQUEST, "用户不存在");
        }
        SkillEntity skill = skillMapper.selectById(sid);
        if (skill == null) {
            throw new ServiceException(ErrorCode.NOT_FOUND, "技能不存在");
        }
        if (!Boolean.TRUE.equals(skill.getIsPublic()) || !SkillStatuses.ACTIVE.equals(skill.getStatus())) {
            throw new ServiceException(ErrorCode.BAD_REQUEST, "技能未公开或不可用");
        }
        UserSkillEntity existing =
                userSkillMapper.selectOne(
                        Wrappers.lambdaQuery(UserSkillEntity.class)
                                .eq(UserSkillEntity::getUserId, uid)
                                .eq(UserSkillEntity::getSkillId, sid));
        LocalDateTime now = LocalDateTime.now();
        if (existing != null) {
            if (SkillStatuses.ACTIVE.equals(existing.getStatus())) {
                return;
            }
            existing.setStatus(SkillStatuses.ACTIVE);
            existing.setLocalVersion(skill.getVersion());
            existing.setServerVersion(skill.getVersion());
            existing.setLastSyncAt(now);
            existing.setUpdatedAt(now);
            userSkillMapper.updateById(existing);
            return;
        }
        UserSkillEntity row = new UserSkillEntity();
        row.setId(UUID.randomUUID().toString());
        row.setUserId(uid);
        row.setSkillId(sid);
        row.setStatus(SkillStatuses.ACTIVE);
        row.setLocalVersion(skill.getVersion());
        row.setServerVersion(skill.getVersion());
        row.setIsExternal(false);
        row.setLastSyncAt(now);
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        userSkillMapper.insert(row);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unsubscribe(String userId, String skillId) {
        if (skillId == null || skillId.isBlank()) {
            throw new ServiceException(ErrorCode.BAD_REQUEST, "skillId 不能为空");
        }
        String uid = userId == null ? "" : userId.trim();
        String sid = skillId.trim();
        if (uid.isEmpty()) {
            throw new ServiceException(ErrorCode.BAD_REQUEST, "userId 不能为空");
        }
        int deleted =
                userSkillMapper.delete(
                        Wrappers.lambdaQuery(UserSkillEntity.class)
                                .eq(UserSkillEntity::getUserId, uid)
                                .eq(UserSkillEntity::getSkillId, sid));
        if (deleted == 0) {
            throw new ServiceException(ErrorCode.NOT_FOUND, "未找到订阅");
        }
    }

    /** 领域实体转列表/详情 VO，避免 Controller 直接依赖 MyBatis 实体。 */
    private static SkillListItemVo toVo(SkillEntity e) {
        SkillListItemVo vo = new SkillListItemVo();
        vo.setId(e.getId());
        vo.setName(e.getName());
        vo.setSlug(e.getSlug());
        vo.setDescription(e.getDescription());
        vo.setVersion(e.getVersion());
        vo.setCategory(e.getCategory());
        vo.setTags(e.getTags());
        vo.setTriggerWords(e.getTriggerWords());
        vo.setIconUrl(e.getIconUrl());
        vo.setOfficial(Boolean.TRUE.equals(e.getIsOfficial()));
        vo.setStatus(e.getStatus());
        vo.setCreatedAt(formatTime(e.getCreatedAt()));
        vo.setUpdatedAt(formatTime(e.getUpdatedAt()));
        return vo;
    }

    /** 时间展示用简化字符串；null 安全。 */
    private static String formatTime(LocalDateTime t) {
        return t == null ? null : t.toString();
    }
}
