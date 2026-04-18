package cn.lysoy.jingu3.skill.tool;

import cn.lysoy.jingu3.common.vo.ToolListItemVo;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <strong>工具注册表</strong>（技能与工具系统运行时核心）：Spring 注入全部 {@link Jingu3Tool} 实现，
 * 在 {@link PostConstruct} 阶段建立 id → 实例映射；供 {@link cn.lysoy.jingu3.service.prompt.PromptAssembly} 生成工具目录、
 * 供各 {@link cn.lysoy.jingu3.service.mode.ActionModeHandler} 通过 {@link #execute(String, String)} 同步调用。
 * <p>重复 id 在启动期即失败，避免运行期非确定行为。</p>
 */
@Slf4j
@Component
public class ToolRegistry {

    /** 保留 Spring 容器声明顺序，便于目录与 REST 列表稳定 */
    private final List<Jingu3Tool> toolsInOrder;

    /** 只读映射：init 完成后由 {@link Map#copyOf} 冻结 */
    private Map<String, Jingu3Tool> byId;

    public ToolRegistry(List<Jingu3Tool> tools) {
        this.toolsInOrder = tools == null ? List.of() : List.copyOf(tools);
    }

    /**
     * 单测等非 Spring 场景：容器外构造后调用，等价于 Spring 对 {@link PostConstruct} 的调用。
     */
    public static ToolRegistry createForTest(List<Jingu3Tool> tools) {
        ToolRegistry r = new ToolRegistry(tools);
        r.initRegistry();
        return r;
    }

    @PostConstruct
    void initRegistry() {
        Map<String, Jingu3Tool> m = new LinkedHashMap<>();
        for (Jingu3Tool t : toolsInOrder) {
            // putIfAbsent：同 id 第二次出现即视为配置错误，阻止带病启动
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

    /** 供 REST 目录：顺序与启动注册顺序一致 */
    public List<ToolListItemVo> buildCatalogList() {
        List<ToolListItemVo> out = new ArrayList<>(byId.size());
        for (Jingu3Tool t : byId.values()) {
            ToolListItemVo row = new ToolListItemVo();
            row.setId(t.id());
            row.setDescription(t.description());
            row.setRiskLevel(t.riskLevel().name());
            out.add(row);
        }
        return out;
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
