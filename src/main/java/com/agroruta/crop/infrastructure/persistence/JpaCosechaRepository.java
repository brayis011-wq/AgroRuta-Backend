package com.agroruta.crop.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
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

    /**
     * Kilos y número de cosechas de cada siembra, en una sola consulta: [siembraId, kg, cosechas].
     * agricultorId null = todas las siembras (Administrador). Las siembras sin cosechas no salen.
     */
    @Query("""
        SELECT c.siembraId, SUM(c.cantidadKg), COUNT(c) FROM CosechaEntity c
        WHERE :agricultorId IS NULL OR c.siembraId IN (
            SELECT s.id FROM SiembraEntity s WHERE s.loteId IN (
                SELECT l.id FROM LoteEntity l WHERE l.fincaId IN (
                    SELECT f.id FROM FincaEntity f WHERE f.agricultorId = :agricultorId
                )
            )
        )
        GROUP BY c.siembraId
        """)
    List<Object[]> totalesPorSiembra(@Param("agricultorId") Long agricultorId);

    @Transactional
    void deleteBySiembraId(Long siembraId);
}
