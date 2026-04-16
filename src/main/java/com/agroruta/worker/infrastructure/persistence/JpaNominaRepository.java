package com.agroruta.worker.infrastructure.persistence;

import com.agroruta.worker.domain.EstadoNomina;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

interface JpaNominaRepository extends JpaRepository<NominaEntity, Long> {

    List<NominaEntity> findByTrabajadorId(Long trabajadorId);

    List<NominaEntity> findByTrabajadorIdAndEstado(Long trabajadorId, EstadoNomina estado);

    @Query("SELECT n FROM NominaEntity n WHERE n.periodoInicio >= :inicio AND n.periodoFin <= :fin")
    List<NominaEntity> findByPeriodo(
            @Param("inicio") LocalDate inicio,
            @Param("fin") LocalDate fin);
}