package com.agroruta.worker.infrastructure.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

interface JpaJornalRepository extends JpaRepository<JornalEntity, Long> {

    /** Jornales de todos los trabajadores del agricultor entre dos fechas (la planilla semanal) */
    @Query("""
        SELECT j FROM JornalEntity j
        WHERE (:agricultorId IS NULL OR j.trabajador.agricultorId = :agricultorId)
          AND j.fecha BETWEEN :inicio AND :fin
        """)
    List<JornalEntity> delEquipoEntre(@Param("agricultorId") Long agricultorId,
                                      @Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);

    /** Jornales del equipo (o de un trabajador), más recientes primero; todo opcional salvo el dueño */
    @Query(value = """
        SELECT j FROM JornalEntity j
        WHERE (:agricultorId IS NULL OR j.trabajador.agricultorId = :agricultorId)
          AND (:trabajadorId IS NULL OR j.trabajador.id = :trabajadorId)
          AND (:inicio IS NULL OR j.fecha >= :inicio)
          AND (:fin IS NULL OR j.fecha <= :fin)
        """, countQuery = """
        SELECT COUNT(j) FROM JornalEntity j
        WHERE (:agricultorId IS NULL OR j.trabajador.agricultorId = :agricultorId)
          AND (:trabajadorId IS NULL OR j.trabajador.id = :trabajadorId)
          AND (:inicio IS NULL OR j.fecha >= :inicio)
          AND (:fin IS NULL OR j.fecha <= :fin)
        """)
    Page<JornalEntity> paginaDelEquipo(@Param("agricultorId") Long agricultorId, @Param("trabajadorId") Long trabajadorId,
                                       @Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin, Pageable pageable);

    @Query("""
        SELECT COUNT(j), SUM(j.valorJornal),
               SUM(CASE WHEN j.liquidado = false THEN 1 ELSE 0 END),
               SUM(CASE WHEN j.liquidado = false THEN j.valorJornal ELSE 0 END),
               MIN(CASE WHEN j.liquidado = false THEN j.fecha END),
               MAX(CASE WHEN j.liquidado = false THEN j.fecha END)
        FROM JornalEntity j
        WHERE (:agricultorId IS NULL OR j.trabajador.agricultorId = :agricultorId)
          AND (:trabajadorId IS NULL OR j.trabajador.id = :trabajadorId)
          AND (:inicio IS NULL OR j.fecha >= :inicio)
          AND (:fin IS NULL OR j.fecha <= :fin)
        """)
    List<Object[]> resumenDelEquipo(@Param("agricultorId") Long agricultorId, @Param("trabajadorId") Long trabajadorId,
                                    @Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);

    /** De esos jornales, los que ya quedaron en alguna nómina */
    /** [id del jornal, estado de su nómina] */
    @Query("SELECT jn.id, n.estado FROM NominaEntity n JOIN n.jornales jn WHERE jn.id IN :ids")
    List<Object[]> estadosDeNomina(@Param("ids") java.util.Collection<Long> ids);

    @Query("SELECT jn.id FROM NominaEntity n JOIN n.jornales jn WHERE jn.id IN :ids")
    List<Long> idsEnNomina(@Param("ids") java.util.Collection<Long> ids);

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

    @Query(value = """
        SELECT j FROM JornalEntity j
        WHERE j.trabajador.id = :trabajadorId
          AND (:inicio IS NULL OR j.fecha >= :inicio)
          AND (:fin IS NULL OR j.fecha <= :fin)
        """, countQuery = """
        SELECT COUNT(j) FROM JornalEntity j
        WHERE j.trabajador.id = :trabajadorId
          AND (:inicio IS NULL OR j.fecha >= :inicio)
          AND (:fin IS NULL OR j.fecha <= :fin)
        """)
    Page<JornalEntity> paginaPorTrabajador(@Param("trabajadorId") Long trabajadorId, @Param("inicio") LocalDate inicio,
                                           @Param("fin") LocalDate fin, Pageable pageable);

    @Query("""
        SELECT COUNT(j), SUM(j.valorJornal),
               SUM(CASE WHEN j.liquidado = false THEN 1 ELSE 0 END),
               SUM(CASE WHEN j.liquidado = false THEN j.valorJornal ELSE 0 END),
               MIN(CASE WHEN j.liquidado = false THEN j.fecha END),
               MAX(CASE WHEN j.liquidado = false THEN j.fecha END)
        FROM JornalEntity j
        WHERE j.trabajador.id = :trabajadorId
          AND (:inicio IS NULL OR j.fecha >= :inicio)
          AND (:fin IS NULL OR j.fecha <= :fin)
        """)
    List<Object[]> resumenPorTrabajador(@Param("trabajadorId") Long trabajadorId, @Param("inicio") LocalDate inicio,
                                        @Param("fin") LocalDate fin);

    /** [jornales desde inicioMes, jornales anteriores a inicioMes] entre desde y hoy, de trabajadores en ese estado */
    @Query("""
        SELECT SUM(CASE WHEN j.fecha >= :inicioMes THEN 1 ELSE 0 END),
               SUM(CASE WHEN j.fecha < :inicioMes THEN 1 ELSE 0 END)
        FROM JornalEntity j
        WHERE (:agricultorId IS NULL OR j.trabajador.agricultorId = :agricultorId)
          AND j.trabajador.estado = :estado
          AND j.fecha >= :desde AND j.fecha <= :hoy
        """)
    List<Object[]> conteoPorMes(@Param("agricultorId") Long agricultorId,
                                @Param("estado") com.agroruta.worker.domain.EstadoTrabajador estado,
                                @Param("desde") LocalDate desde, @Param("inicioMes") LocalDate inicioMes,
                                @Param("hoy") LocalDate hoy);

    @Query("""
        SELECT DISTINCT j.trabajador.id FROM JornalEntity j
        WHERE (:agricultorId IS NULL OR j.trabajador.agricultorId = :agricultorId)
          AND j.trabajador.estado = :estado
          AND j.fecha = :hoy
        """)
    List<Long> trabajadoresConJornal(@Param("agricultorId") Long agricultorId,
                                     @Param("estado") com.agroruta.worker.domain.EstadoTrabajador estado,
                                     @Param("hoy") LocalDate hoy);
}
