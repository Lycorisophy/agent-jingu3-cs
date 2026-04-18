package cn.lysoy.jingu3.skill.workspace.service;

import cn.lysoy.jingu3.skill.workspace.sandbox.SandboxResult;

/**
 * 沙箱执行结果落库（失败不影响主流程）。
 */
public interface WorkspaceExecutionRecorder {

    void recordInline(String userId, String language, String code, SandboxResult result);

    void recordFile(String userId, String language, String relativePath, SandboxResult result);
}
