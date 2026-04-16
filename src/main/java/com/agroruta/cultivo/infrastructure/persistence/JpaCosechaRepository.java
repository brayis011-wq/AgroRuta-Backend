package com.agroruta.cultivo.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface JpaCosechaRepository extends JpaRepository<CosechaEntity, Long> {
    List<CosechaEntity> findBySiembraId(Long siembraId);

    @Query("SELECT SUM(c.cantidadKg) FROM CosechaEntity c WHERE c.siembraId = :siembraId")
    Double sumCantidadKgBySiembraId(Long siembraId);
}