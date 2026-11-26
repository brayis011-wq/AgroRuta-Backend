package com.agroruta.inventory.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaStockMinimoRepository extends JpaRepository<StockMinimoEntity, Long> {
    List<StockMinimoEntity> findByAgricultorId(Long agricultorId);

    Optional<StockMinimoEntity> findByAgricultorIdAndInsumoId(Long agricultorId, Long insumoId);
}
