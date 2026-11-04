package com.agroruta.crop.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaVariedadCultivoRepository extends JpaRepository<VariedadCultivoEntity, Long> {
    boolean existsByTipoCultivoIdAndNombreIgnoreCase(Long tipoCultivoId, String nombre);
}
