package cn.lysoy.jingu3.memory;

import cn.lysoy.jingu3.common.dto.CreateMemoryEntryRequest;
import cn.lysoy.jingu3.common.enums.ErrorCode;
import cn.lysoy.jingu3.common.exception.ServiceException;
import cn.lysoy.jingu3.common.vo.MemoryEntryVo;
import cn.lysoy.jingu3.config.Jingu3Properties;
import cn.lysoy.jingu3.memory.entity.FactMetadataEntity;
import cn.lysoy.jingu3.memory.entity.MemoryEntryEntity;
import cn.lysoy.jingu3.memory.repo.FactMetadataRepository;
import cn.lysoy.jingu3.memory.repo.MemoryEntryRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class DefaultMemoryService implements MemoryService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_INSTANT;

    private final MemoryEntryRepository memoryEntryRepository;

    private final FactMetadataRepository factMetadataRepository;

    private final Jingu3Properties properties;

    public DefaultMemoryService(
            MemoryEntryRepository memoryEntryRepository,
            FactMetadataRepository factMetadataRepository,
            Jingu3Properties properties) {
        this.memoryEntryRepository = memoryEntryRepository;
        this.factMetadataRepository = factMetadataRepository;
        this.properties = properties;
    }

    @Override
    @Transactional
    public MemoryEntryVo create(CreateMemoryEntryRequest request) {
        MemoryEntryKind kind = parseKind(request.getKind());
        Instant now = Instant.now();
        MemoryEntryEntity e = new MemoryEntryEntity();
        e.setUserId(request.getUserId().trim());
        e.setKind(kind);
        e.setSummary(request.getSummary());
        e.setBody(request.getBody());
        e.setCreatedAt(now);
        e.setUpdatedAt(now);
        MemoryEntryEntity saved = memoryEntryRepository.save(e);
        String factTagOut = null;
        if (kind == MemoryEntryKind.FACT && request.getFactTag() != null && !request.getFactTag().isBlank()) {
            factTagOut = request.getFactTag().trim();
            FactMetadataEntity meta = new FactMetadataEntity();
            meta.setMemoryEntryId(saved.getId());
            meta.setTag(factTagOut);
            factMetadataRepository.save(meta);
        }
        return toVo(saved, factTagOut);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemoryEntryVo> listByUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ServiceException(ErrorCode.BAD_REQUEST, "userId 不能为空");
        }
        int max = Math.max(1, properties.getMemory().getMaxListSize());
        List<MemoryEntryEntity> rows =
                memoryEntryRepository.findByUserIdOrderByCreatedAtDesc(userId.trim(), PageRequest.of(0, max));
        return rows.stream().map(this::toVoWithOptionalFactTag).collect(Collectors.toList());
    }

    private MemoryEntryVo toVoWithOptionalFactTag(MemoryEntryEntity e) {
        String tag = null;
        if (e.getKind() == MemoryEntryKind.FACT) {
            tag = factMetadataRepository.findById(e.getId()).map(FactMetadataEntity::getTag).orElse(null);
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
        vo.setCreatedAt(ISO.format(e.getCreatedAt()));
        vo.setUpdatedAt(ISO.format(e.getUpdatedAt()));
        return vo;
    }
}
