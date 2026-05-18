package com.agroruta.worker.infrastructure.persistence;

import com.agroruta.report.domain.WorkerPayrollDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
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
            CAST(SUM(p.nomina.totalJornales) AS int)
        )
        FROM PagoEntity p
        GROUP BY p.nomina.trabajador.id, p.nomina.trabajador.nombre, p.nomina.trabajador.apellido
        """)
    List<WorkerPayrollDetail> findDetailPerWorker();
}