package com.agroruta.worker.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

interface JpaJornalRepository extends JpaRepository<JornalEntity, Long> {

    List<JornalEntity> findByTrabajadorId(Long trabajadorId);

    List<JornalEntity> findByTrabajadorIdAndFechaBetween(
            Long trabajadorId, LocalDate inicio, LocalDate fin);

    List<JornalEntity> findByCultivoId(Long cultivoId);

    List<JornalEntity> findByCultivoIdAndFechaBetween(
            Long cultivoId, LocalDate inicio, LocalDate fin);

    List<JornalEntity> findByTrabajadorIdAndLiquidadoFalse(Long trabajadorId);

    List<JornalEntity> findByTrabajadorIdAndLiquidadoFalseAndFechaBetween(
            Long trabajadorId, LocalDate inicio, LocalDate fin);

    // ✅ NUEVO: jornales no liquidados y que NO están en ninguna nómina
    @Query("""
        SELECT j FROM JornalEntity j
        WHERE j.trabajador.id = :trabajadorId
        AND j.liquidado = false
        AND j.fecha BETWEEN :inicio AND :fin
        AND j.id NOT IN (
            SELECT jn.id FROM NominaEntity n JOIN n.jornales jn
        )
    """)
    List<JornalEntity> findDisponiblesParaNomina(
            @Param("trabajadorId") Long trabajadorId,
            @Param("inicio") LocalDate inicio,
            @Param("fin") LocalDate fin);
}