package cn.lysoy.jingu3.workspace.tool;

import cn.lysoy.jingu3.component.UserConstants;
import cn.lysoy.jingu3.tool.Jingu3Tool;
import cn.lysoy.jingu3.tool.ToolExecutionException;
import cn.lysoy.jingu3.workspace.sandbox.SandboxExecutor;
import cn.lysoy.jingu3.workspace.sandbox.SandboxResult;
import cn.lysoy.jingu3.workspace.WorkspaceManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 在工作空间内执行 Python / JavaScript（进程沙箱）。需 {@code jingu3.workspace.sandbox.enabled=true}。
 */
@Component
@ConditionalOnBean(WorkspaceManager.class)
@ConditionalOnProperty(prefix = "jingu3.workspace.sandbox", name = "enabled", havingValue = "true")
public class WorkspaceExecuteCodeTool implements Jingu3Tool {

    private final SandboxExecutor sandboxExecutor;

    private final UserConstants userConstants;

    private final ObjectMapper objectMapper;

    public WorkspaceExecuteCodeTool(SandboxExecutor sandboxExecutor, UserConstants userConstants, ObjectMapper objectMapper) {
        this.sandboxExecutor = sandboxExecutor;
        this.userConstants = userConstants;
        this.objectMapper = objectMapper;
    }

    @Override
    public String id() {
        return "workspace_execute_code";
    }

    @Override
    public String description() {
        return "在用户工作空间内执行 Python 或 JavaScript（进程沙箱，有超时）。"
                + " input 为 JSON：{\"language\":\"python|javascript\",\"code\":\"...\"} "
                + "或 {\"language\":\"python\",\"relativePath\":\"脚本相对路径\"}；可选 \"timeoutSeconds\": 正整数。";
    }

    @Override
    public String execute(String input) throws ToolExecutionException {
        if (input == null || input.isBlank()) {
            throw new ToolExecutionException("workspace_execute_code 需要 JSON 参数");
        }
        try {
            JsonNode root = objectMapper.readTree(input.trim());
            String language = text(root, "language");
            if (language == null || language.isBlank()) {
                throw new ToolExecutionException("缺少 language（python 或 javascript）");
            }
            int timeoutSec = root.path("timeoutSeconds").asInt(0);
            String userId = userConstants.getId();
            SandboxResult result;
            if (root.hasNonNull("relativePath") && !root.get("relativePath").asText().isBlank()) {
                String rel = root.get("relativePath").asText().trim();
                result = sandboxExecutor.executeFile(userId, language, rel, timeoutSec);
            } else if (root.hasNonNull("code")) {
                String code = root.get("code").asText();
                result = sandboxExecutor.execute(userId, language, code, timeoutSec);
            } else {
                throw new ToolExecutionException("需提供 code 或 relativePath");
            }
            return objectMapper.writeValueAsString(result);
        } catch (ToolExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new ToolExecutionException("workspace_execute_code 失败: " + e.getMessage(), e);
        }
    }

    private static String text(JsonNode root, String field) {
        JsonNode n = root.get(field);
        return n == null || n.isNull() ? null : n.asText();
    }
}
