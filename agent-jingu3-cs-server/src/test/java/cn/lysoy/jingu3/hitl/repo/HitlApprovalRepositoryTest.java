package cn.lysoy.jingu3.hitl.repo;

import cn.lysoy.jingu3.hitl.HitlApprovalStatus;
import cn.lysoy.jingu3.hitl.entity.HitlApprovalEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(
        properties = {
            "spring.jpa.hibernate.ddl-auto=validate",
            "spring.flyway.enabled=true"
        })
class HitlApprovalRepositoryTest {

    @Autowired
    private HitlApprovalRepository repository;

    @Test
    void findPendingByConversation() {
        HitlApprovalEntity a = newEntity("c1", HitlApprovalStatus.PENDING);
        HitlApprovalEntity b = newEntity("c1", HitlApprovalStatus.APPROVED);
        repository.save(a);
        repository.save(b);

        List<HitlApprovalEntity> pending =
                repository.findByStatusAndConversationIdOrderByCreatedAtAsc(HitlApprovalStatus.PENDING, "c1");
        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).getId()).isEqualTo(a.getId());
    }

    private static HitlApprovalEntity newEntity(String conv, HitlApprovalStatus status) {
        HitlApprovalEntity e = new HitlApprovalEntity();
        e.setConversationId(conv);
        e.setStatus(status);
        e.setPayloadJson("{}");
        e.setCreatedAt(Instant.now());
        return e;
    }
}
