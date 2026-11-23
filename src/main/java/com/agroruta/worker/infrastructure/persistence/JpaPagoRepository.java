package com.agroruta.worker.infrastructure.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import com.agroruta.report.domain.WorkerPayrollDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface JpaPagoRepository extends JpaRepository<PagoEntity, Long> {

    Optional<PagoEntity> findByNominaId(Long nominaId);

    @Query("SELECT p FROM PagoEntity p WHERE p.nomina.trabajador.id = :trabajadorId")
    List<PagoEntity> findByTrabajadorId(@Param("trabajadorId") Long trabajadorId);

    @Query("SELECT SUM(p.monto) FROM PagoEntity p")
    BigDecimal sumTotalMonto();

    @Query("""
        SELECT new com.agroruta.report.domain.WorkerPayrollDetail(
            p.nomina.trabajador.id,
            CONCAT(p.nomina.trabajador.nombre, ' ', p.nomina.trabajador.apellido),
            CAST(COUNT(p) AS int),
            SUM(p.monto),
            MIN(p.nomina.periodoInicio),
            MAX(p.nomina.periodoFin),
            CAST(SUM(p.nomina.totalJornales) AS int),
            UPPER(CONCAT(SUBSTRING(p.nomina.trabajador.nombre, 1, 1), SUBSTRING(p.nomina.trabajador.apellido, 1, 1)))
        )
        FROM PagoEntity p
        GROUP BY p.nomina.trabajador.id, p.nomina.trabajador.nombre, p.nomina.trabajador.apellido
        """)
    List<WorkerPayrollDetail> findDetailPerWorker();

    @Query("SELECT SUM(p.monto) FROM PagoEntity p WHERE p.nomina.trabajador.agricultorId = :agricultorId")
    BigDecimal sumMontoByAgricultorId(@Param("agricultorId") Long agricultorId);

    @Query("""
        SELECT new com.agroruta.report.domain.WorkerPayrollDetail(
            p.nomina.trabajador.id,
            CONCAT(p.nomina.trabajador.nombre, ' ', p.nomina.trabajador.apellido),
            CAST(COUNT(p) AS int),
            SUM(p.monto),
            MIN(p.nomina.periodoInicio),
            MAX(p.nomina.periodoFin),
            CAST(SUM(p.nomina.totalJornales) AS int),
            UPPER(CONCAT(SUBSTRING(p.nomina.trabajador.nombre, 1, 1), SUBSTRING(p.nomina.trabajador.apellido, 1, 1)))
        )
        FROM PagoEntity p
        WHERE p.nomina.trabajador.agricultorId = :agricultorId
        GROUP BY p.nomina.trabajador.id, p.nomina.trabajador.nombre, p.nomina.trabajador.apellido
        """)
    List<WorkerPayrollDetail> findDetailPerWorkerByAgricultorId(@Param("agricultorId") Long agricultorId);
    @Query("SELECT p FROM PagoEntity p WHERE p.nomina.trabajador.agricultorId = :agricultorId")
    List<PagoEntity> findByAgricultorId(@Param("agricultorId") Long agricultorId);

    // ── Reporte de nómina por periodo (fechas de pago entre desde y hasta, las dos incluidas) ──
    // agricultorId null = todos (Administrador). Las fechas siempre llegan: el adaptador pone los extremos.

    @Query("""
        SELECT SUM(p.monto) FROM PagoEntity p
        WHERE p.fechaPago BETWEEN :desde AND :hasta
          AND (:agricultorId IS NULL OR p.nomina.trabajador.agricultorId = :agricultorId)
        """)
    BigDecimal sumMontoEnPeriodo(@Param("agricultorId") Long agricultorId,
                                 @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("""
        SELECT new com.agroruta.report.domain.WorkerPayrollDetail(
            p.nomina.trabajador.id,
            CONCAT(p.nomina.trabajador.nombre, ' ', p.nomina.trabajador.apellido),
            CAST(COUNT(p) AS int),
            SUM(p.monto),
            MIN(p.nomina.periodoInicio),
            MAX(p.nomina.periodoFin),
            CAST(SUM(p.nomina.totalJornales) AS int),
            UPPER(CONCAT(SUBSTRING(p.nomina.trabajador.nombre, 1, 1), SUBSTRING(p.nomina.trabajador.apellido, 1, 1)))
        )
        FROM PagoEntity p
        WHERE p.fechaPago BETWEEN :desde AND :hasta
          AND (:agricultorId IS NULL OR p.nomina.trabajador.agricultorId = :agricultorId)
        GROUP BY p.nomina.trabajador.id, p.nomina.trabajador.nombre, p.nomina.trabajador.apellido
        """)
    List<WorkerPayrollDetail> findDetailPerWorkerEnPeriodo(@Param("agricultorId") Long agricultorId,
                                                           @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("""
        SELECT YEAR(p.fechaPago), MONTH(p.fechaPago), SUM(p.monto), COUNT(p) FROM PagoEntity p
        WHERE p.fechaPago BETWEEN :desde AND :hasta
          AND (:agricultorId IS NULL OR p.nomina.trabajador.agricultorId = :agricultorId)
        GROUP BY YEAR(p.fechaPago), MONTH(p.fechaPago)
        """)
    List<Object[]> totalesPorMesEntre(@Param("agricultorId") Long agricultorId,
                                      @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    /**
     * Total y número de pagos de cada mes desde una fecha: [año, mes, total, pagos].
     * agricultorId null = todos (Administrador). Los meses sin pagos no salen.
     */
    @Query("""
        SELECT YEAR(p.fechaPago), MONTH(p.fechaPago), SUM(p.monto), COUNT(p) FROM PagoEntity p
        WHERE p.fechaPago >= :desde
          AND (:agricultorId IS NULL OR p.nomina.trabajador.agricultorId = :agricultorId)
        GROUP BY YEAR(p.fechaPago), MONTH(p.fechaPago)
        """)
    List<Object[]> totalesPorMes(@Param("agricultorId") Long agricultorId, @Param("desde") LocalDate desde);

    @Query(value = "SELECT p FROM PagoEntity p WHERE (:agricultorId IS NULL OR p.nomina.trabajador.agricultorId = :agricultorId) AND (:nominaId IS NULL OR p.nomina.id = :nominaId) AND (:trabajadorId IS NULL OR p.nomina.trabajador.id = :trabajadorId)",
           countQuery = "SELECT COUNT(p) FROM PagoEntity p WHERE (:agricultorId IS NULL OR p.nomina.trabajador.agricultorId = :agricultorId) AND (:nominaId IS NULL OR p.nomina.id = :nominaId) AND (:trabajadorId IS NULL OR p.nomina.trabajador.id = :trabajadorId)")
    Page<PagoEntity> pagina(@Param("agricultorId") Long agricultorId, @Param("nominaId") Long nominaId,
                            @Param("trabajadorId") Long trabajadorId, Pageable pageable);

    @Query("""
        SELECT COUNT(p), SUM(p.monto), COUNT(DISTINCT p.nomina.trabajador.id) FROM PagoEntity p
        WHERE (:agricultorId IS NULL OR p.nomina.trabajador.agricultorId = :agricultorId)
          AND (:nominaId IS NULL OR p.nomina.id = :nominaId)
          AND (:trabajadorId IS NULL OR p.nomina.trabajador.id = :trabajadorId)
        """)
    List<Object[]> resumen(@Param("agricultorId") Long agricultorId, @Param("nominaId") Long nominaId,
                           @Param("trabajadorId") Long trabajadorId);
}
