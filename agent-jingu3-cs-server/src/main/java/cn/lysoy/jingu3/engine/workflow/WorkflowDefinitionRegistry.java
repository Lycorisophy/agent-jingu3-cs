package cn.lysoy.jingu3.engine.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 指南 §6 工作流「定义存储」的 MVP：启动时扫描 {@code classpath*:workflows/*.json}，按 {@link WorkflowDefinition#getId()}
 * 建立内存索引，供 {@link cn.lysoy.jingu3.engine.mode.WorkflowModeHandler} 按 {@code workflowId} 解析节点链。
 * 后续可替换为 DB / 配置中心而不改 handler 契约。
 */
@Slf4j
@Component
public class WorkflowDefinitionRegistry {

    private static final String LOCATION = "classpath*:workflows/*.json";

    /** 工作流 id → 反序列化后的定义（启动期装载，运行期只读） */
    private final Map<String, WorkflowDefinition> byId = new ConcurrentHashMap<>();

    /**
     * 构造时扫描 {@link #LOCATION}，将合法 JSON 定义放入内存；单条资源解析失败时跳过并打 warn，不影响其它文件。
     */
    public WorkflowDefinitionRegistry(ResourcePatternResolver resourcePatternResolver) {
        Resource[] resources;
        try {
            resources = resourcePatternResolver.getResources(LOCATION);
        } catch (IOException e) {
            log.error("failed to resolve workflow resources under {}", LOCATION, e);
            return;
        }
        ObjectMapper mapper = new ObjectMapper();
        for (Resource res : resources) {
            if (!res.exists()) {
                continue;
            }
            try (InputStream in = res.getInputStream()) {
                WorkflowDefinition def = mapper.readValue(in, WorkflowDefinition.class);
                if (def.getId() != null && def.getNodes() != null && !def.getNodes().isEmpty()) {
                    byId.put(def.getId(), def);
                    log.info("loaded workflow definition id={} nodes={}", def.getId(), def.getNodes().size());
                }
            } catch (IOException e) {
                log.warn("skip workflow resource {}: {}", res.getFilename(), e.getMessage());
            }
        }
        if (byId.isEmpty()) {
            log.warn("no workflow definitions under {}", LOCATION);
        }
    }

    public WorkflowDefinition get(String workflowId) {
        if (workflowId == null || workflowId.isBlank()) {
            return null;
        }
        return byId.get(workflowId.trim());
    }

    public boolean hasDefinition(String workflowId) {
        return get(workflowId) != null;
    }

    /** 测试或诊断：已加载的 id 列表 */
    public String[] loadedIds() {
        return byId.keySet().toArray(new String[0]);
    }
}
