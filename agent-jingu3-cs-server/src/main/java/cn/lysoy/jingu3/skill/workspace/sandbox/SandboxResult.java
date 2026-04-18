package cn.lysoy.jingu3.skill.workspace.sandbox;

import lombok.Builder;
import lombok.Data;

/**
 * 沙箱进程执行结果（stdout/stderr 合并或分开展示由实现决定）。
 */
@Data
@Builder
public class SandboxResult {

    private boolean success;

    private String stdout;

    private String stderr;

    private int exitCode;

    private long executionTimeMs;

    private String errorType;

    private boolean timeout;
}
