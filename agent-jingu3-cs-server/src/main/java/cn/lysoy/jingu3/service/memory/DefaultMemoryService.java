package cn.lysoy.jingu3.service.memory;

import cn.lysoy.jingu3.common.dto.CreateMemoryEntryRequest;
import cn.lysoy.jingu3.common.dto.UpdateMemoryEntryRequest;
import cn.lysoy.jingu3.common.enums.ErrorCode;
import cn.lysoy.jingu3.common.exception.ServiceException;
import cn.lysoy.jingu3.common.util.UtcTime;
import cn.lysoy.jingu3.common.vo.MemoryEntryVo;
import cn.lysoy.jingu3.config.Jingu3Properties;
import cn.lysoy.jingu3.memory.FactTemporalTier;
import cn.lysoy.jingu3.memory.MemoryEntryKind;
import cn.lysoy.jingu3.memory.entity.FactMetadataEntity;
import cn.lysoy.jingu3.memory.entity.MemoryEntryEntity;
import cn.lysoy.jingu3.memory.cache.MemoryEntryListCache;
import cn.lysoy.jingu3.persistence.mapper.memory.FactMetadataMapper;
import cn.lysoy.jingu3.memory.vector.MemoryVectorIndexer;
import cn.lysoy.jingu3.persistence.mapper.memory.MemoryEntryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * {@link MemoryService} 的默认实现：<strong>结构化记忆条目</strong>（MyBatis 持久化）与 <strong>向量索引</strong>
 * （{@link MemoryVectorIndexer}，可接 Milvus）之间的编排；列表结果可经 {@link MemoryEntryListCache} 加速。
 * <p>与对话链路的关系：在线检索注入不经过本类，而走 {@link cn.lysoy.jingu3.service.memory.MilvusMemoryRetrievalService}；
 * 本类主要服务 REST CRUD 与写后索引一致性。</p>
 */
