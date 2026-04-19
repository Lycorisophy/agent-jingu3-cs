package cn.lysoy.jingu3.common.dto.bpmn;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * 启动 Flowable 流程实例。
 */
@Getter
@Setter
public class BpmnStartProcessRequest {

    /** 流程定义 key（BPMN process id） */
    @NotBlank
    private String processDefinitionKey;

    /** 流程变量（试运行 / 调试） */
    private Map<String, Object> variables = new HashMap<>();
}
