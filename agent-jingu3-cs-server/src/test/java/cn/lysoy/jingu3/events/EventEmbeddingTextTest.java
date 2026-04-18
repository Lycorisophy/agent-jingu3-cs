package cn.lysoy.jingu3.events;

import cn.lysoy.jingu3.rag.entity.EventEntryEntity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EventEmbeddingTextTest {

    @Test
    void forEmbedding_concatNonEmpty() {
        EventEntryEntity e = new EventEntryEntity();
        e.setAction("开会");
        e.setResult("通过方案");
        assertThat(EventEmbeddingText.forEmbedding(e)).contains("开会").contains("通过方案");
    }
}
