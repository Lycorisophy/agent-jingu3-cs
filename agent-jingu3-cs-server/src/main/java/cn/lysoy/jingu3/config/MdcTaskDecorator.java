package cn.lysoy.jingu3.config;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

/**
 * 将调用线程的 {@link MDC} 上下文复制到 {@code chatStreamExecutor} 工作线程，保证流式异步日志带请求/追踪 ID。
 */
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, String> context = MDC.getCopyOfContextMap();
        return () -> {
            if (context != null && !context.isEmpty()) {
                MDC.setContextMap(context);
            }
            try {
                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    }
}
