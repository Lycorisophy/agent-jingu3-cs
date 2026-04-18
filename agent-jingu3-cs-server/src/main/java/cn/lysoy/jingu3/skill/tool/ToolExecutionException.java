package cn.lysoy.jingu3.skill.tool;

/**
 * 工具执行失败（参数非法、运行时错误等），由 Handler 捕获后写入 Observation 或降级提示。
 */
public class ToolExecutionException extends Exception {

    public ToolExecutionException(String message) {
        super(message);
    }

    public ToolExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
