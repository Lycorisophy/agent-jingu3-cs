package cn.lysoy.jingu3.events;

import cn.lysoy.jingu3.common.enums.EventRelationKind;
import cn.lysoy.jingu3.events.model.EventExtractLlmResult;
import cn.lysoy.jingu3.mapper.event.EventEntryMapper;
import cn.lysoy.jingu3.mapper.event.EventRelationMapper;
import cn.lysoy.jingu3.rag.entity.EventEntryEntity;
import cn.lysoy.jingu3.rag.entity.EventRelationEntity;
import cn.lysoy.jingu3.rag.integration.embedding.OllamaEmbeddingClient;
import cn.lysoy.jingu3.rag.service.MilvusEventVectorService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 事件/关系写 MySQL 并同步 Milvus 向量。
 */
@Slf4j
@Service
@ConditionalOnBean(MilvusEventVectorService.class)
public class EventPersistenceService {

    private final EventEntryMapper eventEntryMapper;
    private final EventRelationMapper eventRelationMapper;
    private final OllamaEmbeddingClient ollamaEmbeddingClient;
    private final MilvusEventVectorService milvusEventVectorService;
    private final ObjectMapper objectMapper;

    public EventPersistenceService(
            EventEntryMapper eventEntryMapper,
            EventRelationMapper eventRelationMapper,
            OllamaEmbeddingClient ollamaEmbeddingClient,
            MilvusEventVectorService milvusEventVectorService,
            ObjectMapper objectMapper) {
        this.eventEntryMapper = eventEntryMapper;
        this.eventRelationMapper = eventRelationMapper;
        this.ollamaEmbeddingClient = ollamaEmbeddingClient;
        this.milvusEventVectorService = milvusEventVectorService;
        this.objectMapper = objectMapper;
    }

    /**
     * 将 LLM 抽取结果落库并量纲化；返回按插入顺序的事件 id。
     */
    @Transactional
    public List<Long> persistFromExtract(EventExtractLlmResult result, String userId) {
        if (result == null || result.getEvents() == null || result.getEvents().isEmpty()) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        for (EventExtractLlmResult.EventDraft d : result.getEvents()) {
            EventEntryEntity e = toEntitySafe(d, userId);
            eventEntryMapper.insert(e);
            Long id = e.getId();
            ids.add(id);
            float[] vec = ollamaEmbeddingClient.embed(EventEmbeddingText.forEmbedding(e));
            milvusEventVectorService.insertVector(id, userId, vec);
        }
        if (result.getRelations() != null) {
            for (EventExtractLlmResult.RelationDraft rd : result.getRelations()) {
                if (rd.getFromIndex() < 0
                        || rd.getToIndex() < 0
                        || rd.getFromIndex() >= ids.size()
                        || rd.getToIndex() >= ids.size()) {
                    log.warn("skip invalid relation indices from={} to={}", rd.getFromIndex(), rd.getToIndex());
                    continue;
                }
                EventRelationKind kind;
                try {
                    kind = EventRelationKind.valueOf(rd.getRelKind().trim());
                } catch (Exception ex) {
                    log.warn("skip invalid relKind {}", rd.getRelKind());
                    continue;
                }
                if (kind == EventRelationKind.OTHER_RELATION
                        && (rd.getExplanation() == null || rd.getExplanation().isBlank())) {
                    log.warn("skip OTHER_RELATION without explanation");
                    continue;
                }
                EventRelationEntity rel = new EventRelationEntity();
                rel.setUserId(userId);
                rel.setEventAId(ids.get(rd.getFromIndex()));
                rel.setEventBId(ids.get(rd.getToIndex()));
                rel.setRelKind(kind.name());
                rel.setExplanation(rd.getExplanation());
                rel.setConfidence(rd.getConfidence());
                rel.setSource("llm");
                eventRelationMapper.insert(rel);
            }
        }
        return ids;
    }

    private EventEntryEntity toEntitySafe(EventExtractLlmResult.EventDraft d, String userId) {
        try {
            return toEntity(d, userId);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private EventEntryEntity toEntity(EventExtractLlmResult.EventDraft d, String userId) throws JsonProcessingException {
        EventEntryEntity e = new EventEntryEntity();
        e.setUserId(userId);
        e.setConversationId(d.getConversationId());
        e.setEventTime(d.getEventTime());
        e.setAction(d.getAction() == null ? "" : d.getAction());
        e.setResult(d.getResult());
        if (d.getActors() != null) {
            e.setActors(objectMapper.writeValueAsString(d.getActors()));
        }
        e.setAssertion(d.getAssertion());
        e.setEventSubject(d.getEventSubject());
        e.setEventLocation(d.getEventLocation());
        if (d.getTriggerTerms() != null) {
            e.setTriggerTerms(objectMapper.writeValueAsString(d.getTriggerTerms()));
        }
        e.setModality(d.getModality());
        e.setTemporalSemantic(d.getTemporalSemantic());
        if (d.getMetadata() != null) {
            e.setMetadata(objectMapper.writeValueAsString(d.getMetadata()));
        }
        e.setMessageId(d.getMessageId());
        return e;
    }
}
