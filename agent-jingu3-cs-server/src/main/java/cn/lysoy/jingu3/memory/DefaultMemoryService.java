package cn.lysoy.jingu3.memory;

import cn.lysoy.jingu3.common.dto.CreateMemoryEntryRequest;
import cn.lysoy.jingu3.common.dto.UpdateMemoryEntryRequest;
import cn.lysoy.jingu3.common.enums.ErrorCode;
import cn.lysoy.jingu3.common.exception.ServiceException;
import cn.lysoy.jingu3.common.util.UtcTime;
import cn.lysoy.jingu3.common.vo.MemoryEntryVo;
import cn.lysoy.jingu3.config.Jingu3Properties;
import cn.lysoy.jingu3.memory.entity.FactMetadataEntity;
import cn.lysoy.jingu3.memory.entity.MemoryEntryEntity;
import cn.lysoy.jingu3.memory.cache.MemoryEntryListCache;
import cn.lysoy.jingu3.memory.mapper.FactMetadataMapper;
import cn.lysoy.jingu3.memory.vector.MemoryVectorIndexer;
import cn.lysoy.jingu3.memory.mapper.MemoryEntryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class DefaultMemoryService implements MemoryService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_INSTANT;

    private final MemoryEntryMapper memoryEntryMapper;

    private final FactMetadataMapper factMetadataMapper;

    private final Jingu3Properties properties;

    private final MemoryEntryListCache memoryEntryListCache;

    private final MemoryVectorIndexer memoryVectorIndexer;

    public DefaultMemoryService(
            MemoryEntryMapper memoryEntryMapper,
            FactMetadataMapper factMetadataMapper,
            Jingu3Properties properties,
            MemoryEntryListCache memoryEntryListCache,
            MemoryVectorIndexer memoryVectorIndexer) {
        this.memoryEntryMapper = memoryEntryMapper;
        this.factMetadataMapper = factMetadataMapper;
        this.properties = properties;
        this.memoryEntryListCache = memoryEntryListCache;
        this.memoryVectorIndexer = memoryVectorIndexer;
    }

    @Override
    @Transactional
    public MemoryEntryVo create(CreateMemoryEntryRequest request) {
        MemoryEntryKind kind = parseKind(request.getKind());
        var now = UtcTime.nowLocalDateTime();
        MemoryEntryEntity e = new MemoryEntryEntity();
        e.setUserId(request.getUserId().trim());
        e.setKind(kind);
        e.setSummary(request.getSummary());
        e.setBody(request.getBody());
        e.setCreatedAt(now);
        e.setUpdatedAt(now);
        memoryEntryMapper.insert(e);
        String factTagOut = null;
        if (kind == MemoryEntryKind.FACT && request.getFactTag() != null && !request.getFactTag().isBlank()) {
            factTagOut = request.getFactTag().trim();
            FactMetadataEntity meta = new FactMetadataEntity();
            meta.setMemoryEntryId(e.getId());
            meta.setTag(factTagOut);
            factMetadataMapper.insert(meta);
        }
        memoryEntryListCache.evictListForUser(e.getUserId());
        memoryVectorIndexer.afterMemoryCreated(e);
        return toVo(e, factTagOut);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemoryEntryVo> listByUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ServiceException(ErrorCode.BAD_REQUEST, "userId 不能为空");
        }
        int max = Math.max(1, properties.getMemory().getMaxListSize());
        String uid = userId.trim();
        var cached = memoryEntryListCache.get(uid, max);
        if (cached.isPresent()) {
            return cached.get();
        }
        List<MemoryEntryEntity> rows = memoryEntryMapper.selectByUserIdOrderByCreatedAtDesc(uid, max);
        List<MemoryEntryVo> out = rows.stream().map(this::toVoWithOptionalFactTag).collect(Collectors.toList());
        memoryEntryListCache.put(uid, max, out);
        return out;
    }

    @Override
    @Transactional
    public MemoryEntryVo update(long id, UpdateMemoryEntryRequest request) {
        if (request.getSummary() == null
                && request.getBody() == null
                && request.getKind() == null
                && request.getFactTag() == null) {
            throw new ServiceException(ErrorCode.BAD_REQUEST, "至少提供 summary、body、kind、factTag 之一");
        }
        MemoryEntryEntity e = memoryEntryMapper.selectById(id);
        if (e == null) {
            throw new ServiceException(ErrorCode.NOT_FOUND, "记忆条目不存在");
        }
        String uid = request.getUserId().trim();
        if (!e.getUserId().equals(uid)) {
            throw new ServiceException(ErrorCode.BAD_REQUEST, "userId 与条目不符");
        }
        var now = UtcTime.nowLocalDateTime();
        if (request.getSummary() != null) {
            e.setSummary(request.getSummary());
        }
        if (request.getBody() != null) {
            e.setBody(request.getBody());
        }
        MemoryEntryKind finalKind = e.getKind();
        if (request.getKind() != null && !request.getKind().isBlank()) {
            finalKind = parseKind(request.getKind());
            e.setKind(finalKind);
        }
        e.setUpdatedAt(now);
        memoryEntryMapper.updateById(e);
        syncFactMetadataOnUpdate(id, request, finalKind);
        memoryEntryListCache.evictListForUser(uid);
        memoryVectorIndexer.afterMemoryUpdated(e);
        return toVoWithOptionalFactTag(e);
    }

    @Override
    @Transactional
    public void delete(long id, String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ServiceException(ErrorCode.BAD_REQUEST, "userId 不能为空");
        }
        MemoryEntryEntity e = memoryEntryMapper.selectById(id);
        if (e == null) {
            throw new ServiceException(ErrorCode.NOT_FOUND, "记忆条目不存在");
        }
        if (!e.getUserId().equals(userId.trim())) {
            throw new ServiceException(ErrorCode.BAD_REQUEST, "userId 与条目不符");
        }
        memoryVectorIndexer.onMemoryDeleted(id);
        memoryEntryMapper.deleteById(id);
        memoryEntryListCache.evictListForUser(e.getUserId());
    }

    private void syncFactMetadataOnUpdate(long id, UpdateMemoryEntryRequest request, MemoryEntryKind finalKind) {
        if (finalKind == MemoryEntryKind.EVENT) {
            factMetadataMapper.deleteById(id);
            return;
        }
        if (request.getFactTag() == null) {
            return;
        }
        String trimmed = request.getFactTag().trim();
        if (trimmed.isEmpty()) {
            factMetadataMapper.deleteById(id);
            return;
        }
        FactMetadataEntity existing = factMetadataMapper.selectById(id);
        if (existing == null) {
            FactMetadataEntity meta = new FactMetadataEntity();
            meta.setMemoryEntryId(id);
            meta.setTag(trimmed);
            factMetadataMapper.insert(meta);
        } else {
            existing.setTag(trimmed);
            factMetadataMapper.updateById(existing);
        }
    }

    private MemoryEntryVo toVoWithOptionalFactTag(MemoryEntryEntity e) {
        String tag = null;
        if (e.getKind() == MemoryEntryKind.FACT) {
            FactMetadataEntity meta = factMetadataMapper.selectById(e.getId());
            tag = meta != null ? meta.getTag() : null;
        }
        return toVo(e, tag);
    }

    private static MemoryEntryKind parseKind(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ServiceException(ErrorCode.BAD_REQUEST, "kind 不能为空");
        }
        try {
            return MemoryEntryKind.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ServiceException(ErrorCode.BAD_REQUEST, "kind 须为 FACT 或 EVENT");
        }
    }

    private MemoryEntryVo toVo(MemoryEntryEntity e, String factTag) {
        MemoryEntryVo vo = new MemoryEntryVo();
        vo.setId(e.getId());
        vo.setUserId(e.getUserId());
        vo.setKind(e.getKind().name());
        vo.setSummary(e.getSummary());
        vo.setBody(e.getBody());
        vo.setFactTag(factTag);
        vo.setCreatedAt(ISO.format(UtcTime.toInstant(e.getCreatedAt())));
        vo.setUpdatedAt(ISO.format(UtcTime.toInstant(e.getUpdatedAt())));
        return vo;
    }
}
