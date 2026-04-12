package cn.lysoy.jingu3.engine.orchestration;

import cn.lysoy.jingu3.common.dto.ChatRequest;
import cn.lysoy.jingu3.component.UserConstants;
import cn.lysoy.jingu3.engine.ModeRegistry;
import cn.lysoy.jingu3.engine.mode.AgentTeamModeHandler;
import cn.lysoy.jingu3.engine.mode.AskModeHandler;
import cn.lysoy.jingu3.engine.mode.CronModeHandler;
import cn.lysoy.jingu3.engine.mode.HumanInLoopModeHandler;
import cn.lysoy.jingu3.engine.mode.PlanAndExecuteModeHandler;
import cn.lysoy.jingu3.engine.mode.ReActModeHandler;
import cn.lysoy.jingu3.engine.mode.StateTrackingModeHandler;
import cn.lysoy.jingu3.engine.mode.WorkflowModeHandler;
import cn.lysoy.jingu3.engine.workflow.WorkflowDefinitionRegistry;
import cn.lysoy.jingu3.prompt.PromptAssembly;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class ModePlanExecutorTest {

    @Test
    void runsTwoSteps() {
        ChatLanguageModel chat = Mockito.mock(ChatLanguageModel.class);
        when(chat.generate(Mockito.anyString())).thenReturn("gen");
        StreamingChatLanguageModel streaming = Mockito.mock(StreamingChatLanguageModel.class);
        var prompts = new PromptAssembly();
        WorkflowDefinitionRegistry wfReg = Mockito.mock(WorkflowDefinitionRegistry.class);
        Mockito.when(wfReg.get(Mockito.any())).thenReturn(null);
        ModeRegistry registry = new ModeRegistry(
                new AskModeHandler(chat, streaming, prompts),
                new ReActModeHandler(chat, prompts, 4),
                new PlanAndExecuteModeHandler(chat, prompts, 8, true),
                new WorkflowModeHandler(chat, prompts, wfReg),
                new AgentTeamModeHandler(chat, prompts),
                new CronModeHandler("0 0 9 * * MON-FRI"),
                new StateTrackingModeHandler(),
                new HumanInLoopModeHandler()
        );
        var executor = new ModePlanExecutor(registry);
        UserConstants users = Mockito.mock(UserConstants.class);
        when(users.getId()).thenReturn("001");
        when(users.getUsername()).thenReturn("user");

        ChatRequest req = new ChatRequest();
        req.setMessage("hello");
        req.setModePlan(List.of("ASK", "REACT"));

        var vo = executor.execute(req, users);

        assertThat(vo.getPlanSteps()).hasSize(2);
        assertThat(vo.getPlanSteps().get(0).getMode()).isEqualTo("ASK");
        assertThat(vo.getPlanSteps().get(1).getMode()).isEqualTo("REACT");
        assertThat(vo.getActionMode()).isEqualTo("REACT");
        assertThat(vo.getRoutingSource()).isEqualTo("CLIENT_EXPLICIT");
    }

    @Test
    void invalidStepName_degradesToReact() {
        ChatLanguageModel chat = Mockito.mock(ChatLanguageModel.class);
        when(chat.generate(Mockito.anyString())).thenReturn("gen");
        StreamingChatLanguageModel streaming = Mockito.mock(StreamingChatLanguageModel.class);
        var prompts = new PromptAssembly();
        WorkflowDefinitionRegistry wfReg = Mockito.mock(WorkflowDefinitionRegistry.class);
        Mockito.when(wfReg.get(Mockito.any())).thenReturn(null);
        ModeRegistry registry = new ModeRegistry(
                new AskModeHandler(chat, streaming, prompts),
                new ReActModeHandler(chat, prompts, 4),
                new PlanAndExecuteModeHandler(chat, prompts, 8, true),
                new WorkflowModeHandler(chat, prompts, wfReg),
                new AgentTeamModeHandler(chat, prompts),
                new CronModeHandler("0 0 9 * * MON-FRI"),
                new StateTrackingModeHandler(),
                new HumanInLoopModeHandler()
        );
        var executor = new ModePlanExecutor(registry);
        UserConstants users = Mockito.mock(UserConstants.class);
        when(users.getId()).thenReturn("001");
        when(users.getUsername()).thenReturn("user");

        ChatRequest req = new ChatRequest();
        req.setMessage("x");
        req.setModePlan(List.of("BAD_MODE_NAME"));

        var vo = executor.execute(req, users);

        assertThat(vo.getPlanSteps()).hasSize(1);
        assertThat(vo.getPlanSteps().get(0).getMode()).isEqualTo("REACT");
    }
}
