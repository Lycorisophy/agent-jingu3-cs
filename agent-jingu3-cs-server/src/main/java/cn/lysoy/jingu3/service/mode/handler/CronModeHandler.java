package cn.lysoy.jingu3.service.mode.handler;

import cn.lysoy.jingu3.common.constant.EngineMessages;
import cn.lysoy.jingu3.config.Jingu3Properties;
import cn.lysoy.jingu3.service.mode.ActionModeHandler;
import cn.lysoy.jingu3.service.guard.ExecutionContext;
import cn.lysoy.jingu3.service.context.stream.StreamEventSink;
import org.springframework.stereotype.Component;

/**
 * <strong>指南 §8 Cron</strong>（八大行动模式之一；<strong>非对话主路径</strong>）：对话内仅作「定时意图已记录」类说明，
 * 使用配置项 {@code jingu3.cron.demo-schedule} 展示演示 Cron 表达式，<strong>不</strong>触发 Quartz / Spring {@code @Scheduled}。
 * 真实调度、存储与幂等触发见路线图 Cron 独立模块与 {@code cron} 包内轮询等实现。
 */
@Component
public class CronModeHandler implements ActionModeHandler {

    /** 部署可配置的演示调度串，便于文档/截图对齐 */
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
