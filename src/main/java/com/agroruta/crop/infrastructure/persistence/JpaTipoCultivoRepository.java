package com.agroruta.crop.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaTipoCultivoRepository extends JpaRepository<TipoCultivoEntity, Long> {
    List<TipoCultivoEntity> findAllByOrderByNombreAsc();
    boolean existsByNombreIgnoreCase(String nombre);
}
