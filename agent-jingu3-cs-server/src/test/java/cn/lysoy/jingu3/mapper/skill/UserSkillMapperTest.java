package cn.lysoy.jingu3.mapper.skill;

import cn.lysoy.jingu3.Jingu3Application;
import cn.lysoy.jingu3.common.vo.SkillSubscriptionItemVo;
import cn.lysoy.jingu3.skill.entity.SkillEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = Jingu3Application.class,
        properties = {"jingu3.cron.scheduler-enabled=false"})
@Transactional
class UserSkillMapperTest {

    @Autowired
    private UserSkillMapper userSkillMapper;

    @Autowired
    private SkillMapper skillMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void selectActiveSubscriptionsWithSkill() {
        jdbcTemplate.update(
                "INSERT INTO users (id, username, password_hash) VALUES (?,?,?)",
                "001",
                "user",
                "hash");

        LocalDateTime t = LocalDateTime.now();
        SkillEntity skill = new SkillEntity();
        skill.setId("sk1");
        skill.setName("S");
        skill.setSlug("slug-s");
        skill.setDescription("d");
        skill.setVersion("1.0.0");
        skill.setStoragePath("skills/sk1/");
        skill.setIsPublic(true);
        skill.setIsOfficial(true);
        skill.setStatus("ACTIVE");
        skill.setCreatedAt(t);
        skill.setUpdatedAt(t);
        skillMapper.insert(skill);

        jdbcTemplate.update(
                "INSERT INTO user_skill (id, user_id, skill_id, status, local_version, server_version) VALUES (?,?,?,?,?,?)",
                "us1",
                "001",
                "sk1",
                "ACTIVE",
                "1.0.0",
                "1.0.0");

        List<SkillSubscriptionItemVo> list = userSkillMapper.selectActiveSubscriptionsWithSkill("001", 20);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getSlug()).isEqualTo("slug-s");
        assertThat(list.get(0).getSkillId()).isEqualTo("sk1");
        assertThat(list.get(0).getSubscriptionId()).isEqualTo("us1");
    }
}
