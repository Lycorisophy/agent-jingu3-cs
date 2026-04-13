package cn.lysoy.jingu3.common.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * ES 检索命中（摘要字段 + 分数）。
 */
@Getter
@Setter
public class EventSearchHitVo {

    private double score;

    private String eventId;

    private String userId;

    private String action;

    private String result;

    private String timestamp;
}
