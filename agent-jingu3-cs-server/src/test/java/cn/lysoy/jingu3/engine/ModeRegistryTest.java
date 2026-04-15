package cn.lysoy.jingu3.engine;

import cn.lysoy.jingu3.config.Jingu3Properties;
import cn.lysoy.jingu3.engine.mode.AgentTeamModeHandler;
import cn.lysoy.jingu3.engine.mode.AskModeHandler;
import cn.lysoy.jingu3.engine.mode.CronModeHandler;
import cn.lysoy.jingu3.engine.mode.HumanInLoopModeHandler;
import cn.lysoy.jingu3.engine.mode.PlanAndExecuteModeHandler;
import cn.lysoy.jingu3.engine.mode.ReActModeHandler;
import cn.lysoy.jingu3.engine.mode.StateTrackingModeHandler;
import cn.lysoy.jingu3.engine.mode.WorkflowModeHandler;
import cn.lysoy.jingu3.engine.routing.RoutingSource;
import cn.lysoy.jingu3.engine.support.ToolStepService;
import cn.lysoy.jingu3.engine.workflow.WorkflowDefinitionRegistry;
import cn.lysoy.jingu3.prompt.PromptAssembly;
import cn.lysoy.jingu3.tool.CalculatorTool;
import cn.lysoy.jingu3.tool.ToolRegistry;
import cn.lysoy.jingu3.tool.UtcTimeTool;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class ModeRegistryTest {

    @Test
    void planAndExecute_returnsStructuredMvpOutput() {
        ChatLanguageModel chat = Mockito.mock(ChatLanguageModel.class);
        when(chat.generate(anyString()))
                .thenReturn("1. step one\n2. step two")
                .thenReturn("r1")
                .thenReturn("r2");
        StreamingChatLanguageModel streaming = Mockito.mock(StreamingChatLanguageModel.class);
        ToolRegistry toolRegistry = new ToolRegistry(List.of(new CalculatorTool(), new UtcTimeTool()));
        Jingu3Properties props = new Jingu3Properties();
        props.getTool().setEnabled(false);
        props.getCron().setDemoSchedule("0 0 9 * * MON-FRI");
        ObjectMapper objectMapper = new ObjectMapper();
        PromptAssembly prompts = new PromptAssembly(toolRegistry, props);
        ToolStepService toolStepService = new ToolStepService(chat, prompts, toolRegistry, props, objectMapper);
        WorkflowDefinitionRegistry wfReg = Mockito.mock(WorkflowDefinitionRegistry.class);
        when(wfReg.get(Mockito.any())).thenReturn(null);

        AskModeHandler ask = new AskModeHandler(chat, streaming, prompts, props, toolRegistry, objectMapper);
        var registry =
                new ModeRegistry(
                        ask,
                        new ReActModeHandler(chat, prompts, props, toolRegistry, objectMapper, 4),
                        new PlanAndExecuteModeHandler(chat, prompts, toolStepService, 8, true),
                        new WorkflowModeHandler(chat, prompts, wfReg, toolStepService),
                        new AgentTeamModeHandler(chat, prompts, ask, props, 1),
                        new CronModeHandler(props),
                        new StateTrackingModeHandler(),
                        new HumanInLoopModeHandler());

        var ctx = ExecutionContext.minimal("001", "user", "hi", ActionMode.PLAN_AND_EXECUTE, RoutingSource.RULE);
        String out = registry.get(ActionMode.PLAN_AND_EXECUTE).execute(ctx);
        assertThat(out).contains("【计划】");
        assertThat(out).contains("【分步结果】");
    }
}
