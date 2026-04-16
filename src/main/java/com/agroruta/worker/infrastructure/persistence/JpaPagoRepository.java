package com.agroruta.worker.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface JpaPagoRepository extends JpaRepository<PagoEntity, Long> {
    Optional<PagoEntity> findByNominaId(Long nominaId);

    @Query("SELECT p FROM PagoEntity p WHERE p.nomina.trabajador.id = :trabajadorId")
    List<PagoEntity> findByTrabajadorId(@Param("trabajadorId") Long trabajadorId);
}
