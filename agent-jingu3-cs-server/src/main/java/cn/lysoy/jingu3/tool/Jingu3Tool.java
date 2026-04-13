package cn.lysoy.jingu3.tool;

/**
 * 内置可调用工具契约：与 LangChain4j {@code Tool} 解耦，由 {@link ToolRegistry} 统一注册与执行。
 */
public interface Jingu3Tool {

    /** 稳定 ID，写入路由 JSON 的 {@code toolId} */
    String id();

    /** 进入 Ask/ReAct 提示词的工具说明（多行 Markdown 列表项之一） */
    String description();

    /**
     * 同步执行工具。
     *
     * @param input 单字符串参数；无参工具可为空
     * @return 供模型作为观察或汇总依据的文本（勿含控制字符）
     */
    String execute(String input) throws ToolExecutionException;
}
