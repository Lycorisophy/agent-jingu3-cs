package cn.lysoy.jingu3.rag;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FactTemporalTierTest {

    @Test
    void blank_defaultsShortTerm() {
        assertThat(FactTemporalTier.parseRequired(null)).isEqualTo(FactTemporalTier.SHORT_TERM);
        assertThat(FactTemporalTier.parseRequired("  ")).isEqualTo(FactTemporalTier.SHORT_TERM);
    }

    @Test
    void parsesCaseInsensitive() {
        assertThat(FactTemporalTier.parseRequired("permanent")).isEqualTo(FactTemporalTier.PERMANENT);
    }

    @Test
    void invalid_throws() {
        assertThatThrownBy(() -> FactTemporalTier.parseRequired("WEEK"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
