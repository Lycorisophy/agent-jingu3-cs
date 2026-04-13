package cn.lysoy.jingu3.component;

import cn.lysoy.jingu3.common.dto.ChatRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;

class ChatRequestValidatorTest {

    private final ChatRequestValidator validator = new ChatRequestValidator();

    @Test
    void workflowWithoutWorkflowId_whenModePlanContainsWorkflow_doesNotThrow() {
        ChatRequest r = new ChatRequest();
        r.setMessage("m");
        r.setModePlan(List.of("WORKFLOW"));
        assertThatCode(() -> validator.validate(r)).doesNotThrowAnyException();
    }

    @Test
    void workflowWithWorkflowId_ok() {
        ChatRequest r = new ChatRequest();
        r.setMessage("m");
        r.setModePlan(List.of("WORKFLOW"));
        r.setWorkflowId("wf-1");
        assertThatCode(() -> validator.validate(r)).doesNotThrowAnyException();
    }
}
