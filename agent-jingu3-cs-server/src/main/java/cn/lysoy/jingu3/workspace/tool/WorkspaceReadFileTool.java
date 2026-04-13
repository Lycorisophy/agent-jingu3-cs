package cn.lysoy.jingu3.workspace.tool;

import cn.lysoy.jingu3.component.UserConstants;
import cn.lysoy.jingu3.tool.Jingu3Tool;
import cn.lysoy.jingu3.tool.ToolExecutionException;
import cn.lysoy.jingu3.workspace.WorkspaceFileService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 读取当前用户工作空间内相对路径文件（UTF-8 文本）。
 */
@Component
@ConditionalOnProperty(prefix = "jingu3.workspace", name = "enabled", havingValue = "true", matchIfMissing = true)
public class WorkspaceReadFileTool implements Jingu3Tool {

    private final WorkspaceFileService fileService;
    private final UserConstants userConstants;

    public WorkspaceReadFileTool(WorkspaceFileService fileService, UserConstants userConstants) {
        this.fileService = fileService;
        this.userConstants = userConstants;
    }

    @Override
    public String id() {
        return "workspace_read_file";
    }

    @Override
    public String description() {
        return "读取工作空间内文本文件。input 为相对路径，例如 notes/hello.txt（相对当前用户工作空间根目录）。"
                + "禁止绝对路径与 .. 越界。";
    }

    @Override
    public String execute(String input) throws ToolExecutionException {
        if (input == null || input.isBlank()) {
            throw new ToolExecutionException("workspace_read_file 需要非空相对路径");
        }
        try {
            return fileService.readFile(userConstants.getId(), input.trim());
        } catch (Exception e) {
            throw new ToolExecutionException("读取失败: " + e.getMessage(), e);
        }
    }
}
