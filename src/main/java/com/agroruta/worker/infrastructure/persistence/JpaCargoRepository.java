package com.agroruta.worker.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface JpaCargoRepository extends JpaRepository<CargoEntity, Long> {
    List<CargoEntity> findByActivoTrue();
    List<CargoEntity> findByActivoTrueAndAgricultorId(Long agricultorId);
    boolean existsByNombre(String nombre);
    boolean existsByNombreAndIdNot(String nombre, Long id);
    boolean existsByNombreAndAgricultorId(String nombre, Long agricultorId);
    boolean existsByNombreAndAgricultorIdAndIdNot(String nombre, Long agricultorId, Long id);
}