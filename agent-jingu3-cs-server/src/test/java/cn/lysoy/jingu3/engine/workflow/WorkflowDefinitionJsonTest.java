package cn.lysoy.jingu3.engine.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowDefinitionJsonTest {

    @Test
    void demoWfTool_deserializesToolNode() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        try (var in = new ClassPathResource("workflows/demo-wf-tool.json").getInputStream()) {
            WorkflowDefinition def = mapper.readValue(in, WorkflowDefinition.class);
            assertThat(def.getId()).isEqualTo("demo-wf-tool");
            assertThat(def.getNodes()).hasSize(3);
            assertThat(def.getNodes().get(0).isToolNode()).isFalse();
            assertThat(def.getNodes().get(1).isToolNode()).isTrue();
            assertThat(def.getNodes().get(1).getToolId()).isEqualTo("calculator");
            assertThat(def.getNodes().get(1).getInstruction()).isEqualTo("2+3");
        }
    }

    @Test
    void demoWf1_missingTypeDefaultsToLlm() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        try (var in = new ClassPathResource("workflows/demo-wf-1.json").getInputStream()) {
            WorkflowDefinition def = mapper.readValue(in, WorkflowDefinition.class);
            assertThat(def.getNodes().get(0).isToolNode()).isFalse();
            assertThat(def.getNodes().get(0).getType()).isNull();
        }
    }
}
