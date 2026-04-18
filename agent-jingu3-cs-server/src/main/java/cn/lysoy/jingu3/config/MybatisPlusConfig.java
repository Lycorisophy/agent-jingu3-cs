package cn.lysoy.jingu3.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus：分页插件与 Mapper 扫描（持久化已从 JPA 迁移）。
 * <p>扫描根包 {@code cn.lysoy.jingu3.mapper}（含子包），避免将业务接口（如 {@code WorkspaceFileService}）误注册为 Mapper。</p>
 */
@Configuration
@MapperScan("cn.lysoy.jingu3.mapper")
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return interceptor;
    }
}
