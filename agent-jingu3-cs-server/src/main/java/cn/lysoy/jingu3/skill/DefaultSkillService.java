package cn.lysoy.jingu3.skill;

import cn.lysoy.jingu3.common.vo.SkillListItemVo;
import cn.lysoy.jingu3.config.Jingu3Properties;
import cn.lysoy.jingu3.skill.entity.SkillEntity;
import cn.lysoy.jingu3.skill.mapper.SkillMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@ConditionalOnProperty(prefix = "jingu3.skill", name = "api-enabled", havingValue = "true", matchIfMissing = true)
public class DefaultSkillService implements SkillService {

    private final SkillMapper skillMapper;

    private final Jingu3Properties properties;

    public DefaultSkillService(SkillMapper skillMapper, Jingu3Properties properties) {
        this.skillMapper = skillMapper;
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

    private static String formatTime(LocalDateTime t) {
        return t == null ? null : t.toString();
    }
}
