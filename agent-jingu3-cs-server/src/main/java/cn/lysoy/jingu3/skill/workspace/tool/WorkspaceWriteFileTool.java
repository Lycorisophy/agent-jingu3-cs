package cn.lysoy.jingu3.skill.workspace.tool;

import cn.lysoy.jingu3.common.enums.ToolRiskLevel;
import cn.lysoy.jingu3.component.UserConstants;
import cn.lysoy.jingu3.skill.tool.Jingu3Tool;
import cn.lysoy.jingu3.skill.tool.ToolExecutionException;
import cn.lysoy.jingu3.service.workspace.WorkspaceFileService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 写入工作空间内文本文件；input 为 JSON：{"path":"相对路径","content":"UTF-8 文本"}。
 */
@Component
@ConditionalOnProperty(prefix = "jingu3.workspace", name = "enabled", havingValue = "true", matchIfMissing = true)
public class WorkspaceWriteFileTool implements Jingu3Tool {

    private final WorkspaceFileService fileService;
    private final UserConstants userConstants;
    private final ObjectMapper objectMapper;

    public WorkspaceWriteFileTool(
            WorkspaceFileService fileService, UserConstants userConstants, ObjectMapper objectMapper) {
        this.fileService = fileService;
        this.userConstants = userConstants;
        this.objectMapper = objectMapper;
    }

    @Override
    public String id() {
        return "workspace_write_file";
    }

    @Override
    public String description() {
        return "写入工作空间文本文件。input 为 JSON 字符串，字段 path（相对路径）、content（文件内容，可空字符串）。"
                + "示例：{\"path\":\"notes/a.txt\",\"content\":\"hello\"}";
    }

    @Override
    public ToolRiskLevel riskLevel() {
        return ToolRiskLevel.MEDIUM;
    }

    @Override
    public String execute(String input) throws ToolExecutionException {
        if (input == null || input.isBlank()) {
            throw new ToolExecutionException("workspace_write_file 需要 JSON 参数");
        }
        try {
            JsonNode n = objectMapper.readTree(input.trim());
            String path = n.path("path").asText("");
            if (path.isBlank()) {
                throw new ToolExecutionException("JSON 缺少非空 path");
            }
            String content = n.path("content").asText("");
            fileService.writeFile(userConstants.getId(), path.trim(), content);
            return "已写入: " + path.trim();
        } catch (ToolExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new ToolExecutionException("写入失败: " + e.getMessage(), e);
        }
    }
}
