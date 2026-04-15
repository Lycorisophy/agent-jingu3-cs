package cn.lysoy.jingu3.engine.support;

import cn.lysoy.jingu3.config.Jingu3Properties;
import cn.lysoy.jingu3.engine.ActionMode;
import cn.lysoy.jingu3.engine.ExecutionContext;
import cn.lysoy.jingu3.engine.routing.RoutingSource;
import cn.lysoy.jingu3.engine.workflow.WorkflowNode;
import cn.lysoy.jingu3.prompt.PromptAssembly;
import cn.lysoy.jingu3.tool.CalculatorTool;
import cn.lysoy.jingu3.tool.ToolRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ToolStepServiceTest {

    @Mock
    private ChatLanguageModel chat;

    @Mock
    private PromptAssembly prompts;

    private ToolRegistry toolRegistry;
    private Jingu3Properties properties;
    private ToolStepService service;

    @BeforeEach
    void setUp() {
        toolRegistry = ToolRegistry.createForTest(List.of(new CalculatorTool()));
        properties = new Jingu3Properties();
        properties.getTool().setEnabled(true);
        service = new ToolStepService(chat, prompts, toolRegistry, properties, new ObjectMapper());
    }

    @Test
    void runWorkflowToolNode_executesCalculator() {
        WorkflowNode node = new WorkflowNode();
        node.setType("TOOL");
        node.setToolId("calculator");
        node.setInstruction("3+4");
        String out = service.runWorkflowToolNode(node, "acc", null);
        assertThat(out).contains("7");
    }

    @Test
    void runPlanSubtask_directSkipsSecondGeneratePath() {
        properties.getTool().setEnabled(true);
        ExecutionContext ctx =
                new ExecutionContext(
                        "001",
                        "u",
                        "c",
                        "msg",
                        ActionMode.PLAN_AND_EXECUTE,
                        RoutingSource.CLIENT_EXPLICIT,
                        List.of(),
                        null,
                        null);
        when(prompts.buildPlanSubtaskToolRouterPrompt(any(), eq("sub"), eq("msg"), eq(1)))
                .thenReturn("router");
        when(prompts.buildSubtaskExecutePrompt(any(), eq("sub"), eq("msg"), eq(1))).thenReturn("p1");
        when(chat.generate(anyString())).thenReturn("{\"route\":\"direct\"}", "done");
        String r = service.runPlanSubtask(ctx, "sub", "msg", 1, null);
        assertThat(r).isEqualTo("done");
        verify(prompts).buildSubtaskExecutePrompt(any(), eq("sub"), eq("msg"), eq(1));
    }
}
