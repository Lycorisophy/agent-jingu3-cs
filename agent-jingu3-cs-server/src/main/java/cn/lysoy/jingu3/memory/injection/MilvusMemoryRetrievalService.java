package cn.lysoy.jingu3.memory.injection;

import cn.lysoy.jingu3.common.constant.PromptFragments;
import cn.lysoy.jingu3.config.Jingu3Properties;
import cn.lysoy.jingu3.memory.entity.MemoryEntryEntity;
import cn.lysoy.jingu3.memory.mapper.MemoryEntryMapper;
import cn.lysoy.jingu3.memory.vector.MilvusMemoryVectorService;
import cn.lysoy.jingu3.memory.vector.OllamaEmbeddingClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <strong>记忆检索 + 拼装</strong>（记忆与知识系统 × 上下文工程）：在 Milvus 启用时，将当前用户句嵌入向量，
 * 按 user 隔离检索 Top-K {@code memory_entry_id}，回表读取 {@link MemoryEntryEntity} 摘要与正文预览，拼成
 * {@link cn.lysoy.jingu3.common.constant.PromptFragments#MEMORY_REFERENCE_HEADER} 引导的引用块并置于用户消息<strong>之前</strong>。
 * <p>本 Bean 仅在 {@code jingu3.milvus.enabled=true} 时注册；外层降级见 {@link MemoryAugmentationService}。</p>
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "jingu3.milvus", name = "enabled", havingValue = "true")
public class MilvusMemoryRetrievalService {

    /** 单条记忆正文注入预览上限，防止提示词爆炸 */
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
     * 将检索到的记忆摘要拼在用户消息前；失败或无结果时返回原消息。
     */
    public String augmentUserMessage(String userMessage, String userId) {
        if (userMessage == null || userMessage.isBlank()) {
            return userMessage;
        }
        String uid = userId == null ? "" : userId.trim();
        try {
            // 1) 查询句嵌入 2) Milvus 相似检索（带 user 过滤）3) 回表取条目 4) 拼引用块
            float[] q = ollamaEmbeddingClient.embed(userMessage);
            int topK = Math.max(1, properties.getMemory().getRetrievalTopK());
            List<Long> ids = milvusMemoryVectorService.searchSimilar(uid, q, topK);
            if (ids.isEmpty()) {
                return userMessage;
            }
            List<MemoryEntryEntity> rows = memoryEntryMapper.selectByIds(ids);
            if (rows.isEmpty()) {
                return userMessage;
            }
            StringBuilder sb = new StringBuilder();
            sb.append(PromptFragments.MEMORY_REFERENCE_HEADER);
            for (MemoryEntryEntity e : rows) {
                sb.append("- ")
                        .append(e.getKind() != null ? e.getKind().name() : "?")
                        .append(": ")
                        .append(nullToEmpty(e.getSummary()))
                        .append(" | ")
                        .append(truncate(nullToEmpty(e.getBody()), BODY_PREVIEW_MAX))
                        .append('\n');
            }
            sb.append(PromptFragments.PARAGRAPH_BREAK).append(userMessage);
            return sb.toString();
        } catch (Exception ex) {
            log.warn("记忆检索注入跳过 userId={}", uid, ex);
            return userMessage;
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
