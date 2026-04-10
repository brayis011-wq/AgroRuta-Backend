package com.bg.agroruta.cultivo.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JpaFumigacionRepository extends JpaRepository<FumigacionEntity, Long> {
    List<FumigacionEntity> findBySiembraId(Long siembraId);
}