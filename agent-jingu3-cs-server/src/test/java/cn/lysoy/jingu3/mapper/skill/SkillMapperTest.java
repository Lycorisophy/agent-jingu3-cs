package cn.lysoy.jingu3.mapper.skill;

import cn.lysoy.jingu3.Jingu3Application;
import cn.lysoy.jingu3.skill.entity.SkillEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = Jingu3Application.class,
        properties = {"jingu3.cron.scheduler-enabled=false"})
@Transactional
class SkillMapperTest {

    @Autowired
    private SkillMapper skillMapper;

    @Test
    void selectPublicActiveOrdersByUpdatedAt() {
        LocalDateTime t = LocalDateTime.now();
        SkillEntity a = row("a1", "skill-a", "slug-a", t);
        SkillEntity b = row("b1", "skill-b", "slug-b", t.plusSeconds(1));
        skillMapper.insert(a);
        skillMapper.insert(b);

        List<SkillEntity> list = skillMapper.selectPublicActive(10);
        assertThat(list).hasSize(2);
        assertThat(list.get(0).getSlug()).isEqualTo("slug-b");
        assertThat(list.get(1).getSlug()).isEqualTo("slug-a");

        assertThat(skillMapper.selectPublicActiveBySlug("slug-a").getName()).isEqualTo("skill-a");
        assertThat(skillMapper.selectPublicActiveBySlug("unknown")).isNull();
    }

    private static SkillEntity row(String id, String name, String slug, LocalDateTime updatedAt) {
        SkillEntity e = new SkillEntity();
        e.setId(id);
        e.setName(name);
        e.setSlug(slug);
        e.setDescription("d");
        e.setVersion("1.0.0");
        e.setStoragePath("s/" + id + "/");
        e.setIsPublic(true);
        e.setIsOfficial(false);
        e.setStatus("ACTIVE");
        e.setCreatedAt(updatedAt);
        e.setUpdatedAt(updatedAt);
        return e;
    }
}
