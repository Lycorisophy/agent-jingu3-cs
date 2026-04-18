package cn.lysoy.jingu3.skill.tool;

import cn.lysoy.jingu3.component.UserConstants;
import cn.lysoy.jingu3.rag.service.MilvusMemoryRetrievalService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 按需检索当前用户的长期记忆（向量相似度），供模型在 Ask/ReAct 等路径中显式调用。
 */
@Component
@ConditionalOnProperty(prefix = "jingu3.milvus", name = "enabled", havingValue = "true")
public class MemorySearchTool implements Jingu3Tool {

    private static final String NO_HIT = "（未检索到与查询相关的记忆条目。）";

    private final MilvusMemoryRetrievalService milvusMemoryRetrievalService;
    private final UserConstants userConstants;

    public MemorySearchTool(
            MilvusMemoryRetrievalService milvusMemoryRetrievalService, UserConstants userConstants) {
        this.milvusMemoryRetrievalService = milvusMemoryRetrievalService;
        this.userConstants = userConstants;
    }

    @Override
    public String id() {
        return "memory_search";
    }

    @Override
    public String description() {
        return "检索当前用户在系统中的长期记忆片段。input 为检索用自然语言查询（可改写或细化，不必与用户原句相同）；"
                + "需已启用 Milvus 且有记忆数据。返回相关条目的摘要与正文预览，供回答时引用。"
                + "若无需查记忆请勿调用。";
    }

    @Override
    public String execute(String input) throws ToolExecutionException {
        if (input == null || input.isBlank()) {
            throw new ToolExecutionException("memory_search 需要非空检索查询");
        }
        String block = milvusMemoryRetrievalService.searchFormattedBlocks(input.trim(), userConstants.getId());
        if (block == null || block.isBlank()) {
            return NO_HIT;
        }
        return block;
    }
}