@Service
public class DefaultMemoryService implements MemoryService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_INSTANT;

    /** 记忆主表 CRUD */
    private final MemoryEntryMapper memoryEntryMapper;

    /** FACT 类条目的时序/确认等元数据 */
    private final FactMetadataMapper factMetadataMapper;

    /** 记忆相关功能开关、向量维等 */
    private final Jingu3Properties properties;

    /** 列表缓存（如 Redis），降低热点用户 list 压力 */
    private final MemoryEntryListCache memoryEntryListCache;

    /** 写入/更新/删除后同步向量侧（实现可为 Milvus 或空操作） */
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
        rejectFactOnlyFieldsOnEvent(kind, request.getTemporalTier(), request.getConfirmed());
        var now = UtcTime.nowLocalDateTime();
        MemoryEntryEntity e = new MemoryEntryEntity();
        e.setUserId(request.getUserId().trim());
        e.setKind(kind);
        e.setSummary(request.getSummary());
        e.setBody(request.getBody());
        e.setCreatedAt(now);
        e.setUpdatedAt(now);
        memoryEntryMapper.insert(e);
        if (kind == MemoryEntryKind.FACT) {
            FactMetadataEntity meta = new FactMetadataEntity();
            meta.setMemoryEntryId(e.getId());
            meta.setTag(
                    request.getFactTag() == null || request.getFactTag().isBlank()
                            ? null
                            : request.getFactTag().trim());
            try {
                meta.setTemporalTier(FactTemporalTier.parseRequired(request.getTemporalTier()).name());
            } catch (IllegalArgumentException ex) {
                throw new ServiceException(ErrorCode.BAD_REQUEST, ex.getMessage());
            }
            if (Boolean.TRUE.equals(request.getConfirmed())) {
                meta.setConfirmedAt(now);
            }
            factMetadataMapper.insert(meta);
        }
        memoryEntryListCache.evictListForUser(e.getUserId());
        memoryVectorIndexer.afterMemoryCreated(e);
        return toVoWithOptionalFactTag(e);
    }

    @Override
    @Transactional
    public MemoryEntryVo confirmFact(long id, String userId) {
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
        if (e.getKind() != MemoryEntryKind.FACT) {
            throw new ServiceException(ErrorCode.BAD_REQUEST, "仅 kind=FACT 的条目可确认");
        }
        var now = UtcTime.nowLocalDateTime();
        FactMetadataEntity meta = factMetadataMapper.selectById(id);
        if (meta == null) {
            meta = new FactMetadataEntity();
            meta.setMemoryEntryId(id);
            meta.setTemporalTier(FactTemporalTier.SHORT_TERM.name());
            meta.setConfirmedAt(now);
            factMetadataMapper.insert(meta);
        } else {
            meta.setConfirmedAt(now);
            factMetadataMapper.updateById(meta);
        }
        e.setUpdatedAt(now);
        memoryEntryMapper.updateById(e);
        memoryEntryListCache.evictListForUser(e.getUserId());
        return toVoWithOptionalFactTag(memoryEntryMapper.selectById(id));
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
                && request.getFactTag() == null
                && request.getTemporalTier() == null
                && request.getConfirmed() == null) {
            throw new ServiceException(ErrorCode.BAD_REQUEST, "至少提供 summary、body、kind、factTag、temporalTier、confirmed 之一");
        }
        MemoryEntryEntity e = memoryEntryMapper.selectById(id);
        if (e == null) {
            throw new ServiceException(ErrorCode.NOT_FOUND, "记忆条目不存在");
        }
        String uid = request.getUserId().trim();
        if (!e.getUserId().equals(uid)) {
            throw new ServiceException(ErrorCode.BAD_REQUEST, "userId 与条目不符");
        }
        MemoryEntryKind finalKind = e.getKind();
        if (request.getKind() != null && !request.getKind().isBlank()) {
            finalKind = parseKind(request.getKind());
        }
        rejectFactOnlyFieldsOnEvent(finalKind, request.getTemporalTier(), request.getConfirmed());
        var now = UtcTime.nowLocalDateTime();
        if (request.getSummary() != null) {
            e.setSummary(request.getSummary());
        }
        if (request.getBody() != null) {
            e.setBody(request.getBody());
        }
        if (request.getKind() != null && !request.getKind().isBlank()) {
            e.setKind(finalKind);
        }
        e.setUpdatedAt(now);
        memoryEntryMapper.updateById(e);
        syncFactMetadataOnUpdate(id, request, finalKind, now);
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

    private void syncFactMetadataOnUpdate(
            long id, UpdateMemoryEntryRequest request, MemoryEntryKind finalKind, LocalDateTime now) {
        if (finalKind == MemoryEntryKind.EVENT) {
            factMetadataMapper.deleteById(id);
            return;
        }
        boolean touchTag = request.getFactTag() != null;
        boolean touchTier =
                request.getTemporalTier() != null && !request.getTemporalTier().isBlank();
        boolean touchConfirmed = request.getConfirmed() != null;
        if (!touchTag && !touchTier && !touchConfirmed) {
            return;
        }
        if (touchTag) {
            String trimmed = request.getFactTag().trim();
            if (trimmed.isEmpty()) {
                factMetadataMapper.deleteById(id);
                return;
            }
        }
        FactMetadataEntity existing = factMetadataMapper.selectById(id);
        FactMetadataEntity meta = existing != null ? existing : new FactMetadataEntity();
        meta.setMemoryEntryId(id);
        if (touchTag) {
            meta.setTag(request.getFactTag().trim());
        } else if (existing == null) {
            meta.setTag(null);
        }
        if (touchTier) {
            try {
                meta.setTemporalTier(FactTemporalTier.parseRequired(request.getTemporalTier()).name());
            } catch (IllegalArgumentException ex) {
                throw new ServiceException(ErrorCode.BAD_REQUEST, ex.getMessage());
            }
        } else if (existing == null && meta.getTemporalTier() == null) {
            meta.setTemporalTier(FactTemporalTier.SHORT_TERM.name());
        }
        if (touchConfirmed) {
            if (Boolean.TRUE.equals(request.getConfirmed())) {
                meta.setConfirmedAt(now);
            } else {
                meta.setConfirmedAt(null);
            }
        }
        if (existing == null) {
            if (meta.getTemporalTier() == null) {
                meta.setTemporalTier(FactTemporalTier.SHORT_TERM.name());
            }
            factMetadataMapper.insert(meta);
        } else {
            factMetadataMapper.updateById(meta);
        }
    }

    private static void rejectFactOnlyFieldsOnEvent(
            MemoryEntryKind kind, String temporalTier, Boolean confirmed) {
        if (kind != MemoryEntryKind.EVENT) {
            return;
        }
        if (temporalTier != null && !temporalTier.isBlank()) {
            throw new ServiceException(ErrorCode.BAD_REQUEST, "temporalTier 仅适用于 kind=FACT");
        }
        if (confirmed != null) {
            throw new ServiceException(ErrorCode.BAD_REQUEST, "confirmed 仅适用于 kind=FACT");
        }
    }

    private MemoryEntryVo toVoWithOptionalFactTag(MemoryEntryEntity e) {
        FactMetadataEntity meta = null;
        if (e.getKind() == MemoryEntryKind.FACT) {
            meta = factMetadataMapper.selectById(e.getId());
        }
        return toVoFromMeta(e, meta);
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

    private MemoryEntryVo toVoFromMeta(MemoryEntryEntity e, FactMetadataEntity meta) {
        MemoryEntryVo vo = new MemoryEntryVo();
        vo.setId(e.getId());
        vo.setUserId(e.getUserId());
        vo.setKind(e.getKind().name());
        vo.setSummary(e.getSummary());
        vo.setBody(e.getBody());
        if (meta != null) {
            vo.setFactTag(meta.getTag());
            vo.setFactTemporalTier(meta.getTemporalTier());
            vo.setFactConfirmedAt(
                    meta.getConfirmedAt() == null ? null : ISO.format(UtcTime.toInstant(meta.getConfirmedAt())));
        }
        vo.setCreatedAt(ISO.format(UtcTime.toInstant(e.getCreatedAt())));
        vo.setUpdatedAt(ISO.format(UtcTime.toInstant(e.getUpdatedAt())));
        return vo;
    }
}
