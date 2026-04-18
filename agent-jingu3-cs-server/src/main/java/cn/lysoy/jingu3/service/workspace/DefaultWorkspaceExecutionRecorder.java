package cn.lysoy.jingu3.service.workspace;

import cn.lysoy.jingu3.config.Jingu3Properties;
import cn.lysoy.jingu3.skill.workspace.service.WorkspaceExecutionRecorder;
import cn.lysoy.jingu3.skill.workspace.constant.WorkspaceExecutionModes;
import cn.lysoy.jingu3.skill.workspace.entity.WorkspaceExecutionEntity;
import cn.lysoy.jingu3.mapper.workspace.WorkspaceExecutionMapper;
import cn.lysoy.jingu3.skill.workspace.sandbox.SandboxResult;
import cn.lysoy.jingu3.util.hash.Sha256Hex;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "jingu3.workspace", name = "execution-history-enabled", havingValue = "true", matchIfMissing = true)
public class DefaultWorkspaceExecutionRecorder implements WorkspaceExecutionRecorder {

    private final WorkspaceMetadataService workspaceMetadataService;

    private final WorkspaceExecutionMapper workspaceExecutionMapper;

    private final Jingu3Properties properties;

    public DefaultWorkspaceExecutionRecorder(
            WorkspaceMetadataService workspaceMetadataService,
            WorkspaceExecutionMapper workspaceExecutionMapper,
            Jingu3Properties properties) {
        this.workspaceMetadataService = workspaceMetadataService;
        this.workspaceExecutionMapper = workspaceExecutionMapper;
        this.properties = properties;
    }

    @Override
    public void recordInline(String userId, String language, String code, SandboxResult result) {
        try {
            int maxChars = properties.getWorkspace().getExecutionHistorySnippetMaxChars();
            String wid = workspaceMetadataService.ensureWorkspaceRow(userId);
            WorkspaceExecutionEntity row = baseRow(wid, userId, language, WorkspaceExecutionModes.INLINE, result);
            row.setRelativePath(null);
            row.setCodeHash(Sha256Hex.ofUtf8(code == null ? "" : code));
            row.setStdoutSnippet(snippet(result.getStdout(), maxChars));
            row.setStderrSnippet(snippet(result.getStderr(), maxChars));
            workspaceExecutionMapper.insert(row);
        } catch (Exception e) {
            log.warn("workspace execution history (inline): {}", e.toString());
        }
    }

    @Override
    public void recordFile(String userId, String language, String relativePath, SandboxResult result) {
        try {
            int maxChars = properties.getWorkspace().getExecutionHistorySnippetMaxChars();
            String wid = workspaceMetadataService.ensureWorkspaceRow(userId);
            WorkspaceExecutionEntity row = baseRow(wid, userId, language, WorkspaceExecutionModes.FILE, result);
            row.setRelativePath(relativePath);
            row.setCodeHash(Sha256Hex.ofUtf8("FILE:" + language + ":" + (relativePath == null ? "" : relativePath)));
            row.setStdoutSnippet(snippet(result.getStdout(), maxChars));
            row.setStderrSnippet(snippet(result.getStderr(), maxChars));
            workspaceExecutionMapper.insert(row);
        } catch (Exception e) {
            log.warn("workspace execution history (file): {}", e.toString());
        }
    }

    private static WorkspaceExecutionEntity baseRow(
            String workspaceId,
            String userId,
            String language,
            String runMode,
            SandboxResult result) {
        WorkspaceExecutionEntity row = new WorkspaceExecutionEntity();
        row.setId(UUID.randomUUID().toString());
        row.setWorkspaceId(workspaceId);
        row.setUserId(userId);
        row.setLanguage(language == null ? "" : language);
        row.setRunMode(runMode);
        row.setExitCode(result.getExitCode());
        row.setDurationMs(result.getExecutionTimeMs());
        row.setSuccess(result.isSuccess());
        row.setErrorType(result.getErrorType());
        row.setTimedOut(result.isTimeout());
        row.setCreatedAt(LocalDateTime.now());
        return row;
    }

    private static String snippet(String s, int maxChars) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        if (maxChars <= 0 || s.length() <= maxChars) {
            return s;
        }
        return s.substring(0, maxChars) + "\n...[truncated]";
    }
}
