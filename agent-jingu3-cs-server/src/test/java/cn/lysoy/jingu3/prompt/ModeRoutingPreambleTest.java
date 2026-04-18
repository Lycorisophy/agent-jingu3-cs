package cn.lysoy.jingu3.service.prompt;

import cn.lysoy.jingu3.service.guard.ActionMode;
import cn.lysoy.jingu3.service.guard.ExecutionContext;
import cn.lysoy.jingu3.service.guard.routing.RoutingSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ModeRoutingPreambleTest {

    @Test
    void build_explicitGuard_containsSwitchExplanation() {
        ExecutionContext ctx =
                ExecutionContext.minimal("1", "u", "hi", ActionMode.ASK, RoutingSource.EXPLICIT_GUARD);
        String p = ModeRoutingPreamble.build(ctx);
        assertThat(p).contains("显式选择较重模式").contains("已自动切换").contains("ASK");
    }
}
