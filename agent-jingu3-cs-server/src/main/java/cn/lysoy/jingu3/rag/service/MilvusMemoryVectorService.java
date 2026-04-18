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
 * <strong>Milvus 向量集合网关</strong>（记忆与知识系统）：维护单集合 MVP（字段：{@link #F_MEMORY_ENTRY_ID}、
 * {@link #F_USER_ID}、{@link #F_EMBEDDING}），负责启动时建表/建索引/Load、插入/删除及按 user 过滤的相似检索。
 * <p>向量维度优先读配置；未配置时用 {@link OllamaEmbeddingClient} 探针向量推断，与 Ollama 嵌入模型输出维一致。</p>
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "jingu3.milvus", name = "enabled", havingValue = "true")
public class MilvusMemoryVectorService {

    /** 与业务表主键对应，便于检索后回表 */
    static final String F_MEMORY_ENTRY_ID = "memory_entry_id";
    /** 多租户隔离标量过滤字段（单用户阶段仍写入固定种子用户） */
    static final String F_USER_ID = "user_id";
    /** 嵌入向量列，维度须与索引及插入数据一致 */
    static final String F_EMBEDDING = "embedding";

    private final Jingu3Properties properties;

    private final OllamaEmbeddingClient ollamaEmbeddingClient;

    /** Milvus SDK 同步客户端；在 {@link #init()} 中创建、{@link jakarta.annotation.PreDestroy} 关闭 */
    private MilvusServiceClient client;

    /** 与 collection 字段维一致；启动期推断或来自配置 */
    private volatile int vectorDimension;

    public MilvusMemoryVectorService(Jingu3Properties properties, OllamaEmbeddingClient ollamaEmbeddingClient) {
        this.properties = properties;
        this.ollamaEmbeddingClient = ollamaEmbeddingClient;
    }

    @PostConstruct
    public void init() {
        // 连接 Milvus → 确定维 → 确保集合存在并已 load，失败时由上层条件装配或日志暴露
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
            log.info("Milvus embedding dimension inferred: {}", vectorDimension);
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
        return properties.getMilvus().getCollectionName();
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
            log.warn("Milvus loadCollection: {} {}", load.getStatus(), load.getMessage());
        }
    }

    private void createCollection(String name) {
        List<FieldType> fields = new ArrayList<>();
        fields.add(FieldType.newBuilder()
                .withName(F_MEMORY_ENTRY_ID)
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
            throw new IllegalStateException("Milvus createCollection: " + r.getMessage());
        }
        log.info("Milvus collection created: {}", name);
    }

    private void createIndex(String name) {
        CreateIndexParam indexParam = CreateIndexParam.newBuilder()
                .withCollectionName(name)
                .withFieldName(F_EMBEDDING)
                .withIndexType(IndexType.FLAT)
                .withMetricType(MetricType.COSINE)
                .build();
        R<?> r = client.createIndex(indexParam);
        if (r.getStatus() != R.Status.Success.getCode()) {
            log.warn("Milvus createIndex: {} {}", r.getStatus(), r.getMessage());
        }
    }

    public void insertVector(long memoryEntryId, String userId, float[] embedding) {
        String name = collectionName();
        List<InsertParam.Field> fieldList = new ArrayList<>();
        fieldList.add(new InsertParam.Field(F_MEMORY_ENTRY_ID, Collections.singletonList(memoryEntryId)));
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
            throw new IllegalStateException("Milvus insert: " + r.getMessage());
        }
        R<?> flush = client.flush(FlushParam.newBuilder().addCollectionName(name).build());
        if (flush.getStatus() != R.Status.Success.getCode()) {
            log.warn("Milvus flush: {}", flush.getMessage());
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
                .build();
        R<?> r = client.search(searchParam);
        if (r.getStatus() != R.Status.Success.getCode() || r.getData() == null) {
            log.warn("Milvus search: {} {}", r.getStatus(), r.getMessage());
            return List.of();
        }
        if (!(r.getData() instanceof SearchResultsWrapper)) {
            log.warn("Milvus search: unexpected result type");
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
            log.warn("Milvus parse search results", ex);
        }
        return ids;
    }

    /**
     * 按主键删除向量行；集合或数据不存在时忽略失败日志。
     */
    public void deleteByMemoryEntryId(long memoryEntryId) {
        String name = collectionName();
        String expr = F_MEMORY_ENTRY_ID + " == " + memoryEntryId;
        DeleteParam deleteParam = DeleteParam.newBuilder().withCollectionName(name).withExpr(expr).build();
        R<?> r = client.delete(deleteParam);
        if (r.getStatus() != R.Status.Success.getCode()) {
            log.warn("Milvus delete entryId={}: {} {}", memoryEntryId, r.getStatus(), r.getMessage());
        }
        R<?> flush = client.flush(FlushParam.newBuilder().addCollectionName(name).build());
        if (flush.getStatus() != R.Status.Success.getCode()) {
            log.warn("Milvus flush after delete: {}", flush.getMessage());
        }
    }
}
