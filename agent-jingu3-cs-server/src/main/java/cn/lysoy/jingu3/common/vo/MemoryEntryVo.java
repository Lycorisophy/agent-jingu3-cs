package cn.lysoy.jingu3.common.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * 记忆条目返回（v0.6 M1）。
 */
@Getter
@Setter
public class MemoryEntryVo {

    private Long id;

    private String userId;

    private String kind;

    private String summary;

    private String body;

    private String factTag;

    private String createdAt;

    private String updatedAt;
}
