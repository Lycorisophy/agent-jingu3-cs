package cn.lysoy.jingu3.engine.mode;

import cn.lysoy.jingu3.common.constant.EngineMessages;
import cn.lysoy.jingu3.config.Jingu3Properties;
import cn.lysoy.jingu3.engine.ActionModeHandler;
import cn.lysoy.jingu3.engine.ExecutionContext;
import cn.lysoy.jingu3.stream.StreamEventSink;
import org.springframework.stereotype.Component;

/**
 * 指南 §8 Cron：定时任务、存储与触发管线；本类仅为对话侧「意图说明」占位，展示默认 cron 表达式，
 * 不连接 Quartz / Spring {@code @Scheduled}。完整能力见路线图「Cron 模块」设计草案。
 */
@Component
public class CronModeHandler implements ActionModeHandler {

    private final String demoSchedule;

    public CronModeHandler(Jingu3Properties properties) {
        this.demoSchedule = properties.getCron().getDemoSchedule();
    }

    /**
     * 返回带演示调度与用户输入摘要的固定模板文案，不调用 LLM。
     */
    @Override
    public String execute(ExecutionContext context) {
        return String.format(EngineMessages.CRON_DEMO_REPLY, demoSchedule, context.llmInput());
    }

    @Override
    public void stream(ExecutionContext context, StreamEventSink sink) {
        sink.stepBegin(1, "cron");
        sink.block(execute(context));
        sink.stepEnd(1);
        sink.done();
    }
}
