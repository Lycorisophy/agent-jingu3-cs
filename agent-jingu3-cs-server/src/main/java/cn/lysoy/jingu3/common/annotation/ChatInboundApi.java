package cn.lysoy.jingu3.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记聊天入口 Controller，由 {@link cn.lysoy.jingu3.aspect.ChatInboundAspect} 统一做限流与访问日志。
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ChatInboundApi {
}
