package cn.lysoy.jingu3.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 为 {@link cn.lysoy.jingu3.service.ChatStreamService} 提供专用线程池：LLM 流式回调与多步阻塞调用在后台执行，
 * 避免长时间占用 Tomcat 工作线程导致吞吐下降。
 */
@Configuration
public class ChatStreamExecutorConfig {

    /**
     * Bean 名 {@code chatStreamExecutor}，供 {@code @Qualifier} 注入。
     */
    @Bean(name = "chatStreamExecutor")
    public TaskExecutor chatStreamExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("chat-stream-");
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.initialize();
        return executor;
    }
}
