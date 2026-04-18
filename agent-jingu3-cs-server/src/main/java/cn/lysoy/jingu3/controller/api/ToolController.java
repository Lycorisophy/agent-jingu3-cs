package cn.lysoy.jingu3.controller.api;

import cn.lysoy.jingu3.common.api.ApiResult;
import cn.lysoy.jingu3.common.vo.ToolListItemVo;
import cn.lysoy.jingu3.skill.tool.ToolRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * v0.7：内置工具只读目录（id、description、riskLevel），供客户端展示与策略编排。
 */
@RestController
@RequestMapping("/api/v1/tools")
@ConditionalOnProperty(prefix = "jingu3.tool", name = "catalog-api-enabled", havingValue = "true", matchIfMissing = true)
public class ToolController {

    private final ToolRegistry toolRegistry;

    public ToolController(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    @GetMapping
    public ApiResult<List<ToolListItemVo>> listBuiltInTools() {
        return ApiResult.ok(toolRegistry.buildCatalogList());
    }
}
