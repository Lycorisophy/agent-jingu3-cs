package cn.lysoy.jingu3.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus：分页插件与 Mapper 扫描（持久化已从 JPA 迁移）。
 * <p>扫描范围仅限 {@code *.mapper} 包，避免将业务接口（如 {@code WorkspaceFileService}）误注册为 Mapper Bean。</p>
 */
@Configuration
@MapperScan({
        "cn.lysoy.jingu3.chat.mapper",
        "cn.lysoy.jingu3.cron.mapper",
        "cn.lysoy.jingu3.dst.mapper",
        "cn.lysoy.jingu3.hitl.mapper",
        "cn.lysoy.jingu3.memory.mapper",
        "cn.lysoy.jingu3.skill.mapper",
        "cn.lysoy.jingu3.user.mapper",
        "cn.lysoy.jingu3.workspace.mapper"
})
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return interceptor;
    }
}
