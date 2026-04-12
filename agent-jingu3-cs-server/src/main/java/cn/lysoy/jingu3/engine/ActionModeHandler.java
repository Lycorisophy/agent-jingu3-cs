package cn.lysoy.jingu3.engine;

/**
 * 单一行动模式的执行契约。
 */
@FunctionalInterface
public interface ActionModeHandler {

    /**
     * 根据上下文生成助手回复文本。
     */
    String execute(ExecutionContext context);
}
