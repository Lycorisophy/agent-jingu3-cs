package cn.lysoy.jingu3.engine.mode;

import cn.lysoy.jingu3.common.constant.EngineMessages;
import cn.lysoy.jingu3.common.constant.PromptFragments;
import cn.lysoy.jingu3.engine.ActionModeHandler;
import cn.lysoy.jingu3.engine.ExecutionContext;
import cn.lysoy.jingu3.stream.StreamEventSink;
import org.springframework.stereotype.Component;

/**
 * 指南 §10 Human-in-the-Loop：人在环审批、待办队列与决策回流。对话模式仍为静态说明 + 用户输入摘要；
 * 持久化审批队列见 {@code hitl_approval} 与 {@code HitlController}（{@code /api/v1/hitl}）。
 */
@Component
public class HumanInLoopModeHandler implements ActionModeHandler {

    /**
     * 不调用 LLM；提示用户当前请求处于待审批状态。
     */
    @Override
    public String execute(ExecutionContext context) {
        return EngineMessages.HUMAN_IN_LOOP_PENDING
                + PromptFragments.PARAGRAPH_BREAK
                + "用户原话摘要："
                + context.llmInput();
    }

    @Override
    public void stream(ExecutionContext context, StreamEventSink sink) {
        sink.stepBegin(1, "human_in_loop");
        sink.block(execute(context));
        sink.stepEnd(1);
        sink.done();
    }
}
