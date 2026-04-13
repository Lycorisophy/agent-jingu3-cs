package cn.lysoy.jingu3.controller;

import cn.lysoy.jingu3.common.api.ApiResult;
import cn.lysoy.jingu3.common.dto.CreateMemoryEntryRequest;
import cn.lysoy.jingu3.common.dto.UpdateMemoryEntryRequest;
import cn.lysoy.jingu3.common.vo.MemoryEntryVo;
import cn.lysoy.jingu3.memory.MemoryService;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * v0.6 M1：记忆条目写入与按用户查询（未接入 {@code ChatService}）。
 */
@RestController
@RequestMapping("/api/v1/memory")
@ConditionalOnProperty(prefix = "jingu3.memory", name = "api-enabled", havingValue = "true", matchIfMissing = true)
public class MemoryController {

    private final MemoryService memoryService;

    public MemoryController(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    @PostMapping("/entries")
    public ApiResult<MemoryEntryVo> create(@Valid @RequestBody CreateMemoryEntryRequest request) {
        return ApiResult.ok(memoryService.create(request));
    }

    @GetMapping("/entries")
    public ApiResult<List<MemoryEntryVo>> list(@RequestParam("userId") String userId) {
        return ApiResult.ok(memoryService.listByUserId(userId));
    }

    @PutMapping("/entries/{id}")
    public ApiResult<MemoryEntryVo> update(
            @PathVariable("id") long id, @Valid @RequestBody UpdateMemoryEntryRequest request) {
        return ApiResult.ok(memoryService.update(id, request));
    }

    @DeleteMapping("/entries/{id}")
    public ApiResult<Void> delete(@PathVariable("id") long id, @RequestParam("userId") String userId) {
        memoryService.delete(id, userId);
        return ApiResult.ok(null);
    }
}
