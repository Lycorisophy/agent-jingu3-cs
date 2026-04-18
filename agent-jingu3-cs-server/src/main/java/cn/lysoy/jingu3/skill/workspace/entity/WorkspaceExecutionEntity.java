package cn.lysoy.jingu3.skill.workspace.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@TableName("workspace_execution")
@Getter
@Setter
public class WorkspaceExecutionEntity {

    @TableId(type = IdType.INPUT)
    private String id;

    @TableField("workspace_id")
    private String workspaceId;

    @TableField("user_id")
    private String userId;

    private String language;

    @TableField("run_mode")
    private String runMode;

    @TableField("relative_path")
    private String relativePath;

    @TableField("code_hash")
    private String codeHash;

    @TableField("stdout_snippet")
    private String stdoutSnippet;

    @TableField("stderr_snippet")
    private String stderrSnippet;

    @TableField("exit_code")
    private Integer exitCode;

    @TableField("duration_ms")
    private Long durationMs;

    private Boolean success;

    @TableField("error_type")
    private String errorType;

    @TableField("timed_out")
    private Boolean timedOut;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
