package cn.lysoy.jingu3.bpmn;

import cn.lysoy.jingu3.common.dto.bpmn.BpmnStartProcessRequest;
import cn.lysoy.jingu3.common.vo.bpmn.BpmnDeployResultVo;
import cn.lysoy.jingu3.common.vo.bpmn.HistoricProcessInstanceItemVo;
import cn.lysoy.jingu3.common.vo.bpmn.ProcessDefinitionItemVo;
import cn.lysoy.jingu3.common.vo.bpmn.ProcessInstanceStartVo;
import cn.lysoy.jingu3.common.vo.bpmn.TaskItemVo;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Flowable 流程部署与运行门面（供 REST 使用）。
 */
@Service
public class BpmnWorkflowService {

    private static final DateTimeFormatter ISO_UTC =
            DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC);

    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;

    public BpmnWorkflowService(
            RepositoryService repositoryService,
            RuntimeService runtimeService,
            TaskService taskService,
            HistoryService historyService) {
        this.repositoryService = repositoryService;
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.historyService = historyService;
    }

    public BpmnDeployResultVo deploy(String deploymentName, InputStream bpmnInput, String resourceName)
            throws IOException {
        Deployment deployment =
                repositoryService.createDeployment().name(deploymentName).addInputStream(resourceName, bpmnInput).deploy();
        BpmnDeployResultVo vo = new BpmnDeployResultVo();
        vo.setDeploymentId(deployment.getId());
        vo.setDeploymentName(deployment.getName());
        List<ProcessDefinition> defs =
                repositoryService.createProcessDefinitionQuery().deploymentId(deployment.getId()).list();
        for (ProcessDefinition def : defs) {
            ProcessDefinitionItemVo item = new ProcessDefinitionItemVo();
            item.setId(def.getId());
            item.setKey(def.getKey());
            item.setName(def.getName());
            item.setVersion(def.getVersion());
            vo.getDefinitions().add(item);
        }
        return vo;
    }

    public BpmnDeployResultVo deployMultipart(MultipartFile file, String deploymentName) throws IOException {
        String name = deploymentName != null && !deploymentName.isBlank()
                ? deploymentName.trim()
                : (file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload.bpmn20.xml");
        String resName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "process.bpmn20.xml";
        try (InputStream in = file.getInputStream()) {
            return deploy(name, in, resName);
        }
    }

    public List<ProcessDefinitionItemVo> listLatestProcessDefinitions() {
        List<ProcessDefinition> defs = repositoryService.createProcessDefinitionQuery().latestVersion().orderByProcessDefinitionKey().asc().list();
        List<ProcessDefinitionItemVo> out = new ArrayList<>();
        for (ProcessDefinition def : defs) {
            ProcessDefinitionItemVo item = new ProcessDefinitionItemVo();
            item.setId(def.getId());
            item.setKey(def.getKey());
            item.setName(def.getName());
            item.setVersion(def.getVersion());
            out.add(item);
        }
        return out;
    }

    public ProcessInstanceStartVo startProcess(BpmnStartProcessRequest request) {
        if (request.getProcessDefinitionKey() == null || request.getProcessDefinitionKey().isBlank()) {
            throw new IllegalArgumentException("processDefinitionKey 不能为空");
        }
        Map<String, Object> vars = request.getVariables() == null ? Map.of() : new HashMap<>(request.getVariables());
        ProcessInstance pi =
                runtimeService.startProcessInstanceByKey(request.getProcessDefinitionKey().trim(), vars);
        ProcessInstanceStartVo vo = new ProcessInstanceStartVo();
        vo.setProcessInstanceId(pi.getId());
        vo.setProcessDefinitionId(pi.getProcessDefinitionId());
        vo.setVariables(resolveVariablesAfterStart(pi.getId()));
        return vo;
    }

    /** 同步结束的流程在运行时表中无变量，需从历史表读取。 */
    private Map<String, Object> resolveVariablesAfterStart(String processInstanceId) {
        boolean stillRunning =
                runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult() != null;
        if (stillRunning) {
            Map<String, Object> live = runtimeService.getVariables(processInstanceId);
            return live != null ? new HashMap<>(live) : new HashMap<>();
        }
        Map<String, Object> m = new HashMap<>();
        List<HistoricVariableInstance> list =
                historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstanceId).list();
        for (HistoricVariableInstance v : list) {
            m.put(v.getVariableName(), v.getValue());
        }
        return m;
    }

    public List<Map<String, String>> listRunningProcessInstances() {
        return runtimeService.createProcessInstanceQuery().list().stream()
                .map(pi -> {
                    Map<String, String> m = new HashMap<>();
                    m.put("processInstanceId", pi.getId());
                    m.put("processDefinitionId", pi.getProcessDefinitionId());
                    return m;
                })
                .collect(Collectors.toList());
    }

    public List<TaskItemVo> listTasks() {
        List<Task> tasks = taskService.createTaskQuery().orderByTaskCreateTime().desc().list();
        List<TaskItemVo> out = new ArrayList<>();
        for (Task t : tasks) {
            TaskItemVo vo = new TaskItemVo();
            vo.setId(t.getId());
            vo.setName(t.getName());
            vo.setProcessInstanceId(t.getProcessInstanceId());
            vo.setAssignee(t.getAssignee());
            out.add(vo);
        }
        return out;
    }

    public void completeTask(String taskId, Map<String, Object> variables) {
        if (variables == null || variables.isEmpty()) {
            taskService.complete(taskId);
        } else {
            taskService.complete(taskId, variables);
        }
    }

    public List<HistoricProcessInstanceItemVo> listRecentHistoric(int limit) {
        int lim = Math.min(Math.max(limit, 1), 100);
        return historyService.createHistoricProcessInstanceQuery().finished().orderByProcessInstanceEndTime().desc().listPage(0, lim).stream()
                .map(h -> {
                    HistoricProcessInstanceItemVo vo = new HistoricProcessInstanceItemVo();
                    vo.setId(h.getId());
                    vo.setProcessDefinitionId(h.getProcessDefinitionId());
                    vo.setProcessDefinitionKey(h.getProcessDefinitionKey());
                    vo.setDurationMs(h.getDurationInMillis());
                    if (h.getEndTime() != null) {
                        vo.setEndTime(ISO_UTC.format(Instant.ofEpochMilli(h.getEndTime().getTime())));
                    }
                    return vo;
                })
                .collect(Collectors.toList());
    }
}
