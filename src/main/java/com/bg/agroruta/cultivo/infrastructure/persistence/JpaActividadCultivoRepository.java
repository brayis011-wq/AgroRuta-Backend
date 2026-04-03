package com.bg.agroruta.cultivo.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JpaActividadCultivoRepository extends JpaRepository<ActividadCultivoEntity, Long> {
    List<ActividadCultivoEntity> findBySiembraId(Long siembraId);
}