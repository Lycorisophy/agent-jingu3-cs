package cn.lysoy.jingu3.service.mode.handler;

import cn.lysoy.jingu3.common.constant.EngineMessages;
import cn.lysoy.jingu3.common.constant.PromptFragments;
import cn.lysoy.jingu3.service.mode.ActionModeHandler;
import cn.lysoy.jingu3.service.guard.ExecutionContext;
import cn.lysoy.jingu3.service.context.stream.StreamEventSink;
import org.springframework.stereotype.Component;

/**
 * <strong>指南 §10 Human-in-the-Loop</strong>（八大行动模式之一）：对话内返回<strong>静态待审批说明</strong>并附带用户输入摘要，
 * 不调用 LLM 做自动通过/拒绝。持久化审批单、REST 查询与后续「决策回流到对话」见 Flyway {@code hitl_approval}、
 * {@code HitlController}（{@code /api/v1/hitl}）及路线图 HITL 史诗。
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
