package com.agroruta.crop.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface JpaCosechaRepository extends JpaRepository<CosechaEntity, Long> {

    List<CosechaEntity> findBySiembraId(Long siembraId);

    @Query("SELECT SUM(c.cantidadKg) FROM CosechaEntity c WHERE c.siembraId = :siembraId")
    Double sumCantidadKgBySiembraId(@Param("siembraId") Long siembraId);

    boolean existsByFechaAndSiembraId(LocalDate fecha, Long siembraId);

    // nuevo — cuenta cosechas por siembra
    @Query("SELECT COUNT(c) FROM CosechaEntity c WHERE c.siembraId = :siembraId")
    int countBySiembraId(@Param("siembraId") Long siembraId);
}