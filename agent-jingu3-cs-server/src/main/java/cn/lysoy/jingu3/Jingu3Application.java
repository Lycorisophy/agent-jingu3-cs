package cn.lysoy.jingu3;

import cn.lysoy.jingu3.config.Jingu3Properties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * jingu3 CS 智能体<strong>服务端入口</strong>（Spring Boot）。
 * <p><strong>运行时主干</strong>：HTTP/WebSocket 对话由 {@link cn.lysoy.jingu3.service.context.chat.ChatService} /
 * {@link cn.lysoy.jingu3.service.context.chat.ChatStreamService} 编排，经 {@link cn.lysoy.jingu3.service.guard.routing.IntentRouter} 与
 * {@link cn.lysoy.jingu3.service.guard.ModeRegistry} 驱动<strong>八大行动模式</strong>；送模前上下文经
 * {@link cn.lysoy.jingu3.service.context.prepare.UserPromptPreparationService}；提示词由 {@link cn.lysoy.jingu3.service.prompt.PromptAssembly} 拼装；
 * 可选工具由 {@link cn.lysoy.jingu3.skill.tool.ToolRegistry} 注册。</p>
 * <p>文档单一事实来源：{@code docs/计划/开发路线图.md}；本阶段默认排除 Redis 自动配置，按需再打开。</p>
 */
@SpringBootApplication(exclude = {RedisAutoConfiguration.class})
@EnableScheduling
@EnableConfigurationProperties(Jingu3Properties.class)
public class Jingu3Application {

    public static void main(String[] args) {
        SpringApplication.run(Jingu3Application.class, args);
    }
}
