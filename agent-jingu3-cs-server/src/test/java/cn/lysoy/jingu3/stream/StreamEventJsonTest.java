package cn.lysoy.jingu3.service.context.stream;

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

    @Test
    void serializesToolResult() throws Exception {
        ObjectMapper om = new ObjectMapper();
        StreamEvent e = StreamEvent.toolResult("calculator", "42");
        String json = om.writeValueAsString(e);
        assertThat(json).contains("\"type\":\"TOOL_RESULT\"");
        assertThat(json).contains("\"toolId\":\"calculator\"");
        assertThat(json).contains("\"toolOutput\":\"42\"");
    }
}
