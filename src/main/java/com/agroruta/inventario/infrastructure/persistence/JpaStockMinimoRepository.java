package com.agroruta.inventario.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaStockMinimoRepository extends JpaRepository<StockMinimoEntity, Long> {
    List<StockMinimoEntity> findByAgricultorId(Long agricultorId);
    Optional<StockMinimoEntity> findByAgricultorIdAndInsumoId(Long agricultorId, Long insumoId);
}
