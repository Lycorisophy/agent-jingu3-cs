package cn.lysoy.jingu3.controller.api.bpmn;

import cn.lysoy.jingu3.bpmn.BpmnWorkflowService;
import cn.lysoy.jingu3.common.api.ApiResult;
import cn.lysoy.jingu3.common.dto.bpmn.BpmnStartProcessRequest;
import cn.lysoy.jingu3.common.enums.ErrorCode;
import cn.lysoy.jingu3.common.vo.bpmn.BpmnDeployResultVo;
import cn.lysoy.jingu3.common.vo.bpmn.HistoricProcessInstanceItemVo;
import cn.lysoy.jingu3.common.vo.bpmn.ProcessDefinitionItemVo;
import cn.lysoy.jingu3.common.vo.bpmn.ProcessInstanceStartVo;
import cn.lysoy.jingu3.common.vo.bpmn.TaskItemVo;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Flowable BPMN：部署、定义列表、启动实例、任务与历史（开发期无鉴权，生产需收敛）。
 */
@RestController
@RequestMapping("/api/v1/bpmn")
@ConditionalOnProperty(prefix = "jingu3.bpmn", name = "api-enabled", havingValue = "true", matchIfMissing = true)
public class BpmnWorkflowController {

    private final BpmnWorkflowService bpmnWorkflowService;

    public BpmnWorkflowController(BpmnWorkflowService bpmnWorkflowService) {
        this.bpmnWorkflowService = bpmnWorkflowService;
    }

    @PostMapping(value = "/deploy", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResult<BpmnDeployResultVo> deploy(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "deploymentName", required = false) String deploymentName) {
        try {
            return ApiResult.ok(bpmnWorkflowService.deployMultipart(file, deploymentName));
        } catch (Exception ex) {
            return ApiResult.fail(ErrorCode.BAD_REQUEST, ex.getMessage() != null ? ex.getMessage() : "deploy failed");
        }
    }

    @GetMapping("/process-definitions")
    public ApiResult<List<ProcessDefinitionItemVo>> listDefinitions() {
        return ApiResult.ok(bpmnWorkflowService.listLatestProcessDefinitions());
    }

    @PostMapping("/process-instances/start")
    public ApiResult<ProcessInstanceStartVo> start(@Valid @RequestBody BpmnStartProcessRequest request) {
        try {
            return ApiResult.ok(bpmnWorkflowService.startProcess(request));
        } catch (IllegalArgumentException ex) {
            return ApiResult.fail(ErrorCode.BAD_REQUEST, ex.getMessage());
        } catch (Exception ex) {
            return ApiResult.fail(
                    ErrorCode.INTERNAL_ERROR, ex.getMessage() != null ? ex.getMessage() : "start failed");
        }
    }

    @GetMapping("/process-instances/running")
    public ApiResult<List<Map<String, String>>> running() {
        return ApiResult.ok(bpmnWorkflowService.listRunningProcessInstances());
    }

    @GetMapping("/tasks")
    public ApiResult<List<TaskItemVo>> tasks() {
        return ApiResult.ok(bpmnWorkflowService.listTasks());
    }

    @PostMapping("/tasks/{taskId}/complete")
    public ApiResult<Void> complete(
            @PathVariable("taskId") String taskId, @RequestBody(required = false) Map<String, Object> variables) {
        try {
            bpmnWorkflowService.completeTask(taskId, variables);
            return ApiResult.ok(null);
        } catch (Exception ex) {
            return ApiResult.fail(ErrorCode.BAD_REQUEST, ex.getMessage() != null ? ex.getMessage() : "complete failed");
        }
    }

    @GetMapping("/history/process-instances")
    public ApiResult<List<HistoricProcessInstanceItemVo>> history(
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return ApiResult.ok(bpmnWorkflowService.listRecentHistoric(limit));
    }
}
