package cn.lysoy.jingu3.skill;

import cn.lysoy.jingu3.Jingu3Application;
import cn.lysoy.jingu3.skill.service.SkillService;
import cn.lysoy.jingu3.skill.entity.SkillEntity;
import cn.lysoy.jingu3.skill.entity.UserSkillEntity;
import cn.lysoy.jingu3.mapper.skill.SkillMapper;
import cn.lysoy.jingu3.mapper.skill.UserSkillMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = Jingu3Application.class,
        properties = {"jingu3.cron.scheduler-enabled=false"})
@Transactional
class SkillSubscriptionWriteIntegrationTest {

    @Autowired
    private SkillService skillService;

    @Autowired
    private SkillMapper skillMapper;

    @Autowired
    private UserSkillMapper userSkillMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void subscribeIdempotentAndUnsubscribe() {
        jdbcTemplate.update(
                "INSERT INTO users (id, username, password_hash) VALUES (?,?,?)",
                "001",
                "user",
                "hash");

        LocalDateTime t = LocalDateTime.now();
        SkillEntity skill = new SkillEntity();
        skill.setId("sk-sub-int");
        skill.setName("SubInt");
        skill.setSlug("sub-int");
        skill.setDescription("d");
        skill.setVersion("1.0.0");
        skill.setStoragePath("skills/sk-sub-int/");
        skill.setIsPublic(true);
        skill.setIsOfficial(false);
        skill.setStatus("ACTIVE");
        skill.setCreatedAt(t);
        skill.setUpdatedAt(t);
        skillMapper.insert(skill);

        skillService.subscribe("001", "sk-sub-int");
        skillService.subscribe("001", "sk-sub-int");

        assertThat(
                        userSkillMapper.selectCount(
                                Wrappers.lambdaQuery(UserSkillEntity.class)
                                        .eq(UserSkillEntity::getUserId, "001")
                                        .eq(UserSkillEntity::getSkillId, "sk-sub-int")))
                .isEqualTo(1L);

        skillService.unsubscribe("001", "sk-sub-int");
        assertThat(userSkillMapper.selectCount(Wrappers.lambdaQuery(UserSkillEntity.class).eq(UserSkillEntity::getUserId, "001")))
                .isZero();
    }
}
