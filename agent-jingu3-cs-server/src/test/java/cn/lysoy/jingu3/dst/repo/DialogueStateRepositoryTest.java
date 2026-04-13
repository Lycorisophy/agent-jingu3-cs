package cn.lysoy.jingu3.dst.repo;

import cn.lysoy.jingu3.dst.entity.DialogueStateEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(
        properties = {
            "spring.jpa.hibernate.ddl-auto=validate",
            "spring.flyway.enabled=true"
        })
class DialogueStateRepositoryTest {

    @Autowired
    private DialogueStateRepository repository;

    @Test
    void findByConversationId() {
        DialogueStateEntity e = new DialogueStateEntity();
        e.setConversationId("conv-1");
        e.setSchemaVersion("1");
        e.setStateJson("{}");
        e.setRevision(0L);
        e.setUpdatedAt(Instant.now());
        repository.save(e);

        assertThat(repository.findByConversationId("conv-1")).isPresent();
    }
}
