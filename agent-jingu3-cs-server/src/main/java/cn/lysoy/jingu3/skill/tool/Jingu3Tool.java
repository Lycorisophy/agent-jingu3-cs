package cn.lysoy.jingu3.skill.tool;

import cn.lysoy.jingu3.common.enums.ToolRiskLevel;

/**
 * <strong>技能与工具系统 — 内置可调用工具契约</strong>：与 LangChain4j {@code Tool} 注解模型解耦，统一由
 * {@link ToolRegistry} 在启动期收集 Spring Bean、构建目录并供 Ask/ReAct/Plan/AgentTeam 等模式<strong>经 JSON 路由调用</strong>。
 * <p>企业级扩展（动态工具、市场下发、客户端执行）见路线图 v0.7 与 {@code docs/workspace/skill-system-design.md}；
 * 当前内置工具均为低风险 {@link ToolRiskLevel#LOW} 示例或工作空间配套能力。</p>
 */
public interface Jingu3Tool {

    /** 稳定 ID，写入路由 JSON 的 {@code toolId} */
    String id();

    /** 进入 Ask/ReAct 提示词的工具说明（多行 Markdown 列表项之一） */
    String description();

    /** 风险等级；覆盖为 MEDIUM/HIGH 的工具有待客户端与策略层二次确认 */
    default ToolRiskLevel riskLevel() {
        return ToolRiskLevel.LOW;
    }

    /**
     * 同步执行工具。
     *
     * @param input 单字符串参数；无参工具可为空
     * @return 供模型作为观察或汇总依据的文本（勿含控制字符）
     */
    String execute(String input) throws ToolExecutionException;
}
