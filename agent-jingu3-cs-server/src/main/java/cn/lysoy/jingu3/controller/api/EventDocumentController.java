package cn.lysoy.jingu3.controller.api;

import cn.lysoy.jingu3.common.api.ApiResult;
import cn.lysoy.jingu3.common.dto.CreateEventDocumentRequest;
import cn.lysoy.jingu3.common.vo.EventSearchHitVo;
import cn.lysoy.jingu3.service.events.EventIndexingService;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * v0.6-C：事件写入 Elasticsearch 与关键词检索（实验 API）。
 */
@RestController
@RequestMapping("/api/v1/events")
@ConditionalOnProperty(prefix = "jingu3.elasticsearch", name = "enabled", havingValue = "true")
public class EventDocumentController {

    private final EventIndexingService eventIndexingService;

    public EventDocumentController(EventIndexingService eventIndexingService) {
        this.eventIndexingService = eventIndexingService;
    }

    @PostMapping
    public ApiResult<Map<String, String>> create(@Valid @RequestBody CreateEventDocumentRequest request) {
        String eventId = eventIndexingService.index(request);
        return ApiResult.ok(Map.of("eventId", eventId));
    }

    @GetMapping("/search")
    public ApiResult<List<EventSearchHitVo>> search(
            @RequestParam("userId") String userId,
            @RequestParam("q") String q,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return ApiResult.ok(eventIndexingService.search(userId, q, size));
    }
}
