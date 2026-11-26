package com.agroruta.crop.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaTipoCultivoRepository extends JpaRepository<TipoCultivoEntity, Long> {
    List<TipoCultivoEntity> findAllByOrderByNombreAsc();

    boolean existsByNombreIgnoreCase(String nombre);
}
