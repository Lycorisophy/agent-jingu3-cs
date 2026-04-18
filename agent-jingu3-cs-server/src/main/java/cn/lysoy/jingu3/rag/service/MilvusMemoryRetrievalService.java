package cn.lysoy.jingu3.rag.service;

import cn.lysoy.jingu3.common.constant.PromptFragments;
import cn.lysoy.jingu3.config.Jingu3Properties;
import cn.lysoy.jingu3.rag.entity.MemoryEntryEntity;
import cn.lysoy.jingu3.mapper.memory.MemoryEntryMapper;
import cn.lysoy.jingu3.rag.integration.embedding.OllamaEmbeddingClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <strong>记忆向量检索 + 引用块拼装</strong>（记忆与知识系统）：在 Milvus 启用时，将查询句嵌入向量，
 * 按 user 隔离检索 Top-K {@code memory_entry_id}，回表读取 {@link MemoryEntryEntity} 摘要与正文预览，拼成
 * {@link cn.lysoy.jingu3.common.constant.PromptFragments#MEMORY_REFERENCE_HEADER} 引导的引用块。
 * <p>供内置工具 {@code memory_search} 按需调用；不再在送模前自动注入用户消息。</p>
 * <p>本 Bean 仅在 {@code jingu3.milvus.enabled=true} 时注册。</p>
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "jingu3.milvus", name = "enabled", havingValue = "true")
public class MilvusMemoryRetrievalService {

    /** 单条记忆正文预览上限，防止提示词爆炸 */
    private static final int BODY_PREVIEW_MAX = 500;

    private final Jingu3Properties properties;

    /** 与 Ollama 嵌入模型对齐的查询向量 */
    private final OllamaEmbeddingClient ollamaEmbeddingClient;

    /** Milvus 向量检索与集合生命周期 */
    private final MilvusMemoryVectorService milvusMemoryVectorService;

    /** 向量 id → 业务字段（MySQL 等） */
    private final MemoryEntryMapper memoryEntryMapper;

    public MilvusMemoryRetrievalService(
            Jingu3Properties properties,
            OllamaEmbeddingClient ollamaEmbeddingClient,
            MilvusMemoryVectorService milvusMemoryVectorService,
            MemoryEntryMapper memoryEntryMapper) {
        this.properties = properties;
        this.ollamaEmbeddingClient = ollamaEmbeddingClient;
        this.milvusMemoryVectorService = milvusMemoryVectorService;
        this.memoryEntryMapper = memoryEntryMapper;
    }

    /**
     * 按查询文本做向量检索，返回引用块（含 {@link PromptFragments#MEMORY_REFERENCE_HEADER}），不含用户原句。
     * <p>无命中、查询为空或异常时返回空串（异常会记日志，不冒泡）。</p>
     *
     * @param queryText 检索用自然语言（可与用户当前问题不同）
     * @param userId      用户隔离
     * @return 非空时为多行引用块；否则空串
     */
    public String searchFormattedBlocks(String queryText, String userId) {
        if (queryText == null || queryText.isBlank()) {
            return "";
        }
        String uid = userId == null ? "" : userId.trim();
        try {
            float[] q = ollamaEmbeddingClient.embed(queryText.trim());
            int topK = Math.max(1, properties.getMemory().getRetrievalTopK());
            List<Long> ids = milvusMemoryVectorService.searchSimilar(uid, q, topK);
            if (ids.isEmpty()) {
                return "";
            }
            List<MemoryEntryEntity> rows = memoryEntryMapper.selectByIds(ids);
            if (rows.isEmpty()) {
                return "";
            }
            Map<Long, MemoryEntryEntity> byId = new HashMap<>(rows.size() * 2);
            for (MemoryEntryEntity e : rows) {
                byId.put(e.getId(), e);
            }
            StringBuilder lines = new StringBuilder();
            for (Long id : ids) {
                MemoryEntryEntity e = byId.get(id);
                if (e == null) {
                    continue;
                }
                lines.append("- ")
                        .append(e.getKind() != null ? e.getKind().name() : "?")
                        .append(": ")
                        .append(nullToEmpty(e.getSummary()))
                        .append(" | ")
                        .append(truncate(nullToEmpty(e.getBody()), BODY_PREVIEW_MAX))
                        .append('\n');
            }
            if (lines.isEmpty()) {
                return "";
            }
            return PromptFragments.MEMORY_REFERENCE_HEADER + lines;
        } catch (Exception ex) {
            log.warn("记忆向量检索失败 userId={}", uid, ex);
            return "";
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String truncate(String s, int max) {
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max) + "...";
    }
}
