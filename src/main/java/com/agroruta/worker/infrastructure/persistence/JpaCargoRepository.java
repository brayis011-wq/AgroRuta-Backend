package com.agroruta.worker.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaCargoRepository extends JpaRepository<CargoEntity, Long> {
    List<CargoEntity> findByActivoTrue();

    List<CargoEntity> findByActivoTrueAndAgricultorId(Long agricultorId);

    boolean existsByNombre(String nombre);

    boolean existsByNombreAndIdNot(String nombre, Long id);

    boolean existsByNombreAndAgricultorId(String nombre, Long agricultorId);

    boolean existsByNombreAndAgricultorIdAndIdNot(String nombre, Long agricultorId, Long id);
}
