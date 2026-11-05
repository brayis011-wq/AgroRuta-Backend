package com.agroruta.crop.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface JpaFumigacionRepository extends JpaRepository<FumigacionEntity, Long> {
    List<FumigacionEntity> findBySiembraId(Long siembraId);

    @Transactional
    void deleteBySiembraId(Long siembraId);
}
