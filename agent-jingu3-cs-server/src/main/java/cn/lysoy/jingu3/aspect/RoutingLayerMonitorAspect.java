package cn.lysoy.jingu3.aspect;

import cn.lysoy.jingu3.engine.routing.RoutingDecision;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 路由层监控：耗时与结果摘要日志（不改变业务降级逻辑，降级仍在各实现内完成）。
 */
@Slf4j
@Aspect
@Component
@Order(10)
public class RoutingLayerMonitorAspect {

    @Around("execution(* cn.lysoy.jingu3.engine.routing.IntentRouter.resolve(..))")
    public Object aroundIntentRouter(ProceedingJoinPoint pjp) throws Throwable {
        return monitor("IntentRouter.resolve", pjp);
    }

    @Around("execution(* cn.lysoy.jingu3.engine.routing.ModelIntentClassifier.classify(..))")
    public Object aroundClassifier(ProceedingJoinPoint pjp) throws Throwable {
        return monitor("ModelIntentClassifier.classify", pjp);
    }

    private static Object monitor(String label, ProceedingJoinPoint pjp) throws Throwable {
        long t0 = System.nanoTime();
        try {
            Object out = pjp.proceed();
            double ms = (System.nanoTime() - t0) / 1_000_000.0;
            log.debug("{} durationMs={} result={}", label, String.format("%.2f", ms), summarize(out));
            return out;
        } catch (Throwable ex) {
            double ms = (System.nanoTime() - t0) / 1_000_000.0;
            log.warn("{} durationMs={} failed: {}", label, String.format("%.2f", ms), ex.toString());
            throw ex;
        }
    }

    private static String summarize(Object out) {
        if (out == null) {
            return "null";
        }
        if (out instanceof RoutingDecision d) {
            return d.getMode() + "/" + d.getSource();
        }
        return out.toString();
    }
}
