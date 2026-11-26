package com.agroruta.crop.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface JpaFumigacionRepository extends JpaRepository<FumigacionEntity, Long> {
    List<FumigacionEntity> findBySiembraId(Long siembraId);

    @Transactional
    void deleteBySiembraId(Long siembraId);
}
