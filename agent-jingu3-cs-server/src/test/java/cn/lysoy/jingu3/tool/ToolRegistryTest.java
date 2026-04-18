package cn.lysoy.jingu3.skill.tool;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ToolRegistryTest {

    @Test
    void executesRegisteredTools() throws Exception {
        ToolRegistry reg = ToolRegistry.createForTest(List.of(new CalculatorTool(), new UtcTimeTool()));
        assertThat(reg.execute("calculator", "3+4")).isEqualTo("7");
        assertThat(reg.execute("utc_time", "")).matches("\\d{4}-\\d{2}-\\d{2}T.*Z");
    }

    @Test
    void unknownToolThrows() {
        ToolRegistry reg = ToolRegistry.createForTest(List.of(new CalculatorTool()));
        assertThatThrownBy(() -> reg.execute("missing", "")).isInstanceOf(ToolExecutionException.class);
    }
}
