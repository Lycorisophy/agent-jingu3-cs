package cn.lysoy.jingu3.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 内置工具注册表：启动时收集全部 {@link Jingu3Tool} Bean，按 id 查找并执行。
 */
@Slf4j
@Component
public class ToolRegistry {

    private final Map<String, Jingu3Tool> byId;

    public ToolRegistry(List<Jingu3Tool> tools) {
        Map<String, Jingu3Tool> m = new LinkedHashMap<>();
        for (Jingu3Tool t : tools) {
            Jingu3Tool prev = m.putIfAbsent(t.id(), t);
            if (prev != null) {
                throw new IllegalStateException("duplicate tool id: " + t.id());
            }
        }
        this.byId = Map.copyOf(m);
    }

    public Collection<Jingu3Tool> all() {
        return byId.values();
    }

    /** 供提示词注入：Markdown 无序列表 */
    public String buildCatalogMarkdown() {
        return byId.values().stream()
                .map(t -> "- `" + t.id() + "`：" + t.description())
                .collect(Collectors.joining("\n"));
    }

    public String execute(String toolId, String input) throws ToolExecutionException {
        Jingu3Tool tool = byId.get(toolId);
        if (tool == null) {
            throw new ToolExecutionException("未知工具: " + toolId);
        }
        long t0 = System.nanoTime();
        try {
            String out = tool.execute(input == null ? "" : input);
            log.info("tool ok id={} ms={}", toolId, (System.nanoTime() - t0) / 1_000_000L);
            return out;
        } catch (ToolExecutionException e) {
            log.warn("tool failed id={}: {}", toolId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.warn("tool failed id={}: {}", toolId, e.toString());
            throw new ToolExecutionException("工具执行异常: " + e.getMessage(), e);
        }
    }
}
