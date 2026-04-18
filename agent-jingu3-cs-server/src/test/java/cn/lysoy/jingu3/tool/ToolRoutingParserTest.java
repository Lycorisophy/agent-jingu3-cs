package cn.lysoy.jingu3.skill.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ToolRoutingParserTest {

    private final ObjectMapper om = new ObjectMapper();

    @Test
    void parseAskDirect() {
        Optional<ToolRoutingParser.AskRoutePayload> r =
                ToolRoutingParser.parseAskRoute("{\"route\":\"direct\"}", om);
        assertThat(r).isPresent();
        assertThat(r.get().useTool()).isFalse();
    }

    @Test
    void parseAskTool() {
        Optional<ToolRoutingParser.AskRoutePayload> r =
                ToolRoutingParser.parseAskRoute(
                        "{\"route\":\"tool\",\"toolId\":\"calculator\",\"input\":\"1+1\"}", om);
        assertThat(r).isPresent();
        assertThat(r.get().useTool()).isTrue();
        assertThat(r.get().toolId()).isEqualTo("calculator");
        assertThat(r.get().input()).isEqualTo("1+1");
    }

    @Test
    void parseReactAfterMarker() {
        String out =
                "思考中\n"
                        + ToolRoutingParser.JINGU3_JSON_MARKER
                        + "\n{\"action\":\"invoke\",\"toolId\":\"utc_time\",\"input\":\"\"}";
        Optional<ToolRoutingParser.ReactFooterPayload> r = ToolRoutingParser.parseReactFooter(out, om);
        assertThat(r).isPresent();
        assertThat(r.get().action()).isEqualTo("invoke");
        assertThat(r.get().toolId()).isEqualTo("utc_time");
    }

    @Test
    void parseReactDoneFromLastJson() {
        String out = "说明文字\n{\"action\":\"done\"}";
        Optional<ToolRoutingParser.ReactFooterPayload> r = ToolRoutingParser.parseReactFooter(out, om);
        assertThat(r).isPresent();
        assertThat(r.get().action()).isEqualTo("done");
    }
}
