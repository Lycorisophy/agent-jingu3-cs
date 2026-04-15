package cn.lysoy.jingu3.persistence.mapper.hitl;

import cn.lysoy.jingu3.Jingu3Application;
import cn.lysoy.jingu3.common.util.UtcTime;
import cn.lysoy.jingu3.hitl.HitlApprovalStatus;
import cn.lysoy.jingu3.hitl.entity.HitlApprovalEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = Jingu3Application.class,
        properties = {"jingu3.cron.scheduler-enabled=false"})
@Transactional
class HitlApprovalMapperTest {

    @Autowired
    private HitlApprovalMapper hitlApprovalMapper;

    @Test
    void findPendingByConversation() {
        HitlApprovalEntity a = newEntity("c1", HitlApprovalStatus.PENDING);
        HitlApprovalEntity b = newEntity("c1", HitlApprovalStatus.APPROVED);
        hitlApprovalMapper.insert(a);
        hitlApprovalMapper.insert(b);

        List<HitlApprovalEntity> pending =
                hitlApprovalMapper.selectByStatusAndConversationIdOrderByCreatedAtAsc(
                        HitlApprovalStatus.PENDING, "c1");
        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).getId()).isEqualTo(a.getId());
    }

    private static HitlApprovalEntity newEntity(String conv, HitlApprovalStatus status) {
        HitlApprovalEntity e = new HitlApprovalEntity();
        e.setConversationId(conv);
        e.setStatus(status);
        e.setPayloadJson("{}");
        e.setCreatedAt(UtcTime.nowLocalDateTime());
        return e;
    }
}
