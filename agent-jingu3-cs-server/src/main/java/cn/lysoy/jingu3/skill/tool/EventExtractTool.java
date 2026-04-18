package cn.lysoy.jingu3.skill.tool;

import cn.lysoy.jingu3.component.UserConstants;
import cn.lysoy.jingu3.events.EventAsyncPersistService;
import cn.lysoy.jingu3.events.EventLlmExtractService;
import cn.lysoy.jingu3.events.EventPersistenceService;
import cn.lysoy.jingu3.events.model.EventExtractLlmResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 从文本抽取事件与有向关系，可选异步写入 MySQL + Milvus。
 */
@Component
@ConditionalOnProperty(prefix = "jingu3.milvus", name = "enabled", havingValue = "true")
@ConditionalOnBean(EventPersistenceService.class)
public class EventExtractTool implements Jingu3Tool {

    private final EventLlmExtractService eventLlmExtractService;
    private final EventPersistenceService eventPersistenceService;
    private final EventAsyncPersistService eventAsyncPersistService;
    private final UserConstants userConstants;
    private final ObjectMapper objectMapper;

    public EventExtractTool(
            EventLlmExtractService eventLlmExtractService,
            EventPersistenceService eventPersistenceService,
            EventAsyncPersistService eventAsyncPersistService,
            UserConstants userConstants,
            ObjectMapper objectMapper) {
        this.eventLlmExtractService = eventLlmExtractService;
        this.eventPersistenceService = eventPersistenceService;
        this.eventAsyncPersistService = eventAsyncPersistService;
        this.userConstants = userConstants;
        this.objectMapper = objectMapper;
    }

    @Override
    public String id() {
        return "event_extract";
    }

    @Override
    public String description() {
        return "从自然语言文本中抽取结构化事件列表与事件间有向关系。input 为 JSON："
                + "{\"text\":\"待分析文本\",\"asyncSave\":false}；asyncSave 为 true 时异步落库并立即返回抽取结果，"
                + "为 false 时同步写入 MySQL 与 Milvus 向量。需启用 Milvus。";
    }

    @Override
    public String execute(String input) throws ToolExecutionException {
        if (input == null || input.isBlank()) {
            throw new ToolExecutionException("event_extract 需要非空 JSON 输入");
        }
        try {
            JsonNode n = objectMapper.readTree(input.trim());
            String text = n.path("text").asText(null);
            if (text == null || text.isBlank()) {
                throw new ToolExecutionException("event_extract 需要 text 字段");
            }
            boolean async = n.path("asyncSave").asBoolean(false);
            EventExtractLlmResult extracted = eventLlmExtractService.extract(text);
            String uid = userConstants.getId();
            if (async) {
                eventAsyncPersistService.persistExtractAsync(extracted, uid);
                Map<String, Object> out = new LinkedHashMap<>();
                out.put("async", true);
                out.put("extracted", extracted);
                return objectMapper.writeValueAsString(out);
            }
            List<Long> ids = eventPersistenceService.persistFromExtract(extracted, uid);
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("async", false);
            out.put("savedEventIds", ids);
            out.put("extracted", extracted);
            return objectMapper.writeValueAsString(out);
        } catch (ToolExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new ToolExecutionException("event_extract 失败: " + e.getMessage(), e);
        }
    }
}
