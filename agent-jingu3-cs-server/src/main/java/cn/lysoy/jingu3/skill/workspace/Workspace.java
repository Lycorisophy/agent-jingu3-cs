package cn.lysoy.jingu3.skill.workspace;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作空间描述（Phase 1 无 DB，由磁盘目录推导）。
 */
@Data
@Builder
public class Workspace {

    private String id;

    private String userId;

    private String rootPath;

    private String name;

    private LocalDateTime createdAt;

    private LocalDateTime lastActiveAt;

    /** ACTIVE, ARCHIVED */
    private String status;

    private Long quotaMb;
}
