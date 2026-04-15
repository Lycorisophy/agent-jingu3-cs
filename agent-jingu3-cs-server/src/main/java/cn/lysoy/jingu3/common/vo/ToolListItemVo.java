package cn.lysoy.jingu3.common.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * 内置工具目录项（{@code GET /api/v1/tools}）；不含执行参数 schema。
 */
@Getter
@Setter
public class ToolListItemVo {

    private String id;

    private String description;

    /** {@link cn.lysoy.jingu3.common.enums.ToolRiskLevel} 名，如 LOW、MEDIUM、HIGH */
    private String riskLevel;
}
