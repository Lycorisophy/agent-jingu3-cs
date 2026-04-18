package cn.lysoy.jingu3.service.context.stream;

/**
 * 流式通道抽象：把一次对话过程拆成 {@link StreamEvent} 序列，由 SSE 与 WebSocket 各自适配写入客户端。
 * <p>约定：正常结束必须调用 {@link #done()}；异常调用 {@link #error(String)} 并停止发送。</p>
 */
public interface StreamEventSink {

    /**
     * 首包元数据：路由得到的模式与来源，便于前端展示标签或调试。
     *
     * @param actionMode     如 ASK、REACT
     * @param routingSource  如 RULE、MODEL、CLIENT_EXPLICIT
     * @param userId         当前用户 id
     * @param username       当前用户名
     */
    void meta(String actionMode, String routingSource, String userId, String username);

    /**
     * 增量文本片段（典型用于 Ask 的打字机效果）。
     *
     * @param delta 本段新增字符，可能为多个 token 合并（取决于 Ollama / LangChain4j 回调粒度）
     */
    void token(String delta);

    /**
     * 多步管线中某一步开始（如 ReAct 第 k 步、工作流节点 id）。
     *
     * @param step  从 1 递增的步序号，或调用方约定的序号
     * @param label 人类可读或机器可读标签（如 {@code react_step_2}、节点 id）
     */
    void stepBegin(int step, String label);

    /**
     * 与 {@link #stepBegin(int, String)} 成对出现，标记该步结束。
     */
    void stepEnd(int step);

    /**
     * 无法或尚未做 token 级流式时，整段输出一次性下发（阻塞 generate 的一步）。
     */
    void block(String text);

    /**
     * 内置工具执行完成（典型在 Ask 流式路径中，位于最终 TOKEN 流之前）。
     *
     * @param toolId    注册表中的工具 id
     * @param toolOutput 工具返回文本（宜为短文本）
     */
    void toolResult(String toolId, String toolOutput);

    /**
     * 本连接上的对话流正常结束；实现类通常会关闭 SSE 或 WebSocket。
     */
    void done();

    /**
     * 管线失败时调用，之后不应再发 TOKEN/BLOCK。
     *
     * @param message 可读错误信息
     */
    void error(String message);
}
