package cn.lysoy.jingu3.events;

import cn.lysoy.jingu3.rag.entity.EventEntryEntity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EventSearchFilterTest {

    @Test
    void keywordMatches_singleToken() {
        EventEntryEntity e = new EventEntryEntity();
        e.setAction("公司宣布破产");
        e.setResult("股价下跌");
        assertThat(EventSearchService.keywordMatches(e, "破产")).isTrue();
        assertThat(EventSearchService.keywordMatches(e, "缺失")).isFalse();
    }

    @Test
    void keywordMatches_multiTokenAnd() {
        EventEntryEntity e = new EventEntryEntity();
        e.setAction("alpha beta");
        assertThat(EventSearchService.keywordMatches(e, "alpha beta")).isTrue();
        assertThat(EventSearchService.keywordMatches(e, "alpha gamma")).isFalse();
    }

    @Test
    void keywordMatches_blankMeansPass() {
        EventEntryEntity e = new EventEntryEntity();
        e.setAction("x");
        assertThat(EventSearchService.keywordMatches(e, "  ")).isTrue();
    }
}
