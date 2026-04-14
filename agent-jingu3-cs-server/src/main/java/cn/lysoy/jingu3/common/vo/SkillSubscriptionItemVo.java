package cn.lysoy.jingu3.common.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 用户技能订阅与技能元数据（不含对象存储路径）。
 */
@Getter
@Setter
public class SkillSubscriptionItemVo {

    private String subscriptionId;

    private String userId;

    private String subscriptionStatus;

    private String localVersion;

    private String serverVersion;

    /** 最近一次同步时间 */
    private LocalDateTime lastSyncAt;

    private boolean externalSkill;

    private String externalPath;

    private String skillId;

    private String name;

    private String slug;

    private String description;

    private String version;

    private String category;

    private String tags;

    private String triggerWords;

    private String iconUrl;

    private boolean official;

    private String skillStatus;
}
