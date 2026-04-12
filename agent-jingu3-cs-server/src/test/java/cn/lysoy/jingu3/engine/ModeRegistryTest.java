package cn.lysoy.jingu3.engine;

import cn.lysoy.jingu3.engine.mode.AgentTeamModeHandler;
import cn.lysoy.jingu3.engine.mode.AskModeHandler;
import cn.lysoy.jingu3.engine.mode.CronModeHandler;
import cn.lysoy.jingu3.engine.mode.HumanInLoopModeHandler;
import cn.lysoy.jingu3.engine.mode.PlanAndExecuteModeHandler;
import cn.lysoy.jingu3.engine.mode.ReActModeHandler;
import cn.lysoy.jingu3.engine.mode.StateTrackingModeHandler;
import cn.lysoy.jingu3.engine.mode.WorkflowModeHandler;
import cn.lysoy.jingu3.engine.routing.RoutingSource;
import cn.lysoy.jingu3.engine.workflow.WorkflowDefinitionRegistry;
import cn.lysoy.jingu3.prompt.PromptAssembly;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

class ModeRegistryTest {

    @Test
    void planAndExecute_returnsStructuredMvpOutput() {
        ChatLanguageModel chat = Mockito.mock(ChatLanguageModel.class);
        Mockito.when(chat.generate(Mockito.anyString())).thenReturn("ok");
        StreamingChatLanguageModel streaming = Mockito.mock(StreamingChatLanguageModel.class);
        var prompts = new PromptAssembly();
        WorkflowDefinitionRegistry wfReg = Mockito.mock(WorkflowDefinitionRegistry.class);
        Mockito.when(wfReg.get(Mockito.any())).thenReturn(null);

        var registry = new ModeRegistry(
                new AskModeHandler(chat, streaming, prompts),
                new ReActModeHandler(chat, prompts, 4),
                new PlanAndExecuteModeHandler(chat, prompts, 8, true),
                new WorkflowModeHandler(chat, prompts, wfReg),
                new AgentTeamModeHandler(chat, prompts),
                new CronModeHandler("0 0 9 * * MON-FRI"),
                new StateTrackingModeHandler(),
                new HumanInLoopModeHandler()
        );

        var ctx = ExecutionContext.minimal("001", "user", "hi", ActionMode.PLAN_AND_EXECUTE, RoutingSource.RULE);
        String out = registry.get(ActionMode.PLAN_AND_EXECUTE).execute(ctx);
        assertThat(out).contains("【计划】");
        assertThat(out).contains("【分步结果】");
    }
}
