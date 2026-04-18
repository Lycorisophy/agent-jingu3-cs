package cn.lysoy.jingu3.skill.tool;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalculatorToolTest {

    private final CalculatorTool tool = new CalculatorTool();

    @Test
    void evaluatesExpression() throws Exception {
        assertThat(tool.execute("1+2*3")).isEqualTo("7");
        assertThat(tool.execute("(10-3)/2")).isEqualTo("3.5");
    }

    @Test
    void rejectsUnsafeCharacters() {
        assertThatThrownBy(() -> tool.execute("1+alert(1)"))
                .isInstanceOf(ToolExecutionException.class);
    }
}
