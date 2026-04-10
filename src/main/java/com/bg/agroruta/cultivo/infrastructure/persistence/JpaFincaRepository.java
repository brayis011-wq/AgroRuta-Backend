package com.bg.agroruta.cultivo.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JpaFincaRepository extends JpaRepository<FincaEntity, Long> {
    List<FincaEntity> findByAgricultorId(Long agricultorId);
}