package cn.lysoy.jingu3;

import cn.lysoy.jingu3.config.Jingu3Properties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * jingu3 CS 智能体服务端入口。
 * <p>史诗①：意图路由 + 八大行动模式契约；详见 docs/v0.1/ 与 docs/计划/开发路线图.md。</p>
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(Jingu3Properties.class)
public class Jingu3Application {

    public static void main(String[] args) {
        SpringApplication.run(Jingu3Application.class, args);
    }
}
