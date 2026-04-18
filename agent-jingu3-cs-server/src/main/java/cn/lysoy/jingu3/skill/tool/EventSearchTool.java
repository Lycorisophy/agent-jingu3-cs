package cn.lysoy.jingu3.skill.tool;

import cn.lysoy.jingu3.component.UserConstants;
import cn.lysoy.jingu3.events.EventSearchService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 事件语义检索（Milvus）+ 关键词过滤 + MySQL 详情与关系扩展。
 */
@Component
@ConditionalOnProperty(prefix = "jingu3.milvus", name = "enabled", havingValue = "true")
@ConditionalOnBean(EventSearchService.class)
public class EventSearchTool implements Jingu3Tool {

    private final EventSearchService eventSearchService;
    private final UserConstants userConstants;
    private final ObjectMapper objectMapper;

    public EventSearchTool(
            EventSearchService eventSearchService, UserConstants userConstants, ObjectMapper objectMapper) {
        this.eventSearchService = eventSearchService;
        this.userConstants = userConstants;
        this.objectMapper = objectMapper;
    }

    @Override
    public String id() {
        return "event_search";
    }

    @Override
    public String description() {
        return "检索用户已存储的事件。input 为 JSON："
                + "{\"semanticQuery\":\"语义检索句\",\"keywords\":\"可选关键词（空格分词 AND）\","
                + "\"topK\":10,\"expandRelated\":true}；先向量召回事件 id，再在 MySQL 取详情；"
                + "keywords 用于对向量结果做词法过滤（无命中时退回纯向量序）。expandRelated 为 true 时加载 1-hop 关联事件。";
    }

    @Override
    public String execute(String input) throws ToolExecutionException {
        if (input == null || input.isBlank()) {
            throw new ToolExecutionException("event_search 需要非空 JSON 输入");
        }
        try {
            JsonNode n = objectMapper.readTree(input.trim());
            String semantic = n.path("semanticQuery").asText(null);
            if (semantic == null || semantic.isBlank()) {
                semantic = n.path("searchCondition").asText(null);
            }
            if (semantic == null || semantic.isBlank()) {
                throw new ToolExecutionException("event_search 需要 semanticQuery 或 searchCondition");
            }
            String keywords = n.path("keywords").asText(null);
            Integer topK = n.has("topK") ? n.get("topK").asInt() : null;
            Boolean expand =
                    n.has("expandRelated") && !n.get("expandRelated").isNull()
                            ? n.get("expandRelated").asBoolean()
                            : null;
            return eventSearchService.searchJson(
                    userConstants.getId(), semantic, keywords, topK, expand);
        } catch (ToolExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new ToolExecutionException("event_search 失败: " + e.getMessage(), e);
        }
    }
}
