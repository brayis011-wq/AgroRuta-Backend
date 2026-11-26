package com.agroruta.worker.infrastructure.persistence;

import com.agroruta.worker.domain.EstadoNomina;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface JpaNominaRepository extends JpaRepository<NominaEntity, Long> {

    /** La nómina en la que quedó un jornal (un jornal está en una sola) */
    @Query("SELECT n FROM NominaEntity n JOIN n.jornales j WHERE j.id = :jornalId")
    List<NominaEntity> conJornal(@Param("jornalId") Long jornalId);

    List<NominaEntity> findByTrabajadorId(Long trabajadorId);

    List<NominaEntity> findByTrabajadorIdAndEstado(Long trabajadorId, EstadoNomina estado);

    @Query("SELECT n FROM NominaEntity n WHERE n.periodoInicio >= :inicio AND n.periodoFin <= :fin")
    List<NominaEntity> findByPeriodo(@Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);

    @Query("SELECT n FROM NominaEntity n WHERE n.trabajador.agricultorId = :agricultorId")
    List<NominaEntity> findByAgricultorId(@Param("agricultorId") Long agricultorId);

    @Query(value = """
        SELECT n FROM NominaEntity n
        WHERE (:agricultorId IS NULL OR n.trabajador.agricultorId = :agricultorId)
          AND (:trabajadorId IS NULL OR n.trabajador.id = :trabajadorId)
          AND (:estado IS NULL OR n.estado = :estado)
        """, countQuery = """
        SELECT COUNT(n) FROM NominaEntity n
        WHERE (:agricultorId IS NULL OR n.trabajador.agricultorId = :agricultorId)
          AND (:trabajadorId IS NULL OR n.trabajador.id = :trabajadorId)
          AND (:estado IS NULL OR n.estado = :estado)
        """)
    Page<NominaEntity> pagina(
            @Param("agricultorId") Long agricultorId,
            @Param("trabajadorId") Long trabajadorId,
            @Param("estado") EstadoNomina estado,
            Pageable pageable);

    @Query("""
        SELECT n.estado, COUNT(n), SUM(n.valorTotal) FROM NominaEntity n
        WHERE (:agricultorId IS NULL OR n.trabajador.agricultorId = :agricultorId)
          AND (:trabajadorId IS NULL OR n.trabajador.id = :trabajadorId)
        GROUP BY n.estado
        """)
    List<Object[]> resumenPorEstado(@Param("agricultorId") Long agricultorId, @Param("trabajadorId") Long trabajadorId);
}
