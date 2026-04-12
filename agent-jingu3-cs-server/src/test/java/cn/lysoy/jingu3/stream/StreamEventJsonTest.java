package cn.lysoy.jingu3.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StreamEventJsonTest {

    @Test
    void serializesMeta() throws Exception {
        ObjectMapper om = new ObjectMapper();
        StreamEvent e = StreamEvent.meta("ASK", "RULE", "001", "u");
        String json = om.writeValueAsString(e);
        assertThat(json).contains("\"type\":\"META\"");
        assertThat(json).contains("\"actionMode\":\"ASK\"");
    }
}
