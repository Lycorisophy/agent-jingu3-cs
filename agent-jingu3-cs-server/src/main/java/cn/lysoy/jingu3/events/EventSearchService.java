package cn.lysoy.jingu3.events;

import cn.lysoy.jingu3.config.Jingu3Properties;
import cn.lysoy.jingu3.mapper.event.EventEntryMapper;
import cn.lysoy.jingu3.mapper.event.EventRelationMapper;
import cn.lysoy.jingu3.rag.entity.EventEntryEntity;
import cn.lysoy.jingu3.rag.entity.EventRelationEntity;
import cn.lysoy.jingu3.rag.integration.embedding.OllamaEmbeddingClient;
import cn.lysoy.jingu3.rag.service.MilvusEventVectorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 向量召回 + MySQL 详情 + 可选关键词过滤 + 1-hop 关系扩展。
 */
@Service
@ConditionalOnBean(MilvusEventVectorService.class)
public class EventSearchService {

    private final OllamaEmbeddingClient ollamaEmbeddingClient;
    private final MilvusEventVectorService milvusEventVectorService;
    private final EventEntryMapper eventEntryMapper;
    private final EventRelationMapper eventRelationMapper;
    private final Jingu3Properties properties;
    private final ObjectMapper objectMapper;

    public EventSearchService(
            OllamaEmbeddingClient ollamaEmbeddingClient,
            MilvusEventVectorService milvusEventVectorService,
            EventEntryMapper eventEntryMapper,
            EventRelationMapper eventRelationMapper,
            Jingu3Properties properties,
            ObjectMapper objectMapper) {
        this.ollamaEmbeddingClient = ollamaEmbeddingClient;
        this.milvusEventVectorService = milvusEventVectorService;
        this.eventEntryMapper = eventEntryMapper;
        this.eventRelationMapper = eventRelationMapper;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public String searchJson(
            String userId,
            String semanticQuery,
            String keywords,
            Integer topK,
            Boolean expandRelated)
            throws Exception {
        if (semanticQuery == null || semanticQuery.isBlank()) {
            return objectMapper.writeValueAsString(Map.of("error", "semanticQuery 不能为空"));
        }
        int lim = topK == null ? properties.getEvents().getSearchTopK() : topK;
        lim = Math.min(Math.max(lim, 1), 50);
        float[] q = ollamaEmbeddingClient.embed(semanticQuery.trim());
        List<Long> milvusIds = milvusEventVectorService.searchSimilar(userId, q, lim);
        if (milvusIds.isEmpty()) {
            return objectMapper.writeValueAsString(
                    Map.of("primary", List.of(), "related", List.of(), "relations", List.of()));
        }
        List<EventEntryEntity> rows = eventEntryMapper.selectByIdsForUser(userId, milvusIds);
        Map<Long, EventEntryEntity> byId = new HashMap<>();
        for (EventEntryEntity e : rows) {
            byId.put(e.getId(), e);
        }
        List<EventEntryEntity> ordered = new ArrayList<>();
        for (Long id : milvusIds) {
            EventEntryEntity e = byId.get(id);
            if (e != null) {
                ordered.add(e);
            }
        }
        List<EventEntryEntity> primary = ordered;
        if (keywords != null && !keywords.isBlank()) {
            List<EventEntryEntity> filtered = ordered.stream()
                    .filter(e -> keywordMatches(e, keywords))
                    .collect(Collectors.toList());
            if (!filtered.isEmpty()) {
                primary = filtered;
            }
        }
        List<Map<String, Object>> primaryJson = primary.stream().map(this::toBriefMap).collect(Collectors.toList());
        List<Map<String, Object>> relJson = List.of();
        List<Map<String, Object>> relEdgeJson = List.of();
        boolean expand = expandRelated == null || expandRelated;
        if (expand && !primary.isEmpty()) {
            List<Long> hitIds = primary.stream().map(EventEntryEntity::getId).collect(Collectors.toList());
            List<EventRelationEntity> edges = eventRelationMapper.findTouching(userId, hitIds);
            LinkedHashSet<Long> neighbor = new LinkedHashSet<>();
            for (EventRelationEntity edge : edges) {
                if (!hitIds.contains(edge.getEventAId())) {
                    neighbor.add(edge.getEventAId());
                }
                if (!hitIds.contains(edge.getEventBId())) {
                    neighbor.add(edge.getEventBId());
                }
            }
            int maxExtra = properties.getEvents().getRelatedExpandMax();
            List<Long> extraIds = neighbor.stream().limit(maxExtra).collect(Collectors.toList());
            List<EventEntryEntity> relatedRows = extraIds.isEmpty()
                    ? List.of()
                    : eventEntryMapper.selectByIdsForUser(userId, extraIds);
            Map<Long, EventEntryEntity> relById = new LinkedHashMap<>();
            for (Long id : extraIds) {
                for (EventEntryEntity e : relatedRows) {
                    if (e.getId().equals(id)) {
                        relById.put(id, e);
                        break;
                    }
                }
            }
            relJson = relById.values().stream().map(this::toBriefMap).collect(Collectors.toList());
            relEdgeJson = edges.stream().map(this::edgeMap).collect(Collectors.toList());
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("primary", primaryJson);
        out.put("related", relJson);
        out.put("relations", relEdgeJson);
        return objectMapper.writeValueAsString(out);
    }

    private Map<String, Object> edgeMap(EventRelationEntity e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("eventAId", e.getEventAId());
        m.put("eventBId", e.getEventBId());
        m.put("relKind", e.getRelKind());
        m.put("explanation", e.getExplanation());
        return m;
    }

    private Map<String, Object> toBriefMap(EventEntryEntity e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("eventTime", e.getEventTime());
        m.put("action", e.getAction());
        m.put("result", e.getResult());
        m.put("eventSubject", e.getEventSubject());
        m.put("eventLocation", e.getEventLocation());
        return m;
    }

    static boolean keywordMatches(EventEntryEntity e, String keywords) {
        String blob = EventEmbeddingText.searchBlob(e).toLowerCase(Locale.ROOT);
        String k = keywords.trim().toLowerCase(Locale.ROOT);
        if (k.isEmpty()) {
            return true;
        }
        if (k.indexOf(' ') >= 0 || k.indexOf('\t') >= 0) {
            for (String part : k.split("\\s+")) {
                if (!part.isEmpty() && !blob.contains(part)) {
                    return false;
                }
            }
            return true;
        }
        return blob.contains(k);
    }
}
