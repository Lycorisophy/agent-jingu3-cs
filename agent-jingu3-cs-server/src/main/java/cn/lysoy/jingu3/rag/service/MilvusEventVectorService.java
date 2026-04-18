package cn.lysoy.jingu3.rag.service;

import cn.lysoy.jingu3.config.Jingu3Properties;
import cn.lysoy.jingu3.rag.integration.embedding.OllamaEmbeddingClient;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.param.ConnectParam;
import io.milvus.param.R;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.collection.FlushParam;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.collection.LoadCollectionParam;
import io.milvus.param.dml.DeleteParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.response.SearchResultsWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 事件向量独立集合：字段 {@link #F_EVENT_ID}、{@link #F_USER_ID}、{@link #F_EMBEDDING}；索引 {@link IndexType#HNSW} + {@link MetricType#COSINE}。
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "jingu3.milvus", name = "enabled", havingValue = "true")
public class MilvusEventVectorService {

    static final String F_EVENT_ID = "event_id";
    static final String F_USER_ID = "user_id";
    static final String F_EMBEDDING = "embedding";

    private final Jingu3Properties properties;
    private final OllamaEmbeddingClient ollamaEmbeddingClient;

    private MilvusServiceClient client;
    private volatile int vectorDimension;

    public MilvusEventVectorService(Jingu3Properties properties, OllamaEmbeddingClient ollamaEmbeddingClient) {
        this.properties = properties;
        this.ollamaEmbeddingClient = ollamaEmbeddingClient;
    }

    @PostConstruct
    public void init() {
        Jingu3Properties.Milvus m = properties.getMilvus();
        ConnectParam.Builder cb = ConnectParam.newBuilder().withHost(m.getHost()).withPort(m.getPort());
        if (m.getUser() != null && !m.getUser().isBlank()) {
            cb.withAuthorization(m.getUser(), m.getPassword() == null ? "" : m.getPassword());
        }
        client = new MilvusServiceClient(cb.build());

        vectorDimension = properties.getMemory().getEmbeddingDimension();
        if (vectorDimension <= 0) {
            float[] probe = ollamaEmbeddingClient.embed("ping");
            vectorDimension = probe.length;
            log.info("Milvus event embedding dimension inferred: {}", vectorDimension);
        }
        ensureCollectionLoaded();
    }

    @PreDestroy
    public void close() {
        if (client != null) {
            client.close();
        }
    }

    private String collectionName() {
        return properties.getMilvus().getEventsCollectionName();
    }

    private void ensureCollectionLoaded() {
        String name = collectionName();
        R<Boolean> has = client.hasCollection(HasCollectionParam.newBuilder().withCollectionName(name).build());
        if (has.getData() == null || !has.getData()) {
            createCollection(name);
            createIndex(name);
        }
        R<?> load = client.loadCollection(LoadCollectionParam.newBuilder().withCollectionName(name).build());
        if (load.getStatus() != R.Status.Success.getCode()) {
            log.warn("Milvus event loadCollection: {} {}", load.getStatus(), load.getMessage());
        }
    }

    private void createCollection(String name) {
        List<FieldType> fields = new ArrayList<>();
        fields.add(FieldType.newBuilder()
                .withName(F_EVENT_ID)
                .withDataType(DataType.Int64)
                .withPrimaryKey(true)
                .withAutoID(false)
                .build());
        fields.add(FieldType.newBuilder()
                .withName(F_USER_ID)
                .withDataType(DataType.VarChar)
                .withMaxLength(64)
                .build());
        fields.add(FieldType.newBuilder()
                .withName(F_EMBEDDING)
                .withDataType(DataType.FloatVector)
                .withDimension(vectorDimension)
                .build());
        CreateCollectionParam param = CreateCollectionParam.newBuilder()
                .withCollectionName(name)
                .withFieldTypes(fields)
                .build();
        R<?> r = client.createCollection(param);
        if (r.getStatus() != R.Status.Success.getCode()) {
            throw new IllegalStateException("Milvus event createCollection: " + r.getMessage());
        }
        log.info("Milvus event collection created: {}", name);
    }

    private String hnswIndexExtra() {
        Jingu3Properties.Milvus m = properties.getMilvus();
        return "{\"M\":" + m.getEventsHnswM() + ",\"efConstruction\":" + m.getEventsHnswEfConstruction() + "}";
    }

