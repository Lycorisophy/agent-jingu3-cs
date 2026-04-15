package cn.lysoy.jingu3.events.service;

import cn.lysoy.jingu3.common.constant.EngineMessages;
import cn.lysoy.jingu3.common.dto.CreateEventDocumentRequest;
import cn.lysoy.jingu3.common.enums.ErrorCode;
import cn.lysoy.jingu3.common.exception.ServiceException;
import cn.lysoy.jingu3.common.vo.EventSearchHitVo;
import cn.lysoy.jingu3.config.Jingu3Properties;
import cn.lysoy.jingu3.events.model.EventDocument;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * v0.6-C：事件写入与 multi_match 检索。
 * <p>检索侧在 ES 异常时返回空列表并打 WARN；写入侧在 ES 异常时抛出 {@link ErrorCode#DEPENDENCY_UNAVAILABLE}（503）。</p>
 */
@Slf4j
@Service
@ConditionalOnBean(ElasticsearchClient.class)
public class DefaultEventIndexingService implements EventIndexingService {

    private final ElasticsearchClient client;

    private final Jingu3Properties properties;

    public DefaultEventIndexingService(ElasticsearchClient client, Jingu3Properties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public String index(CreateEventDocumentRequest request) {
        String eventId =
                request.getEventId() == null || request.getEventId().isBlank()
                        ? "evt_" + UUID.randomUUID().toString().replace("-", "")
                        : request.getEventId().trim();
        String ts =
                request.getTimestamp() == null || request.getTimestamp().isBlank()
                        ? Instant.now().toString()
                        : request.getTimestamp().trim();

        EventDocument doc = new EventDocument();
        doc.setEventId(eventId);
        doc.setUserId(request.getUserId().trim());
        doc.setConversationId(request.getConversationId());
        doc.setTimestamp(ts);
        doc.setAction(request.getAction());
        doc.setResult(request.getResult());
        doc.setActors(request.getActors());
        doc.setAssertion(request.getAssertion());
        doc.setEventSubject(request.getEventSubject());
        doc.setEventLocation(request.getEventLocation());
        doc.setTriggerTerms(request.getTriggerTerms());
        doc.setModality(request.getModality());
        doc.setTemporalSemantic(request.getTemporalSemantic());
        doc.setMetadata(request.getMetadata());
        doc.setVectorId(request.getVectorId());
        doc.setMessageId(request.getMessageId());

        String indexName = properties.getElasticsearch().getIndexEvents();
        try {
            client.index(i -> i.index(indexName).id(eventId).document(doc));
            client.indices().refresh(r -> r.index(indexName));
        } catch (Exception ex) {
            log.error("ES 写入事件失败 eventId={}", eventId, ex);
            throw new ServiceException(ErrorCode.DEPENDENCY_UNAVAILABLE, EngineMessages.ES_EVENT_INDEX_UNAVAILABLE, ex);
        }
        return eventId;
    }

    @Override
    public List<EventSearchHitVo> search(String userId, String queryText, int size) {
        if (userId == null || userId.isBlank()) {
            throw new ServiceException(ErrorCode.BAD_REQUEST, "userId 不能为空");
        }
        if (queryText == null || queryText.isBlank()) {
            throw new ServiceException(ErrorCode.BAD_REQUEST, "q 不能为空");
        }
        int lim = Math.min(Math.max(size, 1), 50);
        String indexName = properties.getElasticsearch().getIndexEvents();
        Query query = buildUserScopedQuery(userId.trim(), queryText);
        try {
            SearchResponse<EventDocument> response =
                    client.search(s -> s.index(indexName).size(lim).query(query), EventDocument.class);

            List<EventSearchHitVo> out = new ArrayList<>();
            for (Hit<EventDocument> hit : response.hits().hits()) {
                EventDocument src = hit.source();
                EventSearchHitVo vo = new EventSearchHitVo();
                vo.setScore(hit.score() != null ? hit.score() : 0.0);
                if (src != null) {
                    vo.setEventId(src.getEventId());
                    vo.setUserId(src.getUserId());
                    vo.setAction(src.getAction());
                    vo.setResult(src.getResult());
                    vo.setTimestamp(src.getTimestamp());
                }
                out.add(vo);
            }
            return out;
        } catch (Exception ex) {
            log.warn("ES 检索不可用，降级为空结果 userId={}", userId, ex);
            return Collections.emptyList();
        }
    }

    private static Query buildUserScopedQuery(String userId, String queryText) {
        return Query.of(q -> q.bool(b -> b
                .must(m -> m.multiMatch(mm -> mm
                        .query(queryText)
                        .fields("action^2", "result", "event_subject", "event_location")
                        .fuzziness("AUTO")))
                .filter(f -> f.term(t -> t.field("user_id").value(userId)))))));
    }
}
