package cn.lysoy.jingu3.engine;

import cn.lysoy.jingu3.engine.mode.AskModeHandler;
import cn.lysoy.jingu3.engine.mode.ReActModeHandler;
import cn.lysoy.jingu3.engine.mode.StubActionModeHandler;
import cn.lysoy.jingu3.engine.routing.RoutingSource;
import cn.lysoy.jingu3.prompt.PromptAssembly;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

class ModeRegistryTest {

    @Test
    void stubUsedForUnsupportedModes() {
        ChatLanguageModel chat = Mockito.mock(ChatLanguageModel.class);
        Mockito.when(chat.generate(Mockito.anyString())).thenReturn("ok");
        var prompts = new PromptAssembly();
        var ask = new AskModeHandler(chat, prompts);
        var react = new ReActModeHandler(chat, prompts);
        var stub = new StubActionModeHandler();
        var registry = new ModeRegistry(ask, react, stub);

        var ctx = ExecutionContext.minimal("001", "user", "hi", ActionMode.PLAN_AND_EXECUTE, RoutingSource.RULE);
        String out = registry.get(ActionMode.PLAN_AND_EXECUTE).execute(ctx);
        assertThat(out).contains("Stub");
        assertThat(out).contains("PLAN_AND_EXECUTE");
    }
}
