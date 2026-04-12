package cn.lysoy.jingu3.component;

import cn.lysoy.jingu3.common.dto.ChatRequest;
import cn.lysoy.jingu3.common.exception.ServiceException;
import cn.lysoy.jingu3.engine.ActionMode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatRequestValidatorTest {

    private final ChatRequestValidator validator = new ChatRequestValidator();

    @Test
    void workflowWithoutWorkflowId_whenModePlanContainsWorkflow_throws() {
        ChatRequest r = new ChatRequest();
        r.setMessage("m");
        r.setModePlan(List.of("WORKFLOW"));
        assertThatThrownBy(() -> validator.validate(r)).isInstanceOf(ServiceException.class);
    }

    @Test
    void workflowWithWorkflowId_ok() {
        ChatRequest r = new ChatRequest();
        r.setMessage("m");
        r.setModePlan(List.of("WORKFLOW"));
        r.setWorkflowId("wf-1");
        assertThatCode(() -> validator.validate(r)).doesNotThrowAnyException();
    }

    @Test
    void requireWorkflowIdIfWorkflowMode_throwsWhenMissing() {
        ChatRequest r = new ChatRequest();
        r.setMessage("m");
        assertThatThrownBy(() -> validator.requireWorkflowIdIfWorkflowMode(ActionMode.WORKFLOW, r))
                .isInstanceOf(ServiceException.class);
    }
}
