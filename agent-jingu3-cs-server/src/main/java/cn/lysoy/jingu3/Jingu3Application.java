package cn.lysoy.jingu3;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * jingu3 CS 智能体服务端入口。
 * <p>史诗①：意图路由 + 八大行动模式契约；详见 docs/v0.1/ 与 docs/计划/开发路线图.md。</p>
 */
@SpringBootApplication
public class Jingu3Application {

    public static void main(String[] args) {
        SpringApplication.run(Jingu3Application.class, args);
    }
}
