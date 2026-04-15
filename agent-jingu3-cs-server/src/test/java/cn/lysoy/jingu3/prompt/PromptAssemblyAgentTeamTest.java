package cn.lysoy.jingu3.prompt;

import cn.lysoy.jingu3.common.constant.PromptTemplates;
import cn.lysoy.jingu3.config.Jingu3Properties;
import cn.lysoy.jingu3.engine.ActionMode;
import cn.lysoy.jingu3.engine.ExecutionContext;
import cn.lysoy.jingu3.engine.routing.RoutingSource;
import cn.lysoy.jingu3.tool.CalculatorTool;
import cn.lysoy.jingu3.tool.ToolRegistry;
import cn.lysoy.jingu3.tool.UtcTimeTool;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PromptAssemblyAgentTeamTest {

    private static PromptAssembly assembly() {
        ToolRegistry registry = new ToolRegistry(List.of(new CalculatorTool(), new UtcTimeTool()));
        return new PromptAssembly(registry, new Jingu3Properties());
    }

    @Test
    void specialistToolRouter_containsSubBoundaryAndAskRouterInstruction() {
        ExecutionContext ctx =
                ExecutionContext.minimal("001", "user", "用户问什么", ActionMode.AGENT_TEAM, RoutingSource.CLIENT_EXPLICIT);
        PromptAssembly pa = assembly();
        String p = pa.buildAgentTeamSpecialistToolRouterPrompt(ctx, "子任务段落");
        assertThat(p).contains(PromptTemplates.AGENT_TEAM_SUB_BOUNDARY);
        assertThat(p).contains(PromptTemplates.ASK_TOOL_ROUTER_INSTRUCTION);
        assertThat(p).contains("子任务段落");
    }

    @Test
    void specialistDirect_containsBoundaryAndAskSystem() {
        ExecutionContext ctx =
                ExecutionContext.minimal("001", "user", "u1", ActionMode.AGENT_TEAM, RoutingSource.RULE);
        PromptAssembly pa = assembly();
        String p = pa.buildAgentTeamSpecialistDirectPrompt(ctx, "子任务段落");
        assertThat(p).contains(PromptTemplates.AGENT_TEAM_SUB_BOUNDARY);
        assertThat(p).contains(PromptTemplates.ASK_SYSTEM);
    }
}
