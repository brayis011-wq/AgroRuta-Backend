package com.agroruta.inventory.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaMovimientoInventarioRepository extends JpaRepository<MovimientoInventarioEntity, Long> {
    List<MovimientoInventarioEntity> findByAgricultorIdOrderByFechaDescIdDesc(Long agricultorId);
    List<MovimientoInventarioEntity> findByAgricultorIdAndInsumoIdOrderByFechaDescIdDesc(Long agricultorId, Long insumoId);
    Optional<MovimientoInventarioEntity> findByFumigacionId(Long fumigacionId);
}
