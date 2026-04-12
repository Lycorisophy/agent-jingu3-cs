package cn.lysoy.jingu3.engine.mode;

import cn.lysoy.jingu3.common.constant.EngineMessages;
import cn.lysoy.jingu3.engine.ActionModeHandler;
import cn.lysoy.jingu3.engine.ExecutionContext;
import org.springframework.stereotype.Component;

/**
 * v0.1 占位：非 Ask/ReAct 模式统一提示「后续版本实现」。
 */
@Component
public class StubActionModeHandler implements ActionModeHandler {

    @Override
    public String execute(ExecutionContext context) {
        return String.format(
                EngineMessages.STUB_REPLY_TEMPLATE,
                context.selectedMode().name(),
                context.username(),
                context.userId()
        );
    }
}
