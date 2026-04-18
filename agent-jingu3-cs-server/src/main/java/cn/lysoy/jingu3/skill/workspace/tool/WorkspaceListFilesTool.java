package cn.lysoy.jingu3.skill.workspace.tool;

import cn.lysoy.jingu3.component.UserConstants;
import cn.lysoy.jingu3.skill.tool.Jingu3Tool;
import cn.lysoy.jingu3.skill.tool.ToolExecutionException;
import cn.lysoy.jingu3.service.workspace.WorkspaceFileService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 列出工作空间子目录下的直接子项名称。
 */
@Component
@ConditionalOnProperty(prefix = "jingu3.workspace", name = "enabled", havingValue = "true", matchIfMissing = true)
public class WorkspaceListFilesTool implements Jingu3Tool {

    private final WorkspaceFileService fileService;
    private final UserConstants userConstants;

    public WorkspaceListFilesTool(WorkspaceFileService fileService, UserConstants userConstants) {
        this.fileService = fileService;
        this.userConstants = userConstants;
    }

    @Override
    public String id() {
        return "workspace_list_files";
    }

    @Override
    public String description() {
        return "列出工作空间某目录下的文件与子目录名（仅一层）。input 为相对目录路径，空或 . 表示用户工作空间根。";
    }

    @Override
    public String execute(String input) throws ToolExecutionException {
        String rel = input == null || input.isBlank() ? "." : input.trim();
        try {
            List<String> names = fileService.listDirectory(userConstants.getId(), rel);
            return names.isEmpty() ? "(空目录)" : String.join("\n", names);
        } catch (Exception e) {
            throw new ToolExecutionException("列出目录失败: " + e.getMessage(), e);
        }
    }
}
