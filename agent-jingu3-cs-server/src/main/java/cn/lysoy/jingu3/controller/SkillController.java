package cn.lysoy.jingu3.controller;

import cn.lysoy.jingu3.common.api.ApiResult;
import cn.lysoy.jingu3.common.dto.SubscribeSkillRequest;
import cn.lysoy.jingu3.common.vo.SkillListItemVo;
import cn.lysoy.jingu3.common.vo.SkillSubscriptionItemVo;
import cn.lysoy.jingu3.component.UserConstants;
import cn.lysoy.jingu3.skill.SkillService;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * v0.7：技能市场元数据与订阅（{@code skill} / {@code user_skill}）；技能包下载 URL / MinIO 见后续迭代。
 */
@RestController
@RequestMapping("/api/v1/skills")
@ConditionalOnProperty(prefix = "jingu3.skill", name = "api-enabled", havingValue = "true", matchIfMissing = true)
public class SkillController {

    private final SkillService skillService;

    private final UserConstants userConstants;

    public SkillController(SkillService skillService, UserConstants userConstants) {
        this.skillService = skillService;
        this.userConstants = userConstants;
    }

    @GetMapping
    public ApiResult<List<SkillListItemVo>> list() {
        return ApiResult.ok(skillService.listPublicCatalog());
    }

    @GetMapping("/subscriptions")
    public ApiResult<List<SkillSubscriptionItemVo>> mySubscriptions() {
        return ApiResult.ok(skillService.listMySubscriptions(userConstants.getId()));
    }

    @PostMapping("/subscriptions")
    public ApiResult<Void> subscribe(@Valid @RequestBody SubscribeSkillRequest request) {
        skillService.subscribe(userConstants.getId(), request.getSkillId());
        return ApiResult.ok(null);
    }

    @DeleteMapping("/subscriptions/{skillId}")
    public ApiResult<Void> unsubscribe(@PathVariable("skillId") String skillId) {
        skillService.unsubscribe(userConstants.getId(), skillId);
        return ApiResult.ok(null);
    }

    @GetMapping("/{slug}")
    public ApiResult<SkillListItemVo> getBySlug(@PathVariable("slug") String slug) {
        return ApiResult.ok(skillService.getPublicBySlug(slug));
    }
}
