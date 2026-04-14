package cn.lysoy.jingu3.common.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * 沙箱执行历史条目（摘要字段）。
 */
@Getter
@Setter
public class WorkspaceExecutionItemVo {

    private String id;

    private String language;

    private String runMode;

    private String relativePath;

    private String codeHash;

    private boolean success;

    private int exitCode;

    private long durationMs;

    private String errorType;

    private boolean timedOut;

    private String createdAt;
}
