package com.agroruta.cultivo.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JpaLoteRepository extends JpaRepository<LoteEntity, Long> {
    List<LoteEntity> findByFincaId(Long fincaId);
}