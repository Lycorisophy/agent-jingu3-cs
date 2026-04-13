package cn.lysoy.jingu3.memory.repo;

import cn.lysoy.jingu3.memory.entity.FactMetadataEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FactMetadataRepository extends JpaRepository<FactMetadataEntity, Long> {
}
