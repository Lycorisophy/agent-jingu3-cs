package cn.lysoy.jingu3.memory.repo;

import cn.lysoy.jingu3.memory.entity.MemoryEntryEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemoryEntryRepository extends JpaRepository<MemoryEntryEntity, Long> {

    List<MemoryEntryEntity> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
}
