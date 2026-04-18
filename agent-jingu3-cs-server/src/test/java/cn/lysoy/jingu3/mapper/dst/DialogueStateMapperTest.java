package cn.lysoy.jingu3.mapper.dst;

import cn.lysoy.jingu3.Jingu3Application;
import cn.lysoy.jingu3.util.UtcTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = Jingu3Application.class,
        properties = {"jingu3.cron.scheduler-enabled=false"})
@Transactional
class DialogueStateMapperTest {

    @Autowired
    private DialogueStateMapper dialogueStateMapper;

    @Test
    void findByConversationId() {
        DialogueStateEntity e = new DialogueStateEntity();
        e.setConversationId("conv-1");
        e.setSchemaVersion("1");
        e.setStateJson("{}");
        e.setRevision(0L);
        e.setUpdatedAt(UtcTime.nowLocalDateTime());
        dialogueStateMapper.insert(e);

        assertThat(dialogueStateMapper.selectByConversationId("conv-1")).isNotNull();
    }
}
