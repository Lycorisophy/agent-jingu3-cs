package cn.lysoy.jingu3.service.workspace;

import cn.lysoy.jingu3.config.Jingu3Properties;
import cn.lysoy.jingu3.skill.workspace.WorkspaceManager;
import cn.lysoy.jingu3.skill.workspace.entity.WorkspaceEntity;
import cn.lysoy.jingu3.mapper.workspace.WorkspaceMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "jingu3.workspace", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DefaultWorkspaceMetadataService implements WorkspaceMetadataService {

    private static final String STATUS_ACTIVE = "ACTIVE";

    private final WorkspaceMapper workspaceMapper;

    private final WorkspaceManager workspaceManager;

    private final Jingu3Properties properties;

    public DefaultWorkspaceMetadataService(
            WorkspaceMapper workspaceMapper,
            WorkspaceManager workspaceManager,
            Jingu3Properties properties) {
        this.workspaceMapper = workspaceMapper;
        this.workspaceManager = workspaceManager;
        this.properties = properties;
    }

    @Override
    public Optional<WorkspaceEntity> findRow(String userId) {
        return Optional.ofNullable(workspaceMapper.selectByUserId(userId));
    }

    @Override
    public String ensureWorkspaceRow(String userId) throws IOException {
        WorkspaceEntity existing = workspaceMapper.selectByUserId(userId);
        if (existing != null) {
            return existing.getId();
        }
        Path root = workspaceManager.resolveUserRoot(userId);
        WorkspaceEntity row = new WorkspaceEntity();
        row.setId(UUID.randomUUID().toString());
        row.setUserId(userId);
        row.setRootPath(root.toString());
        row.setName(userId);
        row.setQuotaMb(properties.getWorkspace().getDefaultQuotaMb());
        row.setStatus(STATUS_ACTIVE);
        LocalDateTime now = LocalDateTime.now();
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        try {
            workspaceMapper.insert(row);
            return row.getId();
        } catch (DataIntegrityViolationException e) {
            log.debug("workspace row race for userId={}", userId);
            WorkspaceEntity again = workspaceMapper.selectByUserId(userId);
            if (again != null) {
                return again.getId();
            }
            throw new IOException("workspace 元数据并发写入失败", e);
        }
    }

    @Override
    public void deleteMetadataByUserId(String userId) {
        workspaceMapper.delete(Wrappers.lambdaQuery(WorkspaceEntity.class).eq(WorkspaceEntity::getUserId, userId));
    }
}
