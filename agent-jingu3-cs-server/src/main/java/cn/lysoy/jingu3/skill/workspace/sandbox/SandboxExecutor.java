package cn.lysoy.jingu3.skill.workspace.sandbox;

/**
 * 在用户工作空间目录下执行代码（进程级沙箱，Phase 2 MVP）。
 */
public interface SandboxExecutor {

    /**
     * 执行内联代码。
     *
     * @param language {@code python} 或 {@code javascript}
     */
    SandboxResult execute(String userId, String language, String code, int timeoutSeconds);

    /**
     * 执行工作空间内已有脚本文件（相对路径）。
     */
    SandboxResult executeFile(String userId, String language, String relativePath, int timeoutSeconds);
}
