package cn.lysoy.jingu3.events.service;

import cn.lysoy.jingu3.common.dto.CreateEventDocumentRequest;
import cn.lysoy.jingu3.common.vo.EventSearchHitVo;

import java.util.List;

/**
 * 事件全文索引（Elasticsearch）。
 */
public interface EventIndexingService {

    /** 写入或覆盖文档（id 为 event_id） */
    String index(CreateEventDocumentRequest request);

    /** 按用户与关键词检索 */
    List<EventSearchHitVo> search(String userId, String queryText, int size);
}