    private String hnswSearchParams() {
        return "{\"ef\":" + properties.getMilvus().getEventsHnswEf() + "}";
    }

    private void createIndex(String name) {
        CreateIndexParam indexParam = CreateIndexParam.newBuilder()
                .withCollectionName(name)
                .withFieldName(F_EMBEDDING)
                .withIndexType(IndexType.HNSW)
                .withMetricType(MetricType.COSINE)
                .withExtraParam(hnswIndexExtra())
                .build();
        R<?> r = client.createIndex(indexParam);
        if (r.getStatus() != R.Status.Success.getCode()) {
            log.warn("Milvus event createIndex: {} {}", r.getStatus(), r.getMessage());
        }
    }

    public void insertVector(long eventId, String userId, float[] embedding) {
        String name = collectionName();
        List<InsertParam.Field> fieldList = new ArrayList<>();
        fieldList.add(new InsertParam.Field(F_EVENT_ID, Collections.singletonList(eventId)));
        fieldList.add(new InsertParam.Field(F_USER_ID, Collections.singletonList(userId)));
        List<List<Float>> vecRows = new ArrayList<>();
        List<Float> row = new ArrayList<>();
        for (float v : embedding) {
            row.add(v);
        }
        vecRows.add(row);
        fieldList.add(new InsertParam.Field(F_EMBEDDING, vecRows));
        InsertParam insertParam =
                InsertParam.newBuilder().withCollectionName(name).withFields(fieldList).build();
        R<?> r = client.insert(insertParam);
        if (r.getStatus() != R.Status.Success.getCode()) {
            throw new IllegalStateException("Milvus event insert: " + r.getMessage());
        }
        R<?> flush = client.flush(FlushParam.newBuilder().addCollectionName(name).build());
        if (flush.getStatus() != R.Status.Success.getCode()) {
            log.warn("Milvus event flush: {}", flush.getMessage());
        }
    }

    public List<Long> searchSimilar(String userId, float[] queryVector, int topK) {
        String name = collectionName();
        List<List<Float>> searchVectors = new ArrayList<>();
        List<Float> q = new ArrayList<>();
        for (float v : queryVector) {
            q.add(v);
        }
        searchVectors.add(q);
        String safeUser = userId.replace("\\", "\\\\").replace("\"", "\\\"");
        String expr = F_USER_ID + " == \"" + safeUser + "\"";
        SearchParam searchParam = SearchParam.newBuilder()
                .withCollectionName(name)
                .withVectorFieldName(F_EMBEDDING)
                .withMetricType(MetricType.COSINE)
                .withTopK(Math.max(1, topK))
                .withVectors(searchVectors)
                .withExpr(expr)
                .withParams(hnswSearchParams())
                .build();
        R<?> r = client.search(searchParam);
        if (r.getStatus() != R.Status.Success.getCode() || r.getData() == null) {
            log.warn("Milvus event search: {} {}", r.getStatus(), r.getMessage());
            return List.of();
        }
        if (!(r.getData() instanceof SearchResultsWrapper)) {
            log.warn("Milvus event search: unexpected result type");
            return List.of();
        }
        SearchResultsWrapper wrapper = (SearchResultsWrapper) r.getData();
        List<Long> ids = new ArrayList<>();
        try {
            List<?> idScores = wrapper.getIDScore(0);
            for (Object o : idScores) {
                if (o instanceof SearchResultsWrapper.IDScore) {
                    ids.add(((SearchResultsWrapper.IDScore) o).getLongID());
                }
            }
        } catch (Exception ex) {
            log.warn("Milvus event parse search results", ex);
        }
        return ids;
    }

    public void deleteByEventId(long eventId) {
        String name = collectionName();
        String expr = F_EVENT_ID + " == " + eventId;
        DeleteParam deleteParam = DeleteParam.newBuilder().withCollectionName(name).withExpr(expr).build();
        R<?> r = client.delete(deleteParam);
        if (r.getStatus() != R.Status.Success.getCode()) {
            log.warn("Milvus event delete eventId={}: {} {}", eventId, r.getStatus(), r.getMessage());
        }
        R<?> flush = client.flush(FlushParam.newBuilder().addCollectionName(name).build());
        if (flush.getStatus() != R.Status.Success.getCode()) {
            log.warn("Milvus event flush after delete: {}", flush.getMessage());
        }
    }
}
