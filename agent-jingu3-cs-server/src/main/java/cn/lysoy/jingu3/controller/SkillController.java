package cn.lysoy.jingu3.controller;

import cn.lysoy.jingu3.common.api.ApiResult;
import cn.lysoy.jingu3.common.vo.SkillListItemVo;
import cn.lysoy.jingu3.skill.SkillService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * v0.7：技能市场元数据只读列表（{@code skill} 表）；下载 URL / MinIO 见后续迭代。
 */
@RestController
@RequestMapping("/api/v1/skills")
@ConditionalOnProperty(prefix = "jingu3.skill", name = "api-enabled", havingValue = "true", matchIfMissing = true)
public class SkillController {

    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    @GetMapping
    public ApiResult<List<SkillListItemVo>> list() {
        return ApiResult.ok(skillService.listPublicCatalog());
    }

    @GetMapping("/{slug}")
    public ApiResult<SkillListItemVo> getBySlug(@PathVariable("slug") String slug) {
        return ApiResult.ok(skillService.getPublicBySlug(slug));
    }
}
